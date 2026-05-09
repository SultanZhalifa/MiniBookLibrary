package com.example.minibooklibrary.ui.books.edit

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.minibooklibrary.R
import com.example.minibooklibrary.databinding.FragmentAddEditBookBinding
import com.example.minibooklibrary.domain.ReadingStatus
import com.example.minibooklibrary.ui.common.ViewModelFactory
import com.example.minibooklibrary.util.ImageStorage
import com.example.minibooklibrary.util.show
import com.example.minibooklibrary.util.showIf
import com.example.minibooklibrary.util.toast
import kotlinx.coroutines.launch
import java.io.File

/**
 * Add a new book or edit an existing one. The form's stored state lives in
 * [AddEditBookViewModel] so config changes don't blow away half-typed input.
 */
class AddEditBookFragment : Fragment() {

    private var _binding: FragmentAddEditBookBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddEditBookViewModel by viewModels {
        ViewModelFactory(requireContext())
    }

    /**
     * Photo Picker — preferred over `ACTION_GET_CONTENT` because it doesn't require a
     * runtime permission on Android 13+ and falls back to the system gallery on older
     * versions. We copy the resulting URI's bytes into private storage in [onCoverPicked].
     */
    private val pickCoverLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onCoverPicked(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bookId = arguments?.getLong("bookId", 0L) ?: 0L
        if (bookId > 0L) viewModel.loadForEdit(bookId)

        setupCategoryDropdown()
        setupStatusDropdown()
        wireForm()
        wireButtons()
        observeForm()
        observeValidation()
        observeEvents()
    }

    private fun setupCategoryDropdown() {
        val categories = resources.getStringArray(R.array.book_categories)
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, categories)
        binding.inputCategory.setAdapter(adapter)
        binding.inputCategory.setOnItemClickListener { parent, _, position, _ ->
            val value = parent.getItemAtPosition(position) as String
            viewModel.updateField { copy(category = value) }
        }
    }

    private fun setupStatusDropdown() {
        val statuses = ReadingStatus.entries
        val labels = statuses.map { it.displayLabel }.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, labels)
        binding.inputStatus.setAdapter(adapter)
        binding.inputStatus.setOnItemClickListener { _, _, position, _ ->
            viewModel.updateField { copy(readingStatus = statuses[position]) }
        }
    }

    private fun wireForm() {
        binding.inputTitle.doAfterChange { viewModel.updateField { copy(title = it) } }
        binding.inputAuthor.doAfterChange { viewModel.updateField { copy(author = it) } }
        binding.inputYear.doAfterChange { viewModel.updateField { copy(year = it) } }
        binding.inputCurrentPage.doAfterChange { viewModel.updateField { copy(currentPage = it) } }
        binding.inputTotalPages.doAfterChange { viewModel.updateField { copy(totalPages = it) } }
        binding.inputNotes.doAfterChange { viewModel.updateField { copy(notes = it) } }

        binding.ratingBar.setOnRatingBarChangeListener { _, value, fromUser ->
            if (fromUser) viewModel.updateField { copy(rating = value) }
        }
    }

    private fun wireButtons() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnSave.setOnClickListener { viewModel.save() }
        binding.btnPickCover.setOnClickListener {
            pickCoverLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        binding.btnRemoveCover.setOnClickListener {
            // Delete the on-disk file too so we don't leak storage.
            ImageStorage.deleteCoverImage(viewModel.form.value.coverImageUri)
            viewModel.updateField { copy(coverImageUri = null) }
        }
        binding.btnLookupIsbn.setOnClickListener {
            val isbn = binding.inputIsbn.text?.toString().orEmpty()
            viewModel.lookupIsbn(isbn)
        }
    }

    private fun observeForm() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.form.collect { state ->
                    binding.toolbar.title = getString(
                        if (state.isEditing) R.string.addedit_edit_title
                        else R.string.addedit_add_title
                    )
                    binding.btnSave.text = getString(
                        if (state.isEditing) R.string.addedit_update else R.string.addedit_save
                    )

                    setIfDifferent(binding.inputTitle, state.title)
                    setIfDifferent(binding.inputAuthor, state.author)
                    setIfDifferent(binding.inputYear, state.year)
                    setIfDifferent(binding.inputCategory, state.category, false)
                    setIfDifferent(binding.inputStatus, state.readingStatus.displayLabel, false)
                    setIfDifferent(binding.inputCurrentPage, state.currentPage)
                    setIfDifferent(binding.inputTotalPages, state.totalPages)
                    setIfDifferent(binding.inputNotes, state.notes)

                    if (binding.ratingBar.rating != state.rating) {
                        binding.ratingBar.rating = state.rating
                    }

                    val showProgressFields = state.readingStatus == ReadingStatus.CURRENTLY_READING
                    binding.tilCurrentPage.showIf(showProgressFields)
                    binding.tilTotalPages.showIf(showProgressFields)

                    binding.lookupProgress.showIf(state.isLooking)
                    binding.btnLookupIsbn.isEnabled = !state.isLooking

                    val cover = state.coverImageUri
                    if (!cover.isNullOrBlank()) {
                        binding.cover.load(File(cover)) {
                            crossfade(true)
                            placeholder(R.drawable.bg_cover_placeholder)
                        }
                        binding.cover.background = null
                        binding.coverIcon.visibility = View.GONE
                        binding.btnRemoveCover.show()
                    } else {
                        binding.cover.setImageDrawable(null)
                        binding.cover.setBackgroundResource(R.drawable.bg_cover_placeholder)
                        binding.coverIcon.show()
                        binding.btnRemoveCover.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun observeValidation() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.validation.collect { errors ->
                    binding.tilTitle.error = errors.titleError
                    binding.tilAuthor.error = errors.authorError
                    binding.tilYear.error = errors.yearError
                    binding.tilTotalPages.error = errors.pagesError
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is EditEvent.Saved -> {
                            toast(
                                getString(
                                    if (event.wasEdit) R.string.snackbar_book_updated
                                    else R.string.snackbar_book_added
                                )
                            )
                            findNavController().popBackStack()
                        }
                        is EditEvent.LookupSucceeded -> {
                            toast(getString(R.string.addedit_isbn_lookup_success, event.title))
                        }
                        is EditEvent.LookupFailed -> toast(event.message)
                    }
                }
            }
        }
    }

    private fun onCoverPicked(uri: Uri) {
        val saved = ImageStorage.saveCoverImage(requireContext(), uri)
        if (saved == null) {
            toast("Couldn't load that image")
            return
        }
        // Replace previous cover file (if any) so we don't leak storage on repeated picks.
        val previous = viewModel.form.value.coverImageUri
        if (!previous.isNullOrBlank() && previous != saved) {
            ImageStorage.deleteCoverImage(previous)
        }
        viewModel.updateField { copy(coverImageUri = saved) }
    }

    /** Avoid feedback loops when programmatically setting EditText values. */
    private fun setIfDifferent(
        view: com.google.android.material.textfield.TextInputEditText,
        value: String,
        isEditable: Boolean = true
    ) {
        if (view.text?.toString() != value) {
            view.setText(value)
            if (isEditable) view.setSelection(value.length)
        }
    }

    private fun setIfDifferent(
        view: com.google.android.material.textfield.MaterialAutoCompleteTextView,
        value: String,
        isEditable: Boolean = false
    ) {
        if (view.text?.toString() != value) view.setText(value, false)
    }

    private fun setIfDifferent(
        view: com.google.android.material.textfield.TextInputEditText,
        value: Int
    ) = setIfDifferent(view, if (value > 0) value.toString() else "")

    private fun com.google.android.material.textfield.TextInputEditText.doAfterChange(
        callback: (String) -> Unit
    ) {
        addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                callback(s?.toString().orEmpty())
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
