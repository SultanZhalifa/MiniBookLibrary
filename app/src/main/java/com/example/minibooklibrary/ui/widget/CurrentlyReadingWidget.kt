package com.example.minibooklibrary.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.minibooklibrary.R
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.di.ServiceLocator
import com.example.minibooklibrary.ui.RouterActivity
import com.example.minibooklibrary.util.ImageStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * Home-screen widget showing the book most recently touched while "Currently Reading".
 *
 * Refreshed by the system on its schedule and by [WidgetRefresher] whenever the app
 * changes something the widget shows (save, delete, progress, restore, sign-out).
 */
class CurrentlyReadingWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // goAsync keeps the process alive until the Room read finishes; without it the
        // system is free to kill us as soon as onUpdate returns.
        val pending = goAsync()
        scope.launch {
            try {
                appWidgetIds.forEach { render(context, appWidgetManager, it) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Widget cover is ~56dp tall; 256px covers xxxhdpi with room to spare. */
        private const val COVER_MAX_PX = 256

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            scope.launch { render(context, appWidgetManager, appWidgetId) }
        }

        private suspend fun render(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = ServiceLocator.providePreferences(context)
            val views = RemoteViews(context.packageName, R.layout.widget_currently_reading)

            // Through the router, so a signed-out user lands on sign-in, not an empty shell.
            val launchIntent = Intent(context, RouterActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)

            if (!prefs.isLoggedIn) {
                showEmpty(views, context.getString(R.string.widget_not_logged_in))
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
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

        private fun showBook(context: Context, views: RemoteViews, book: BookEntity) {
            views.setViewVisibility(R.id.widgetTitle, View.VISIBLE)
            views.setViewVisibility(R.id.widgetAuthor, View.VISIBLE)
            views.setViewVisibility(R.id.widgetProgress, View.VISIBLE)
            views.setViewVisibility(R.id.widgetPageInfo, View.VISIBLE)
            views.setViewVisibility(R.id.widgetEmptyText, View.GONE)

            views.setTextViewText(R.id.widgetTitle, book.title)
            views.setTextViewText(R.id.widgetAuthor, book.author)

            val cover = book.coverImageUri
                ?.takeIf { File(it).exists() }
                ?.let { ImageStorage.loadThumbnail(it, COVER_MAX_PX) }
            if (cover != null) {
                views.setImageViewBitmap(R.id.widgetCover, cover)
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
