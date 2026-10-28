package com.example.minibooklibrary.ui.dashboard

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.minibooklibrary.R
import com.example.minibooklibrary.databinding.FragmentDashboardBinding
import com.example.minibooklibrary.ui.books.list.BookAdapter
import com.example.minibooklibrary.ui.common.ViewModelFactory
import com.example.minibooklibrary.ui.common.applySystemBarInsets
import com.example.minibooklibrary.ui.common.show
import com.example.minibooklibrary.ui.common.showIf
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Home tab. Shows greeting, totals card, status breakdown, recently added books, and a
 * quick "Add book" CTA.
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels {
        ViewModelFactory(requireContext())
    }

    private lateinit var recentAdapter: BookAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.header.applySystemBarInsets(top = true)
        recentAdapter = BookAdapter(BookAdapter.LayoutVariant.HORIZONTAL) { book ->
            findNavController().navigate(DashboardFragmentDirections.actionDashboardToDetail(book.id))
        }
        binding.recyclerRecent.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = recentAdapter
        }

        binding.btnAddBook.setOnClickListener {
            findNavController().navigate(DashboardFragmentDirections.actionDashboardToAddEdit())
        }
        binding.btnViewAll.setOnClickListener {
            findNavController().navigate(DashboardFragmentDirections.actionDashboardToLibrary())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: DashboardUiState) {
        binding.tvGreeting.text = getString(viewModel.greetingRes)
        binding.tvUserName.text = state.username.ifEmpty { getString(R.string.dashboard_default_name) }

        binding.tvTotalBooks.text = state.totalBooks.toString()
        binding.tvAvgRating.text = if (state.averageRating > 0f)
            String.format(Locale.getDefault(), "%.1f", state.averageRating)
        else "—"

        binding.progressWantToRead.progress = state.wantToReadPercent
        binding.progressReading.progress = state.currentlyReadingPercent
        binding.progressFinished.progress = state.finishedPercent
        binding.tvWantToReadCount.text = state.wantToReadCount.toString()
        binding.tvCurrentlyReadingCount.text = state.currentlyReadingCount.toString()
        binding.tvFinishedCount.text = state.finishedCount.toString()

        renderReading(state)

        recentAdapter.submitList(state.recentBooks)
        binding.emptyRecent.showIf(state.recentBooks.isEmpty())
        binding.recyclerRecent.showIf(state.recentBooks.isNotEmpty())
        binding.dashboardContent.show()
    }

    private fun renderReading(state: DashboardUiState) {
        val stats = state.reading
        binding.tvStreak.text = stats.currentStreak.toString()
        binding.tvStreakLabel.setText(R.string.dashboard_streak_label)
        binding.tvPagesThisWeek.text = resources.getQuantityString(
            R.plurals.dashboard_pages_this_week, stats.pagesThisWeek, stats.pagesThisWeek
        )
        binding.tvReadingFooter.text = getString(
            R.string.dashboard_reading_footer,
            state.year,
            state.finishedThisYear,
            resources.getQuantityString(
                R.plurals.dashboard_longest_streak, stats.longestStreak, stats.longestStreak
            )
        )
        // Nothing logged yet: explain where the numbers come from instead of showing zeros.
        binding.tvReadingHint.showIf(stats.longestStreak == 0)
        renderWeekBars(stats.lastSevenDays)
    }

    /**
     * Seven plain Views sized by weight. A charting library would be a lot of APK for one
     * row of bars, and this keeps the colors on the theme's own attributes.
     */
    private fun renderWeekBars(pages: List<Int>) {
        val container = binding.weekBars
        val density = resources.displayMetrics.density
        if (container.childCount == 0) {
            repeat(pages.size) {
                val column = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                    layoutParams = LinearLayout.LayoutParams(0, ((BAR_MAX_DP + 20) * density).toInt(), 1f)
                }
                val bar = View(requireContext()).apply {
                    background = AppCompatResources.getDrawable(context, R.drawable.bg_week_bar)
                    layoutParams = LinearLayout.LayoutParams((14 * density).toInt(), 0)
                }
                val label = TextView(requireContext()).apply {
                    setTextAppearance(R.style.TextAppearance_MiniBookLibrary_Caption)
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = (4 * density).toInt() }
                }
                column.addView(bar)
                column.addView(label)
                container.addView(column)
            }
        }

        val max = pages.maxOrNull()?.takeIf { it > 0 } ?: 1
        val today = LocalDate.now()
        pages.forEachIndexed { index, value ->
            val column = container.getChildAt(index) as LinearLayout
            val bar = column.getChildAt(0)
            val label = column.getChildAt(1) as TextView
            val heightDp = if (value == 0) BAR_EMPTY_DP else maxOf(BAR_EMPTY_DP, BAR_MAX_DP * value / max)
            bar.updateLayoutParams { height = (heightDp * density).toInt() }
            bar.alpha = if (value == 0) 0.3f else 1f
            label.text = today.minusDays((pages.size - 1 - index).toLong())
                .dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
        }
        container.contentDescription = resources.getQuantityString(
            R.plurals.dashboard_pages_this_week, pages.sum(), pages.sum()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val BAR_MAX_DP = 56
        const val BAR_EMPTY_DP = 4
    }
}
