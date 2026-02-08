package com.rahul.stocksim.data

import android.net.Uri
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
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
    private val analytics = Firebase.analytics

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    private val defaultWatchlistSymbols = listOf(
        "AAPL", "GOOGL", "MSFT", "AMZN", "TSLA",
        "META", "NVDA", "NFLX", "AMD", "PYPL",
        "INTC", "CSCO", "ADBE", "CRM", "QCOM"
    )

    private fun logEventWithUser(eventName: String, bundle: Bundle = Bundle()) {
        val user = auth.currentUser
        bundle.apply {
            putString("user_id", user?.uid ?: "anonymous")
            putString("user_name", user?.displayName ?: "anonymous")
            putString("user_email", user?.email ?: "anonymous")
        }
        analytics.logEvent(eventName, bundle)
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    logEventWithUser(FirebaseAnalytics.Event.LOGIN, Bundle().apply {
                        putString(FirebaseAnalytics.Param.METHOD, "email")
                    })
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
                    logEventWithUser(FirebaseAnalytics.Event.SIGN_UP, Bundle().apply {
                        putString(FirebaseAnalytics.Param.METHOD, "email")
                    })
                    onResult(true, null)
                } else {
                    val error = task.exception?.localizedMessage ?: "Registration failed."
                    onResult(false, error)
                }
            }
    }

    fun signInWithGoogle(idToken: String, onResult: (Boolean, Boolean) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                val isNewUser = task.result?.additionalUserInfo?.isNewUser ?: false
                if (task.isSuccessful) {
                    val event = if (isNewUser) FirebaseAnalytics.Event.SIGN_UP else FirebaseAnalytics.Event.LOGIN
                    logEventWithUser(event, Bundle().apply {
                        putString(FirebaseAnalytics.Param.METHOD, "google")
                    })
                }
                onResult(task.isSuccessful, isNewUser)
            }
    }

    suspend fun checkEmailExists(email: String): Boolean {
        return try {
            val result = auth.fetchSignInMethodsForEmail(email).await()
            result.signInMethods?.isNotEmpty() == true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            currentUser?.sendEmailVerification()?.await()
            logEventWithUser("send_email_verification")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDisplayName(newName: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Account not authenticated"))
        return try {
            val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(newName).build()
            user.updateProfile(profileUpdates).await()
            firestore.collection("users").document(user.uid)
                .set(mapOf("displayName" to newName), SetOptions.merge()).await()
            logEventWithUser("update_display_name", Bundle().apply {
                putString("new_name", newName)
            })
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Account not authenticated"))
        return try {
            user.updatePassword(newPassword).await()
            logEventWithUser("update_password")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(password: String): Result<Unit> {
        val user = currentUser ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val credential = EmailAuthProvider.getCredential(user.email!!, password)
            user.reauthenticate(credential).await()
            
            val uid = user.uid
            logEventWithUser("delete_account")
            firestore.collection("users").document(uid).delete().await()
            
            try {
                storage.reference.child("profile_pictures/$uid").delete().await()
            } catch (e: Exception) {}
            
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setUserBalance(balance: Double, level: Int): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Account not authenticated"))
        return try {
            val userRef = firestore.collection("users").document(user.uid)
            val userData = hashMapOf(
                "balance" to balance,
                "level" to level,
                "email" to user.email,
                "displayName" to user.displayName,
                "photoUrl" to user.photoUrl?.toString()
            )
            userRef.set(userData, SetOptions.merge()).await()

            val batch = firestore.batch()
            defaultWatchlistSymbols.forEach { symbol ->
                val watchlistRef = userRef.collection("watchlist").document(symbol)
                batch.set(watchlistRef, mapOf("symbol" to symbol))
            }
            batch.commit().await()

            logEventWithUser("select_difficulty_level", Bundle().apply {
                putInt("level", level)
                putDouble("initial_balance", balance)
            })

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfilePicture(imageUri: Uri): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Account not authenticated"))
        return try {
            val storageRef = storage.reference.child("profile_pictures/${user.uid}")
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await()
            val profileUpdates = UserProfileChangeRequest.Builder().setPhotoUri(downloadUrl).build()
            user.updateProfile(profileUpdates).await()
            
            firestore.collection("users").document(user.uid)
                .set(mapOf("photoUrl" to downloadUrl.toString()), SetOptions.merge()).await()
            logEventWithUser("update_profile_picture")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotificationSettings(): NotificationSettings {
        val user = auth.currentUser ?: return NotificationSettings()
        return try {
            val snapshot = firestore.collection("users").document(user.uid).get().await()
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
        val user = auth.currentUser ?: return Result.failure(Exception("Account not authenticated"))
        return try {
            val data = hashMapOf(
                "notif_master" to settings.masterEnabled,
                "notif_push" to settings.viaPush,
                "notif_large_drop" to settings.notifyLargeDrop,
                "notif_low_balance" to settings.notifyLowBalance,
                "notif_new_signin" to settings.notifyNewSignIn
            )
            firestore.collection("users").document(user.uid).update(data as Map<String, Any>).await()
            logEventWithUser("update_notification_settings")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        logEventWithUser("logout")
        auth.signOut()
    }
}