package com.app.secretmessenger

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AddUsersAdapter(
    private val userList: List<AddUsersData>,
    private val selectedUsers: MutableList<AddUsersData>,
    private val onLongPress: (AddUsersData) -> Unit
) : RecyclerView.Adapter<AddUsersAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_add_users, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.bind(user, selectedUsers.contains(user), onLongPress)
    }

    override fun getItemCount(): Int = userList.size

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        private val userFieldTextView: TextView = itemView.findViewById(R.id.userFieldTextView)
        private val profileImage: ImageView = itemView.findViewById(R.id.profileImage)

        fun bind(user: AddUsersData, isSelected: Boolean, onLongPress: (AddUsersData) -> Unit) {
            userNameTextView.text = user.name
            userFieldTextView.text = user.field

            // Set profile image (decode Base64 or use placeholder)
            if (user.profilePicBase64.isNotEmpty()) {
                try {
                    val decodedString = Base64.decode(user.profilePicBase64, Base64.DEFAULT)
                    val decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                    profileImage.setImageBitmap(decodedBitmap)
                } catch (e: Exception) {
                    profileImage.setImageResource(R.drawable.ic_profile_placeholder)
                }
            } else {
                profileImage.setImageResource(R.drawable.ic_profile_placeholder)
            }

            // Set background color based on selection
            itemView.setBackgroundResource(
                if (isSelected) R.color.light_gray else android.R.color.transparent
            )

            // Handle long press to toggle selection
            itemView.setOnLongClickListener {
                onLongPress(user)
                true
            }
        }
    }
}