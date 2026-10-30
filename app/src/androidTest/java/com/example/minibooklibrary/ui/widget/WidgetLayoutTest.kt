package com.example.minibooklibrary.ui.widget

import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.minibooklibrary.R
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Inflates the widget layout the way a launcher does. Until this existed nothing checked
 * it, and the layout shipped with Material views RemoteViews refuse, so the widget
 * never loaded. Lint's RemoteViewLayout check covers the same ground statically; this
 * also catches what lint can't see, like a view id the code updates but the layout lacks.
 */
@RunWith(AndroidJUnit4::class)
class WidgetLayoutTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun widgetLayoutInflatesAsRemoteViewsWithEveryUpdatedView() {
        val views = RemoteViews(context.packageName, R.layout.widget_currently_reading).apply {
            // The same calls CurrentlyReadingWidget makes, so a renamed id fails here.
            setTextViewText(R.id.widgetTitle, "Project Hail Mary")
            setTextViewText(R.id.widgetAuthor, "Andy Weir")
            setTextViewText(R.id.widgetPageInfo, "Page 212 of 476")
            setTextViewText(R.id.widgetEmptyText, "")
            setProgressBar(R.id.widgetProgress, 100, 44, false)
            setImageViewResource(R.id.widgetCover, R.drawable.ic_book)
            setViewVisibility(R.id.widgetRoot, View.VISIBLE)
        }
        instrumentation.runOnMainSync {
            // Throws if the layout holds a view RemoteViews can't inflate or an id is missing.
            views.apply(context, FrameLayout(context))
        }
    }
}
