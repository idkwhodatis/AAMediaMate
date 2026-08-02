package com.gululu.aamediamate.models

import android.graphics.Bitmap

data class MediaInfo(
    val appPackageName: String,
    val appName: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val position: Long,
    val isPlaying: Boolean,
    val albumArt: Bitmap?,
    val appIcon: Bitmap?
) {
    internal val mediaId: String
        get() = listOf(appPackageName, title, artist, album, duration.toString())
            .joinToString(separator = "") { value -> "${value.length}:$value" }

    internal fun isSameTrack(other: MediaInfo?): Boolean =
        other != null && mediaId == other.mediaId
}
