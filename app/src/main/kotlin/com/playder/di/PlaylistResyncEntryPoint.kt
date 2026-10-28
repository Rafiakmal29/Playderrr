package com.playder.di

import com.playder.importer.PlaylistResync
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** for the playlist menus, which aren't Hilt view models, to start a Re-sync */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlaylistResyncEntryPoint {
    fun playlistResync(): PlaylistResync
}
