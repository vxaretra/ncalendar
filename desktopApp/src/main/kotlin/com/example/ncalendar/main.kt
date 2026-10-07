package com.example.ncalendar

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import data.createSampleRepository
import ui.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Calendar",
    ) {
        App(repository = createSampleRepository())
    }
}