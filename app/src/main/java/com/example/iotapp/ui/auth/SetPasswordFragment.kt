package com.example.iotapp.ui.auth

import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.iotapp.R
import com.example.iotapp.base.BaseFragment
import com.example.iotapp.base.sendOtpEmail
import com.example.iotapp.base.setSingleClick
import com.example.iotapp.databinding.FragmentSetPasswordBinding
import com.example.iotapp.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class SetPasswordFragment :
    BaseFragment<FragmentSetPasswordBinding>(FragmentSetPasswordBinding::inflate) {

    private val authRepository = AuthRepository()

    override fun FragmentSetPasswordBinding.initView() {
        setupPasswordVisibilityToggle()
    }

    override fun FragmentSetPasswordBinding.initListener() {
        btnContinue.setSingleClick { handlePasswordSubmission() }
    }

    override fun initObserver() {
        mainViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnContinue.isEnabled = !isLoading
        }

        mainViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.takeIf { it.isNotBlank() }?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handlePasswordSubmission() {
        val newPassword = binding.etNewPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (!validatePasswords(newPassword, confirmPassword)) return

        val email = mainViewModel.userEmail.value
        val fullName = mainViewModel.fullName.value.orEmpty()

        if (email.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Missing email information", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            mainViewModel.apply {
                password.value = newPassword
                errorMessage.value = ""
                isLoading.value = true
            }

            val createResult = authRepository.createUserWithEmailAndPassword(email, newPassword)
            createResult.fold(
                onSuccess = { user ->
                    val saveResult = authRepository.saveUserToFirestore(user.uid, fullName, email)
                    saveResult.fold(
                        onSuccess = {
                            generateOtpAndNavigate()
                        },
                        onFailure = { exception ->
                            mainViewModel.errorMessage.value =
                                exception.message ?: "Unable to save user information."
                        }
                    )
                },
                onFailure = { exception ->
                    mainViewModel.errorMessage.value =
                        exception.message ?: "Failed to create user with Firebase."
                }
            )

            mainViewModel.isLoading.value = false
        }
    }

    private fun generateOtpAndNavigate() {
        val userEmail = mainViewModel.userEmail.value ?: return
        val randomOtp = Random.nextInt(1000, 9999 + 1)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                sendOtpEmail(userEmail, randomOtp)
            }
            mainViewModel.otp.value = randomOtp.toString()
            Toast.makeText(requireContext(), "OTP sent to your email.", Toast.LENGTH_SHORT).show()
            navigateTo(R.id.otpVerificationFragment)
        }
    }


    private fun FragmentSetPasswordBinding.setupPasswordVisibilityToggle() {
        tilNewPassword.setEndIconOnClickListener {
            togglePasswordVisibility(etNewPassword)
        }
        tilConfirmPassword.setEndIconOnClickListener {
            togglePasswordVisibility(etConfirmPassword)
        }
    }

    private fun togglePasswordVisibility(editText: android.widget.EditText) {
        val isPasswordVisible = editText.transformationMethod is HideReturnsTransformationMethod
        editText.transformationMethod = if (isPasswordVisible) {
            PasswordTransformationMethod.getInstance()
        } else {
            HideReturnsTransformationMethod.getInstance()
        }
        editText.setSelection(editText.text?.length ?: 0)
    }

    private fun validatePasswords(newPassword: String, confirmPassword: String): Boolean {
        var isValid = true

        when {
            newPassword.isEmpty() -> {
                binding.tilNewPassword.error = "Password is required"
                isValid = false
            }

            newPassword.length < 6 -> {
                binding.tilNewPassword.error = "Password must be at least 6 characters"
                isValid = false
            }

            else -> binding.tilNewPassword.error = null
        }

        when {
            confirmPassword.isEmpty() -> {
                binding.tilConfirmPassword.error = "Confirm password is required"
                isValid = false
            }

            confirmPassword != newPassword -> {
                binding.tilConfirmPassword.error = "Passwords do not match"
                isValid = false
            }

            else -> binding.tilConfirmPassword.error = null
        }

        return isValid
    }
}