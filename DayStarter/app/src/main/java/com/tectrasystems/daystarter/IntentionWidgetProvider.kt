package com.tectrasystems.daystarter

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import android.widget.RemoteViews

class IntentionWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Perform this loop procedure for each App Widget that belongs to this provider
        appWidgetIds.forEach { appWidgetId ->
            val pendingIntent: PendingIntent = Intent(context, MainActivity::class.java)
                .let { intent ->
                    PendingIntent.getActivity(context, 0, intent, 0)
                }

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            val views: RemoteViews = RemoteViews(
                context.packageName,
                R.layout.intention_provider_layout
            ).apply {
                setOnClickPendingIntent(R.id.intentionWidgetLayout, pendingIntent)

                val intention = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString("intentionString", "")

                if (intention?.trim() ?: "" != "") {
                    //show intention if not blank
                    setTextViewText(R.id.intentionWidgetTxt, "\"$intention\"")
                } else {
                    setTextViewText(R.id.intentionWidgetTxt, "Set an intention in the binoc app.")
                }
            }

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}