package data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import platform.appDataDirectory
import java.io.File

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val dbFile = File(appDataDirectory(), DB_FILE_NAME)
        val isNew = !dbFile.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        if (isNew) {
            AppDatabase.Schema.create(driver)
        }
        return driver
    }

    private companion object {
        const val DB_FILE_NAME = "calendar.db"
    }
}
