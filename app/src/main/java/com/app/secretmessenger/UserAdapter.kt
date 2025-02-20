package com.app.secretmessenger

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.secretmessenger.R

class UsersAdapter(
    private val userList: List<Users>,
    private val selectedUsers: MutableList<Users>, // Added to track selected users
    private val onLongPress: (Users) -> Unit,
    private val onItemClick: (Users) -> Unit
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    // ViewHolder Class
    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProfile: ImageView = view.findViewById(R.id.ivProfile)
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvUserField: TextView = view.findViewById(R.id.tvUserField)

        fun bind(user: Users, isSelected: Boolean, onLongPress: (Users) -> Unit, onItemClick: (Users) -> Unit) {
            tvUserName.text = user.name
            tvUserField.text = user.field

            // Set profile image
            setProfileImage(ivProfile, user.profilePicBase64)

            // Set background based on selection
            itemView.setBackgroundResource(
                if (isSelected) R.color.light_gray else android.R.color.transparent
            )

            // Long press to toggle selection
            itemView.setOnLongClickListener {
                onLongPress(user)
                true
            }

            // Normal click to open chat
            itemView.setOnClickListener {
                onItemClick(user)
            }
        }
    }

    // Create ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    // Bind Data to ViewHolder
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.bind(user, selectedUsers.contains(user), onLongPress, onItemClick)
    }

    // Get Item Count
    override fun getItemCount(): Int = userList.size

    // Helper Function: Set Profile Image
    private fun setProfileImage(imageView: ImageView, profilePicBase64: String) {
        if (profilePicBase64.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(profilePicBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                imageView.setImageResource(R.drawable.ic_profile_placeholder)
            }
        } else {
            imageView.setImageResource(R.drawable.ic_profile_placeholder)
        }
    }
}