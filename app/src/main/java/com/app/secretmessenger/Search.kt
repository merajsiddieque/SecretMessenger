package com.app.secretmessenger

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.secretmessenger.adapter.SearchUserAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.textfield.TextInputEditText
import androidx.recyclerview.widget.RecyclerView

class Search : AppCompatActivity() {

    private lateinit var recyclerViewResults: RecyclerView
    private lateinit var etSearch: TextInputEditText
    private lateinit var imgbtnSearchAdd: ImageButton
    private lateinit var userList: MutableList<SearchUser>
    private lateinit var adapter: SearchUserAdapter
    private val db = FirebaseFirestore.getInstance()
    private val selectedUsers = mutableListOf<SearchUser>()  // To track selected users

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)  // Set your activity layout manually

        // Initialize views
        recyclerViewResults = findViewById(R.id.recyclerViewResults)
        etSearch = findViewById(R.id.etSearch)
        imgbtnSearchAdd = findViewById(R.id.imgbtnSearchAdd)

        // Initialize user list and adapter
        userList = mutableListOf()
        adapter = SearchUserAdapter(userList, { user ->
            // Handle item click (optional)
        }, { user, isSelected ->
            // Handle checkbox selection
            if (isSelected) {
                selectedUsers.add(user)  // Add to the selected users list
            } else {
                selectedUsers.remove(user)  // Remove from the selected users list
            }
            updateSearchAddButtonVisibility()  // Update button visibility
        })

        // Set RecyclerView
        recyclerViewResults.layoutManager = LinearLayoutManager(this)
        recyclerViewResults.adapter = adapter

        // Set search input listener
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                val query = charSequence.toString().trim()
                if (query.isNotEmpty()) {
                    searchUsers(query)
                } else {
                    userList.clear()
                    adapter.notifyDataSetChanged()
                }
            }

            override fun afterTextChanged(editable: Editable?) {}
        })

        // Edge to edge setup for UI (Optional)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set onClickListener for imgbtnSearchAdd to handle adding friends
        imgbtnSearchAdd.setOnClickListener {
            if (selectedUsers.isNotEmpty()) {
                val currentUser = FirebaseAuth.getInstance().currentUser
                val email = currentUser?.email
                if (!email.isNullOrEmpty()) {
                    addSelectedUsersAsFriends(email)
                } else {
                    Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No users selected.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to fetch users based on search query
    private fun searchUsers(query: String) {
        db.collection("Users")
            .whereGreaterThanOrEqualTo("username", query)
            .whereLessThanOrEqualTo("username", query + "\uf8ff")
            .get()
            .addOnSuccessListener { querySnapshot ->
                userList.clear() // Clear previous data
                for (document in querySnapshot.documents) {
                    val username = document.getString("username") ?: ""
                    val fullName = document.getString("fullName") ?: ""
                    val profilePicBase64 = document.getString("profilePic") ?: ""
                    val user = SearchUser(profilePicBase64, username, fullName)
                    userList.add(user) // Add matched user
                }
                adapter.notifyDataSetChanged() // Notify adapter to refresh RecyclerView
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching users: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to add selected users as friends
    private fun addSelectedUsersAsFriends(email: String) {
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
                if (currentUsername.isNotEmpty()) {
                    for (user in selectedUsers) {
                        val friendDocId = "$currentUsername-${user.username}"
                        db.collection("Friends").document(friendDocId)
                            .set(mapOf("isFriendAdded" to true))  // Adding a boolean value
                            .addOnSuccessListener {
                                Toast.makeText(this, "Friend added: ${user.username}", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to add friend: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error retrieving user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to update the visibility of the "Add Friends" button
    private fun updateSearchAddButtonVisibility() {
        imgbtnSearchAdd.visibility = if (selectedUsers.isNotEmpty()) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
    }
}