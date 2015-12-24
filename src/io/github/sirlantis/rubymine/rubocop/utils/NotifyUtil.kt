package io.github.sirlantis.rubymine.rubocop.utils

import com.intellij.notification.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowId
import javax.swing.event.HyperlinkEvent

class NotifyUtil {
    class Listener(): NotificationListener {
        override fun hyperlinkUpdate(notification: Notification, event: HyperlinkEvent) {
            if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                // TODO
            }
        }
    }

    companion object {
        private val TOOLWINDOW_NOTIFICATION = NotificationGroup.toolWindowGroup("RuboCop Errors", ToolWindowId.MESSAGES_WINDOW, true)
        private val STICKY_NOTIFICATION = NotificationGroup("Sticky RuboCop Errors", NotificationDisplayType.STICKY_BALLOON, true)
        private val BALLOON_NOTIFICATION = NotificationGroup("RuboCop Notifications", NotificationDisplayType.BALLOON, true)
        private val SHOWN_NOTIFICATIONS = hashMapOf<NotificationGroup, Notification>()

        public fun notifySuccess(project: Project, title: String, message: String) {
            notify(NotificationType.INFORMATION, BALLOON_NOTIFICATION, project, title, message)
        }

        public fun notifyInfo(project: Project, title: String, message: String) {
            notify(NotificationType.INFORMATION, TOOLWINDOW_NOTIFICATION, project, title, message)
        }

        public fun notifyError(project: Project, title: String, message: String) {
            notify(NotificationType.ERROR, TOOLWINDOW_NOTIFICATION, project, title, message)
        }

        public fun notifyError(project: Project, title: String, exception: Exception) {
            val message = exception.message ?: exception.javaClass.canonicalName
            notify(NotificationType.ERROR, STICKY_NOTIFICATION, project, title, message)
        }

        private fun notify(notificationType: NotificationType, group: NotificationGroup, project: Project, title: String, message: String) {
            SHOWN_NOTIFICATIONS[group]?.expire()

            val listener = Listener()
            val notification = group.createNotification(title, message, notificationType, listener)

            notification.whenExpired {
                if (SHOWN_NOTIFICATIONS[group] == notification) {
                    SHOWN_NOTIFICATIONS.remove(group)
                }
            }

            SHOWN_NOTIFICATIONS[group] = notification
            notification.notify(project)
        }
    }
}
