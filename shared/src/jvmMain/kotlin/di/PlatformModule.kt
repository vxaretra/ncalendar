package di

import data.db.DriverFactory
import org.koin.dsl.module

val desktopModule = module {
    single<DriverFactory> { DriverFactory() }
}
