package com.example.capstone_login.ui.auth

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
import com.example.capstone_login.R
import com.example.capstone_login.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

/**
 * Login screen fragment.
 * Wires the email/password form to AuthViewModel.
 * Navigates to CalendarFragment on Success state (Phase 2 wires actual navigation).
 *
 * ViewBinding is used — no findViewById calls.
 * ViewModel provided via by viewModels() — no Hilt required.
 */
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

        // Phase 2: wire login button click
        // binding.loginButton.setOnClickListener {
        //     val email = binding.emailEditText.text?.toString().orEmpty()
        //     val password = binding.passwordEditText.text?.toString().orEmpty()
        //     viewModel.login(email, password)
        // }

        // Observe auth state — Phase 2 adds navigation on Success
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AuthUiState.Loading -> {
                            // Phase 2: show progress, disable button
                        }
                        is AuthUiState.Success -> {
                            // Phase 2: navigate with popUpTo(loginFragment, inclusive=true)
                            // findNavController().navigate(
                            //     R.id.action_loginFragment_to_calendarFragment,
                            //     null,
                            //     NavOptions.Builder().setPopUpTo(R.id.loginFragment, true).build()
                            // )
                        }
                        is AuthUiState.Error -> {
                            // Phase 2: show error message
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Prevent memory leak — binding holds View references
    }
}
