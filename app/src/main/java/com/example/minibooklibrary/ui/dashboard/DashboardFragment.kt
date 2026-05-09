package com.example.minibooklibrary.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.minibooklibrary.util.show
import com.example.minibooklibrary.util.showIf
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
        recentAdapter = BookAdapter(BookAdapter.LayoutVariant.HORIZONTAL) { book ->
            findNavController().navigate(
                R.id.action_dashboard_to_detail,
                Bundle().apply { putLong("bookId", book.id) }
            )
        }
        binding.recyclerRecent.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = recentAdapter
        }

        binding.btnAddBook.setOnClickListener {
            findNavController().navigate(
                R.id.action_dashboard_to_addEdit,
                Bundle().apply { putLong("bookId", 0L) }
            )
        }
        binding.btnViewAll.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_library)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: DashboardUiState) {
        binding.tvGreeting.text = "${viewModel.timeOfDayGreeting},"
        binding.tvUserName.text = state.username.ifEmpty { "reader" }

        binding.tvTotalBooks.text = state.totalBooks.toString()
        binding.tvAvgRating.text = if (state.averageRating > 0f)
            String.format("%.1f", state.averageRating)
        else "—"

        binding.progressWantToRead.progress = state.wantToReadPercent
        binding.progressReading.progress = state.currentlyReadingPercent
        binding.progressFinished.progress = state.finishedPercent
        binding.tvWantToReadCount.text = state.wantToReadCount.toString()
        binding.tvCurrentlyReadingCount.text = state.currentlyReadingCount.toString()
        binding.tvFinishedCount.text = state.finishedCount.toString()

        recentAdapter.submitList(state.recentBooks)
        binding.emptyRecent.showIf(state.recentBooks.isEmpty())
        binding.recyclerRecent.showIf(state.recentBooks.isNotEmpty())
        binding.dashboardContent.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
