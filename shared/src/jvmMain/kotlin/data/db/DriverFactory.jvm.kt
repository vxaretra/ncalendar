package data.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import platform.appDataDirectory
import java.io.File

actual class DriverFactory {
    actual fun createDriver(): SqlDriver {
        val dbFile = File(appDataDirectory(), DB_FILE_NAME)
        val isNew = !dbFile.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        // Reminders rely on ON DELETE CASCADE; SQLite disables foreign keys by default.
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        if (isNew) {
            AppDatabase.Schema.create(driver)
        } else {
            migrateIfNeeded(driver)
        }
        driver.execute(null, "PRAGMA user_version = ${AppDatabase.Schema.version}", 0)
        return driver
    }

    /**
     * Upgrades existing databases.
     *
     * Databases created before SQLDelight tracked versions report `user_version = 0` even
     * though EventEntity already exists, so they are treated as version 1 and the reminder
     * migration (1.sqm) is applied instead of re-running CREATE TABLE on existing tables.
     */
    private fun migrateIfNeeded(driver: SqlDriver) {
        val currentVersion = readUserVersion(driver)
        if (currentVersion >= AppDatabase.Schema.version) return

        if (currentVersion == 0L && !hasEventTable(driver)) {
            AppDatabase.Schema.create(driver)
            return
        }

        val fromVersion = maxOf(currentVersion, 1L)
        if (fromVersion < AppDatabase.Schema.version) {
            AppDatabase.Schema.migrate(driver, fromVersion, AppDatabase.Schema.version)
        }
    }

    private fun readUserVersion(driver: SqlDriver): Long =
        driver.executeQuery(
            null,
            "PRAGMA user_version",
            { cursor ->
                val hasRow = cursor.next().value
                QueryResult.Value(if (hasRow) cursor.getLong(0) ?: 0L else 0L)
            },
            0,
        ).value

    private fun hasEventTable(driver: SqlDriver): Boolean =
        driver.executeQuery(
            null,
            "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = 'EventEntity'",
            { cursor ->
                val hasRow = cursor.next().value
                QueryResult.Value(hasRow && (cursor.getLong(0) ?: 0L) > 0L)
            },
            0,
        ).value

    private companion object {
        const val DB_FILE_NAME = "calendar.db"
    }
}
