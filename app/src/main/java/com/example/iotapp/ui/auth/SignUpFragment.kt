package com.example.iotapp.ui.auth

import android.util.Patterns
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.iotapp.R
import com.example.iotapp.base.BaseFragment
import com.example.iotapp.base.sendOtpEmail
import com.example.iotapp.base.setSingleClick
import com.example.iotapp.databinding.FragmentSignUpBinding
import com.example.iotapp.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class SignUpFragment :
    BaseFragment<FragmentSignUpBinding>(FragmentSignUpBinding::inflate) {

    private val authRepository = AuthRepository()

    override fun FragmentSignUpBinding.initView() = Unit

    override fun FragmentSignUpBinding.initListener() {
        btnContinue.setSingleClick { handleSignUp() }
        btnBack.setSingleClick { onBack() }
        tvSignIn.setSingleClick { onBack() }
    }

    override fun initObserver() {
        mainViewModel.isLoading.observe(viewLifecycleOwner) {
            binding.btnContinue.isEnabled = !it
        }

        mainViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.takeIf { it.isNotBlank() }?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleSignUp() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etNewPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (!validateInputs(fullName, email, password, confirmPassword)) return

        lifecycleScope.launch {
            mainViewModel.apply {
                isLoading.value = true
                errorMessage.value = ""
            }

            val createResult =
                authRepository.createUserWithEmailAndPassword(email, password)

            createResult.fold(
                onSuccess = { user ->
                    val saveResult =
                        authRepository.saveUserToFirestore(user.uid, fullName, email)

                    saveResult.fold(
                        onSuccess = {
                            sendOtpAndNavigate(email)
                        },
                        onFailure = {
                            mainViewModel.errorMessage.value =
                                it.message ?: "Save user failed"
                        }
                    )
                },
                onFailure = {
                    mainViewModel.errorMessage.value =
                        it.message ?: "Create user failed"
                }
            )

            mainViewModel.isLoading.value = false
        }
    }

    private fun sendOtpAndNavigate(email: String) {
        val otp = Random.nextInt(1000, 9999).toString()

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                sendOtpEmail(email, otp.toInt())
            }
            mainViewModel.otp.value = otp
            Toast.makeText(requireContext(), "OTP sent to your email", Toast.LENGTH_SHORT).show()
            navigateTo(R.id.otpVerificationFragment)
        }
    }
    private fun validateInputs(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var valid = true

        if (fullName.isEmpty()) {
            binding.tilFullName.error = "Full name is required"
            valid = false
        } else binding.tilFullName.error = null

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Invalid email"
            valid = false
        } else binding.tilEmail.error = null

        if (password.length < 6) {
            binding.etNewPassword.error = "Minimum 6 characters"
            valid = false
        } else binding.etNewPassword.error = null

        if (confirmPassword != password) {
            binding.tilConfirmPassword.error = "Password not match"
            valid = false
        } else binding.tilConfirmPassword.error = null

        return valid
    }
}
