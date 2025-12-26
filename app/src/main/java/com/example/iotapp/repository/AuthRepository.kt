package com.example.iotapp.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
class AuthRepository {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    companion object {
        private const val TAG = "AuthRepository"
    }

    suspend fun createUserWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                // Send email verification
                user.sendEmailVerification().await()
                Log.d(TAG, "User created and verification email sent")
                kotlin.Result.success(user)
            } ?: kotlin.Result.failure(Exception("User creation failed"))
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user", e)
            kotlin.Result.failure(e)
        }
    }

    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                Log.d(TAG, "User signed in successfully")
                kotlin.Result.success(user)
            } ?: kotlin.Result.failure(Exception("Sign in failed"))
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in", e)
            kotlin.Result.failure(e)
        }
    }

    suspend fun saveUserToFirestore(uid: String, fullName: String, email: String): Result<Unit> {
        return try {
            val userData = hashMapOf(
                "uid" to uid,
                "fullName" to fullName,
                "email" to email,
                "createdAt" to com.google.firebase.Timestamp.now(),
                "isVerified" to false
            )

            firestore.collection("users")
                .document(uid)
                .set(userData)
                .await()
            
            Log.d(TAG, "User data saved to Firestore")
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user data", e)
            kotlin.Result.failure(e)
        }
    }

    suspend fun updateUserVerificationStatus(uid: String, isVerified: Boolean): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(uid)
                .update("isVerified", isVerified)
                .await()
            
            Log.d(TAG, "User verification status updated")
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating verification status", e)
            kotlin.Result.failure(e)
        }
    }

    suspend fun getUserData(uid: String): Result<Map<String, Any>?> {
        return try {
            val document = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            
            if (document.exists()) {
                kotlin.Result.success(document.data)
            } else {
                kotlin.Result.failure(Exception("User document not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user data", e)
            kotlin.Result.failure(e)
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun signOut() {
        auth.signOut()
        Log.d(TAG, "User signed out")
    }

    fun isUserSignedIn(): Boolean {
        return auth.currentUser != null
    }

    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified ?: false
    }
}
