package com.app.secretmessenger

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddUsers : AppCompatActivity() {

    private lateinit var appNameTextView: TextView
    private lateinit var addButton: ImageButton
    private lateinit var searchInput: EditText
    private lateinit var searchIcon: ImageButton
    private lateinit var recyclerViewResults: RecyclerView

    private var userList: ArrayList<AddUsersData> = ArrayList()
    private val selectedUsers: MutableList<AddUsersData> = mutableListOf()
    private lateinit var adapter: AddUsersAdapter

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_users)

        // Initialize views
        appNameTextView = findViewById(R.id.appName)
        addButton = findViewById(R.id.addButton)
        searchInput = findViewById(R.id.searchInput)
        searchIcon = findViewById(R.id.searchIcon)
        recyclerViewResults = findViewById(R.id.recyclerViewResults)

        // Set up RecyclerView with long-press selection
        adapter = AddUsersAdapter(userList, selectedUsers) { user ->
            // Toggle selection on long press
            if (selectedUsers.contains(user)) {
                selectedUsers.remove(user)
            } else {
                selectedUsers.add(user)
            }
            adapter.notifyDataSetChanged() // Update UI to reflect selection
        }
        recyclerViewResults.layoutManager = LinearLayoutManager(this)
        recyclerViewResults.adapter = adapter

        // Edge-to-edge UI handling
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Add button click listener
        addButton.setOnClickListener {
            if (selectedUsers.isNotEmpty()) {
                selectedUsers.forEach { user ->
                    Toast.makeText(this, "${user.name} Added", Toast.LENGTH_SHORT).show()
                    addFriend(user)
                }
                selectedUsers.clear()
                adapter.notifyDataSetChanged()
            } else {
                Toast.makeText(this, "No user selected", Toast.LENGTH_SHORT).show()
            }
        }

        // Fetch all users on activity start
        fetchAllUsers()

        // Search functionality
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    searchUsers(query)
                } else {
                    fetchAllUsers()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun fetchAllUsers() {
        userList.clear()
        db.collection("Users")
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val profilePicBase64 = document.getString("profilePic") ?: ""
                    val user = AddUsersData(
                        profilePicBase64 = profilePicBase64,
                        name = document.getString("username") ?: "",
                        field = document.getString("fullName") ?: ""
                    )
                    userList.add(user)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching users: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun searchUsers(query: String) {
        val filteredList = userList.filter { user ->
            user.name.contains(query, ignoreCase = true) || user.field.contains(query, ignoreCase = true)
        }
        adapter = AddUsersAdapter(filteredList as ArrayList<AddUsersData>, selectedUsers) { user ->
            if (selectedUsers.contains(user)) {
                selectedUsers.remove(user)
            } else {
                selectedUsers.add(user)
            }
            adapter.notifyDataSetChanged()
        }
        recyclerViewResults.adapter = adapter
    }

    private fun addFriend(user: AddUsersData) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val email = currentUser?.email

        if (email.isNullOrEmpty()) {
            Toast.makeText(this, "Current user data not available.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("Users")
            .get()
            .addOnSuccessListener { usersSnapshot ->
                var currentUsername = ""
                for (document in usersSnapshot.documents) {
                    if (document.id.endsWith("-$email")) {
                        currentUsername = document.id.split("-")[0]
                        break
                    }
                }
                if (currentUsername.isEmpty()) {
                    Toast.makeText(this, "Current username not found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                db.collection("Friends")
                    .document("$currentUsername-${user.name}")
                    .set(hashMapOf("isFriend" to "True"))
                    .addOnSuccessListener {
                        Toast.makeText(this, "Friend added successfully.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to add friend: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching current user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}