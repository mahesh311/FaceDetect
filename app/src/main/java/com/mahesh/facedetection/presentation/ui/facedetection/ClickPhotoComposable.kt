package com.mahesh.facedetection.presentation.ui.facedetection

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.mahesh.facedetection.R
import com.mahesh.facedetection.presentation.ui.permission.PermissionCheckComposable
import com.mahesh.facedetection.presentation.utils.ImageUtils
import java.io.File

// Main Composable that checks for camera permission and either shows the camera or permission request
@OptIn(ExperimentalGetImage::class)
@Composable
fun ClickPhotoComposable(
    onDismiss: () -> Unit,         // Called when permission is denied or user exits
    onPhotoCaptured: (imagePath : String) -> Unit    // Called after successful capture and crop
) {
    val isCameraPermissionGranted = remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isCameraPermissionGranted.value) {
            // If permission is granted, show the camera capture screen
            CameraCaptureUI(
                onDismiss = onDismiss,
                onPhotoCaptured = onPhotoCaptured
            )
        } else {
            // If permission is not granted, show permission request
            PermissionCheckComposable(
                onPermissionGranted = { granted ->
                    Log.d("ClickPhotoComposable", "Camera permission granted: $granted")
                    isCameraPermissionGranted.value = granted
                    if (!granted) onDismiss()
                }
            )
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraCaptureUI(
    onDismiss: () -> Unit,
    onPhotoCaptured: (imagePath : String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val imageCaptureInstance =
        remember { ImageCapture.Builder().build() }  // Image capture use case
    val previewViewHolder =
        remember { mutableStateOf<PreviewView?>(null) } // Holder for camera preview view
    val detectedFaceBounds =
        remember { mutableStateOf<Rect?>(null) }       // Coordinates of detected face

    val isFaceValid = remember { mutableStateOf(false) }          // Flag to indicate a valid face
    val isCapturingPhoto = remember { mutableStateOf(false) }     // Flag to show capture in progress

    // Side-effect to initialize and bind camera + analyzer
    DisposableEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()

        // Create PreviewView (invisible initially to prevent flicker)
        val previewView = PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(1, 1)
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            visibility = View.INVISIBLE
        }

        previewViewHolder.value = previewView

        // Camera preview setup
        val cameraPreview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        // Set up image analyzer for face detection
        val faceAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(720, 1280)) // Match canvas size
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        // Configure ML Kit face detector
        val faceDetector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )

        // Analyze each frame for faces
        faceAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val inputImage =
                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                faceDetector.process(inputImage)
                    .addOnSuccessListener { faces ->
                        // Choose largest face with both eyes open
                        val largestFace = faces
                            .filter {
                                val leftOpen = it.leftEyeOpenProbability ?: 0f
                                val rightOpen = it.rightEyeOpenProbability ?: 0f
                                val faceArea = it.boundingBox.width() * it.boundingBox.height()
                                leftOpen > 0.8f && rightOpen > 0.8f && faceArea > (250 * 250)
                            }
                            .maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }

                        detectedFaceBounds.value = largestFace?.boundingBox
                        isFaceValid.value = largestFace != null
                    }
                    .addOnFailureListener {
                        detectedFaceBounds.value = null
                        isFaceValid.value = false
                    }
                    .addOnCompleteListener { imageProxy.close() }
            } else {
                imageProxy.close()
            }
        }

        // Bind camera to lifecycle with preview, analyzer, and capture
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_FRONT_CAMERA,
            cameraPreview,
            faceAnalyzer,
            imageCaptureInstance
        )

        // Make the preview view visible
        previewView.post { previewView.visibility = View.VISIBLE }

        // Cleanup when composable is disposed
        onDispose {
            faceAnalyzer.clearAnalyzer()
            faceDetector.close()
            cameraProvider.unbindAll()
            previewViewHolder.value = null
        }
    }

    // UI Layer: Camera + Face overlay
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(50.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
        ) {
            // Camera feed
            previewViewHolder.value?.let { previewView ->
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxSize()
                )
            }

            // Draw face circle overlay if detected
            detectedFaceBounds.value?.let { rect ->
                Log.d("CameraCaptureUI", "CameraCaptureUI: rect = $rect")
                val cornerLength = 40f
                val lineColor = Color(0xFF00FFAA) // Neon AI-style color

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasW = size.width
                    val canvasH = size.height
                    val imageW = 720f
                    val imageH = 1280f

                    val scaleX = canvasW / imageW
                    val scaleY = canvasH / imageH

                    withTransform({
                        scale(-1f, 1f, Offset(canvasW / 2, canvasH / 2))
                    }) {
                        val left = rect.left * scaleX
                        val top = rect.top * scaleY
                        val right = rect.right * scaleX
                        val bottom = rect.bottom * scaleY

                        val strokeWidth = 4f

                        // Top-left corner
                        drawLine(
                            color = lineColor,
                            start = Offset(left, top),
                            end = Offset(left + cornerLength, top),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = lineColor,
                            start = Offset(left, top),
                            end = Offset(left, top + cornerLength),
                            strokeWidth = strokeWidth
                        )
                        // Top-right corner
                        drawLine(
                            color = lineColor,
                            start = Offset(right, top),
                            end = Offset(right - cornerLength, top),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = lineColor,
                            start = Offset(right, top),
                            end = Offset(right, top + cornerLength),
                            strokeWidth = strokeWidth
                        )

                        // Bottom-left corner
                        drawLine(
                            color = lineColor,
                            start = Offset(left, bottom),
                            end = Offset(left + cornerLength, bottom),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = lineColor,
                            start = Offset(left, bottom),
                            end = Offset(left, bottom - cornerLength),
                            strokeWidth = strokeWidth
                        )

                        // Bottom-right corner
                        drawLine(
                            color = lineColor,
                            start = Offset(right, bottom),
                            end = Offset(right - cornerLength, bottom),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = lineColor,
                            start = Offset(right, bottom),
                            end = Offset(right, bottom - cornerLength),
                            strokeWidth = strokeWidth
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(50.dp))

        // Button to capture image (only enabled if face detected and not capturing)
        Button(
            onClick = {
                val faceRect = detectedFaceBounds.value ?: return@Button
                isCapturingPhoto.value = true
                val outputFile = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")

                capturePhoto(context, imageCaptureInstance, outputFile) {
                    // Crop face from the saved image
                    val croppedFaceFile = ImageUtils().cropFaceFromImage(
                        imageFile = outputFile,
                        faceRectFromPreview = faceRect,
                        previewSize = Size(720, 1280),
                        isFrontCamera = true,
                        context = context
                    )

                    if (croppedFaceFile != null) {
                        Log.d("CameraCaptureUI", "Face cropped successfully")
                        onPhotoCaptured(croppedFaceFile.absolutePath)
                    } else {
                        Log.d("CameraCaptureUI", "Face crop failed")
                        Toast.makeText(
                            context,
                            context.getString(R.string.photo_capture_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    isCapturingPhoto.value = false
                }
            },
            enabled = isFaceValid.value && !isCapturingPhoto.value,
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.app_primary))
        ) {
            Text(stringResource(R.string.click), color = Color.White)
        }

    }
}

// Utility function to take a photo and invoke callback when done
fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    outputFile: File,
    onImageSaved: (File) -> Unit
) {
    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                Log.d("capturePhoto", "Image saved successfully")
                onImageSaved(outputFile)
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(
                    context,
                    context.getString(R.string.photo_capture_failed),
                    Toast.LENGTH_SHORT
                ).show()
                exception.printStackTrace()
            }
        }
    )
}