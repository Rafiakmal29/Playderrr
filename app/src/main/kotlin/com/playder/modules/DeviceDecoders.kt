// DeviceDecoders.kt

package com.playder.modules

import android.media.MediaCodecList
import com.playder.module.SourceStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Whether this phone can decode a module's stream. The player only has the phone's own decoders
 * (the app ships none of its own), and a song whose audio no decoder takes doesn't fail: the
 * player runs on in silence, its clock moving, nothing heard. A FLAC on a phone without a FLAC
 * decoder (Android 8.0 has none), or at a sample rate the phone's decoders don't take (a
 * 192 kHz master), did exactly that - so such a stream is passed over for the next provider in
 * the order, YouTube at the latest.
 */
object DeviceDecoders {
    private val known = ConcurrentHashMap<String, Boolean>()

    fun canPlay(stream: SourceStream): Boolean = canPlay(stream.codec, stream.sampleRateHz ?: 0)

    /** [codec] ("FLAC", "ALAC"...) at [sampleRateHz], or at any rate when 0 */
    fun canPlay(codec: String?, sampleRateHz: Int = 0): Boolean {
        val mime = when (codec?.uppercase()) {
            "FLAC" -> "audio/flac"
            "ALAC" -> "audio/alac"
            // AAC and the rest: every phone decodes them
            else -> return true
        }
        return known.getOrPut("$mime@$sampleRateHz") { hasDecoder(mime, sampleRateHz) }
    }

    /** a decoder for [mime] that takes [sampleRateHz] (any rate when 0); true when it can't be told */
    private fun hasDecoder(mime: String, sampleRateHz: Int): Boolean = runCatching {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
            !info.isEncoder &&
                info.supportedTypes.any { it.equals(mime, ignoreCase = true) } &&
                (
                    sampleRateHz <= 0 ||
                        runCatching {
                            info.getCapabilitiesForType(mime).audioCapabilities?.isSampleRateSupported(sampleRateHz) != false
                        }.getOrDefault(true)
                    )
        }
    }.getOrDefault(true)
}
