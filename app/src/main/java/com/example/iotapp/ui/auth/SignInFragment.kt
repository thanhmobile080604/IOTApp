package com.example.iotapp.ui.auth

import android.widget.Toast
import androidx.fragment.app.viewModels
import com.example.iotapp.MainViewModel
import com.example.iotapp.R
import com.example.iotapp.base.BaseFragment
import com.example.iotapp.databinding.FragmentSignInBinding

class SignInFragment : BaseFragment<FragmentSignInBinding>(FragmentSignInBinding::inflate) {

    override fun FragmentSignInBinding.initView() {
        // Initialize UI components if needed
    }

    override fun FragmentSignInBinding.initListener() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.btnContinue.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (validateEmail(email)) {
                mainViewModel.setEmail(email)
                navigateTo(R.id.setPasswordFragment)
            }
        }

        binding.tvSignUp.setOnClickListener {
            navigateTo(R.id.signUpFragment)
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

    private fun validateEmail(email: String): Boolean {
        return if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Please enter a valid email"
            false
        } else {
            binding.tilEmail.error = null
            true
        }
    }
}
