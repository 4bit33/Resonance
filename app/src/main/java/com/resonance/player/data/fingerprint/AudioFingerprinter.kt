package com.resonance.player.data.fingerprint

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext

/** A song's fingerprint and its length in whole seconds, as AcoustID wants them. */
data class AudioFingerprint(val fingerprint: String, val durationSec: Int)

/**
 * Decodes the start of a song with the system decoders (MediaExtractor +
 * MediaCodec, so every format the phone plays works) and fingerprints it
 * with Chromaprint. Works fully offline; only the lookup needs the network.
 */
class AudioFingerprinter(
    private val context: Context,
    private val io: kotlin.coroutines.CoroutineContext
) {
    /** AcoustID matches on the first two minutes (as fpcalc feeds); more only costs time. */
    private val maxAudioSec = 120
    private val maxAudioUs = maxAudioSec * 1_000_000L

    suspend fun fingerprint(uri: Uri): Result<AudioFingerprint> = withContext(io) {
        runCatching { decode(uri) }
    }

    private suspend fun decode(uri: Uri): AudioFingerprint {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        var printer: Chromaprint? = null
        try {
            extractor.setDataSource(context, uri, null)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("No audio track")
            extractor.selectTrack(track)
            val input = extractor.getTrackFormat(track)
            val durationUs = if (input.containsKey(MediaFormat.KEY_DURATION)) input.getLong(MediaFormat.KEY_DURATION) else 0L
            codec = MediaCodec.createDecoderByType(input.getString(MediaFormat.KEY_MIME)!!).apply {
                configure(input, null, null, 0)
                start()
            }

            val info = MediaCodec.BufferInfo()
            var samples = ShortArray(0)
            var inputDone = false
            var outputDone = false
            var floatPcm = false
            /** Interleaved samples still allowed in (exactly two minutes); set once the format is known. */
            var budget = Long.MAX_VALUE
            while (!outputDone) {
                coroutineContext.ensureActive()
                if (!inputDone) {
                    val i = codec.dequeueInputBuffer(10_000)
                    if (i >= 0) {
                        val buffer = codec.getInputBuffer(i)!!
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0 || extractor.sampleTime > maxAudioUs) {
                            codec.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(i, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val o = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val format = codec.outputFormat
                        floatPcm = format.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                            format.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                        printer?.close()
                        val rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        printer = Chromaprint(rate, channels)
                        budget = maxAudioSec.toLong() * rate * channels
                    }
                    o >= 0 -> {
                        if (info.size > 0) {
                            val p = printer ?: run {
                                val rate = codec.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                                val channels = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                                budget = maxAudioSec.toLong() * rate * channels
                                Chromaprint(rate, channels).also { printer = it }
                            }
                            val buffer = codec.getOutputBuffer(o)!!.order(ByteOrder.nativeOrder())
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            val count: Int
                            if (floatPcm) {
                                val floats = buffer.asFloatBuffer()
                                count = floats.remaining()
                                if (samples.size < count) samples = ShortArray(count)
                                for (k in 0 until count) {
                                    samples[k] = (floats.get(k).coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                                }
                            } else {
                                val shorts = buffer.asShortBuffer()
                                count = shorts.remaining()
                                if (samples.size < count) samples = ShortArray(count)
                                shorts.get(samples, 0, count)
                            }
                            val take = minOf(count.toLong(), budget).toInt()
                            if (take > 0) p.feed(samples, take)
                            budget -= take
                            if (budget <= 0L) outputDone = true
                        }
                        codec.releaseOutputBuffer(o, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                }
            }
            val fingerprint = printer?.finish() ?: error("Too little audio to fingerprint")
            return AudioFingerprint(fingerprint, (durationUs / 1_000_000L).toInt())
        } finally {
            printer?.close()
            runCatching { codec?.stop() }
            codec?.release()
            extractor.release()
        }
    }
}
