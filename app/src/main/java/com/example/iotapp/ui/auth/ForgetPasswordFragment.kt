package com.example.iotapp.ui.auth

import android.util.Patterns
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.iotapp.R
import com.example.iotapp.base.BaseFragment
import com.example.iotapp.base.sendOtpEmail
import com.example.iotapp.base.setSingleClick
import com.example.iotapp.databinding.FragmentForgetPasswordBinding
import com.example.iotapp.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class ForgetPasswordFragment :
    BaseFragment<FragmentForgetPasswordBinding>(FragmentForgetPasswordBinding::inflate) {
    private val authRepository = AuthRepository()

    override fun FragmentForgetPasswordBinding.initView() {
    }

    override fun FragmentForgetPasswordBinding.initListener() {
        btnContinue.setSingleClick { handleEmailSubmission() }
        btnBack.setSingleClick { onBack() }
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

    private fun handleEmailSubmission() {
        val email = binding.etEmail.text.toString().trim()
        if (!validateInputs(email)) return

        lifecycleScope.launch {
            val isEmailAlreadyRegistered = authRepository.isEmailAlreadyRegistered(email)

            if (!isEmailAlreadyRegistered) {
                Toast.makeText(requireContext(), "Email is not registered", Toast.LENGTH_LONG).show()
                return@launch
            }
            sendOtpAndNavigate(email)
        }
    }



    private fun sendOtpAndNavigate(email: String) {
        val otp = Random.nextInt(1000, 9999).toString()

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                sendOtpEmail(email, otp.toInt())
            }
            mainViewModel.otp.value = otp
            mainViewModel.fromForgetPassword.value = true
            mainViewModel.userEmail.value = email
            Toast.makeText(requireContext(), "OTP sent to your email", Toast.LENGTH_SHORT).show()
            navigateTo(R.id.otpVerificationFragment)
        }
    }
    private fun validateInputs(
        email: String
    ): Boolean {
        var valid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Invalid email"
            valid = false
        } else binding.tilEmail.error = null

        return valid
    }
}
