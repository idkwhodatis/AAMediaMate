package com.gululu.aamediamate.models

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaInfoTest {

    @Test
    fun `same track ignores playback position and state`() {
        val playing = mediaInfo(position = 100L, isPlaying = true)
        val paused = mediaInfo(position = 500L, isPlaying = false)

        assertTrue(playing.isSameTrack(paused))
    }

    @Test
    fun `different title is a different track`() {
        assertFalse(mediaInfo().isSameTrack(mediaInfo(title = "Next Song")))
    }

    @Test
    fun `different app is a different track`() {
        assertFalse(mediaInfo().isSameTrack(mediaInfo(appPackageName = "com.other.app")))
    }

    private fun mediaInfo(
        appPackageName: String = "com.music.app",
        title: String = "Song Title",
        position: Long = 0L,
        isPlaying: Boolean = true
    ) = MediaInfo(
        appPackageName = appPackageName,
        appName = "MusicApp",
        title = title,
        artist = "Artist Name",
        album = "Album Name",
        duration = 1000L,
        position = position,
        isPlaying = isPlaying,
        albumArt = null,
        appIcon = null
    )
}
