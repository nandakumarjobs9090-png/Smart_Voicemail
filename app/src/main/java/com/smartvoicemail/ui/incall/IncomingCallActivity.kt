package com.smartvoicemail.ui.incall

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.smartvoicemail.databinding.ActivityIncomingCallBinding
import com.smartvoicemail.service.SmartCallService
import com.smartvoicemail.util.ContactHelper

class IncomingCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncomingCallBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockscreen()

        binding = ActivityIncomingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCallInfo()
        setupButtons()
        setupCallbacks()
    }

    private fun showOverLockscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguard = getSystemService(KeyguardManager::class.java)
            keyguard.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    private fun setupCallInfo() {
        val call = SmartCallService.currentCall
        val number = call?.details?.handle?.schemeSpecificPart ?: "Unknown"
        val name = ContactHelper.getContactName(this, number)

        binding.callerName.text = name ?: "Unknown Caller"
        binding.callerNumber.text = number
        binding.callerInitial.text = (name ?: number).firstOrNull()?.uppercase() ?: "?"
    }

    private fun setupButtons() {
        binding.btnAnswer.setOnClickListener {
            SmartCallService.instance?.answerCall()
            finish()
        }
        binding.btnReject.setOnClickListener {
            SmartCallService.instance?.rejectCall()
            finish()
        }
    }

    private fun setupCallbacks() {
        SmartCallService.onCallStateChanged = { state ->
            runOnUiThread {
                when (state) {
                    Call.STATE_DISCONNECTED -> finish()
                    Call.STATE_ACTIVE -> {
                        if (SmartCallService.isVoicemailActive) {
                            binding.statusText.text = "Recording voicemail..."
                        }
                    }
                }
            }
        }
        SmartCallService.onTimerTick = { secondsLeft ->
            runOnUiThread {
                binding.timerText.text = "Voicemail in ${secondsLeft}s"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SmartCallService.onCallStateChanged = null
        SmartCallService.onTimerTick = null
    }
}
