package com.example.minibooklibrary.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.minibooklibrary.BuildConfig
import com.example.minibooklibrary.R
import com.example.minibooklibrary.databinding.FragmentSettingsBinding
import com.example.minibooklibrary.ui.common.ViewModelFactory
import com.example.minibooklibrary.ui.common.applySystemBarInsets
import com.example.minibooklibrary.ui.common.toast
import com.example.minibooklibrary.ui.main.MainActivity
import com.example.minibooklibrary.ui.widget.WidgetRefresher
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        ViewModelFactory(requireContext())
    }

    /** Lets the user pick any JSON file from storage for the Restore action. */
    private val pickRestoreLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) viewModel.importBackup(requireContext(), uri)
    }

    // The system save dialog picks the destination, so exports work on every API level
    // without WRITE_EXTERNAL_STORAGE (which the old Downloads path needed below API 29).
    private val createPdfLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument(SettingsViewModel.MIME_PDF)
    ) { uri: Uri? ->
        if (uri != null) viewModel.exportPdf(requireContext(), uri)
    }

    private val createBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument(SettingsViewModel.MIME_JSON)
    ) { uri: Uri? ->
        if (uri != null) viewModel.exportBackup(requireContext(), uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.header.applySystemBarInsets(top = true)

        // Theme toggle group: maps Material's chip ids to AppCompatDelegate constants.
        binding.themeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                R.id.themeLight -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.themeDark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            viewModel.setThemeMode(mode)
        }

        // Populate each settings row template (title, summary, icon) — they share one layout.
        with(binding.rowExportPdf) {
            title.setText(R.string.settings_export_pdf)
            summary.setText(R.string.settings_export_pdf_summary)
            icon.setImageResource(R.drawable.ic_pdf)
            root.setOnClickListener { createPdfLauncher.launch(viewModel.suggestedFileName("pdf")) }
        }
        with(binding.rowBackup) {
            title.setText(R.string.settings_backup)
            summary.setText(R.string.settings_backup_summary)
            icon.setImageResource(R.drawable.ic_backup)
            root.setOnClickListener {
                createBackupLauncher.launch(viewModel.suggestedFileName("json", prefix = "MiniBookLibrary_backup"))
            }
        }
        with(binding.rowRestore) {
            title.setText(R.string.settings_restore)
            summary.setText(R.string.settings_restore_summary)
            icon.setImageResource(R.drawable.ic_restore)
            root.setOnClickListener { pickRestoreLauncher.launch(RESTORE_MIME_TYPES) }
        }
        binding.btnLogout.setOnClickListener { confirmLogout() }
        binding.aboutFooter.text = getString(R.string.settings_about_footer, BuildConfig.VERSION_NAME)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect(::handleEvent)
            }
        }
    }

    private fun render(state: SettingsUiState) {
        val themeId = when (state.themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> R.id.themeLight
            AppCompatDelegate.MODE_NIGHT_YES -> R.id.themeDark
            else -> R.id.themeSystem
        }
        if (binding.themeToggle.checkedButtonId != themeId) {
            binding.themeToggle.check(themeId)
        }
        binding.signedInAs.text = getString(
            R.string.settings_signed_in_as,
            state.username.ifEmpty { getString(R.string.settings_signed_in_fallback) }
        )
        binding.progress.visibility = if (state.isWorking) View.VISIBLE else View.GONE
    }

    private fun handleEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.Exported -> {
                Snackbar.make(binding.root, event.messageRes, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_open) { openFile(event.uri, event.mimeType) }
                    .show()
            }
            is SettingsEvent.RestoreCompleted -> {
                WidgetRefresher.refresh(requireContext())
                toast(
                    resources.getQuantityString(
                        R.plurals.snackbar_restore_done, event.importedCount, event.importedCount
                    )
                )
            }
            is SettingsEvent.Error -> toast(event.messageRes)
            SettingsEvent.LoggedOut -> {
                // The widget would otherwise keep showing the signed-out user's book.
                WidgetRefresher.refresh(requireContext())
                (activity as? MainActivity)?.routeBackToAuth()
            }
        }
    }

    private fun openFile(uri: Uri, mime: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(intent) }.onFailure { toast(R.string.settings_no_app_to_open) }
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_logout_title)
            .setMessage(R.string.confirm_logout_body)
            .setPositiveButton(R.string.settings_logout) { _, _ -> viewModel.logout() }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        // Plenty of file managers report .json as octet-stream; without it the backup is greyed out.
        val RESTORE_MIME_TYPES = arrayOf(SettingsViewModel.MIME_JSON, "application/octet-stream")
    }
}
