package com.example.ncalendar

import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.ncalendar.window.loadWindowGeometry
import com.example.ncalendar.window.saveWindowGeometry
import data.db.SqlDelightEventRepository
import data.db.seedIfEmpty
import di.appModule
import di.desktopModule
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import ui.App
import ui.shortcuts.AppShortcutHandler

fun main() {
    println("App Data Directory: ${platform.appDataDirectory().absolutePath}")
    startKoin {
        modules(appModule, desktopModule)
    }
    seedIfEmpty(KoinPlatform.getKoin().get<SqlDelightEventRepository>())

    // App-level shortcut state lives in commonMain; the window below is the desktop
    // layer that feeds it window-level key events.
    val shortcutHandler = AppShortcutHandler()

    application {
        val geometry = remember { loadWindowGeometry() }
        val windowState = rememberWindowState(
            size = geometry?.let { DpSize(it.widthDp.dp, it.heightDp.dp) }
                ?: DpSize(1180.dp, 780.dp),
            position = geometry?.let {
                if (it.xDp != null && it.yDp != null) WindowPosition(it.xDp.dp, it.yDp.dp)
                else WindowPosition(Alignment.Center)
            } ?: WindowPosition(Alignment.Center),
            placement = if (geometry?.maximized == true) WindowPlacement.Maximized
            else WindowPlacement.Floating,
        )

        Window(
            onCloseRequest = {
                saveWindowGeometry(windowState)
                exitApplication()
            },
            state = windowState,
            title = "Calendar",
            onPreviewKeyEvent = { event -> shortcutHandler.handleKeyEvent(event) },
        ) {
            App(shortcutHandler = shortcutHandler)
        }
    }
}
