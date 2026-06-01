package com.example.minibooklibrary.ui.books.detail

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
import coil.load
import com.example.minibooklibrary.R
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.databinding.FragmentBookDetailBinding
import com.example.minibooklibrary.domain.ReadingStatus
import com.example.minibooklibrary.ui.common.ViewModelFactory
import com.example.minibooklibrary.ui.widget.WidgetRefresher
import com.example.minibooklibrary.util.formatDate
import com.example.minibooklibrary.util.show
import com.example.minibooklibrary.util.showIf
import com.example.minibooklibrary.util.toast
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

class BookDetailFragment : Fragment() {

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookDetailViewModel by viewModels {
        ViewModelFactory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bookId = arguments?.getLong("bookId", 0L) ?: 0L
        viewModel.load(bookId)

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        
        // Handle toolbar title appearance on scroll
        binding.appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val isCollapsed = abs(verticalOffset) >= appBarLayout.totalScrollRange
            binding.toolbar.title = if (isCollapsed) viewModel.state.value.book?.title else ""
        })

        binding.btnEdit.setOnClickListener {
            findNavController().navigate(
                R.id.action_detail_to_addEdit,
                Bundle().apply { putLong("bookId", bookId) }
            )
        }
        binding.btnDelete.setOnClickListener { confirmDelete() }
        binding.btnShare.setOnClickListener { share() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (state.notFound) {
                        toast("Book not found")
                        findNavController().popBackStack()
                    } else state.book?.let(::render)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        DetailEvent.Deleted -> {
                            toast("Book deleted")
                            WidgetRefresher.refresh(requireContext())
                            findNavController().popBackStack()
                        }
                    }
                }
            }
        }
    }

    private fun render(book: BookEntity) {
        binding.title.text = book.title
        binding.author.text = "by ${book.author}"
        binding.category.text = book.category
        binding.year.text = if (book.year > 0) book.year.toString() else "—"

        val status = ReadingStatus.fromStorageValue(book.readingStatus)
        binding.statusChip.text = status.displayLabel

        binding.ratingValue.text = if (book.rating > 0f)
            String.format("%.1f", book.rating)
        else getString(R.string.rating_unrated)
        binding.ratingBar.rating = book.rating

        val showProgress = status == ReadingStatus.CURRENTLY_READING && book.totalPages > 0
        binding.progressGroup.showIf(showProgress)
        if (showProgress) {
            val pct = ((book.currentPage * 100f) / book.totalPages).coerceIn(0f, 100f).toInt()
            binding.progressBar.progress = pct
            binding.progressLabel.text = "Page ${book.currentPage} of ${book.totalPages} ($pct%)"
        }

        binding.notes.text = book.notes.ifBlank { getString(R.string.detail_no_notes) }
        binding.dateAdded.text = getString(R.string.detail_added_on, book.dateAdded.formatDate())

        val cover = book.coverImageUri
        if (!cover.isNullOrBlank()) {
            binding.cover.load(File(cover)) {
                crossfade(true)
                placeholder(R.drawable.bg_cover_placeholder)
                error(R.drawable.bg_cover_placeholder)
            }
        } else {
            binding.cover.setImageResource(R.drawable.bg_cover_placeholder)
        }

        binding.scrollContent.show()
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_title)
            .setMessage(R.string.confirm_delete_body)
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.delete() }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun share() {
        val book = viewModel.state.value.book ?: return
        val text = buildString {
            append("📖 ${book.title}\n")
            append("by ${book.author}\n")
            if (book.year > 0) append("Published: ${book.year}\n")
            append("Category: ${book.category}\n")
            val status = ReadingStatus.fromStorageValue(book.readingStatus)
            append("Status: ${status.displayLabel}\n")
            if (book.rating > 0f) append("Rating: ${"%.1f".format(book.rating)}/5\n")
            if (book.notes.isNotBlank()) append("\nNotes: ${book.notes}")
        }
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, text)
        }
        startActivity(android.content.Intent.createChooser(intent, getString(R.string.action_share)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
