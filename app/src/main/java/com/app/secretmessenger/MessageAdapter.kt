package com.app.secretmessenger.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.secretmessenger.Message
import com.app.secretmessenger.R

class MessageAdapter(
    private val messageList: List<Message>
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvMessageContent: TextView = view.findViewById(R.id.tvMessageContent)
        val tvMessageTime: TextView = view.findViewById(R.id.tvMessageTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]
        holder.tvUsername.text = message.username
        holder.tvMessageContent.text = message.content
        holder.tvMessageTime.text = message.time
    }

    override fun getItemCount(): Int = messageList.size
}
