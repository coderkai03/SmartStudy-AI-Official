package com.example.ai_flashcards

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

class SignupActivity : AppCompatActivity() {
    //inputs
    lateinit var toLogin: Button
    lateinit var signupEmail: EditText
    lateinit var signupPassword: EditText
    lateinit var signupBtn: Button
    lateinit var signupUname: EditText

    //Firebase
    lateinit var auth: FirebaseAuth
    lateinit var fs: FirebaseFirestore

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        //load firebase assets
        auth = FirebaseAuth.getInstance()
        fs = Firebase.firestore


        toLogin = findViewById(R.id.to_login)
        signupEmail = findViewById(R.id.signup_email)
        signupPassword = findViewById(R.id.signup_password)
        signupBtn = findViewById(R.id.signup_btn)
        signupUname = findViewById(R.id.signup_username)

        //go to signup screen
        toLogin.setOnClickListener{
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        //save email/password and go to topic screen
        signupBtn.setOnClickListener{
            val email = signupEmail.text.toString()
            val password = signupPassword.text.toString()
            val uname = signupUname.text.toString()

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Enter email", Toast.LENGTH_SHORT).show()
            }

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show()
            }

            if (TextUtils.isEmpty(uname)) {
                Toast.makeText(this, "Enter username", Toast.LENGTH_SHORT).show()
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // If sign in succeeds, display a message to the user.
                        Toast.makeText(
                            baseContext,
                            "Account created.",
                            Toast.LENGTH_SHORT,
                        ).show()

                        val user = hashMapOf(
                            "email" to email,
                            "username" to uname,
                            "password" to password
                        )

                        fs.collection("users")
                            .add(user)
                            .addOnSuccessListener { docRef ->
                                Log.d("SIGNUP", "DocSnapshot added with ID: ${docRef}")
                            }
                            .addOnFailureListener{ e ->
                                Log.d("SIGNUP", "Error adding doc", e)
                            }

                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.e(
                            "Registration",
                            "Authentication failed: ${task.exception?.message}"
                        )
                        Toast.makeText(
                            baseContext,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }
    }
}