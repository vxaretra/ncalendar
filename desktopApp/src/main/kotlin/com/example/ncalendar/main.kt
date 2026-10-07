package com.example.ncalendar

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import data.db.SqlDelightEventRepository
import data.db.seedIfEmpty
import di.appModule
import di.desktopModule
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import ui.App

fun main() {
    startKoin {
        modules(appModule, desktopModule)
    }
    seedIfEmpty(KoinPlatform.getKoin().get<SqlDelightEventRepository>())

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Calendar",
        ) {
            App()
        }
    }
}
