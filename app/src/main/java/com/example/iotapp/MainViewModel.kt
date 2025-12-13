package com.example.iotapp

import androidx.lifecycle.LiveData
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
    val navigateToOtp = MutableLiveData<Boolean>()
    val navigateToDashboard = MutableLiveData<Boolean>()
    val navigateToSignIn = MutableLiveData<Boolean>()
    val otp = MutableLiveData<String>()

    val fireBaseInformation = MutableLiveData<PlantInformation>()
}
