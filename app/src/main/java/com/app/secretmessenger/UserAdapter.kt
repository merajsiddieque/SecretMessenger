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

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProfile: ImageView = view.findViewById(R.id.ivProfile)
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvUserField: TextView = view.findViewById(R.id.tvUserField)
        val checkBox: CheckBox = view.findViewById(R.id.checkbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        // 🔹 Set Username and Field
        holder.tvUserName.text = user.name
        holder.tvUserField.text = user.field

        // 🔹 Decode and Display Profile Picture
        if (user.profilePicBase64.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(user.profilePicBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.ivProfile.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.ivProfile.setImageResource(R.drawable.ic_profile_placeholder)
            }
        } else {
            holder.ivProfile.setImageResource(R.drawable.ic_profile_placeholder)
        }

        // 🔹 Handle Checkbox Selection
        holder.checkBox.setOnCheckedChangeListener(null) // Prevent unwanted triggers during recycling
        holder.checkBox.isChecked = false
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            onUserSelected(user, isChecked)
        }

        // 🔹 Handle Item Click
        holder.itemView.setOnClickListener {
            onItemClick(user)
        }
    }

    override fun getItemCount(): Int = userList.size
}
