package di

import data.db.DriverFactory
import org.koin.dsl.module
import platform.SystemTrayNotifier
import platform.TraySupport
import reminder.InAppNotifier
import reminder.Notifier

val desktopModule = module {
    single<DriverFactory> { DriverFactory() }

    // System tray when the platform has one; in-app Compose banners otherwise.
    single<Notifier> {
        if (TraySupport.isSupported) {
            SystemTrayNotifier(activationBus = get(), fallback = get())
        } else {
            get<InAppNotifier>()
        }
    }
}
