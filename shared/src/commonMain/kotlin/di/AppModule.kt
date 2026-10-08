package di

import data.EventRepository
import data.InMemoryReminderRepository
import data.ReminderRepository
import data.SqlDelightReminderRepository
import data.db.AppDatabase
import data.db.DriverFactory
import data.db.SqlDelightEventRepository
import data.db.createDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module
import reminder.InAppNotifier
import reminder.ReminderActivationBus
import reminder.ReminderScheduler

val appModule = module {
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { createDatabase(get<DriverFactory>()) }
    single { get<AppDatabase>().eventQueries }
    single { SqlDelightEventRepository(get(), get()) }
    single<EventRepository> { get<SqlDelightEventRepository>() }

    single { get<AppDatabase>().reminderQueries }
    single { SqlDelightReminderRepository(get()) }
    single<ReminderRepository> { get<SqlDelightReminderRepository>() }
    single { InMemoryReminderRepository() }

    single { InAppNotifier() }
    single { ReminderActivationBus() }
    single { ReminderScheduler(reminders = get(), notifier = get()) }
}
