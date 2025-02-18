package com.app.secretmessenger.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.secretmessenger.R
import com.app.secretmessenger.Users

class UsersAdapter(
    private val userList: List<Users>,
    private val onUserSelected: (Users, Boolean) -> Unit,
    private val onItemClick: (Users) -> Unit
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    // 🔹 ViewHolder Class
    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProfile: ImageView = view.findViewById(R.id.ivProfile)
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvUserField: TextView = view.findViewById(R.id.tvUserField)
        val checkBox: CheckBox = view.findViewById(R.id.checkbox)
    }

    // 🔹 Create ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    // 🔹 Bind Data to ViewHolder
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        // Set Username and Field
        holder.tvUserName.text = user.name
        holder.tvUserField.text = user.field

        // Decode and Display Profile Picture
        setProfileImage(holder.ivProfile, user.profilePicBase64)

        // Handle Checkbox Selection
        handleCheckboxSelection(holder.checkBox, user)

        // Handle Item Click
        holder.itemView.setOnClickListener {
            onItemClick(user)
        }
    }

    // 🔹 Get Item Count
    override fun getItemCount(): Int = userList.size

    // 🔹 Helper Function: Set Profile Image
    private fun setProfileImage(imageView: ImageView, profilePicBase64: String) {
        if (profilePicBase64.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(profilePicBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                // Fallback to placeholder if decoding fails
                imageView.setImageResource(R.drawable.ic_profile_placeholder)
            }
        } else {
            // Use placeholder if no image is available
            imageView.setImageResource(R.drawable.ic_profile_placeholder)
        }
    }

    // 🔹 Helper Function: Handle Checkbox Selection
    private fun handleCheckboxSelection(checkBox: CheckBox, user: Users) {
        // Reset the checkbox state to avoid recycling issues
        checkBox.setOnCheckedChangeListener(null)
        checkBox.isChecked = false

        // Set the listener for checkbox state changes
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            onUserSelected(user, isChecked)
        }
    }
}