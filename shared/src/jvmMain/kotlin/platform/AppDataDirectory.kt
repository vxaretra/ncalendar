package platform

import java.io.File

/**
 * Root directory for data that must survive app restarts (SQLite database, window layout).
 *
 * This lives in the desktop (JVM) source set so shared common code never depends on
 * platform file APIs except through expect/actual or injected platform services.
 */
fun appDataDirectory(): File =
    File(System.getProperty("user.home") ?: ".", ".ncalendar").apply { mkdirs() }
