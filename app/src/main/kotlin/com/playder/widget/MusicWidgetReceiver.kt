// MusicWidgetReceiver.kt

package com.playder.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.playder.playback.MusicService

class MusicWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        if (MusicService.isRunning) {
            val intent = Intent(context, MusicService::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            try {
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {

            }
        }

    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)

        if (MusicService.isRunning) {
            val intent = Intent(context, MusicService::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            try {
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {

            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_PLAY_PAUSE, ACTION_LIKE, ACTION_NEXT, ACTION_PREVIOUS -> {

                val serviceIntent = Intent(context, MusicService::class.java).apply {
                    action = intent.action
                    putExtras(intent)
                }
                try {
                    androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {

                }
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.playder.widget.PLAY_PAUSE"
        const val ACTION_LIKE = "com.playder.widget.LIKE"
        const val ACTION_NEXT = "com.playder.widget.NEXT"
        const val ACTION_PREVIOUS = "com.playder.widget.PREVIOUS"
        const val ACTION_UPDATE_WIDGET = "com.playder.widget.UPDATE_WIDGET"
    }
}
