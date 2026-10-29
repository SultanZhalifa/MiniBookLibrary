package com.example.minibooklibrary.ui.books.list

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.minibooklibrary.R
import com.example.minibooklibrary.databinding.FragmentBookListBinding
import com.example.minibooklibrary.domain.SortOrder
import com.example.minibooklibrary.domain.StatusFilter
import com.example.minibooklibrary.ui.common.ViewModelFactory
import com.example.minibooklibrary.ui.common.applySystemBarInsets
import com.example.minibooklibrary.ui.common.labelRes
import com.example.minibooklibrary.ui.common.showIf
import com.example.minibooklibrary.ui.widget.WidgetRefresher
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Library tab. Houses search, sort, filter, swipe-to-delete with undo, and tap-to-view.
 */
class BookListFragment : Fragment() {

    private var _binding: FragmentBookListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookListViewModel by viewModels {
        ViewModelFactory(requireContext())
    }

    private lateinit var adapter: BookAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // The header lives in a CollapsingToolbarLayout, which eats insets; read them from the root.
        binding.header.applySystemBarInsets(top = true, source = binding.root)

        adapter = BookAdapter(BookAdapter.LayoutVariant.VERTICAL) { book ->
            findNavController().navigate(BookListFragmentDirections.actionLibraryToDetail(book.id))
        }
        binding.recycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@BookListFragment.adapter
        }

        // Swipe-to-delete with undo via Snackbar — repository delete is fire-and-forget
        // because Room's reactive flow re-emits the post-delete state.
        val touchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ) = false

                override fun onSwiped(
                    viewHolder: RecyclerView.ViewHolder,
                    direction: Int
                ) {
                    val pos = viewHolder.bindingAdapterPosition
                    // A second swipe can land while the list is mid-diff.
                    if (pos == RecyclerView.NO_POSITION) return
                    val book = adapter.currentList[pos]
                    val appContext = requireContext().applicationContext
                    viewModel.delete(book)
                    WidgetRefresher.refresh(appContext)
                    Snackbar.make(
                        binding.root,
                        getString(R.string.snackbar_book_deleted, book.title),
                        Snackbar.LENGTH_LONG
                    )
                        .setAction(R.string.action_undo) {
                            viewModel.restore(book)
                            WidgetRefresher.refresh(appContext)
                        }
                        .addCallback(object : Snackbar.Callback() {
                            override fun onDismissed(bar: Snackbar, event: Int) {
                                // The cover file is kept until Undo is no longer possible.
                                if (event != DISMISS_EVENT_ACTION) viewModel.discardCover(book)
                            }
                        })
                        .show()
                }
            }
        )
        touchHelper.attachToRecyclerView(binding.recycler)

        binding.fabAdd.setOnClickListener {
            findNavController().navigate(BookListFragmentDirections.actionLibraryToAddEdit())
        }

        binding.btnSort.setOnClickListener { showSortMenu() }
        binding.btnFilter.setOnClickListener { showFilterMenu() }

        // Search box: avoid emitting on every keystroke during config changes by using a
        // simple TextWatcher and trusting the flow's combineLatest semantics in the VM.
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun afterTextChanged(s: Editable?) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setQuery(s?.toString().orEmpty())
            }
        })

        binding.btnEmptyAdd.setOnClickListener {
            findNavController().navigate(BookListFragmentDirections.actionLibraryToAddEdit())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: BookListUiState) {
        adapter.submitList(state.books)

        // Header counter — show "5 of 12" when filtered, else "12 books".
        binding.tvCount.text = if (state.totalUnfiltered != state.books.size) {
            getString(R.string.library_count_filtered, state.books.size, state.totalUnfiltered)
        } else {
            getString(R.string.library_count, state.totalUnfiltered)
        }

        // Empty states: differentiate "no books at all" from "no matches".
        binding.emptyNoBooks.showIf(state.hasNoBooksAtAll)
        binding.emptyNoMatches.showIf(state.hasNoMatchingResults)
        binding.recycler.showIf(state.books.isNotEmpty())
        binding.progress.showIf(state.isLoading)
    }

    private fun showSortMenu() {
        val options = SortOrder.entries.toTypedArray()
        val current = viewModel.sortOrder.value
        val labels = options.map { getString(it.labelRes) }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.library_sort_title)
            .setSingleChoiceItems(labels, options.indexOf(current)) { dialog, which ->
                viewModel.setSortOrder(options[which])
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showFilterMenu() {
        val options = StatusFilter.entries.toTypedArray()
        val current = viewModel.statusFilter.value
        val labels = options.map { getString(it.labelRes) }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.library_filter_title)
            .setSingleChoiceItems(labels, options.indexOf(current)) { dialog, which ->
                viewModel.setStatusFilter(options[which])
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
