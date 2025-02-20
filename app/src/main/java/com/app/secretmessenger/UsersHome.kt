package com.app.secretmessenger

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UsersHome : AppCompatActivity() {

    private lateinit var adapter: UsersAdapter
    private lateinit var userList: ArrayList<Users>
    private lateinit var selectedUsers: ArrayList<Users>
    private val db = FirebaseFirestore.getInstance()
    private var currentUsernameGlobal: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users_home)

        // Initialize views
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val btnSearch: ImageButton = findViewById(R.id.btnSearch)
        val btnAdd: ImageButton = findViewById(R.id.imgbtnAdd)
        val etSearch: EditText = findViewById(R.id.etSearch)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewUsers)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        userList = ArrayList()
        selectedUsers = ArrayList()

        adapter = UsersAdapter(
            userList,
            selectedUsers, // Pass the selectedUsers list
            onLongPress = { user ->
                if (selectedUsers.contains(user)) {
                    selectedUsers.remove(user)
                } else {
                    selectedUsers.add(user)
                }
                adapter.notifyDataSetChanged()
            },
            onItemClick = { clickedUser ->
                val intent = Intent(this, Message::class.java).apply {
                    putExtra("username", clickedUser.name)
                    putExtra("profilePic", clickedUser.profilePicBase64)
                }
                startActivity(intent)
            }
        )
        recyclerView.adapter = adapter

        // Fetch and display friends
        fetchCurrentUsernameAndShowFriends()

        // Add button click listener
        btnAdd.setOnClickListener {
            startActivity(Intent(this, AddUsers::class.java))
        }

        // Search button click listener
        btnSearch.setOnClickListener {
            val query = etSearch.text.toString().trim()
            if (query.isEmpty()) {
                fetchCurrentUsernameAndShowFriends() // Show all friends if search query is empty
            } else {
                searchUsers(query) // Search for users
            }
        }
    }

    private fun fetchCurrentUsernameAndShowFriends() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val email = currentUser?.email

        if (email.isNullOrEmpty()) {
            Toast.makeText(this, "Current user data not available.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("Users")
            .get()
            .addOnSuccessListener { usersSnapshot ->
                for (document in usersSnapshot.documents) {
                    if (document.id.endsWith("-$email")) {
                        currentUsernameGlobal = document.id.split("-")[0]
                        break
                    }
                }
                if (currentUsernameGlobal.isEmpty()) {
                    Toast.makeText(this, "Current username not found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                showFriends()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching current user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showFriends() {
        db.collection("Friends")
            .get()
            .addOnSuccessListener { friendsSnapshot ->
                val friendUsernames = ArrayList<String>()
                for (friendDoc in friendsSnapshot.documents) {
                    if (friendDoc.id.startsWith("$currentUsernameGlobal-")) {
                        val isFriend = friendDoc.getString("isFriend")
                        if (isFriend == "True") {
                            val friendUsername = friendDoc.id.split("-")[1]
                            friendUsernames.add(friendUsername)
                        }
                    }
                }

                if (friendUsernames.isEmpty()) {
                    Toast.makeText(this, "No friends found.", Toast.LENGTH_SHORT).show()
                    userList.clear()
                    adapter.notifyDataSetChanged()
                } else {
                    db.collection("Users")
                        .whereIn("username", friendUsernames)
                        .get()
                        .addOnSuccessListener { friendDetailsSnapshot ->
                            userList.clear()
                            for (userDoc in friendDetailsSnapshot.documents) {
                                val username = userDoc.getString("username") ?: ""
                                if (friendUsernames.contains(username)) {
                                    val profilePicBase64 = userDoc.getString("profilePic") ?: ""
                                    val friendUser = Users(
                                        profilePicBase64 = profilePicBase64,
                                        name = username,
                                        field = userDoc.getString("fullName") ?: ""
                                    )
                                    userList.add(friendUser)
                                }
                            }
                            adapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error fetching friend details: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching friends list: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun searchUsers(query: String) {
        val filteredList = userList.filter { user ->
            user.name.startsWith(query, ignoreCase = true)
        }
        userList.clear()
        userList.addAll(filteredList)
        adapter.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_delete -> {
                if (selectedUsers.isNotEmpty()) {
                    for (user in selectedUsers.toList()) { // Use toList() to avoid concurrent modification
                        val conversationId = "$currentUsernameGlobal-${user.name}"
                        db.collection("Friends").document(conversationId)
                            .delete()
                            .addOnSuccessListener {
                                userList.remove(user)
                                selectedUsers.remove(user)
                                adapter.notifyDataSetChanged()
                                Toast.makeText(this, "Friend removed: ${user.name}", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to remove friend: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "No users selected.", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.menu_settings -> {
                startActivity(Intent(this, Settings::class.java))
                true
            }
            R.id.menu_logout -> {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        fetchCurrentUsernameAndShowFriends()
    }
}