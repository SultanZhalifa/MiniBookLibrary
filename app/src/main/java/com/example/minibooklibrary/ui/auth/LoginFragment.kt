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
import com.example.minibooklibrary.R
import com.example.minibooklibrary.databinding.FragmentLoginBinding
import com.example.minibooklibrary.ui.common.ViewModelFactory
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels {
        ViewModelFactory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            val username = binding.inputUsername.text?.toString().orEmpty()
            val password = binding.inputPassword.text?.toString().orEmpty()
            // Clear inline error visuals before re-validating.
            binding.tilUsername.error = null
            binding.tilPassword.error = null

            if (username.isBlank()) {
                binding.tilUsername.error = "Username is required"
                return@setOnClickListener
            }
            if (password.isBlank()) {
                binding.tilPassword.error = "Password is required"
                return@setOnClickListener
            }
            viewModel.login(username, password)
        }

        binding.btnGoRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.btnLogin.isEnabled = !state.isLoading
                    binding.progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    state.errorMessage?.let { error ->
                        binding.tilPassword.error = error
                        viewModel.consumeError()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
