package com.example.iotapp.ui.dashboard

import android.widget.Toast
import androidx.fragment.app.viewModels
import com.example.iotapp.MainViewModel
import com.example.iotapp.R
import com.example.iotapp.base.BaseFragment
import com.example.iotapp.databinding.FragmentDashboardBinding

class DashboardFragment : BaseFragment<FragmentDashboardBinding>(FragmentDashboardBinding::inflate) {

    override fun FragmentDashboardBinding.initView() {
        // Initialize UI components if needed
    }

    override fun FragmentDashboardBinding.initListener() {
        binding.btnLogout.setOnClickListener {
            mainViewModel.logout()
        }
    }

    override fun initObserver() {
        mainViewModel.userEmail.observe(this) { email ->
            binding.tvUserInfo.text = "Welcome, $email!\nYou have successfully signed up and verified your email."
        }

        mainViewModel.navigateToSignIn.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
                navigateTo(R.id.signInFragment)
            }
        }
    }
}
