package com.rahul.stocksim.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

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
                    onResult(false, task.exception?.localizedMessage)
                }
            }
    }

    fun register(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        // Initialize user balance with $1000 in Firestore
                        val userData = hashMapOf(
                            "balance" to 1000.0,
                            "email" to email
                        )
                        firestore.collection("users").document(user.uid).set(userData)
                            .addOnCompleteListener { firestoreTask ->
                                if (firestoreTask.isSuccessful) {
                                    onResult(true, null)
                                } else {
                                    onResult(false, firestoreTask.exception?.localizedMessage)
                                }
                            }
                    } else {
                        onResult(true, null)
                    }
                } else {
                    onResult(false, task.exception?.localizedMessage)
                }
            }
    }

    fun signInWithGoogle(idToken: String, onResult: (Boolean) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result?.additionalUserInfo?.isNewUser == true) {
                    val user = task.result?.user
                    if (user != null) {
                        val userData = hashMapOf(
                            "balance" to 1000.0,
                            "email" to user.email
                        )
                        firestore.collection("users").document(user.uid).set(userData)
                            .addOnCompleteListener { 
                                onResult(task.isSuccessful)
                            }
                    } else {
                        onResult(task.isSuccessful)
                    }
                } else {
                    onResult(task.isSuccessful)
                }
            }
    }

    suspend fun updateProfilePicture(imageUri: Uri): Result<Unit> {
        val user = currentUser ?: return Result.failure(Exception("User not authenticated"))
        
        return try {
            val storageRef = storage.reference.child("profile_pictures/${user.uid}")
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await()
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setPhotoUri(downloadUrl)
                .build()
            user.updateProfile(profileUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }
}