package com.app.secretmessenger

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    // ActivityResultLauncher for requesting multiple permissions
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            Snackbar.make(findViewById(android.R.id.content), "Storage permissions granted", Snackbar.LENGTH_SHORT).show()
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Storage permissions are required for this app.", Snackbar.LENGTH_LONG).show()
        }
    }

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
                Snackbar.make(it, "Email and Password are required", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate email format
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Snackbar.make(it, "Enter a valid email", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Attempt login
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user: FirebaseUser? = auth.currentUser
                        if (user != null) {
                            if (user.isEmailVerified) {
                                Snackbar.make(it, "Login Successful!", Snackbar.LENGTH_SHORT).show()
                                startActivity(Intent(this, UsersHome::class.java))
                                finish()
                            } else {
                                Snackbar.make(it, "Please verify your email before logging in.", Snackbar.LENGTH_LONG).show()
                                auth.signOut() // Prevent login if not verified
                            }
                        } else {
                            Snackbar.make(it, "User doesn't exist. Please sign up.", Snackbar.LENGTH_SHORT).show()
                        }
                    } else {
                        Snackbar.make(it, "Login failed: ${task.exception?.message}", Snackbar.LENGTH_SHORT).show()
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
                requestPermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
                dialog.dismiss()
            }
            .setNegativeButton("Deny") { dialog, _ ->
                Snackbar.make(findViewById(android.R.id.content), "Storage permissions are required for this app.", Snackbar.LENGTH_LONG).show()
                dialog.dismiss()
                // Optionally, you can disable features or close the app if permission is critical.
            }
            .create()
            .show()
    }
}