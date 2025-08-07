package com.mahesh.facedetection.presentation.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mahesh.facedetection.R
import com.mahesh.facedetection.presentation.ui.facedetection.ClickPhotoComposable
import com.mahesh.facedetection.presentation.ui.facedetection.CroppedImagePreview
import com.mahesh.facedetection.presentation.ui.home.HomeScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph() {
    val context = LocalContext.current
    val navController = rememberNavController()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Face Detection") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = colorResource(R.color.white),
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable<Destination.Home> {
                HomeScreen {
                    navController.navigate(Destination.ClickPhoto)
                }
            }

            composable<Destination.ClickPhoto> {
                ClickPhotoComposable(
                    onDismiss = { navController.navigate(Destination.Home) },
                    onPhotoCaptured = {
                        Toast.makeText(context, "Photo captured", Toast.LENGTH_SHORT).show()
                        navController.navigate(Destination.CroppedImagePreview(it))
                    }
                )
            }

            composable<Destination.CroppedImagePreview> { navBackStackEntry ->
                val croppedImagePreview = navBackStackEntry.toRoute<Destination.CroppedImagePreview>()
                CroppedImagePreview(croppedImagePreview.imagePath)
            }
        }
    }
}
