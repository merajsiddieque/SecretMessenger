package com.app.secretmessenger

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
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
            tvMessageContent.text = message.content
            tvMessageTime.text = message.time

            if (message.fileData != null) {
                try {
                    val fileBytes = Base64.decode(message.fileData, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size)
                    ivFilePreview.visibility = View.VISIBLE
                    ivFilePreview.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    ivFilePreview.visibility = View.GONE
                    tvMessageContent.text = "${message.content} (Preview not available)"
                }
            } else {
                ivFilePreview.visibility = View.GONE
            }

            // Initially hide the download button; it’s controlled by context menu
            btnDownload.visibility = View.GONE

            itemView.setOnLongClickListener {
                showContextMenu(itemView, message)
                true
            }
        }

        private fun showContextMenu(view: View, message: MessageData) {
            val popupMenu = PopupMenu(view.context, view)
            val menu = popupMenu.menu

            menu.add(0, 1, 0, "Delete")
            menu.add(0, 2, 1, "Copy")
            if (message.fileData != null) {
                menu.add(0, 3, 2, "Download")
            }

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> { // Delete
                        onDelete(message)
                        Toast.makeText(view.context, "Message deleted", Toast.LENGTH_SHORT).show()
                        true
                    }
                    2 -> { // Copy
                        val clipboard = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Message Content", message.content)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(view.context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        true
                    }
                    3 -> { // Download
                        onDownloadAndOpen(message)
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
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