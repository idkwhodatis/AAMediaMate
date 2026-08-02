package com.gululu.aamediamate

import android.content.Context
import android.graphics.Bitmap
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.gululu.aamediamate.models.MediaInfo

class MediaStateUpdater(private val context: Context) {

    private var lastMetadataSnapshot: MetadataSnapshot? = null

    fun update(mediaSession: MediaSessionCompat, info: MediaInfo) {
        val snapshot = createMetadataSnapshot(info)
        if (lastMetadataSnapshot?.hasSameContent(snapshot) != true) {
            updateMetadata(mediaSession, snapshot)
            lastMetadataSnapshot = snapshot
        }
        updatePlaybackState(mediaSession, info)
        Log.d("MediaBridge", "🎵 Updated MediaSession state: ${info.title} by ${info.artist}")
    }

    private fun createMetadataSnapshot(info: MediaInfo): MetadataSnapshot {
        val artist = info.artist.takeIf { it.isNotBlank() }
        val album = info.album.takeIf { it.isNotBlank() }
        val showAlbumName = SettingsManager.getShowAlbumName(context)
        val artistText = if (showAlbumName) {
            listOfNotNull(artist, album).joinToString(" - ")
        } else {
            artist ?: ""
        }

        val sourceAppText = if (SettingsManager.getShowSourceApp(context)) {
            "From ${info.appName}"
        } else {
            null
        }

        return MetadataSnapshot(
            mediaId = info.mediaId,
            title = info.title,
            artist = artistText,
            sourceApp = sourceAppText,
            duration = info.duration,
            isPlaying = info.isPlaying,
            albumArt = info.albumArt,
            albumArtGenerationId = info.albumArt?.let { artwork ->
                runCatching { artwork.generationId }.getOrNull()
            }
        )
    }

    private fun updateMetadata(mediaSession: MediaSessionCompat, snapshot: MetadataSnapshot) {
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, snapshot.mediaId)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, snapshot.title)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, snapshot.duration)

        if (snapshot.artist.isNotBlank()) {
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, snapshot.artist)
        }

        snapshot.sourceApp?.let {
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, it)
        }

        snapshot.albumArt?.let {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
        }

        mediaSession.setMetadata(metadataBuilder.build())
    }

    private fun updatePlaybackState(mediaSession: MediaSessionCompat, info: MediaInfo) {
        val swapEnabled = SettingsManager.isAppSwapRewindFastForward(context, info.appPackageName)

        val actions = if (swapEnabled) {
            SWAPPED_ACTIONS
        } else {
            DEFAULT_ACTIONS
        }

        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(actions)
        stateBuilder.addCustomAction(createRewindAction())
        stateBuilder.addCustomAction(createFastForwardAction())

        if (swapEnabled) {
            // In swap mode, we hide standard Skip buttons (via actions mask) and rely on standard FF/RW buttons.
            // So we provide Skip actions as custom buttons.
            stateBuilder.addCustomAction(createSkipPreviousAction())
            stateBuilder.addCustomAction(createSkipNextAction())
        }

        stateBuilder.setState(
                if (info.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                info.position,
                1.0f
            )
        mediaSession.setPlaybackState(stateBuilder.build())
    }

    fun clear(mediaSession: MediaSessionCompat) {
        lastMetadataSnapshot = null
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                .build()
        )
        mediaSession.setMetadata(null)
        Log.d("MediaBridge", "Reset session states.")
    }

    private data class MetadataSnapshot(
        val mediaId: String,
        val title: String,
        val artist: String,
        val sourceApp: String?,
        val duration: Long,
        val isPlaying: Boolean,
        val albumArt: Bitmap?,
        val albumArtGenerationId: Int?
    ) {
        fun hasSameContent(other: MetadataSnapshot): Boolean =
            mediaId == other.mediaId &&
                    title == other.title &&
                    artist == other.artist &&
                    sourceApp == other.sourceApp &&
                    duration == other.duration &&
                    isPlaying == other.isPlaying &&
                    albumArt.hasSamePixels(
                        other = other.albumArt,
                        generationId = albumArtGenerationId,
                        otherGenerationId = other.albumArtGenerationId
                    )
    }

    private fun createRewindAction(): PlaybackStateCompat.CustomAction {
        return PlaybackStateCompat.CustomAction.Builder(
            ACTION_REWIND_10S,
            context.getString(R.string.action_rewind_10s),
            R.drawable.ic_replay_10
        ).build()
    }

    private fun createFastForwardAction(): PlaybackStateCompat.CustomAction {
        return PlaybackStateCompat.CustomAction.Builder(
            ACTION_FAST_FORWARD_10S,
            context.getString(R.string.action_fast_forward_10s),
            R.drawable.ic_forward_10
        ).build()
    }

    private fun createSkipNextAction(): PlaybackStateCompat.CustomAction {
        return PlaybackStateCompat.CustomAction.Builder(
            ACTION_SKIP_NEXT,
            context.getString(R.string.action_skip_next),
            R.drawable.ic_skip_next
        ).build()
    }

    private fun createSkipPreviousAction(): PlaybackStateCompat.CustomAction {
        return PlaybackStateCompat.CustomAction.Builder(
            ACTION_SKIP_PREVIOUS,
            context.getString(R.string.action_skip_previous),
            R.drawable.ic_skip_previous
        ).build()
    }

    companion object {
        const val ACTION_REWIND_10S = "action_rewind_10s"
        const val ACTION_FAST_FORWARD_10S = "action_fast_forward_10s"
        const val ACTION_SKIP_NEXT = "action_skip_next"
        const val ACTION_SKIP_PREVIOUS = "action_skip_previous"

        const val DEFAULT_ACTIONS =
            PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_REWIND or
                    PlaybackStateCompat.ACTION_FAST_FORWARD

        const val SWAPPED_ACTIONS =
            PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_REWIND or
                    PlaybackStateCompat.ACTION_FAST_FORWARD
    }
}

private fun Bitmap?.hasSamePixels(
    other: Bitmap?,
    generationId: Int?,
    otherGenerationId: Int?
): Boolean {
    if (this === other) return generationId == otherGenerationId
    if (this == null || other == null) return false
    if (width != other.width || height != other.height || config != other.config) return false
    return runCatching { sameAs(other) }.getOrDefault(false)
}
