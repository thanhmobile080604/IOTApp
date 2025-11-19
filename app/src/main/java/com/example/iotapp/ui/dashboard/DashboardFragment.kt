package com.example.iotapp.ui.dashboard

import android.widget.Toast
import com.example.iotapp.R
import com.example.iotapp.base.BaseFragment
import com.example.iotapp.base.setSingleClick
import com.example.iotapp.databinding.FragmentDashboardBinding
import com.example.iotapp.repository.AuthRepository

class DashboardFragment : BaseFragment<FragmentDashboardBinding>(FragmentDashboardBinding::inflate) {

    private val authRepository = AuthRepository()

    override fun FragmentDashboardBinding.initView() = Unit

    override fun FragmentDashboardBinding.initListener() {
        btnLogout.setSingleClick { performLogout() }
    }

    override fun initObserver() {
        mainViewModel.userEmail.observe(viewLifecycleOwner) { email ->
            binding.tvUserInfo.text =
                "Welcome, $email!\nYou have successfully signed up and verified your email."
        }

        mainViewModel.navigateToSignIn.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate == true) {
                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
                navigateTo(R.id.signInFragment)
                mainViewModel.navigateToSignIn.value = false
            }
        }
    }

    private fun performLogout() {
        authRepository.signOut()
        mainViewModel.apply {
            userEmail.value = ""
            fullName.value = ""
            password.value = ""
            isLoading.value = false
            errorMessage.value = ""
            navigateToDashboard.value = false
            navigateToSignIn.value = true
        }
    }
}
