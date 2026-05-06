package com.example.capstone_login.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.capstone_login.R
import com.example.capstone_login.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

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
        setupClickListeners()
        collectUiState()
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text?.toString()?.trim().orEmpty()
            val password = binding.passwordEditText.text?.toString().orEmpty()
            val error = validateInput(email, password)
            if (error != null) {
                binding.errorTextView.text = error
                binding.errorTextView.isVisible = true
                return@setOnClickListener
            }
            viewModel.login(email, password)
        }

        binding.registerButton.setOnClickListener {
            val email = binding.emailEditText.text?.toString()?.trim().orEmpty()
            val password = binding.passwordEditText.text?.toString().orEmpty()
            val error = validateInput(email, password)
            if (error != null) {
                binding.errorTextView.text = error
                binding.errorTextView.isVisible = true
                return@setOnClickListener
            }
            viewModel.register(email, password)
        }
    }

    private fun validateInput(email: String, password: String): String? {
        if (email.isBlank()) return "이메일을 입력하세요."
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "이메일 형식이 올바르지 않습니다."
        }
        if (password.length < 6) return "비밀번호는 6자 이상이어야 합니다."
        return null
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AuthUiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.loginButton.isEnabled = false
                            binding.registerButton.isEnabled = false
                            binding.errorTextView.isVisible = false
                        }
                        is AuthUiState.Success -> {
                            binding.progressBar.isVisible = false
                            // resetState() before navigate prevents duplicate navigation
                            // when repeatOnLifecycle restarts (e.g., screen rotation).
                            viewModel.resetState()
                            findNavController().navigate(
                                R.id.action_loginFragment_to_calendarFragment,
                                null,
                                NavOptions.Builder()
                                    .setPopUpTo(R.id.loginFragment, true)
                                    .build()
                            )
                        }
                        is AuthUiState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.loginButton.isEnabled = true
                            binding.registerButton.isEnabled = true
                            binding.errorTextView.text = state.message
                            binding.errorTextView.isVisible = true
                        }
                        is AuthUiState.Idle -> {
                            binding.progressBar.isVisible = false
                            binding.loginButton.isEnabled = true
                            binding.registerButton.isEnabled = true
                            binding.errorTextView.isVisible = false
                        }
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
