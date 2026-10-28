package com.playder.di

import com.playder.discord.DiscordAuth
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** for the Discord login callback, a bare Activity that Hilt doesn't build */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DiscordEntryPoint {
    fun discordAuth(): DiscordAuth
}
