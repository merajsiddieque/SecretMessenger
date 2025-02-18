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
import com.app.secretmessenger.adapter.UsersAdapter
import com.app.secretmessenger.Users
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UsersHome : AppCompatActivity() {
    private lateinit var adapter: UsersAdapter
    private lateinit var userList: ArrayList<Users>
    private lateinit var selectedUsers: ArrayList<Users>
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users_home)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val btnSearch = findViewById<ImageButton>(R.id.btnSearch)
        val btnAdd = findViewById<ImageButton>(R.id.imgbtnAdd)
        val etSearch = findViewById<EditText>(R.id.etSearch)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewUsers)

        recyclerView.layoutManager = LinearLayoutManager(this)
        userList = ArrayList()
        selectedUsers = ArrayList()

        adapter = UsersAdapter(
            userList,
            onUserSelected = { user, isChecked ->
                if (isChecked) {
                    selectedUsers.add(user)
                } else {
                    selectedUsers.remove(user)
                }
            },
            onItemClick = { clickedUser ->
                val intent = Intent(this, Home::class.java).apply {
                    putExtra("username", clickedUser.name)
                    putExtra("profilePic", clickedUser.profilePicBase64)
                }
                startActivity(intent)
            }
        )
        recyclerView.adapter = adapter

        showFriends()

        btnAdd.setOnClickListener {
            startActivity(Intent(this, AddUsers::class.java))
        }

        btnSearch.setOnClickListener {
            val query = etSearch.text.toString().trim()
            if (query.isEmpty()) {
                showFriends()
            } else {
                searchUsers(query)
            }
        }
    }

    private fun searchUsers(query: String) {
        db.collection("Users")
            .get()
            .addOnSuccessListener { querySnapshot ->
                userList.clear()
                for (document in querySnapshot.documents) {
                    val documentId = document.id
                    val parts = documentId.split("-")
                    if (parts.size > 1) {
                        val usernameFromDoc = parts[0]
                        if (usernameFromDoc.startsWith(query, ignoreCase = true)) {
                            val profilePicBase64 = document.getString("profilePic") ?: ""
                            val user = Users(
                                profilePicBase64 = profilePicBase64,
                                name = document.getString("username") ?: "",
                                field = document.getString("fullName") ?: ""
                            )
                            userList.add(user)
                        }
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching users: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showFriends() {
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
                    .get()
                    .addOnSuccessListener { friendsSnapshot ->
                        val friendNames = ArrayList<String>()
                        for (friendDoc in friendsSnapshot.documents) {
                            if (friendDoc.id.startsWith("$currentUsername-")) {
                                val parts = friendDoc.id.split("-")
                                if (parts.size >= 2) {
                                    val friendName = parts[1]
                                    friendNames.add(friendName)
                                }
                            }
                        }

                        if (friendNames.isEmpty()) {
                            Toast.makeText(this, "No friends found.", Toast.LENGTH_SHORT).show()
                            userList.clear()
                            adapter.notifyDataSetChanged()
                        } else {
                            db.collection("Users")
                                .whereIn("username", friendNames)
                                .get()
                                .addOnSuccessListener { friendDetailsSnapshot ->
                                    userList.clear()
                                    for (userDoc in friendDetailsSnapshot.documents) {
                                        val username = userDoc.getString("username") ?: ""
                                        if (friendNames.contains(username)) {
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
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching current user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_delete -> {
                if (selectedUsers.isNotEmpty()) {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val email = currentUser?.email
                    if (!email.isNullOrEmpty()) {
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
                                        val friendDocId = "$currentUsername-${user.name}"
                                        db.collection("Friends").document(friendDocId)
                                            .set(emptyMap<String, Any>())
                                            .addOnSuccessListener {
                                                Toast.makeText(this, "Friend added: ${user.name}", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(this, "Failed to add friend: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error retrieving user data: ${e.message}", Toast.LENGTH_SHORT).show()
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
}
