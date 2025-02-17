package com.app.secretmessenger

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class ForgetPassword : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth // Firebase Authentication instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forget_password)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.forgetPasswordLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find UI elements
        val etResetEmail = findViewById<EditText>(R.id.etResetEmail)
        val btnResetPassword = findViewById<Button>(R.id.btnResetPassword)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        // Click listener for "Back to Login"
        tvBackToLogin.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish() // Close ForgetPassword activity
        }

        // Click listener for "Reset Password"
        btnResetPassword.setOnClickListener {
            val email = etResetEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Send password reset email
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Password reset link sent to your email.",
                            Toast.LENGTH_LONG
                        ).show()
                        startActivity(Intent(this, MainActivity::class.java)) // Redirect to login
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            "Error: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}
