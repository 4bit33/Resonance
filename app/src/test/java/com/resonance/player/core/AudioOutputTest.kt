package com.resonance.player.core

import com.resonance.player.core.media.AudioOutput
import com.resonance.player.core.media.AudioOutputKind
import com.resonance.player.core.media.pickAudioOutput
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioOutputTest {

    @Test
    fun noPersonalDevice_isTheSpeaker() {
        assertEquals(AudioOutput(AudioOutputKind.SPEAKER, null), pickAudioOutput(emptyList()))
    }

    @Test
    fun bluetooth_winsOverWiredAndUsb() {
        val picked = pickAudioOutput(
            listOf(
                AudioOutputKind.WIRED to null,
                AudioOutputKind.USB to "Barracuda X",
                AudioOutputKind.BLUETOOTH to "Nothing Ear"
            )
        )
        assertEquals(AudioOutput(AudioOutputKind.BLUETOOTH, "Nothing Ear"), picked)
    }

    @Test
    fun usbDongle_winsOverWired() {
        val picked = pickAudioOutput(listOf(AudioOutputKind.WIRED to null, AudioOutputKind.USB to "Barracuda X"))
        assertEquals(AudioOutput(AudioOutputKind.USB, "Barracuda X"), picked)
    }
}
