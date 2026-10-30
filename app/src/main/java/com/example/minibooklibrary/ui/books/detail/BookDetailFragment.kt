package com.example.minibooklibrary.ui.books.detail

import android.content.Intent
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
import androidx.navigation.fragment.navArgs
import coil.load
import com.example.minibooklibrary.R
import com.example.minibooklibrary.data.local.entity.BookEntity
import com.example.minibooklibrary.databinding.DialogUpdateProgressBinding
import com.example.minibooklibrary.databinding.FragmentBookDetailBinding
import com.example.minibooklibrary.domain.ReadingStatus
import com.example.minibooklibrary.ui.common.ViewModelFactory
import com.example.minibooklibrary.ui.common.applySystemBarInsets
import com.example.minibooklibrary.ui.common.applySystemBarMargins
import com.example.minibooklibrary.ui.common.labelRes
import com.example.minibooklibrary.ui.common.show
import com.example.minibooklibrary.ui.common.showIf
import com.example.minibooklibrary.ui.common.toast
import com.example.minibooklibrary.ui.widget.WidgetRefresher
import com.example.minibooklibrary.util.formatDate
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.launch

class BookDetailFragment : Fragment() {

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!

    private val args: BookDetailFragmentArgs by navArgs()

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

        val bookId = args.bookId
        viewModel.load(bookId)

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        // The toolbar is inside a CollapsingToolbarLayout, which eats insets; read them from the root.
        binding.toolbar.applySystemBarMargins(top = true, source = binding.root)
        binding.btnEdit.applySystemBarMargins(bottom = true)
        binding.bottomActions.applySystemBarMargins(bottom = true)
        binding.scrollContent.applySystemBarInsets(bottom = true)

        // Handle toolbar title appearance on scroll
        binding.appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val isCollapsed = abs(verticalOffset) >= appBarLayout.totalScrollRange
            binding.toolbar.title = if (isCollapsed) viewModel.state.value.book?.title else ""
        })

        binding.btnEdit.setOnClickListener {
            findNavController().navigate(BookDetailFragmentDirections.actionDetailToAddEdit(bookId))
        }
        binding.btnDelete.setOnClickListener { confirmDelete() }
        binding.btnShare.setOnClickListener { share() }
        binding.btnUpdateProgress.setOnClickListener { promptProgress() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (state.notFound) {
                        toast(R.string.detail_not_found)
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
                            toast(R.string.detail_deleted)
                            WidgetRefresher.refresh(requireContext())
                            findNavController().popBackStack()
                        }
                        DetailEvent.ProgressUpdated -> WidgetRefresher.refresh(requireContext())
                    }
                }
            }
        }
    }

    private fun render(book: BookEntity) {
        binding.title.text = book.title
        binding.author.text = getString(R.string.book_by_author, book.author)
        binding.category.text = book.category
        binding.year.text = if (book.year > 0) book.year.toString() else "—"

        val status = ReadingStatus.fromStorageValue(book.readingStatus)
        binding.statusChip.setText(status.labelRes)

        binding.ratingValue.text = if (book.rating > 0f)
            String.format(Locale.getDefault(), "%.1f", book.rating)
        else getString(R.string.rating_unrated)
        binding.ratingBar.rating = book.rating

        val showProgress = status == ReadingStatus.CURRENTLY_READING && book.totalPages > 0
        binding.progressGroup.showIf(showProgress)
        if (showProgress) {
            val pct = ((book.currentPage * 100f) / book.totalPages).coerceIn(0f, 100f).toInt()
            binding.progressBar.progress = pct
            binding.progressLabel.text =
                getString(R.string.detail_progress_label, book.currentPage, book.totalPages, pct)
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

    /** Small number dialog so a day's reading can be logged without opening the full form. */
    private fun promptProgress() {
        val book = viewModel.state.value.book ?: return
        val dialogBinding = DialogUpdateProgressBinding.inflate(layoutInflater)
        dialogBinding.inputPage.setText(book.currentPage.toString())
        dialogBinding.inputPage.setSelection(dialogBinding.inputPage.length())
        dialogBinding.tilPage.suffixText = getString(R.string.detail_progress_of_total, book.totalPages)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.detail_update_progress)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.action_save) { _, _ ->
                dialogBinding.inputPage.text?.toString()?.toIntOrNull()?.let(viewModel::updateProgress)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun share() {
        val book = viewModel.state.value.book ?: return
        val status = ReadingStatus.fromStorageValue(book.readingStatus)
        val text = buildString {
            append("📖 ${book.title}\n")
            append(getString(R.string.book_by_author, book.author)).append('\n')
            if (book.year > 0) append(getString(R.string.share_published, book.year)).append('\n')
            append(getString(R.string.share_category, book.category)).append('\n')
            append(getString(R.string.share_status, getString(status.labelRes))).append('\n')
            if (book.rating > 0f) {
                append(getString(R.string.share_rating, String.format(Locale.getDefault(), "%.1f", book.rating)))
                append('\n')
            }
            if (book.notes.isNotBlank()) append('\n').append(getString(R.string.share_notes, book.notes))
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
