package com.example.iotapp.ui.auth

import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.iotapp.R
import com.example.iotapp.base.BaseFragment
import com.example.iotapp.base.sendOtpEmail
import com.example.iotapp.base.setSingleClick
import com.example.iotapp.databinding.FragmentSetPasswordBinding
import com.example.iotapp.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class SetPasswordFragment :
    BaseFragment<FragmentSetPasswordBinding>(FragmentSetPasswordBinding::inflate) {

    private val authRepository = AuthRepository()

    override fun FragmentSetPasswordBinding.initView() {
        disableBackPress(true)
        lifecycleScope.launch {
            val email = mainViewModel.userEmail.value ?: ""
            authRepository.sendPasswordResetEmail(email)
        }
    }

    override fun FragmentSetPasswordBinding.initListener() {
        btnContinue.setSingleClick {
            navigateTo(R.id.signInFragment, inclusive = true)
        }
    }

    override fun initObserver() {
    }

    override fun onDestroyView() {
        navigateTo(R.id.signInFragment, inclusive = true)
        super.onDestroyView()
    }
}