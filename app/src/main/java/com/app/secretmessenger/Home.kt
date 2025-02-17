package com.app.secretmessenger

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.secretmessenger.adapter.MessageAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Home : AppCompatActivity() {

    // List of messages backing the RecyclerView
    private val messagesList = mutableListOf<Message>()
    private lateinit var messageAdapter: MessageAdapter

    // Firestore instance
    private val db = FirebaseFirestore.getInstance()

    // Global variable to store current user's username retrieved from Firestore
    private var currentUsernameGlobal: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // Retrieve friend data passed from UsersHome
        val friendUsername = intent.getStringExtra("username") ?: "Friend"
        val friendProfilePicBase64 = intent.getStringExtra("profilePic") ?: ""

        // Find views in the top bar
        val tvUsername = findViewById<TextView>(R.id.tvUsername)
        val ivProfilePic = findViewById<ImageView>(R.id.ivProfilePic)

        // Set the friend’s username and profile picture in the top bar
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

        // Find message-related views
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)
        val messageInputLayout = findViewById<LinearLayout>(R.id.messageInputLayout)
        val recyclerViewMessages = findViewById<RecyclerView>(R.id.recyclerViewMessages)

        // Set up RecyclerView with MessageAdapter
        messageAdapter = MessageAdapter(messagesList)
        recyclerViewMessages.layoutManager = LinearLayoutManager(this)
        recyclerViewMessages.adapter = messageAdapter

        // Adjust input layout when keyboard appears
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            messageInputLayout.translationY = -imeHeight.toFloat()
            recyclerViewMessages.translationY = -imeHeight.toFloat()
            insets
        }

        // Auto-scroll RecyclerView when etMessage gains focus
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

        // Retrieve the current user's username from Firestore.
        // The document ID is in the format "username-email". We extract the username part.
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        if (currentUserEmail.isNullOrEmpty()) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
        } else {
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
                        // Build both conversation IDs (both orderings)
                        val conversationId1 = "$currentUsernameGlobal-$friendUsername"
                        val conversationId2 = "$friendUsername-$currentUsernameGlobal"
                        // Load messages from both conversation IDs, ordering by the numeric timestamp.
                        loadMessagesForConversations(listOf(conversationId1, conversationId2), recyclerViewMessages)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error retrieving user data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Handle btnSend click
        btnSend.setOnClickListener {
            val messageText = etMessage.text.toString().trim()
            if (messageText.isEmpty()) {
                Toast.makeText(this, "Please enter a message.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Clear the input field after reading its content
            etMessage.text.clear()

            if (currentUsernameGlobal.isEmpty()) {
                Toast.makeText(this, "Current username not available.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Prepare time values
            // The displayed time remains unchanged.
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val formattedTime = timeFormat.format(Date())
            // Use the current timestamp (in milliseconds) for reliable sorting.
            val timestamp = System.currentTimeMillis()

            // When sending a message, we use conversationId1 by default.
            val conversationId = "$currentUsernameGlobal-$friendUsername"

            // Prepare a map with the message fields.
            // "time" is for display and "timestamp" is for sorting.
            val messageMap = hashMapOf(
                "username" to currentUsernameGlobal,
                "content" to messageText,
                "time" to formattedTime,
                "timestamp" to timestamp
            )

            // Save the message in Firestore under the "Friends" collection.
            // The message is added to a subcollection called "messages" inside the conversation document.
            db.collection("Friends")
                .document(conversationId)
                .collection("messages")
                .add(messageMap)
                .addOnSuccessListener {
                    // The snapshot listeners will update the RecyclerView.
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    /**
     * Loads messages from multiple conversation IDs in realtime.
     * Orders by the numeric "timestamp" field for accurate sequence.
     */
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
                                val newMessage = Message(username, content, time, timestamp)
                                // Avoid adding duplicates.
                                if (!messagesList.contains(newMessage)) {
                                    messagesList.add(newMessage)
                                }
                            }
                        }
                        // Re-sort the combined list by timestamp so that the newest message is at the bottom.
                        messagesList.sortBy { it.timestamp }
                        messageAdapter.notifyDataSetChanged()
                        recyclerView.scrollToPosition(messagesList.size - 1)
                    }
                }
        }
    }
}
