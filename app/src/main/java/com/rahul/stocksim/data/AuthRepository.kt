package com.rahul.stocksim.data

import android.net.Uri
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

data class NotificationSettings(
    val masterEnabled: Boolean = true,
    val viaPush: Boolean = true,
    val notifyLargeDrop: Boolean = true,
    val notifyLowBalance: Boolean = true,
    val notifyNewSignIn: Boolean = true
)

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, "Invalid credentials.")
                }
            }
    }

    fun register(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, "Registration failed.")
                }
            }
    }

    fun signInWithGoogle(idToken: String, onResult: (Boolean, Boolean) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                val isNewUser = task.result?.additionalUserInfo?.isNewUser ?: false
                onResult(task.isSuccessful, isNewUser)
            }
    }

    suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            currentUser?.sendEmailVerification()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDisplayName(newName: String): Result<Unit> {
        val user = currentUser ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(newName).build()
            user.updateProfile(profileUpdates).await()
            // Sync to Firestore for leaderboard
            firestore.collection("users").document(user.uid).update("displayName", newName).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> {
        val user = currentUser ?: return Result.failure(Exception("Not authenticated"))
        return try {
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(password: String): Result<Unit> {
        val user = currentUser ?: return Result.failure(Exception("Not authenticated"))
        return try {
            // Re-authenticate user before deletion (industry standard)
            val credential = EmailAuthProvider.getCredential(user.email!!, password)
            user.reauthenticate(credential).await()
            
            val uid = user.uid
            // 1. Delete Firestore Data
            firestore.collection("users").document(uid).delete().await()
            
            // 2. Delete Storage Data (Profile Pic)
            try {
                storage.reference.child("profile_pictures/$uid").delete().await()
            } catch (e: Exception) {
                // Ignore if no profile pic exists
            }
            
            // 3. Delete Firebase Auth User
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setUserBalance(balance: Double, level: Int): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Session expired."))
        return try {
            val userRef = firestore.collection("users").document(userId)
            val userData = hashMapOf(
                "balance" to balance,
                "level" to level,
                "email" to auth.currentUser?.email,
                "displayName" to auth.currentUser?.displayName
            )
            userRef.set(userData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfilePicture(imageUri: Uri): Result<Unit> {
        val user = currentUser ?: return Result.failure(Exception("Auth error"))
        return try {
            val storageRef = storage.reference.child("profile_pictures/${user.uid}")
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await()
            val profileUpdates = UserProfileChangeRequest.Builder().setPhotoUri(downloadUrl).build()
            user.updateProfile(profileUpdates).await()
            // Sync to Firestore for leaderboard
            firestore.collection("users").document(user.uid).update("photoUrl", downloadUrl.toString()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotificationSettings(): NotificationSettings {
        val userId = auth.currentUser?.uid ?: return NotificationSettings()
        return try {
            val snapshot = firestore.collection("users").document(userId).get().await()
            val data = snapshot.data ?: return NotificationSettings()
            NotificationSettings(
                masterEnabled = data["notif_master"] as? Boolean ?: true,
                viaPush = data["notif_push"] as? Boolean ?: true,
                notifyLargeDrop = data["notif_large_drop"] as? Boolean ?: true,
                notifyLowBalance = data["notif_low_balance"] as? Boolean ?: true,
                notifyNewSignIn = data["notif_new_signin"] as? Boolean ?: true
            )
        } catch (e: Exception) {
            NotificationSettings()
        }
    }

    suspend fun saveNotificationSettings(settings: NotificationSettings): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val data = hashMapOf(
                "notif_master" to settings.masterEnabled,
                "notif_push" to settings.viaPush,
                "notif_large_drop" to settings.notifyLargeDrop,
                "notif_low_balance" to settings.notifyLowBalance,
                "notif_new_signin" to settings.notifyNewSignIn
            )
            firestore.collection("users").document(userId).update(data as Map<String, Any>).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }
}