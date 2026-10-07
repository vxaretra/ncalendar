package di

import data.EventRepository
import data.db.AppDatabase
import data.db.DriverFactory
import data.db.SqlDelightEventRepository
import data.db.createDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

val appModule = module {
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { createDatabase(get<DriverFactory>()) }
    single { get<AppDatabase>().eventQueries }
    single { SqlDelightEventRepository(get(), get()) }
    single<EventRepository> { get<SqlDelightEventRepository>() }
}
