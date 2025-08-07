package com.mahesh.facedetection.presentation.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.mahesh.facedetection.R

@Composable
fun HomeScreen(onCameraOpen: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = {
                onCameraOpen()
            },
            modifier = Modifier.align(Alignment.Center),
            enabled = true,
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(text = "Open Camera", color = colorResource(R.color.white))
        }
    }
}