package com.mahesh.facedetection.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Destination {

    @Serializable
    data object Home : Destination()

    @Serializable
    data object ClickPhoto : Destination()

    @Serializable
    data class CroppedImagePreview(val imagePath: String) : Destination()
}