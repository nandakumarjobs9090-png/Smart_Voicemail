package com.smartvoicemail.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.smartvoicemail.databinding.FragmentSettingsBinding
import com.smartvoicemail.util.PrefsManager

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val delayOptions = listOf(5, 10, 15, 20)
    private val delayLabels = listOf("5 seconds", "10 seconds", "15 seconds", "20 seconds")

    private val maxRecordingOptions = listOf(30, 60, 90, 120)
    private val maxRecordingLabels = listOf("30 seconds", "60 seconds", "90 seconds", "2 minutes")

    private val autoDeleteOptions = listOf(0, 7, 14, 30, 60)
    private val autoDeleteLabels = listOf("Never", "After 7 days", "After 14 days", "After 30 days", "After 60 days")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDelaySpinner()
        setupMaxRecordingSpinner()
        setupAutoDeleteSpinner()
    }

    private fun setupDelaySpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, delayLabels)
        binding.spinnerDelay.adapter = adapter

        val currentDelay = PrefsManager.getAnswerDelay(requireContext())
        val index = delayOptions.indexOf(currentDelay)
        if (index >= 0) binding.spinnerDelay.setSelection(index)

        binding.spinnerDelay.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                PrefsManager.setAnswerDelay(requireContext(), delayOptions[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupMaxRecordingSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, maxRecordingLabels)
        binding.spinnerMaxRecording.adapter = adapter

        val currentMax = PrefsManager.getMaxRecordingTime(requireContext())
        val index = maxRecordingOptions.indexOf(currentMax)
        if (index >= 0) binding.spinnerMaxRecording.setSelection(index)

        binding.spinnerMaxRecording.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                PrefsManager.setMaxRecordingTime(requireContext(), maxRecordingOptions[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupAutoDeleteSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, autoDeleteLabels)
        binding.spinnerAutoDelete.adapter = adapter

        val currentDelete = PrefsManager.getAutoDeleteDays(requireContext())
        val index = autoDeleteOptions.indexOf(currentDelete)
        if (index >= 0) binding.spinnerAutoDelete.setSelection(index)

        binding.spinnerAutoDelete.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                PrefsManager.setAutoDeleteDays(requireContext(), autoDeleteOptions[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
