package com.app.secretmessenger

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class Settings : AppCompatActivity() {

    // UI Elements
    private lateinit var ivProfilePic: ImageView
    private lateinit var etFullName: EditText
    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnSave: Button

    // Firebase Instances
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Constants and Variables
    private var selectedProfilePic: String? = null
    private val PICK_IMAGE_REQUEST = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize UI Elements
        ivProfilePic = findViewById(R.id.ivProfilePic)
        etFullName = findViewById(R.id.etFullName)
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        btnSave = findViewById(R.id.btnSave)

        // Load User Data if Email Exists
        checkEmailAndLoadData()

        // Open Image Picker when Profile Picture is Clicked
        ivProfilePic.setOnClickListener {
            openImagePicker()
        }

        // Save Button Click Listener
        btnSave.setOnClickListener {
            saveUserData()
        }
    }

    /**
     * Open the Image Picker for Selecting Profile Picture
     */
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    /**
     * Handle Image Selection and Convert to Base64
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val imageUri: Uri? = data.data
            ivProfilePic.setImageURI(imageUri)

            // Convert to Base64
            val bitmap = (ivProfilePic.drawable as BitmapDrawable).bitmap
            selectedProfilePic = encodeImageToBase64(bitmap)
        }
    }

    /**
     * Convert Bitmap Image to Base64 String
     */
    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * Check if Email Exists and Load Data if it does
     */
    private fun checkEmailAndLoadData() {
        val currentUser = auth.currentUser
        val email = currentUser?.email

        if (email != null) {
            etEmail.setText(email)

            // 🔹 Fetch All Documents from Users Collection
            db.collection("Users")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    var foundDocument = false

                    for (document in querySnapshot.documents) {
                        val documentId = document.id

                        // 🔹 Check if Document ID Ends with -email@example.com
                        if (documentId.endsWith("-$email")) {
                            foundDocument = true

                            // 🔹 Extract Data from Document
                            val username = document.getString("username")
                            val fullName = document.getString("fullName")
                            val profilePic = document.getString("profilePic")

                            // 🔹 Set Data in UI
                            etUsername.setText(username)
                            etFullName.setText(fullName)

                            // 🔹 Load Profile Picture if Available
                            if (!profilePic.isNullOrEmpty()) {
                                val imageBytes = Base64.decode(profilePic, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                ivProfilePic.setImageBitmap(bitmap)
                            }
                            break // Exit loop once a match is found
                        }
                    }

                    if (!foundDocument) {
                        // 🔹 If no matching document, clear the fields
                        etUsername.setText("")
                        etFullName.setText("")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("SettingsActivity", "Failed to load data", e)
                }
        }
    }

    /**
     * Save User Data to Firestore
     */
    private fun saveUserData() {
        val currentUser = auth.currentUser
        val email = currentUser?.email

        if (email.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val username = etUsername.text.toString().trim()
        val fullName = etFullName.text.toString().trim()

        if (username.isEmpty() || fullName.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // 🔹 Combine Username and Email for Document ID
        val documentId = "$username-$email"

        // Prepare User Data
        val userData = hashMapOf(
            "username" to username,
            "fullName" to fullName,
            "email" to email,
            "profilePic" to selectedProfilePic
        )

        // 🔹 Save Data using Combined Username and Email as Document ID
        db.collection("Users").document(documentId)
            .set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("SettingsActivity", "Failed to save data", e)
                Toast.makeText(this, "Failed to save data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
