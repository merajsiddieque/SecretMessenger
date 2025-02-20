package com.app.secretmessenger

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SignUp : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth  // Firebase Authentication instance

    // List of allowed email domains
    private val allowedDomains = listOf(
        "@gmail.com",
        "@mail.com",
        "@outlook.com",
        "@hotmail.com",
        "@live.com",
        "@yahoo.com",
        "@ymail.com",
        "@icloud.com",
        "@me.com",
        "@mac.com",
        "@protonmail.com",
        "@zoho.com"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Get references to UI elements
        val etEmail = findViewById<EditText>(R.id.etSignUpEmail)
        val etPassword = findViewById<EditText>(R.id.etSignUpPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmSignUpPassword)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)
        val tvAlreadyHaveAccount = findViewById<TextView>(R.id.tvAlreadyHaveAccount)

        // Sign-Up button click listener
        btnSignUp.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // Check if any field is empty
            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate email format
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if email contains one of the allowed domains
            if (!isEmailDomainValid(email)) {
                Toast.makeText(this, "Invalid email address. Use a valid Email .", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check password length
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if passwords match
            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords should match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create a new user in Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user: FirebaseUser? = auth.currentUser

                        // Send email verification
                        user?.sendEmailVerification()?.addOnCompleteListener { verifyTask ->
                            if (verifyTask.isSuccessful) {
                                Toast.makeText(
                                    this,
                                    "Sign Up Successful! Please verify your email before logging in.",
                                    Toast.LENGTH_LONG
                                ).show()

                                auth.signOut() // Sign out user so they can't log in without verification

                                // Redirect user to Login screen
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Failed to send verification email. Try again.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // "Already have an account?" click listener
        tvAlreadyHaveAccount.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    /**
     * Check if the email contains one of the allowed domains.
     */
    private fun isEmailDomainValid(email: String): Boolean {
        for (domain in allowedDomains) {
            if (email.endsWith(domain)) {
                return true
            }
        }
        return false
    }
}