package com.mahesh.facedetection.presentation.ui.permission

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mahesh.facedetection.R
import com.mahesh.facedetection.presentation.ui.common.OneButtonDialog
import com.mahesh.facedetection.presentation.ui.common.TwoButtonDialog
import com.mahesh.facedetection.presentation.utils.AppEnums

@Composable
fun PermissionCheckComposable(
    onPermissionGranted: (Boolean) -> Unit
) {
    val tag = "PermissionCheckComposable"
    val context = LocalContext.current
    val activity = context as? Activity
    val permissionState = remember { mutableStateOf(AppEnums.CameraPermissionStatus.NONE) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val shouldShowRationale =
            activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    it,
                    Manifest.permission.CAMERA
                )
            } == true

        if (isGranted) {
            Log.d(tag, "PermissionCheckComposable: Permission Granted")
            permissionState.value = AppEnums.CameraPermissionStatus.GRANTED
        } else {
            if (shouldShowRationale) {
                Log.d(tag, "PermissionCheckComposable: Permission Denied")
                permissionState.value = AppEnums.CameraPermissionStatus.DENIED
            } else {
                Log.d(tag, "PermissionCheckComposable: Permission Permanently Denied")
                permissionState.value = AppEnums.CameraPermissionStatus.PERMANENTLY_DENIED
            }
        }
    }

    // Check if permission is granted
    fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            permissionState.value = AppEnums.CameraPermissionStatus.GRANTED
            Log.d(tag, "checkCameraPermission: Permission is Granted already")
        } else {
            Log.d(tag, "checkCameraPermission: Permission is not Granted, requesting")
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(Unit) {
        checkCameraPermission()
    }

    when (permissionState.value) {
        AppEnums.CameraPermissionStatus.GRANTED -> {
            onPermissionGranted(true)
        }

        AppEnums.CameraPermissionStatus.DENIED -> {
            TwoButtonDialog(
                title = stringResource(R.string.permission_title),
                subTitle = stringResource(R.string.permission_required),
                firstButtonText = stringResource(R.string.retry),
                secondButtonText = stringResource(R.string.cancel),
                onSecondButtonClicked = {
                    permissionState.value = AppEnums.CameraPermissionStatus.NONE
                    checkCameraPermission()
                },
                onFirstButtonClicked = { onPermissionGranted(false) },
                onDismiss = { onPermissionGranted(false) })
        }

        AppEnums.CameraPermissionStatus.PERMANENTLY_DENIED -> {
            OneButtonDialog(
                onDismiss = { onPermissionGranted(false) },
                onButtonClick = { onPermissionGranted(false) },
                title = stringResource(R.string.permission_title),
                message = stringResource(R.string.permission_denied),
                btnText = stringResource(R.string.ok)
            )
        }

        AppEnums.CameraPermissionStatus.NONE -> {

        }
    }
}