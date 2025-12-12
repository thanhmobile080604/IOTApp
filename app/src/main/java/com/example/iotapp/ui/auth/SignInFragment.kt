package com.example.iotapp.ui.auth

import android.util.Patterns
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.iotapp.R
import com.example.iotapp.base.BaseFragment
import com.example.iotapp.base.setSingleClick
import com.example.iotapp.databinding.FragmentSignInBinding
import com.example.iotapp.repository.AuthRepository
import kotlinx.coroutines.launch

class SignInFragment : BaseFragment<FragmentSignInBinding>(FragmentSignInBinding::inflate) {

    private val authRepository = AuthRepository()

    override fun FragmentSignInBinding.initView() = Unit

    override fun FragmentSignInBinding.initListener() {
        btnSignIn.setSingleClick { handleSignIn() }
        tvSignUp.setSingleClick {
            navigateTo(R.id.signUpFragment)
        }
    }

    override fun initObserver() {
        mainViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.takeIf { it.isNotBlank() }?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        mainViewModel.navigateToDashboard.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate == true) {
                navigateTo(R.id.homeFragment)
                mainViewModel.navigateToDashboard.value = false
            }
        }
    }

    private fun handleSignIn() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (!validateCredentials(email, password)) return

        lifecycleScope.launch {
            mainViewModel.apply {
                userEmail.value = email
                this.password.value = password
                errorMessage.value = ""
                isLoading.value = true
            }

            val result = authRepository.signInWithEmailAndPassword(email, password)
            result.fold(
                onSuccess = { user ->
                    mainViewModel.fullName.value = user.displayName ?: ""
                    mainViewModel.navigateToDashboard.value = true
                },
                onFailure = { exception ->
                    mainViewModel.errorMessage.value =
                        exception.message ?: "Unable to sign in. Please try again."
                }
            )

            mainViewModel.isLoading.value = false
        }
    }

    private fun validateCredentials(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        return isValid
    }
}