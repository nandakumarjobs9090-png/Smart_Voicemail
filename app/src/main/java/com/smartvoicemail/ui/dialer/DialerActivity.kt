package com.smartvoicemail.ui.dialer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.smartvoicemail.databinding.ActivityDialerBinding
import com.smartvoicemail.util.ContactHelper

class DialerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDialerBinding
    private val numberBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDialerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDialPad()
        setupButtons()
        handleIncomingIntent()
    }

    private fun handleIncomingIntent() {
        // Handle tel: intents from other apps
        intent?.data?.let { uri ->
            if (uri.scheme == "tel") {
                val number = uri.schemeSpecificPart ?: ""
                if (number.isNotEmpty()) {
                    numberBuilder.append(number)
                    updateDisplay()
                }
            }
        }
    }

    private fun setupDialPad() {
        val buttons = mapOf(
            binding.btn0 to "0",
            binding.btn1 to "1",
            binding.btn2 to "2",
            binding.btn3 to "3",
            binding.btn4 to "4",
            binding.btn5 to "5",
            binding.btn6 to "6",
            binding.btn7 to "7",
            binding.btn8 to "8",
            binding.btn9 to "9",
            binding.btnStar to "*",
            binding.btnHash to "#"
        )

        buttons.forEach { (button, digit) ->
            button.setOnClickListener {
                numberBuilder.append(digit)
                updateDisplay()
            }
        }

        // Long press 0 for +
        binding.btn0.setOnLongClickListener {
            if (numberBuilder.isEmpty() || numberBuilder.last() != '+') {
                numberBuilder.append("+")
                updateDisplay()
            }
            true
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnDelete.setOnClickListener {
            if (numberBuilder.isNotEmpty()) {
                numberBuilder.deleteCharAt(numberBuilder.length - 1)
                updateDisplay()
            }
        }

        binding.btnDelete.setOnLongClickListener {
            numberBuilder.clear()
            updateDisplay()
            true
        }

        binding.btnCall.setOnClickListener {
            makeCall()
        }
    }

    private fun updateDisplay() {
        val number = numberBuilder.toString()
        binding.numberDisplay.text = number

        if (number.isNotEmpty()) {
            binding.btnDelete.visibility = View.VISIBLE
            // Try to find contact name
            val contactName = ContactHelper.getContactName(this, number)
            if (contactName != null) {
                binding.contactName.text = contactName
                binding.contactName.visibility = View.VISIBLE
            } else {
                binding.contactName.visibility = View.GONE
            }
        } else {
            binding.btnDelete.visibility = View.GONE
            binding.contactName.visibility = View.GONE
        }
    }

    private fun makeCall() {
        val number = numberBuilder.toString().trim()
        if (number.isEmpty()) {
            Toast.makeText(this, "Enter a phone number", Toast.LENGTH_SHORT).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
            startActivity(intent)
        } else {
            // Fall back to ACTION_DIAL which doesn't need permission
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            startActivity(intent)
        }
    }
}
