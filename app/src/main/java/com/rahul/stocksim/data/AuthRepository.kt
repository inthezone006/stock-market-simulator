package com.rahul.stocksim.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class AuthRepository {
    //create firebase auth private instance for the entry point of the auth, allowing app to communicate with the servers
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    //authenticate an existing user taking email password and callback function
    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            //when complete, it triggers add on success listener and passes status of the operation
            .addOnCompleteListener { task ->
                //uses on result lambda function
                if (task.isSuccessful) {
                onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage)
                }
            }
    }

    //create a new user
    fun register(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                //uses on result lambda function
                if (task.isSuccessful) {
                onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage)
                }
            }
    }

    //sign in with google
    fun signInWithGoogle(idToken: String, onResult: (Boolean) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful)
            }
    }
}