package com.gululu.aamediamate

import android.app.Notification
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MediaNotificationListener : NotificationListenerService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var syncGeneration = 0

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.notification.category != Notification.CATEGORY_TRANSPORT) return

        val packageName = sbn.packageName
        Log.d("MediaBridge", "📥 Media Notification from $packageName")

        scheduleSync(packageName)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.notification.category != Notification.CATEGORY_TRANSPORT) return
        scheduleSync(sbn.packageName)
    }

    override fun onDestroy() {
        syncGeneration++
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun scheduleSync(preferredPackageName: String? = null) {
        val generation = ++syncGeneration

        SYNC_DELAYS_MS.forEachIndexed { index, delayMs ->
            mainHandler.postDelayed({
                if (generation == syncGeneration) {
                    val info = MediaInformationRetriever.refreshCurrentMediaInfo(
                        this,
                        preferredPackageName
                    )

                    // Preserve the display through transient empty states, then clear on the final attempt.
                    if (info != null || index == SYNC_DELAYS_MS.lastIndex) {
                        MediaBridgeSessionManager.updateFromMediaInfo(info)
                        MediaBridgeService.refreshBrowserData()
                    }
                }
            }, delayMs)
        }
    }

    companion object {
        private val SYNC_DELAYS_MS = longArrayOf(0L, 100L, 350L, 1_000L)
    }
}
