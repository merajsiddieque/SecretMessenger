package com.app.secretmessenger

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_STORAGE_PERMISSION = 100
    }

    private lateinit var auth: FirebaseAuth // Firebase Authentication instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Check if storage permission is granted, and if not, show a custom pop-up dialog.
        if (!isStoragePermissionGranted()) {
            showStoragePermissionDialog()
        }

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Get references to UI elements
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val tvCreateAccount = findViewById<TextView>(R.id.tvCreateAccount)

        // Login button click listener
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Check if fields are empty
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email and Password are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate email format
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Attempt login
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user: FirebaseUser? = auth.currentUser
                        if (user != null) {
                            if (user.isEmailVerified) {
                                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, UsersHome::class.java))
                                finish()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Please verify your email before logging in.",
                                    Toast.LENGTH_LONG
                                ).show()
                                auth.signOut() // Prevent login if not verified
                            }
                        } else {
                            Toast.makeText(
                                this,
                                "User doesn't exist. Please sign up.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Login failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        // Set click listeners for navigation
        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgetPassword::class.java))
        }

        tvCreateAccount.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }
    }

    override fun onStart() {
        super.onStart()

        // Check if the user is already logged in and email verified
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            startActivity(Intent(this, UsersHome::class.java))
            finish()
        }
    }

    /**
     * Check if both read and write storage permissions are granted.
     */
    private fun isStoragePermissionGranted(): Boolean {
        val readPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        val writePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        return readPermission == PackageManager.PERMISSION_GRANTED &&
                writePermission == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Displays a custom dialog to ask for storage permissions.
     */
    private fun showStoragePermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Storage Permission Needed")
            .setMessage("This app requires storage access to function properly. Please grant storage permissions.")
            .setPositiveButton("Allow") { dialog, _ ->
                // Launch the system permission dialog when "Allow" is clicked.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    REQUEST_CODE_STORAGE_PERMISSION
                )
                dialog.dismiss()
            }
            .setNegativeButton("Deny") { dialog, _ ->
                Toast.makeText(
                    this,
                    "Storage permissions are required for this app.",
                    Toast.LENGTH_LONG
                ).show()
                dialog.dismiss()
                // Optionally, you can disable features or close the app if permission is critical.
            }
            .create()
            .show()
    }

    /**
     * Handle the result of permission requests.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Storage permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Storage permissions are required for this app.",
                    Toast.LENGTH_LONG
                ).show()
                // Optionally, take further action if permissions are denied.
            }
        }
    }
}
