package com.example.iotapp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.iotapp.model.PlantInformation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    // User data
    val userEmail = MutableLiveData<String>()
    val fullName = MutableLiveData<String>()
    val password = MutableLiveData<String>()

    // UI state
    val isLoading = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String>()

    // Navigation
    val navigateToDashboard = MutableLiveData<Boolean>()
    val otp = MutableLiveData<String>()
    val fromForgetPassword = MutableLiveData<Boolean>()

    val fireBaseInformation = MutableLiveData<PlantInformation?>()
    val isNetworkAvailable = MutableLiveData<Boolean>()

    fun resetViewModel(){
        userEmail.value = ""
        fullName.value = ""
        password.value = ""
        isLoading.value = false
        errorMessage.value = ""
        navigateToDashboard.value = false
        otp.value = ""
        fromForgetPassword.value = false
        fireBaseInformation.value = null
    }
 }
