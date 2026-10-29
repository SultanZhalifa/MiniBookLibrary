package com.example.minibooklibrary

import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.data.local.entity.ReadingLogEntity
import com.example.minibooklibrary.di.ServiceLocator
import com.example.minibooklibrary.domain.ReadingStatus
import com.example.minibooklibrary.ui.onboarding.OnboardingActivity
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Walks a fresh install through every screen on a real emulator and saves a screenshot
 * of each one. It asserts little on purpose: its job is to show what edge-to-edge,
 * dark mode and the Indonesian strings actually look like, which unit tests can't.
 *
 * Screenshots are taken with `screencap` (so they include the system bars, which is the
 * point) into /data/local/tmp, where they survive the test APK being uninstalled. CI
 * pulls that folder and uploads it as the `screenshots` artifact.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTourTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun tour() {
        shell("mkdir -p $OUT_DIR")
        try {
            walkThroughApp()
        } catch (t: Throwable) {
            // Whatever was on screen when a step failed is the fastest way to see why.
            shell("screencap -p $OUT_DIR/zz_failure.png")
            throw t
        }
    }

    private fun walkThroughApp() {
        ActivityScenario.launch(OnboardingActivity::class.java)
        capture("01_onboarding")
        onView(withId(R.id.btnSkip)).perform(click())

        capture("02_login")
        onView(withId(R.id.btnGoRegister)).perform(click())
        onView(withId(R.id.inputUsername)).perform(typeText(USERNAME))
        onView(withId(R.id.inputEmail)).perform(typeText("reader@example.com"))
        onView(withId(R.id.inputPassword)).perform(typeText(PASSWORD))
        onView(withId(R.id.inputConfirmPassword)).perform(typeText(PASSWORD))
        closeSoftKeyboard()
        onView(withId(R.id.btnRegister)).perform(click())

        // Hashing runs on a background dispatcher Espresso doesn't track.
        waitFor(R.id.btnLogin)
        onView(withId(R.id.inputUsername)).perform(typeText(USERNAME))
        onView(withId(R.id.inputPassword)).perform(typeText(PASSWORD))
        closeSoftKeyboard()
        onView(withId(R.id.btnLogin)).perform(click())
        waitFor(R.id.bottomNav)

        seedLibrary()
        capture("03_dashboard")

        onView(withId(R.id.bookListFragment)).perform(click())
        capture("04_library")

        waitFor(R.id.recycler)
        onView(allOf(withText(READING_TITLE), isDisplayed())).perform(click())
        waitFor(R.id.btnEdit)
        capture("05_detail")
        backUntil(R.id.fabAdd)

        onView(withId(R.id.fabAdd)).perform(click())
        waitFor(R.id.btnSave)
        capture("06_edit")
        onView(withId(R.id.inputTitle)).perform(click())
        capture("06b_edit_keyboard")
        closeSoftKeyboard()
        backUntil(R.id.bottomNav)
        // Focus lands on the library search box, which can bring the keyboard back over
        // the bottom nav. Clicks on another app's window (the IME) are rejected.
        closeSoftKeyboard()

        onView(withId(R.id.settingsFragment)).perform(click())
        capture("07_settings")

        onView(withId(R.id.themeDark)).perform(click())
        // Switching theme recreates the activity.
        waitFor(R.id.bottomNav)
        onView(withId(R.id.dashboardFragment)).perform(click())
        capture("08_dark_mode")
        onView(withId(R.id.bookListFragment)).perform(click())
        capture("08b_dark_library")
        onView(withId(R.id.dashboardFragment)).perform(click())
        capture("08c_dark_dashboard_again")

        onView(withId(R.id.settingsFragment)).perform(click())
        onView(withId(R.id.themeLight)).perform(click())
        waitFor(R.id.bottomNav)
        instrumentation.runOnMainSync {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("in"))
        }
        waitFor(R.id.bottomNav)
        onView(withId(R.id.dashboardFragment)).perform(click())
        capture("10_dashboard_indonesian")
    }

    /** A handful of books and five days of reading, so the dashboard has real numbers. */
    private fun seedLibrary() = runBlocking {
        val userId = ServiceLocator.providePreferences(context).currentUserId
        val today = LocalDate.now()
        fun book(
            title: String,
            author: String,
            category: String,
            year: Int,
            status: ReadingStatus,
            rating: Float,
            current: Int = 0,
            total: Int = 0,
            daysAgo: Long = 0
        ): BookEntity {
            val stamp = System.currentTimeMillis() - daysAgo * DAY_MS
            return BookEntity(
                userId = userId,
                title = title,
                author = author,
                category = category,
                year = year,
                readingStatus = status.storageValue,
                rating = rating,
                currentPage = current,
                totalPages = total,
                dateAdded = stamp,
                lastModified = stamp,
                finishedAt = if (status == ReadingStatus.FINISHED) stamp else null
            )
        }

        val books = listOf(
            book(READING_TITLE, "Andy Weir", "Fiction", 2021, ReadingStatus.CURRENTLY_READING,
                4.5f, current = 212, total = 476, daysAgo = 1),
            book("The Pragmatic Programmer", "David Thomas & Andrew Hunt", "Technology", 1999,
                ReadingStatus.FINISHED, 5f, daysAgo = 3),
            book("Atomic Habits", "James Clear", "Self-Help", 2018,
                ReadingStatus.FINISHED, 4f, daysAgo = 5),
            book("Sapiens", "Yuval Noah Harari", "History", 2011,
                ReadingStatus.WANT_TO_READ, 0f, daysAgo = 8),
            book("Laskar Pelangi", "Andrea Hirata", "Fiction", 2005,
                ReadingStatus.WANT_TO_READ, 0f, daysAgo = 9)
        )
        val pages = listOf(18, 32, 0, 25, 40, 22, 35)
        val log = pages.mapIndexedNotNull { index, count ->
            if (count == 0) null
            else ReadingLogEntity(
                userId = userId,
                bookId = null,
                epochDay = today.minusDays((pages.size - 1 - index).toLong()).toEpochDay(),
                pages = count
            )
        }
        ServiceLocator.provideBookRepository(context).addAll(books, log)
    }

    /** Poll until [id] is on screen, for work Espresso's idling doesn't cover. */
    private fun waitFor(@IdRes id: Int, timeoutMs: Long = 30_000) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (!isShown(id)) {
            check(SystemClock.uptimeMillis() < deadline) {
                "View ${context.resources.getResourceName(id)} never appeared"
            }
            Thread.sleep(250)
        }
    }

    /** Press Back until [id] shows. The first press may only dismiss the keyboard. */
    private fun backUntil(@IdRes id: Int) {
        repeat(3) {
            if (isShown(id)) return
            pressBack()
            instrumentation.waitForIdleSync()
        }
        waitFor(id)
    }

    private fun isShown(@IdRes id: Int): Boolean = try {
        onView(withId(id)).check(matches(isDisplayed()))
        true
    } catch (e: NoMatchingViewException) {
        false
    } catch (e: AssertionError) {
        false
    }

    private fun capture(name: String) {
        // screencap isn't synchronized with Espresso: let Room flows, toasts and
        // activity recreation settle first.
        instrumentation.waitForIdleSync()
        Thread.sleep(SETTLE_MS)
        shell("screencap -p $OUT_DIR/$name.png")
    }

    private fun shell(command: String) {
        val pfd = instrumentation.uiAutomation.executeShellCommand(command)
        // Reading to EOF waits for the command to finish.
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }

    private companion object {
        const val OUT_DIR = "/data/local/tmp/screenshots"
        const val USERNAME = "reader"
        const val PASSWORD = "readmore123"
        const val READING_TITLE = "Project Hail Mary"
        const val SETTLE_MS = 1_500L
        const val DAY_MS = 86_400_000L
    }
}
