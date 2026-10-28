package com.playder.di

import com.playder.scrobble.Scrobbler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** for WorkManager's scrobble sender, which Hilt doesn't build */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ScrobblerEntryPoint {
    fun scrobbler(): Scrobbler
}
