package com.example.iotapp.ui.auth

import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.iotapp.R
import com.example.iotapp.base.BaseFragment
import com.example.iotapp.base.sendOtpEmail
import com.example.iotapp.base.setSingleClick
import com.example.iotapp.databinding.FragmentOtpVerificationBinding
import com.example.iotapp.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class OtpVerificationFragment : BaseFragment<FragmentOtpVerificationBinding>(FragmentOtpVerificationBinding::inflate) {

    private var resendTimer: CountDownTimer? = null
    private var canResendOtp = false
    private val authRepository = AuthRepository()

    override fun FragmentOtpVerificationBinding.initView() {
        setupOtpInputListeners()
        updateVerifyButtonState()
        startResendCountdown()
    }

    override fun FragmentOtpVerificationBinding.initListener() {
        btnVerify.setSingleClick { verifyOtp(getOtpCode()) }
        tvTimer.setSingleClick {
            if (canResendOtp) resendOtp()
        }
    }

    override fun initObserver() {
        mainViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnVerify.isEnabled = !isLoading && binding.getOtpCode().length == OTP_LENGTH
        }

        mainViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.takeIf { it.isNotBlank() }?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }

        mainViewModel.navigateToDashboard.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate == true) {
                navigateTo(R.id.dashboardFragment)
                mainViewModel.navigateToDashboard.value = false
            }
        }
    }

    private fun resendOtp() {
        canResendOtp = false
        val userEmail = mainViewModel.userEmail.value ?: return
        val randomOtp = Random.nextInt(1000, 9999 + 1)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                sendOtpEmail(userEmail, randomOtp)
            }
            mainViewModel.otp.value = randomOtp.toString()
        }
        Toast.makeText(requireContext(), "New OTP sent: $randomOtp", Toast.LENGTH_SHORT).show()
        binding.clearOtpInputs()
        binding.startResendCountdown()
    }

    private fun verifyOtp(otpInput: String) {
        if (otpInput.length < OTP_LENGTH) {
            mainViewModel.errorMessage.value = "Please enter the complete OTP."
            return
        }

        val currentOtp = mainViewModel.otp.value
        if (currentOtp.isNullOrEmpty()) {
            mainViewModel.errorMessage.value = "OTP not generated. Please request a new one."
            return
        }

        lifecycleScope.launch {
            mainViewModel.isLoading.value = true
            mainViewModel.errorMessage.value = ""

            if (otpInput == currentOtp) {
                val firebaseUser = authRepository.getCurrentUser()
                firebaseUser?.let { user ->
                    authRepository.updateUserVerificationStatus(user.uid, true)
                        .onFailure { exception ->
                            mainViewModel.errorMessage.value =
                                exception.message ?: "Failed to update verification status."
                        }
                }
                mainViewModel.navigateToDashboard.value = true
            } else {
                mainViewModel.errorMessage.value = "Invalid OTP. Please try again."
            }

            mainViewModel.isLoading.value = false
        }
    }

    private fun FragmentOtpVerificationBinding.setupOtpInputListeners() {
        val fields = listOf(etOtp1, etOtp2, etOtp3, etOtp4)
        fields.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

                override fun afterTextChanged(editable: Editable?) {
                    if (editable?.length == 1 && index < fields.lastIndex) {
                        fields[index + 1].requestFocus()
                    }
                    updateVerifyButtonState()
                }
            })

            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    editText.text?.isEmpty() == true &&
                    index > 0
                ) {
                    fields[index - 1].apply {
                        requestFocus()
                        setSelection(text?.length ?: 0)
                    }
                    updateVerifyButtonState()
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun FragmentOtpVerificationBinding.clearOtpInputs() {
        listOf(etOtp1, etOtp2, etOtp3, etOtp4).forEach { it.text?.clear() }
        etOtp1.requestFocus()
        updateVerifyButtonState()
    }

    private fun FragmentOtpVerificationBinding.startResendCountdown() {
        resendTimer?.cancel()
        canResendOtp = false
        tvTimer.text = "59s"
        resendTimer = object : CountDownTimer(RESEND_INTERVAL_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvTimer.text = "${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                canResendOtp = true
                tvTimer.text = "Resend"
            }
        }.start()
    }

    private fun FragmentOtpVerificationBinding.getOtpCode(): String {
        return listOf(etOtp1, etOtp2, etOtp3, etOtp4)
            .joinToString(separator = "") { it.text?.toString().orEmpty() }
    }

    private fun updateVerifyButtonState() {
        binding.tvEmail.text = mainViewModel.userEmail.value
        binding.btnVerify.isEnabled =
            binding.getOtpCode().length == OTP_LENGTH && (mainViewModel.isLoading.value != true)
    }

    companion object {
        private const val OTP_LENGTH = 4
        private const val RESEND_INTERVAL_MS = 60_000L
    }

    override fun onDestroyView() {
        super.onDestroyView()
        resendTimer?.cancel()
        resendTimer = null
        canResendOtp = false
    }
}