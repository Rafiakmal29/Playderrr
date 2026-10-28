// QQMusicLyricsProvider.kt

package com.playder.lyrics

import android.content.Context
import com.music.directlyrics.QQMusic
import com.playder.constants.EnableQQMusicKey
import com.playder.utils.dataStore
import com.playder.utils.get

object QQMusicLyricsProvider : LyricsProvider {
    override val name = "QQMusic"
    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableQQMusicKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = QQMusic.getLyrics(title, artist, duration)
}
