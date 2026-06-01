package com.example.minibooklibrary.ui.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

object WidgetRefresher {

    fun refresh(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, CurrentlyReadingWidget::class.java)
        )
        if (ids.isEmpty()) return
        for (id in ids) {
            CurrentlyReadingWidget.updateWidget(context, manager, id)
        }
    }
}
