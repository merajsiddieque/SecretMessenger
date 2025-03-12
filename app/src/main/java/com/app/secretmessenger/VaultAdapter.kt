package com.app.secretmessenger

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VaultAdapter(
    private val items: MutableList<VaultData>,
    private val onLongClick: (VaultData) -> Boolean,
    private val onClick: (VaultData) -> Unit,
    private val onDownloadClick: (VaultData) -> Unit
) : RecyclerView.Adapter<VaultAdapter.VaultViewHolder>() {

    inner class VaultViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUsername: TextView = view.findViewById(R.id.tvContent)
        val ivPreview: ImageView = view.findViewById(R.id.ivPreview)
        val btnDownload: Button = view.findViewById(R.id.btnDownload)

        fun bind(item: VaultData) {
            tvUsername.text = item.username

            if (item.image.isNotEmpty()) {
                try {
                    val imageBytes = Base64.decode(item.image, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    ivPreview.visibility = View.VISIBLE
                    ivPreview.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    ivPreview.visibility = View.GONE
                }
            } else {
                ivPreview.visibility = View.GONE
            }

            val isSelected = item in (itemView.context as PrivateVaultActivity).selectedItems

            // Apply background color based on selection
            itemView.setBackgroundColor(
                if (isSelected) itemView.context.getColor(R.color.light_gray)
                else itemView.context.getColor(android.R.color.white)
            )

            btnDownload.visibility = if (isSelected) View.VISIBLE else View.GONE
            btnDownload.setOnClickListener { onDownloadClick(item) }

            itemView.setOnClickListener { onClick(item) }
            itemView.setOnLongClickListener {
                val result = onLongClick(item)
                notifyItemChanged(adapterPosition) // Refresh the view after selection
                result
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_vault, parent, false)
        return VaultViewHolder(view)
    }

    override fun onBindViewHolder(holder: VaultViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}