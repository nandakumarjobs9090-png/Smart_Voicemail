package com.smartvoicemail.ui.greeting

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.smartvoicemail.databinding.FragmentGreetingBinding
import com.smartvoicemail.util.PrefsManager
import com.smartvoicemail.util.StorageHelper
import java.io.File
import java.io.FileOutputStream
import java.util.*

class GreetingFragment : Fragment() {

    private var _binding: FragmentGreetingBinding? = null
    private val binding get() = _binding!!

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = false
    private var isPlaying = false
    private var tempRecordingFile: File? = null
    private var tts: TextToSpeech? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val greetingFile = StorageHelper.getGreetingFile(requireContext())
                    requireContext().contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(greetingFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    PrefsManager.setGreetingSet(requireContext(), true)
                    updateGreetingStatus()
                    Toast.makeText(context, "Greeting uploaded successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to upload greeting", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGreetingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTTS()
        setupButtons()
        updateGreetingStatus()
    }

    private fun initTTS() {
        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
    }

    private fun setupButtons() {
        binding.btnRecord.setOnClickListener { toggleRecording() }
        binding.btnUpload.setOnClickListener { uploadGreeting() }
        binding.btnPlayGreeting.setOnClickListener { togglePlayGreeting() }
        binding.btnResetGreeting.setOnClickListener { resetGreeting() }
        binding.btnGenerateDefault.setOnClickListener { generateDefaultGreeting() }
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        val file = File(requireContext().cacheDir, "temp_greeting.mp3")
        tempRecordingFile = file

        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(requireContext())
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            binding.btnRecord.text = "⏹ Stop Recording"
            binding.recordingIndicator.visibility = View.VISIBLE
            binding.btnUpload.isEnabled = false
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to start recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) { /* ignore */ }
        mediaRecorder = null
        isRecording = false

        binding.btnRecord.text = "🎙 Record Greeting"
        binding.recordingIndicator.visibility = View.GONE
        binding.btnUpload.isEnabled = true

        // Copy temp file to greeting location
        tempRecordingFile?.let { temp ->
            if (temp.exists() && temp.length() > 0) {
                val greetingFile = StorageHelper.getGreetingFile(requireContext())
                temp.copyTo(greetingFile, overwrite = true)
                temp.delete()
                PrefsManager.setGreetingSet(requireContext(), true)
                updateGreetingStatus()
                Toast.makeText(context, "Greeting saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadGreeting() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(intent)
    }

    private fun togglePlayGreeting() {
        if (isPlaying) {
            stopPlaying()
        } else {
            playGreeting()
        }
    }

    private fun playGreeting() {
        val greetingFile = StorageHelper.getGreetingFile(requireContext())
        if (!greetingFile.exists()) {
            Toast.makeText(context, "No greeting recorded yet", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(greetingFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    stopPlaying()
                }
                start()
            }
            isPlaying = true
            binding.btnPlayGreeting.text = "⏸ Stop Playing"
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to play greeting", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopPlaying() {
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        binding.btnPlayGreeting.text = "▶ Play Greeting"
    }

    private fun resetGreeting() {
        val greetingFile = StorageHelper.getGreetingFile(requireContext())
        if (greetingFile.exists()) greetingFile.delete()
        PrefsManager.setGreetingSet(requireContext(), false)
        updateGreetingStatus()
        Toast.makeText(context, "Greeting reset to default", Toast.LENGTH_SHORT).show()
    }

    private fun generateDefaultGreeting() {
        val greetingFile = StorageHelper.getGreetingFile(requireContext())
        val defaultText = "Sorry, I can't answer your call right now. Please leave a message after the beep."

        tts?.synthesizeToFile(
            defaultText,
            Bundle(),
            greetingFile,
            "greeting_synthesis"
        )

        // TTS synthesis is async, wait briefly
        binding.root.postDelayed({
            if (greetingFile.exists() && greetingFile.length() > 0) {
                PrefsManager.setGreetingSet(requireContext(), true)
                updateGreetingStatus()
                Toast.makeText(context, "Default greeting generated!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to generate greeting. Try again.", Toast.LENGTH_SHORT).show()
            }
        }, 3000)
    }

    private fun updateGreetingStatus() {
        val isSet = PrefsManager.isGreetingSet(requireContext())
        binding.greetingStatus.text = if (isSet) "✅ Custom greeting is set" else "⚠ No greeting set — use buttons below"
        binding.btnPlayGreeting.isEnabled = isSet
        binding.btnResetGreeting.isEnabled = isSet
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isRecording) {
            try { mediaRecorder?.stop() } catch (_: Exception) {}
            mediaRecorder?.release()
        }
        mediaPlayer?.release()
        tts?.shutdown()
        _binding = null
    }
}
