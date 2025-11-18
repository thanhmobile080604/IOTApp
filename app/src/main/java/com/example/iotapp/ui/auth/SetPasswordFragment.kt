package com.example.iotapp.ui.auth

import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.iotapp.MainViewModel
import com.example.iotapp.R
import com.example.iotapp.base.BaseFragment
import com.example.iotapp.databinding.FragmentSetPasswordBinding

class SetPasswordFragment : BaseFragment<FragmentSetPasswordBinding>(FragmentSetPasswordBinding::inflate) {

    override fun FragmentSetPasswordBinding.initView() {
        setupPasswordVisibilityToggle()
    }

    override fun FragmentSetPasswordBinding.initListener() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.btnContinue.setOnClickListener {
            val newPassword = binding.etNewPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            
            if (validatePasswords(newPassword, confirmPassword)) {
                mainViewModel.setPassword(newPassword)
                mainViewModel.sendOTP()
                mainViewModel.createUserAccount()
            }
        }

        binding.tvSignIn.setOnClickListener {
            navigateTo(R.id.signInFragment)
        }

        binding.btnGoogle.setOnClickListener {
            Toast.makeText(requireContext(), "Google Sign In - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnApple.setOnClickListener {
            Toast.makeText(requireContext(), "Apple Sign In - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnFacebook.setOnClickListener {
            Toast.makeText(requireContext(), "Facebook Sign In - Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }

    override fun initObserver() {
        mainViewModel.isLoading.observe(this) { isLoading ->
            binding.btnContinue.isEnabled = !isLoading
            binding.btnContinue.text = if (isLoading) "Creating Account..." else "Continue"
        }

        mainViewModel.errorMessage.observe(this) { error ->
            if (error.isNotEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }

        mainViewModel.navigateToOtp.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                navigateTo(R.id.otpVerificationFragment)
            }
        }
    }

    private fun setupPasswordVisibilityToggle() {
        binding.tilNewPassword.setEndIconOnClickListener {
            togglePasswordVisibility(binding.etNewPassword)
        }

        binding.tilConfirmPassword.setEndIconOnClickListener {
            togglePasswordVisibility(binding.etConfirmPassword)
        }
    }

    private fun togglePasswordVisibility(editText: com.google.android.material.textfield.TextInputEditText) {
        val currentInputType = editText.inputType
        if (currentInputType == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD or android.text.InputType.TYPE_CLASS_TEXT) {
            editText.inputType = android.text.InputType.TYPE_CLASS_TEXT
        } else {
            editText.inputType = android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD or android.text.InputType.TYPE_CLASS_TEXT
        }
        editText.setSelection(editText.text?.length ?: 0)
    }

    private fun validatePasswords(newPassword: String, confirmPassword: String): Boolean {
        var isValid = true

        if (newPassword.isEmpty()) {
            binding.tilNewPassword.error = "Password is required"
            isValid = false
        } else if (newPassword.length < 6) {
            binding.tilNewPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.tilNewPassword.error = null
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm your password"
            isValid = false
        } else if (newPassword != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        return isValid
    }
}
