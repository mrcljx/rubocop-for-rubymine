package io.github.sirlantis.rubymine.rubocop.utils

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowId

class NotifyUtil {
    companion object {
        private val TOOLWINDOW_NOTIFICATION = NotificationGroup.toolWindowGroup("RuboCop Errors", ToolWindowId.MESSAGES_WINDOW, true)
        private val STICKY_NOTIFICATION = NotificationGroup("RuboCop Errors", NotificationDisplayType.STICKY_BALLOON, true)
        private val BALLOON_NOTIFICATION = NotificationGroup("RuboCop Notifications", NotificationDisplayType.BALLOON, true)

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
            val message = exception.getMessage() ?: exception.javaClass.getCanonicalName()
            notify(NotificationType.ERROR, STICKY_NOTIFICATION, project, title, message)
        }

        private fun notify(notificationType: NotificationType, group: NotificationGroup, project: Project, title: String, message: String) {
            group.createNotification(title, message, notificationType, null).notify(project)
        }
    }
}
