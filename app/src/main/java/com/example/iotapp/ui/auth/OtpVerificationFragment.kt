package com.example.iotapp.ui.auth

import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.iotapp.MainViewModel
import com.example.iotapp.R
import com.example.iotapp.base.BaseFragment
import com.example.iotapp.databinding.FragmentOtpVerificationBinding

class OtpVerificationFragment : BaseFragment<FragmentOtpVerificationBinding>(FragmentOtpVerificationBinding::inflate) {
    private var countDownTimer: CountDownTimer? = null
    private var timeLeft: Long = 60 // 60 seconds

    override fun FragmentOtpVerificationBinding.initView() {
        setupOtpInputListeners()
        startCountdownTimer()
    }

    override fun FragmentOtpVerificationBinding.initListener() {
        binding.btnBack.setOnClickListener {
            onBack()
        }

        binding.btnVerify.setOnClickListener {
            val otp = getOtpCode()
            if (validateOtp(otp)) {
                mainViewModel.verifyOtp(otp)
            }
        }

        binding.tvSignIn.setOnClickListener {
            navigateTo(R.id.signInFragment)
        }
    }

    override fun initObserver() {
        mainViewModel.userEmail.observe(this) { email ->
            binding.tvEmail.text = email
        }

        mainViewModel.isLoading.observe(this) { isLoading ->
            binding.btnVerify.isEnabled = !isLoading
            binding.btnVerify.text = if (isLoading) "Verifying..." else "Verify"
        }

        mainViewModel.errorMessage.observe(this) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }

        mainViewModel.navigateToDashboard.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                navigateTo(R.id.dashboardFragment)
            }
        }
    }

    private fun setupOtpInputListeners() {
        val otpInputs = listOf(
            binding.etOtp1,
            binding.etOtp2,
            binding.etOtp3,
            binding.etOtp4
        )

        otpInputs.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.isNotEmpty() == true) {
                        // Move to next field
                        if (index < otpInputs.size - 1) {
                            otpInputs[index + 1].requestFocus()
                        } else {
                            // Last field, hide keyboard
                            editText.clearFocus()
                        }
                    }
                }
                
                override fun afterTextChanged(s: Editable?) {}
            })

            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL && editText.text?.isEmpty() == true) {
                    // Move to previous field on backspace
                    if (index > 0) {
                        otpInputs[index - 1].requestFocus()
                        otpInputs[index - 1].setText("")
                    }
                }
                false
            }
        }
    }

    private fun getOtpCode(): String {
        return "${binding.etOtp1.text}${binding.etOtp2.text}${binding.etOtp3.text}${binding.etOtp4.text}"
    }

    private fun validateOtp(otp: String): Boolean {
        return if (otp.length != 4) {
            Toast.makeText(requireContext(), "Please enter complete OTP", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    private fun startCountdownTimer() {
        countDownTimer = object : CountDownTimer(timeLeft * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft = millisUntilFinished / 1000
                binding.tvTimer.text = "${timeLeft}s"
            }

            override fun onFinish() {
                binding.tvTimer.text = "0s"
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }
}
