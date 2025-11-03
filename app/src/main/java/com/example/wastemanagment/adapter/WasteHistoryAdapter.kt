package com.example.wastemanagment.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.wastemanagment.R
import com.example.wastemanagment.model.WasteRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WasteHistoryAdapter : ListAdapter<WasteRecord, WasteHistoryAdapter.WasteViewHolder>(WasteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WasteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_waste_history, parent, false)
        return WasteViewHolder(view)
    }

    override fun onBindViewHolder(holder: WasteViewHolder, position: Int) {
        val record = getItem(position)
        holder.bind(record)
    }

    class WasteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageViewWaste: ImageView = itemView.findViewById(R.id.imageViewWaste)
        private val textViewWasteType: TextView = itemView.findViewById(R.id.textViewWasteType)
        private val textViewConfidence: TextView = itemView.findViewById(R.id.textViewConfidence)
        private val textViewDate: TextView = itemView.findViewById(R.id.textViewDate)
        private val textViewAdvice: TextView = itemView.findViewById(R.id.textViewAdvice)

        fun bind(record: WasteRecord) {
            textViewWasteType.text = record.wasteType
            textViewConfidence.text = "${(record.confidence * 100).toInt()}% confidence"
            textViewAdvice.text = record.disposalAdvice

            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            textViewDate.text = dateFormat.format(Date(record.timestamp))

            record.imageUri?.let { uriString ->
                try {
                    val uri = Uri.parse(uriString)
                    imageViewWaste.setImageURI(uri)
                } catch (e: Exception) {
                    imageViewWaste.setImageResource(R.drawable.ic_launcher_foreground)
                }
            } ?: run {
                imageViewWaste.setImageResource(R.drawable.ic_launcher_foreground)
            }
        }
    }

    class WasteDiffCallback : DiffUtil.ItemCallback<WasteRecord>() {
        override fun areItemsTheSame(oldItem: WasteRecord, newItem: WasteRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WasteRecord, newItem: WasteRecord): Boolean {
            return oldItem == newItem
        }
    }
}