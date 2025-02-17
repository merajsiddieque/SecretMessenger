package com.app.secretmessenger.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.secretmessenger.R
import com.app.secretmessenger.SearchUser
import com.bumptech.glide.Glide

class SearchUserAdapter(
    private val userList: List<SearchUser>,
    private val onItemClick: (SearchUser) -> Unit, // Item click callback
    private val onUserSelected: (SearchUser, Boolean) -> Unit // Checkbox selection callback
) : RecyclerView.Adapter<SearchUserAdapter.SearchUserViewHolder>() {

    inner class SearchUserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePic: ImageView = itemView.findViewById(R.id.ivProfilePic)
        val username: TextView = itemView.findViewById(R.id.tvUsername)
        val fullName: TextView = itemView.findViewById(R.id.tvFullName)
        val checkbox: CheckBox = itemView.findViewById(R.id.cbSelectUser)

        fun bind(user: SearchUser) {
            // Load profile picture using Glide
            if (user.profilePicBase64.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(user.profilePicBase64)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(profilePic)
            } else {
                profilePic.setImageResource(R.drawable.ic_profile_placeholder) // Default profile pic
            }

            username.text = user.username
            fullName.text = user.fullName

            // Set checkbox state
            checkbox.isChecked = user.isSelected

            // Handle checkbox selection changes
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                user.isSelected = isChecked
                onUserSelected(user, isChecked) // Notify the activity or fragment
            }

            // Handle item clicks (optional)
            itemView.setOnClickListener {
                onItemClick(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchUserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_user, parent, false)
        return SearchUserViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchUserViewHolder, position: Int) {
        holder.bind(userList[position])
    }

    override fun getItemCount(): Int {
        return userList.size
    }
}