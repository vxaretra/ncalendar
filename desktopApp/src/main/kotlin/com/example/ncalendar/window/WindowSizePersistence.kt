package com.example.ncalendar.window

import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import platform.appDataDirectory
import java.io.File
import java.util.Properties

private const val WINDOW_FILE_NAME = "window.properties"
private const val KEY_WIDTH = "widthDp"
private const val KEY_HEIGHT = "heightDp"
private const val KEY_X = "xDp"
private const val KEY_Y = "yDp"
private const val KEY_MAXIMIZED = "maximized"

/** Persisted window geometry in dp, restored on the next app start. */
data class WindowGeometry(
    val widthDp: Float,
    val heightDp: Float,
    val xDp: Float?,
    val yDp: Float?,
    val maximized: Boolean,
)

/** Reads the last saved geometry, or null when nothing valid has been persisted yet. */
fun loadWindowGeometry(): WindowGeometry? {
    val file = windowFile()
    if (!file.exists()) return null
    return runCatching {
        val properties = Properties()
        file.inputStream().use { properties.load(it) }

        val width = properties.getProperty(KEY_WIDTH)?.toFloatOrNull()
        val height = properties.getProperty(KEY_HEIGHT)?.toFloatOrNull()
        if (width == null || height == null) return null
        if (!width.isFinite() || !height.isFinite() || width <= 0f || height <= 0f) return null

        WindowGeometry(
            widthDp = width,
            heightDp = height,
            xDp = properties.getProperty(KEY_X)?.toFloatOrNull(),
            yDp = properties.getProperty(KEY_Y)?.toFloatOrNull(),
            maximized = properties.getProperty(KEY_MAXIMIZED)?.toBoolean() == true,
        )
    }.getOrNull()
}

/** Persists the current window size/position so the next launch can reuse it. */
fun saveWindowGeometry(state: WindowState) {
    runCatching {
        val properties = Properties()
        val size = state.size
        if (size.width.value.isFinite() && size.height.value.isFinite() &&
            size.width.value > 0f && size.height.value > 0f
        ) {
            properties.setProperty(KEY_WIDTH, size.width.value.toString())
            properties.setProperty(KEY_HEIGHT, size.height.value.toString())
        }
        if (state.placement == WindowPlacement.Maximized) {
            properties.setProperty(KEY_MAXIMIZED, "true")
        }
        if (state.position.isSpecified) {
            properties.setProperty(KEY_X, state.position.x.value.toString())
            properties.setProperty(KEY_Y, state.position.y.value.toString())
        }
        windowFile().outputStream().use { properties.store(it, "Calendar window geometry") }
    }
}

private fun windowFile(): File = File(appDataDirectory(), WINDOW_FILE_NAME)
