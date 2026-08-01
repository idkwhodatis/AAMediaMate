package com.gululu.aamediamate

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import com.gululu.aamediamate.models.MediaInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaStateUpdaterTest {

    private val context = mockk<Context>(relaxed = true)
    private val mediaSession = mockk<MediaSessionCompat>(relaxed = true)
    private lateinit var prefs: SharedPreferences
    private lateinit var updater: MediaStateUpdater

    @Before
    fun setUp() {
        prefs = mockk(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { context.getString(any()) } returns "Action"
        every { prefs.getString("bridged_apps", "[]") } returns "[]"
        every { prefs.getBoolean("show_album_name", true) } returns true
        every { prefs.getBoolean("show_source_app", true) } returns true
        updater = MediaStateUpdater(context)
    }

    @Test
    fun `update includes source app by default`() {
        updater.update(mediaSession, mediaInfo())

        val metadata = capturedMetadata()
        assertEquals("From MusicApp", metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM))
        assertEquals(mediaInfo().mediaId, metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID))
    }

    @Test
    fun `update omits source app when disabled`() {
        every { prefs.getBoolean("show_source_app", true) } returns false

        updater.update(mediaSession, mediaInfo())

        val metadata = capturedMetadata()
        assertNull(metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM))
    }

    @Test
    fun `repeated update for same track does not republish metadata`() {
        updater.update(mediaSession, mediaInfo(position = 0L))
        updater.update(mediaSession, mediaInfo(position = 500L))

        verify(exactly = 1) { mediaSession.setMetadata(any()) }
        verify(exactly = 2) { mediaSession.setPlaybackState(any()) }
    }

    @Test
    fun `equivalent artwork does not republish metadata`() {
        val firstArtwork = solidArtwork(Color.RED)
        val copiedArtwork = firstArtwork.copy(firstArtwork.config ?: Bitmap.Config.ARGB_8888, false)

        updater.update(mediaSession, mediaInfo(albumArt = firstArtwork))
        updater.update(mediaSession, mediaInfo(albumArt = copiedArtwork, position = 500L))

        verify(exactly = 1) { mediaSession.setMetadata(any()) }
    }

    @Test
    fun `changed artwork republishes metadata`() {
        updater.update(mediaSession, mediaInfo(albumArt = solidArtwork(Color.RED)))
        updater.update(mediaSession, mediaInfo(albumArt = solidArtwork(Color.BLUE)))

        verify(exactly = 2) { mediaSession.setMetadata(any()) }
    }

    @Test
    fun `mutated artwork republishes metadata`() {
        val artwork = solidArtwork(Color.RED)
        updater.update(mediaSession, mediaInfo(albumArt = artwork))

        artwork.eraseColor(Color.BLUE)
        updater.update(mediaSession, mediaInfo(albumArt = artwork))

        verify(exactly = 2) { mediaSession.setMetadata(any()) }
    }

    @Test
    fun `changed track republishes metadata`() {
        updater.update(mediaSession, mediaInfo())
        updater.update(mediaSession, mediaInfo(title = "Next Song"))

        verify(exactly = 2) { mediaSession.setMetadata(any()) }
    }

    @Test
    fun `playback transition republishes original metadata once`() {
        updater.update(mediaSession, mediaInfo(isPlaying = true))
        updater.update(mediaSession, mediaInfo(isPlaying = false, position = 500L))

        verify(exactly = 2) { mediaSession.setMetadata(any()) }
    }

    private fun capturedMetadata(): MediaMetadataCompat {
        val metadata = slot<MediaMetadataCompat>()
        verify { mediaSession.setMetadata(capture(metadata)) }
        return metadata.captured
    }

    private fun solidArtwork(color: Int): Bitmap =
        Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

    private fun mediaInfo(
        title: String = "Song Title",
        position: Long = 0L,
        isPlaying: Boolean = true,
        albumArt: Bitmap? = null
    ) = MediaInfo(
        title = title,
        artist = "Artist Name",
        album = "Album Name",
        appName = "MusicApp",
        appPackageName = "com.music.app",
        duration = 1000L,
        isPlaying = isPlaying,
        position = position,
        albumArt = albumArt,
        appIcon = null
    )
}
