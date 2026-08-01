package com.gululu.aamediamate

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.PlaybackState

object MediaControllerManager {
    fun getAllControllers(context: Context): List<MediaController> {
        return try {
            val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as android.media.session.MediaSessionManager
            val component = ComponentName(context, MediaNotificationListener::class.java)
            sessionManager.getActiveSessions(component)
                .filter { it.packageName != context.packageName && Global.packageAllowed(context, it.packageName) }
        } catch (e: Exception) {
            // Likely SecurityException due to missing notification access
            e.printStackTrace()
            emptyList()
        }
    }

    fun getFirstController(context: Context, preferredPackageName: String? = null): MediaController? =
        selectController(getAllControllers(context), preferredPackageName)

    internal fun selectController(
        controllers: List<MediaController>,
        preferredPackageName: String?
    ): MediaController? {
        val preferredController = preferredPackageName?.let { packageName ->
            controllers.firstOrNull { it.packageName == packageName }
        }

        if (preferredController?.isPlayingOrBuffering() == true) {
            return preferredController
        }

        return controllers.firstOrNull { it.isPlayingOrBuffering() }
            ?: preferredController
            ?: controllers.firstOrNull()
    }

    fun getActiveController(context: Context): MediaController? {
        val target = MediaBridgeSessionManager.getCurrentMediaPackage() ?: return null
        return getAllControllers(context).firstOrNull { it.packageName == target }
    }

    private fun MediaController.isPlayingOrBuffering(): Boolean =
        playbackState?.state == PlaybackState.STATE_PLAYING ||
                playbackState?.state == PlaybackState.STATE_BUFFERING
}
