package com.example.iotapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iotapp.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import papaya.`in`.sendmail.SendMail
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    // User data

    private val authRepository: AuthRepository = AuthRepository()
    private val _userEmail = MutableLiveData<String>()
    val userEmail: LiveData<String> = _userEmail

    private val _fullName = MutableLiveData<String>()
    val fullName: LiveData<String> = _fullName

    private val _password = MutableLiveData<String>()
    val password: LiveData<String> = _password

    // UI state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // Navigation
    private val _navigateToOtp = MutableLiveData<Boolean>()
    val navigateToOtp: LiveData<Boolean> = _navigateToOtp

    private val _navigateToDashboard = MutableLiveData<Boolean>()
    val navigateToDashboard: LiveData<Boolean> = _navigateToDashboard

    private val _navigateToSignIn = MutableLiveData<Boolean>()
    val navigateToSignIn: LiveData<Boolean> = _navigateToSignIn

    private val _otp = MutableLiveData<String>()
    val otp: LiveData<String> = _otp

    // Set user data methods
    fun setEmail(email: String) {
        _userEmail.value = email
    }

    fun setFullName(fullName: String) {
        _fullName.value = fullName
    }

    fun setPassword(password: String) {
        _password.value = password
    }

    // Authentication methods
    fun createUserAccount() {
        val email = _userEmail.value ?: return
        val password = _password.value ?: return
        val fullName = _fullName.value ?: ""

        _isLoading.value = true
        _errorMessage.value = ""

        viewModelScope.launch {
            try {
                val result = authRepository?.createUserWithEmailAndPassword(email, password)
                result?.fold(
                    onSuccess = { user ->
                        // Save user data to Firestore
                        authRepository.saveUserToFirestore(user.uid, fullName, email)
                        _navigateToOtp.value = true
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message ?: "Failed to create account"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signInWithEmail() {
        val email = _userEmail.value ?: return
        val password = _password.value ?: return

        _isLoading.value = true
        _errorMessage.value = ""

        viewModelScope.launch {
            try {
                val result = authRepository?.signInWithEmailAndPassword(email, password)
                result?.fold(
                    onSuccess = { user ->
                        if (user.isEmailVerified) {
                            _navigateToDashboard.value = true
                        } else {
                            _navigateToOtp.value = true
                        }
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message ?: "Failed to sign in"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendOTP() {
        val email = _userEmail.value ?: return
        var random = 0
        random = Random.nextInt(100000, 999999 + 1)
        _otp.value = random.toString()
        var mail = SendMail(
            "thanh08062004@gmail.com",
            "dbipfpjbbbhxgvfa",
            email,
            "Login Signup app's OTP",
            "Your OTP is -> $random"
        )
        mail.execute()
    }

    fun verifyOtp(otp: String) {
        _isLoading.value = true
        _errorMessage.value = ""

        viewModelScope.launch {
            try {
                // Simulate OTP verification (demo: use "1234")
                val _otp = _otp.value ?: return@launch
                if (otp == _otp) {
                    val user = authRepository?.getCurrentUser()
                    user?.let { firebaseUser ->
                        authRepository?.updateUserVerificationStatus(firebaseUser.uid, true)
                        _navigateToDashboard.value = true
                    }
                } else {
                    _errorMessage.value = "Invalid OTP. Please try again."
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        authRepository?.signOut()
        _navigateToSignIn.value = true
    }

    // Reset navigation flags
    fun resetNavigationFlags() {
        _navigateToOtp.value = false
        _navigateToDashboard.value = false
        _navigateToSignIn.value = false
    }

    // Clear error message
    fun clearErrorMessage() {
        _errorMessage.value = ""
    }

    // Check if user is signed in
    fun isUserSignedIn(): Boolean {
        return authRepository!!.isUserSignedIn()
    }

    // Get current user email
    fun getCurrentUserEmail(): String? {
        return authRepository!!.getCurrentUser()?.email
    }

    // Initialize user data if already signed in
    fun initializeUserData() {
        if (isUserSignedIn()) {
            getCurrentUserEmail()?.let { email ->
                _userEmail.value = email
            }
        }
    }
}
