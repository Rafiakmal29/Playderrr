// NetEaseLyricsProvider.kt

package com.playder.lyrics

import android.content.Context
import com.music.directlyrics.NetEase
import com.playder.constants.EnableNetEaseKey
import com.playder.utils.dataStore
import com.playder.utils.get

object NetEaseLyricsProvider : LyricsProvider {
    override val name = "NetEase"
    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableNetEaseKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = NetEase.getLyrics(title, artist, duration)
}
