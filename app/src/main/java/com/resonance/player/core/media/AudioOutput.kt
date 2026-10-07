package com.resonance.player.core.media

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class AudioOutputKind { SPEAKER, WIRED, USB, BLUETOOTH }

/** Where sound is going right now, e.g. "Nothing Ear" over Bluetooth. */
data class AudioOutput(val kind: AudioOutputKind, val name: String?)

/**
 * Android routes media to the most recently connected personal device, so the
 * pick is: Bluetooth > USB > wired > built-in speaker. Pure so it can be tested.
 */
fun pickAudioOutput(devices: List<Pair<AudioOutputKind, String?>>): AudioOutput {
    val order = listOf(AudioOutputKind.BLUETOOTH, AudioOutputKind.USB, AudioOutputKind.WIRED)
    for (kind in order) {
        devices.firstOrNull { it.first == kind }?.let { return AudioOutput(kind, it.second) }
    }
    return AudioOutput(AudioOutputKind.SPEAKER, null)
}

private fun AudioDeviceInfo.kind(): AudioOutputKind? = when (type) {
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> AudioOutputKind.BLUETOOTH
    AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> AudioOutputKind.USB
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> AudioOutputKind.WIRED
    else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
    ) {
        AudioOutputKind.BLUETOOTH
    } else {
        null
    }
}

/** The current audio output, updated when headphones connect or disconnect. No permission needed. */
fun observeAudioOutput(context: Context): Flow<AudioOutput> = callbackFlow {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    fun emitCurrent() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).mapNotNull { device ->
            device.kind()?.let { it to device.productName?.toString()?.takeIf(String::isNotBlank) }
        }
        trySend(pickAudioOutput(devices))
    }
    val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = emitCurrent()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = emitCurrent()
    }
    emitCurrent()
    audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
    awaitClose { audioManager.unregisterAudioDeviceCallback(callback) }
}.distinctUntilChanged()
