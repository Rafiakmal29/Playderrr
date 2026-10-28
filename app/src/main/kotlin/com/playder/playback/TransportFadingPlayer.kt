package com.playder.playback

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

// what the media session drives (notification, lock screen, headset, car): its pause, play and skips get the same fades as the app's buttons
@UnstableApi
class TransportFadingPlayer(
    val exoPlayer: ExoPlayer,
    private val transport: TransportTransitions,
) : ForwardingPlayer(exoPlayer) {
    override fun play() = transport.play()

    override fun pause() = transport.pause()

    override fun setPlayWhenReady(playWhenReady: Boolean) = if (playWhenReady) transport.play() else transport.pause()

    override fun seekToNext() = transport.skipNext()

    override fun seekToNextMediaItem() = transport.skipNext()

    override fun seekToPrevious() = transport.skipPrevious()

    override fun seekToPreviousMediaItem() = transport.skipPrevious()

    override fun seekTo(positionMs: Long) {
        transport.cancel()
        super.seekTo(positionMs)
    }

    override fun stop() {
        transport.cancel()
        super.stop()
    }
}
