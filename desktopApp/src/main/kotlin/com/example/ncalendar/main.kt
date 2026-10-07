package com.example.ncalendar

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import data.db.DriverFactory
import data.db.SqlDelightEventRepository
import data.db.createDatabase
import data.db.seedIfEmpty
import ui.App

fun main() = application {
    val repo = SqlDelightEventRepository(createDatabase(DriverFactory()))
    seedIfEmpty(repo)
    Window(
        onCloseRequest = ::exitApplication,
        title = "Calendar",
    ) {
        App(repository = repo)
    }
}