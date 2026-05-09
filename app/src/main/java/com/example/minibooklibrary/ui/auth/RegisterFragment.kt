package com.example.minibooklibrary.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.minibooklibrary.databinding.FragmentRegisterBinding
import com.example.minibooklibrary.ui.common.ViewModelFactory
import com.example.minibooklibrary.util.toast
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels {
        ViewModelFactory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRegister.setOnClickListener {
            val username = binding.inputUsername.text?.toString().orEmpty()
            val email = binding.inputEmail.text?.toString().orEmpty()
            val password = binding.inputPassword.text?.toString().orEmpty()
            val confirm = binding.inputConfirmPassword.text?.toString().orEmpty()
            clearErrors()

            if (username.isBlank()) { binding.tilUsername.error = "Username is required"; return@setOnClickListener }
            if (email.isBlank()) { binding.tilEmail.error = "Email is required"; return@setOnClickListener }
            if (password.isBlank()) { binding.tilPassword.error = "Password is required"; return@setOnClickListener }
            if (confirm.isBlank()) { binding.tilConfirmPassword.error = "Confirm your password"; return@setOnClickListener }

            viewModel.register(username, email, password, confirm)
        }

        binding.btnBackToLogin.setOnClickListener { findNavController().popBackStack() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.btnRegister.isEnabled = !state.isLoading
                    binding.progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    state.errorMessage?.let { error ->
                        binding.tilPassword.error = error
                        viewModel.consumeError()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    if (event is AuthEvent.Registered) {
                        toast("Welcome, ${event.username} — please sign in.")
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun clearErrors() {
        binding.tilUsername.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
