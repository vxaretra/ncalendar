package data.db

fun createDatabase(driverFactory: DriverFactory): AppDatabase =
    AppDatabase(driverFactory.createDriver())
