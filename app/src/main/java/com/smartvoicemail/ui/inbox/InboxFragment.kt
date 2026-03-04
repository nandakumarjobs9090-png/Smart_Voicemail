package com.smartvoicemail.ui.inbox

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.smartvoicemail.databinding.FragmentInboxBinding
import com.smartvoicemail.model.VoicemailEntry
import com.smartvoicemail.util.StorageHelper
import java.io.File

class InboxFragment : Fragment() {

    private var _binding: FragmentInboxBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: VoicemailAdapter
    private var mediaPlayer: MediaPlayer? = null
    private var currentlyPlayingId: String? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInboxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadVoicemails()
    }

    override fun onResume() {
        super.onResume()
        loadVoicemails()
    }

    private fun setupRecyclerView() {
        adapter = VoicemailAdapter(
            onPlay = { entry -> togglePlayback(entry) },
            onDelete = { entry -> confirmDelete(entry) },
            onShare = { entry -> shareVoicemail(entry) }
        )
        binding.recyclerVoicemails.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerVoicemails.adapter = adapter
    }

    private fun loadVoicemails() {
        val entries = StorageHelper.loadVoicemailEntries(requireContext())
        adapter.submitList(entries)
        binding.emptyState.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerVoicemails.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun togglePlayback(entry: VoicemailEntry) {
        if (currentlyPlayingId == entry.id) {
            stopPlayback()
            return
        }

        stopPlayback()
        currentlyPlayingId = entry.id
        StorageHelper.markAsRead(requireContext(), entry)

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(entry.filePath)
                prepare()
                setOnCompletionListener {
                    stopPlayback()
                    loadVoicemails()
                }
                start()
            }
            adapter.setPlayingId(entry.id)
        } catch (e: Exception) {
            stopPlayback()
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentlyPlayingId = null
        adapter.setPlayingId(null)
    }

    private fun confirmDelete(entry: VoicemailEntry) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Voicemail")
            .setMessage("Are you sure you want to delete this voicemail?")
            .setPositiveButton("Delete") { _, _ ->
                if (currentlyPlayingId == entry.id) stopPlayback()
                StorageHelper.deleteVoicemail(requireContext(), entry)
                loadVoicemails()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareVoicemail(entry: VoicemailEntry) {
        val file = File(entry.filePath)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/mp3"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Voicemail"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPlayback()
        _binding = null
    }
}
