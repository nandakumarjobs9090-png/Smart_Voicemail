package com.smartvoicemail.ui.inbox

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartvoicemail.R
import com.smartvoicemail.databinding.ItemVoicemailBinding
import com.smartvoicemail.model.VoicemailEntry
import com.smartvoicemail.util.AudioHelper
import java.text.SimpleDateFormat
import java.util.*

class VoicemailAdapter(
    private val onPlay: (VoicemailEntry) -> Unit,
    private val onDelete: (VoicemailEntry) -> Unit,
    private val onShare: (VoicemailEntry) -> Unit
) : ListAdapter<VoicemailEntry, VoicemailAdapter.ViewHolder>(DiffCallback()) {

    private var playingId: String? = null

    fun setPlayingId(id: String?) {
        val oldId = playingId
        playingId = id
        currentList.forEachIndexed { index, entry ->
            if (entry.id == oldId || entry.id == id) {
                notifyItemChanged(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVoicemailBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemVoicemailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: VoicemailEntry) {
            val context = binding.root.context
            val isPlaying = entry.id == playingId

            // Caller info
            binding.callerName.text = entry.callerName ?: entry.callerNumber
            if (entry.callerName != null) {
                binding.callerNumber.text = entry.callerNumber
            } else {
                binding.callerNumber.text = ""
            }

            // Initial avatar
            val initial = (entry.callerName ?: entry.callerNumber).firstOrNull()?.uppercase() ?: "?"
            binding.callerInitial.text = initial

            // Date and time
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val date = Date(entry.timestamp)
            binding.dateText.text = dateFormat.format(date)
            binding.timeText.text = timeFormat.format(date)

            // Duration
            binding.durationText.text = "Duration: ${AudioHelper.formatDuration(entry.duration)}"

            // Unread indicator
            if (!entry.isRead) {
                binding.callerName.setTextColor(
                    ContextCompat.getColor(context, R.color.primary)
                )
                binding.unreadDot.visibility = android.view.View.VISIBLE
            } else {
                binding.callerName.setTextColor(
                    ContextCompat.getColor(context, R.color.text_primary)
                )
                binding.unreadDot.visibility = android.view.View.GONE
            }

            // Play button state
            binding.btnPlay.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
            binding.btnPlay.contentDescription = if (isPlaying) "Pause" else "Play"

            // Click listeners
            binding.btnPlay.setOnClickListener { onPlay(entry) }
            binding.btnDelete.setOnClickListener { onDelete(entry) }
            binding.btnShare.setOnClickListener { onShare(entry) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<VoicemailEntry>() {
        override fun areItemsTheSame(oldItem: VoicemailEntry, newItem: VoicemailEntry) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: VoicemailEntry, newItem: VoicemailEntry) =
            oldItem == newItem
    }
}
