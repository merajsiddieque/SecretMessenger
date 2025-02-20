package com.app.secretmessenger

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(
    private val messageList: MutableList<MessageData>,
    private val onDelete: (MessageData) -> Unit,
    private val onDownloadAndOpen: (MessageData) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvMessageContent: TextView = view.findViewById(R.id.tvMessageContent)
        val tvMessageTime: TextView = view.findViewById(R.id.tvMessageTime)
        val ivFilePreview: ImageView = view.findViewById(R.id.ivFilePreview)
        val btnDownload: Button = view.findViewById(R.id.btnDownload)

        fun bind(message: MessageData) {
            tvUsername.text = message.username
            tvMessageTime.text = message.time

            if (message.fileData != null) {
                val fileName = message.content.removePrefix("File: ").trim()
                tvMessageContent.text = message.content

                // Check if it's a PDF file
                if (fileName.lowercase().endsWith(".pdf")) {
                    ivFilePreview.visibility = View.GONE
                    btnDownload.visibility = View.VISIBLE
                    btnDownload.setOnClickListener {
                        onDownloadAndOpen(message)
                    }
                } else {
                    // Try to display as image
                    btnDownload.visibility = View.GONE
                    try {
                        val fileBytes = Base64.decode(message.fileData, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size)
                        ivFilePreview.visibility = View.VISIBLE
                        ivFilePreview.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        ivFilePreview.visibility = View.GONE
                        tvMessageContent.text = "${message.content} (Preview not available)"
                    }
                }

                // Long press for document files (excluding PDFs since they have download button)
                itemView.setOnLongClickListener {
                    if (message.content.startsWith("File:") && message.fileData != null && !fileName.lowercase().endsWith(".pdf")) {
                        showFileOptionsDialog(message)
                    }
                    true
                }
            } else {
                tvMessageContent.text = message.content
                ivFilePreview.visibility = View.GONE
                btnDownload.visibility = View.GONE
                itemView.setOnLongClickListener(null)
            }
        }

        private fun showFileOptionsDialog(message: MessageData) {
            val options = arrayOf("Download", "Delete")
            AlertDialog.Builder(itemView.context)
                .setTitle("File Options")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> onDownloadAndOpen(message)
                        1 -> onDelete(message)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messageList[position])
    }

    override fun getItemCount(): Int = messageList.size
}