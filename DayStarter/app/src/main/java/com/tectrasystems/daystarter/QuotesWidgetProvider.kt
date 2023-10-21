package com.tectrasystems.daystarter

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews


class QuotesWidgetProvider : AppWidgetProvider() {
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
                R.layout.quotes_provider_layout
            ).apply {
                //open binoc when tapped
                setOnClickPendingIntent(R.id.quotesWidgetLayout, pendingIntent)

                //get array
                val quotes = context.resources.getStringArray(R.array.quotes)

                //pick random quote
                val randomQuote = kotlin.random.Random.Default.nextInt(quotes.size - 1)
                val selectedQuote = quotes[randomQuote]

                //split text and author
                val quoteArray: List<String> = selectedQuote.split("@")

                //set text
                setTextViewText(R.id.quotesWidgetTxt, '"' + quoteArray[0] + '"') //quote
                setTextViewText(R.id.quotesWidgetAuthor, " - " + quoteArray[1]) //author
            }

            //tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}