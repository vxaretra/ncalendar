package platform

import java.awt.SystemTray

/** Whether this environment can show system-tray notifications. */
object TraySupport {
    val isSupported: Boolean get() = SystemTray.isSupported()
}
