package com.example.iotapp.ui.auth

import android.widget.Toast
import androidx.fragment.app.viewModels
import com.example.iotapp.MainViewModel
import com.example.iotapp.R
import com.example.iotapp.base.BaseFragment
import com.example.iotapp.databinding.FragmentSignUpBinding

class SignUpFragment : BaseFragment<FragmentSignUpBinding>(FragmentSignUpBinding::inflate) {


    override fun FragmentSignUpBinding.initView() {
        // Initialize UI components if needed
    }

    override fun FragmentSignUpBinding.initListener() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.btnContinue.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            
            if (validateInputs(fullName, email)) {
                mainViewModel.setFullName(fullName)
                mainViewModel.setEmail(email)
                navigateTo(R.id.setPasswordFragment)
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
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Please enter a valid email"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        return isValid
    }
}
