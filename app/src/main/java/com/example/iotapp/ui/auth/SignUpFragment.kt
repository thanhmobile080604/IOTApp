package com.example.iotapp.ui.auth

import android.util.Patterns
import android.widget.Toast
import com.example.iotapp.R
import com.example.iotapp.base.BaseFragment
import com.example.iotapp.base.setSingleClick
import com.example.iotapp.databinding.FragmentSignUpBinding

class SignUpFragment : BaseFragment<FragmentSignUpBinding>(FragmentSignUpBinding::inflate) {

    override fun FragmentSignUpBinding.initView() = Unit

    override fun FragmentSignUpBinding.initListener() {
        btnContinue.setSingleClick { handleSignUp() }
        btnBack.setSingleClick {
            onBack()
        }
        tvSignIn.setSingleClick {
            onBack()
        }
    }

    override fun initObserver() {
        mainViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.takeIf { it.isNotBlank() }?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleSignUp() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()

        if (!validateInputs(fullName, email)) return

        mainViewModel.apply {
            this.fullName.value = fullName
            userEmail.value = email
            errorMessage.value = ""
        }

        Toast.makeText(
            requireContext(),
            "Information saved. Please set your password.",
            Toast.LENGTH_SHORT
        ).show()
        navigateTo(R.id.setPasswordFragment)
    }

    private fun validateInputs(fullName: String, email: String): Boolean {
        var isValid = true

        if (fullName.isEmpty()) {
            binding.tilFullName.error = "Full name is required"
            isValid = false
        } else {
            binding.tilFullName.error = null
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Please enter a valid email"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        return isValid
    }
}