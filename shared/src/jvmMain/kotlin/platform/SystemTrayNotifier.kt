package platform

import data.Event
import data.Reminder
import reminder.InAppNotifier
import reminder.Notifier
import reminder.OffsetFormat
import reminder.ReminderActivationBus
import java.awt.Color
import java.awt.Image
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import javax.swing.SwingUtilities

/**
 * Desktop [Notifier] built on the AWT system tray.
 *
 * The scheduler runs on a background dispatcher, so every tray call is marshalled onto the
 * AWT event thread. When the tray is unavailable (or its setup fails) notifications fall
 * back to [InAppNotifier] banners, so reminders are never silently dropped.
 *
 * A tray click (including a click on the notification balloon on most platforms) asks the
 * app to raise its window through [ReminderActivationBus] and select the event's day.
 */
class SystemTrayNotifier(
    private val activationBus: ReminderActivationBus,
    private val fallback: InAppNotifier,
) : Notifier {

    @Volatile
    private var lastEvent: Event? = null

    private val trayIcon: TrayIcon? = createTrayIcon()

    override fun notify(reminder: Reminder, event: Event) {
        val icon = trayIcon
        if (icon == null) {
            fallback.notify(reminder, event)
            return
        }
        lastEvent = event
        val title = event.title
        val body = OffsetFormat.notificationBody(reminder.offsetMinutes, event.startTime)
        SwingUtilities.invokeLater {
            icon.displayMessage(title, body, TrayIcon.MessageType.NONE)
        }
    }

    private fun createTrayIcon(): TrayIcon? {
        if (!TraySupport.isSupported) return null
        return runCatching {
            val icon = TrayIcon(trayImage(), "Calendar")
            icon.isImageAutoSize = true
            icon.addActionListener { activationBus.activate(lastEvent) }
            SystemTray.getSystemTray().add(icon)
            icon
        }.getOrNull()
    }

    /** Minimal programmatic tray icon: a rounded calendar-ish square. */
    private fun trayImage(): Image {
        val size = 32
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.color = Color(0x2F, 0x6F, 0xED)
            g.fillRoundRect(2, 2, size - 4, size - 4, 10, 10)
            g.color = Color.WHITE
            g.fillRect(9, 10, 14, 3)
            g.fillRect(9, 17, 9, 3)
        } finally {
            g.dispose()
        }
        return image
    }
}
