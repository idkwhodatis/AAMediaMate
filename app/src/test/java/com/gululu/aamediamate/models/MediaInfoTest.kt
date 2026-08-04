package com.gululu.aamediamate.models

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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

    @Test
    fun `missing artwork retains artwork for same track`() {
        val artwork = solidArtwork(Color.RED)
        val previous = mediaInfo(albumArt = artwork)

        val updated = mediaInfo(position = 500L).retainAlbumArtFrom(previous)

        assertSame(artwork, updated.albumArt)
    }

    @Test
    fun `missing artwork does not carry into next track`() {
        val previous = mediaInfo(albumArt = solidArtwork(Color.RED))

        val updated = mediaInfo(title = "Next Song").retainAlbumArtFrom(previous)

        assertNull(updated.albumArt)
    }

    @Test
    fun `new artwork replaces retained artwork`() {
        val previous = mediaInfo(albumArt = solidArtwork(Color.RED))
        val newArtwork = solidArtwork(Color.BLUE)

        val updated = mediaInfo(albumArt = newArtwork).retainAlbumArtFrom(previous)

        assertSame(newArtwork, updated.albumArt)
    }

    private fun solidArtwork(color: Int): Bitmap =
        Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

    private fun mediaInfo(
        appPackageName: String = "com.music.app",
        title: String = "Song Title",
        position: Long = 0L,
        isPlaying: Boolean = true,
        albumArt: Bitmap? = null
    ) = MediaInfo(
        appPackageName = appPackageName,
        appName = "MusicApp",
        title = title,
        artist = "Artist Name",
        album = "Album Name",
        duration = 1000L,
        position = position,
        isPlaying = isPlaying,
        albumArt = albumArt,
        appIcon = null
    )
}
