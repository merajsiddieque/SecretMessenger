package com.app.secretmessenger

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Message : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_PICK_FILE = 1001
        private const val STORAGE_PERMISSION_CODE = 1002
    }

    private val messagesList = mutableListOf<MessageData>()
    private lateinit var messageAdapter: MessageAdapter
    private val db = FirebaseFirestore.getInstance()
    private var currentUsernameGlobal: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_message)

        if (!hasStoragePermissions()) {
            requestStoragePermissions()
        }

        val friendUsername = intent.getStringExtra("username") ?: "Friend"
        val friendProfilePicBase64 = intent.getStringExtra("profilePic") ?: ""

        val tvUsername = findViewById<TextView>(R.id.tvUsername)
        val ivProfilePic = findViewById<ImageView>(R.id.ivProfilePic)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)
        val docsbtn = findViewById<ImageButton>(R.id.docbutton)
        val messageMoreOptions = findViewById<ImageButton>(R.id.btnMoreOptions)
        val messageInputLayout = findViewById<LinearLayout>(R.id.messageInputLayout)
        val recyclerViewMessages = findViewById<RecyclerView>(R.id.recyclerViewMessages)

        tvUsername.text = friendUsername
        if (friendProfilePicBase64.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(friendProfilePicBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ivProfilePic.setImageBitmap(bitmap)
            } catch (e: Exception) {
                ivProfilePic.setImageResource(R.drawable.ic_profile_placeholder)
            }
        } else {
            ivProfilePic.setImageResource(R.drawable.ic_profile_placeholder)
        }

        messageAdapter = MessageAdapter(
            messagesList,
            onDelete = { message -> deleteMessage(message) },
            onDownloadAndOpen = { message -> downloadAndOpenFile(message) }
        )
        recyclerViewMessages.layoutManager = LinearLayoutManager(this)
        recyclerViewMessages.adapter = messageAdapter

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            messageInputLayout.translationY = -imeHeight.toFloat()
            recyclerViewMessages.translationY = -imeHeight.toFloat()
            insets
        }

        etMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                recyclerViewMessages.postDelayed({
                    recyclerViewMessages.scrollToPosition(messageAdapter.itemCount - 1)
                }, 100)
            }
        }

        etMessage.setOnClickListener {
            etMessage.isFocusableInTouchMode = true
            etMessage.requestFocus()
        }

        messageMoreOptions.setOnClickListener {
            showMessageMenu(it)
        }

        docsbtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(intent, REQUEST_CODE_PICK_FILE)
        }

        btnSend.setOnClickListener {
            sendMessage()
        }

        loadUserDataAndMessages(friendUsername, recyclerViewMessages)
    }

    private fun showMessageMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.message_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_search -> {
                    Toast.makeText(this, "Searched Clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_block -> {
                    Toast.makeText(this, "Blocked clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_message_settings -> {
                    Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun loadUserDataAndMessages(friendUsername: String, recyclerView: RecyclerView) {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        if (currentUserEmail.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("Users")
            .get()
            .addOnSuccessListener { snapshot ->
                var currentUsername = ""
                for (document in snapshot.documents) {
                    if (document.id.endsWith("-$currentUserEmail")) {
                        currentUsername = document.id.split("-")[0]
                        break
                    }
                }
                if (currentUsername.isEmpty()) {
                    Toast.makeText(this, "Current username not found.", Toast.LENGTH_SHORT).show()
                } else {
                    currentUsernameGlobal = currentUsername
                    val conversationId1 = "$currentUsernameGlobal-$friendUsername"
                    val conversationId2 = "$friendUsername-$currentUsernameGlobal"
                    loadMessagesForConversations(listOf(conversationId1, conversationId2), recyclerView)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error retrieving user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendMessage() {
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val messageText = etMessage.text.toString().trim()
        if (messageText.isEmpty()) {
            Toast.makeText(this, "Please enter a message.", Toast.LENGTH_SHORT).show()
            return
        }
        etMessage.text.clear()

        if (currentUsernameGlobal.isEmpty()) {
            Toast.makeText(this, "Current username not available.", Toast.LENGTH_SHORT).show()
            return
        }

        val friendUsername = findViewById<TextView>(R.id.tvUsername).text.toString()
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val formattedTime = timeFormat.format(Date())
        val timestamp = System.currentTimeMillis()
        val conversationId = "$currentUsernameGlobal-$friendUsername"

        val messageMap = hashMapOf(
            "username" to currentUsernameGlobal,
            "content" to messageText,
            "time" to formattedTime,
            "timestamp" to timestamp
        )

        db.collection("Friends")
            .document(conversationId)
            .collection("messages")
            .add(messageMap)
            .addOnSuccessListener {
                // Snapshot listener will handle UI update
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val fileName = getFileNameFromUri(uri)
                sendFileMessage(uri, fileName)
            } ?: run {
                Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            it.getString(nameIndex)
        }
    }

    private fun sendFileMessage(uri: Uri, fileName: String?) {
        if (currentUsernameGlobal.isEmpty()) {
            Toast.makeText(this, "Current username not available.", Toast.LENGTH_SHORT).show()
            return
        }

        val friendUsername = findViewById<TextView>(R.id.tvUsername).text.toString()
        val conversationId = "$currentUsernameGlobal-$friendUsername"
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val formattedTime = timeFormat.format(Date())
        val timestamp = System.currentTimeMillis()

        try {
            val inputStream = contentResolver.openInputStream(uri)
            val byteArray = inputStream?.readBytes()
            inputStream?.close()

            if (byteArray != null) {
                if (byteArray.size > 1_048_576) {
                    Toast.makeText(this, "File too large (max 1 MB)", Toast.LENGTH_SHORT).show()
                    return
                }

                val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)

                val messageMap = hashMapOf(
                    "username" to currentUsernameGlobal,
                    "content" to "File: ${fileName ?: "Unnamed file"}",
                    "fileData" to base64String,
                    "time" to formattedTime,
                    "timestamp" to timestamp
                )

                db.collection("Friends")
                    .document(conversationId)
                    .collection("messages")
                    .add(messageMap)
                    .addOnSuccessListener {
                        Toast.makeText(this, "File message sent", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to send file: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error reading file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadMessagesForConversations(conversationIds: List<String>, recyclerView: RecyclerView) {
        conversationIds.forEach { conversationId ->
            db.collection("Friends")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Toast.makeText(this, "Error loading messages: ${error.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        for (change in snapshot.documentChanges) {
                            if (change.type == DocumentChange.Type.ADDED) {
                                val username = change.document.getString("username") ?: ""
                                val content = change.document.getString("content") ?: ""
                                val time = change.document.getString("time") ?: ""
                                val timestamp = change.document.getLong("timestamp") ?: 0L
                                val fileData = change.document.getString("fileData")
                                val newMessage = MessageData(username, content, time, timestamp, fileData).apply {
                                    documentId = change.document.id
                                }
                                if (!messagesList.contains(newMessage)) {
                                    messagesList.add(newMessage)
                                }
                            }
                        }
                        messagesList.sortBy { it.timestamp }
                        messageAdapter.notifyDataSetChanged()
                        recyclerView.scrollToPosition(messagesList.size - 1)
                    }
                }
        }
    }

    private fun deleteMessage(message: MessageData) {
        val documentId = message.documentId ?: return

        val friendUsername = findViewById<TextView>(R.id.tvUsername).text.toString()
        val conversationId = if (message.username == currentUsernameGlobal) {
            "$currentUsernameGlobal-$friendUsername"
        } else {
            "$friendUsername-$currentUsernameGlobal"
        }

        db.collection("Friends")
            .document(conversationId)
            .collection("messages")
            .document(documentId)
            .delete()
            .addOnSuccessListener {
                messagesList.remove(message)
                messageAdapter.notifyDataSetChanged()
                Toast.makeText(this, "Message deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun downloadAndOpenFile(message: MessageData) {
        if (!hasStoragePermissions()) {
            requestStoragePermissions()
            return
        }

        val fileData = message.fileData ?: return
        val content = message.content
        val fileName = content.removePrefix("File: ").trim()

        try {
            val fileBytes = Base64.decode(fileData, Base64.DEFAULT)
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "SecretMessenger"
            )

            if (!directory.exists()) {
                val created = directory.mkdirs()
                if (!created) {
                    Toast.makeText(this, "Failed to create directory", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            val file = File(directory, fileName)
            FileOutputStream(file).use { output ->
                output.write(fileBytes)
                output.flush()
            }

            if (file.exists() && file.length() > 0) {
                Toast.makeText(this, "File downloaded to Pictures/SecretMessenger/$fileName", Toast.LENGTH_LONG).show()

                val uri = FileProvider.getUriForFile(this, "com.app.secretmessenger.fileprovider", file)
                val mimeType = contentResolver.getType(uri) ?: if (fileName.lowercase().endsWith(".pdf")) "application/pdf" else "*/*"
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "No app found to open this file", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "File was not saved correctly", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to download or open file: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun hasStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        ActivityCompat.requestPermissions(this, permissions, STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Storage permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Storage permissions denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}