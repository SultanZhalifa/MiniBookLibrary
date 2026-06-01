package com.example.minibooklibrary.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.View
import android.widget.RemoteViews
import com.example.minibooklibrary.R
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.di.ServiceLocator
import com.example.minibooklibrary.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class CurrentlyReadingWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    companion object {

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            scope.launch {
                val prefs = ServiceLocator.providePreferences(context)
                val views = RemoteViews(context.packageName, R.layout.widget_currently_reading)

                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)

                if (!prefs.isLoggedIn) {
                    showEmpty(views, context.getString(R.string.widget_not_logged_in))
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    return@launch
                }

                val dao = ServiceLocator.provideDatabase(context).bookDao()
                val book = dao.getCurrentlyReadingBook(prefs.currentUserId)

                if (book == null) {
                    showEmpty(views, context.getString(R.string.widget_no_book))
                } else {
                    showBook(context, views, book)
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private fun showBook(context: Context, views: RemoteViews, book: BookEntity) {
            views.setViewVisibility(R.id.widgetTitle, View.VISIBLE)
            views.setViewVisibility(R.id.widgetAuthor, View.VISIBLE)
            views.setViewVisibility(R.id.widgetProgress, View.VISIBLE)
            views.setViewVisibility(R.id.widgetPageInfo, View.VISIBLE)
            views.setViewVisibility(R.id.widgetEmptyText, View.GONE)

            views.setTextViewText(R.id.widgetTitle, book.title)
            views.setTextViewText(R.id.widgetAuthor, book.author)

            val hasCover = !book.coverImageUri.isNullOrBlank()
            if (hasCover) {
                val file = File(book.coverImageUri!!)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        views.setImageViewBitmap(R.id.widgetCover, bitmap)
                    } else {
                        views.setImageViewResource(R.id.widgetCover, R.drawable.ic_book)
                    }
                } else {
                    views.setImageViewResource(R.id.widgetCover, R.drawable.ic_book)
                }
            } else {
                views.setImageViewResource(R.id.widgetCover, R.drawable.ic_book)
            }

            val progress = if (book.totalPages > 0) {
                (book.currentPage * 100 / book.totalPages).coerceIn(0, 100)
            } else 0
            views.setProgressBar(R.id.widgetProgress, 100, progress, false)

            val pageInfo = if (book.totalPages > 0) {
                context.getString(R.string.widget_page_progress, book.currentPage, book.totalPages)
            } else {
                context.getString(R.string.widget_tap_to_open)
            }
            views.setTextViewText(R.id.widgetPageInfo, pageInfo)
        }

        private fun showEmpty(views: RemoteViews, message: String) {
            views.setViewVisibility(R.id.widgetTitle, View.GONE)
            views.setViewVisibility(R.id.widgetAuthor, View.GONE)
            views.setViewVisibility(R.id.widgetProgress, View.GONE)
            views.setViewVisibility(R.id.widgetPageInfo, View.GONE)
            views.setViewVisibility(R.id.widgetEmptyText, View.VISIBLE)
            views.setTextViewText(R.id.widgetEmptyText, message)
            views.setImageViewResource(R.id.widgetCover, R.drawable.ic_book)
        }
    }
}
