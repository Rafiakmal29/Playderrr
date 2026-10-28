package com.playder.eq

import com.playder.eq.audio.LoudnessParams
import com.playder.eq.audio.ReplayGainParams
import com.playder.eq.audio.ReverbParams
import com.playder.eq.audio.SpatialParams
import com.playder.eq.audio.VolumeCurve
import javax.inject.Inject
import javax.inject.Singleton

/** the audio fx settings every player's effects read at the start of each buffer */
@Singleton
class AudioFxService @Inject constructor() {
    @Volatile
    var reverb: ReverbParams = ReverbParams()

    @Volatile
    var loudness: LoudnessParams = LoudnessParams()

    @Volatile
    var spatial: SpatialParams = SpatialParams()

    /** replaced, never mutated: the sink notices a new curve by identity */
    @Volatile
    var volumeCurve: VolumeCurve = VolumeCurve()

    /** "even out songs": youtube's loudness numbers, or a file's own replaygain */
    @Volatile
    var normalize: Boolean = true

    @Volatile
    var replayGain: ReplayGainParams = ReplayGainParams()

    /** how much the loudness stage is pulling peaks down right now, in db (0 or less), for the meter */
    @Volatile
    var loudnessReductionDb: Float = 0f
}
