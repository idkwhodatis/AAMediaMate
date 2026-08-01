package com.gululu.aamediamate

import android.media.session.MediaController
import android.media.session.PlaybackState
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaControllerManagerTest {

    @Test
    fun `active preferred controller wins over list order`() {
        val other = controller("com.other.app", PlaybackState.STATE_PLAYING)
        val preferred = controller("com.music.app", PlaybackState.STATE_PLAYING)

        val selected = MediaControllerManager.selectController(
            listOf(other, preferred),
            preferredPackageName = "com.music.app"
        )

        assertSame(preferred, selected)
    }

    @Test
    fun `inactive preferred controller does not replace active controller`() {
        val preferred = controller("com.music.app", PlaybackState.STATE_PAUSED)
        val active = controller("com.other.app", PlaybackState.STATE_PLAYING)

        val selected = MediaControllerManager.selectController(
            listOf(preferred, active),
            preferredPackageName = "com.music.app"
        )

        assertSame(active, selected)
    }

    @Test
    fun `preferred controller is used when none are active`() {
        val other = controller("com.other.app", PlaybackState.STATE_PAUSED)
        val preferred = controller("com.music.app", PlaybackState.STATE_PAUSED)

        val selected = MediaControllerManager.selectController(
            listOf(other, preferred),
            preferredPackageName = "com.music.app"
        )

        assertSame(preferred, selected)
    }

    private fun controller(packageName: String, state: Int): MediaController {
        val playbackState = mockk<PlaybackState>()
        val controller = mockk<MediaController>()
        every { playbackState.state } returns state
        every { controller.packageName } returns packageName
        every { controller.playbackState } returns playbackState

        return controller
    }
}
