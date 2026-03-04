# 📬 Smart Voicemail

**Smart Voicemail** is an intelligent voicemail system for Android that automatically answers missed calls, plays a custom greeting, and records the caller's message — all without requiring any carrier voicemail service.

---

## 📱 About

Never miss an important message again. Smart Voicemail transforms your Android phone into a fully functional voicemail system. When you can't answer a call, the app automatically picks up after a configurable delay, plays your personalized greeting, and records the caller's voice message — saving it directly to your phone.

No carrier plans. No monthly fees. No cloud storage. Everything stays on your device.

---

## ✨ Features

### 📞 Automatic Call Answering
- Automatically answers incoming calls after a configurable delay (5, 10, 15, or 20 seconds)
- Seamlessly integrates with Android's Telecom system as the default dialer
- Shows a full-screen incoming call UI with caller info and countdown timer

### 🎙️ Custom Greetings
- **Record** your own greeting using the built-in microphone
- **Upload** a greeting file (supports MP3, WAV, M4A)
- **Generate** a default greeting using Text-to-Speech
- Reset to default anytime

### 🔴 Voicemail Recording
- Records the caller's message after a beep tone
- Configurable maximum recording time (30s, 60s, 90s, 2 min)
- Automatically stops when the caller hangs up
- Saves recordings in AAC/MP3 format

### 📬 Voicemail Inbox
- View all received voicemails with caller name/number
- See date, time, and duration for each message
- Unread message indicators
- **Play** voicemails directly in the app
- **Share** voicemails via any app (WhatsApp, Email, etc.)
- **Delete** voicemails with confirmation

### ⚙️ Configurable Settings
- Answer delay timer (5–20 seconds)
- Maximum recording duration
- Auto-delete old voicemails (7, 14, 30, or 60 days)

### 🔒 Privacy First
- All voicemails stored **locally on your device**
- No cloud uploads, no data collection
- No internet connection required
- Full compliance with Google Play policies

---

## 📋 How It Works

```
Incoming Call
     ↓
Phone Rings → Incoming Call Screen Shown
     ↓
Timer Countdown Starts (default: 15 seconds)
     ↓
User Answers? → YES → Normal Call (voicemail cancelled)
     ↓ NO
Auto-Answer Call
     ↓
Play Custom Greeting
     ↓
Play Beep Tone
     ↓
Record Caller's Message (max 60 seconds)
     ↓
Save Voicemail to Local Storage
     ↓
End Call Automatically
```

---

## 🔧 Technical Details

| Specification | Detail |
|---|---|
| **Platform** | Android |
| **Min SDK** | Android 9 (API 28) |
| **Language** | Kotlin |
| **Architecture** | Single Activity + Fragments |
| **UI** | Material Design 3 |
| **Navigation** | Jetpack Navigation Component |
| **Call Handling** | Android Telecom API (InCallService) |
| **Audio Recording** | MediaRecorder (AAC codec) |
| **Audio Playback** | MediaPlayer + AudioTrack |
| **Storage** | Internal app storage (JSON metadata) |
| **Greeting TTS** | Android TextToSpeech engine |

---

## 📂 File Storage Structure

```
Internal Storage / SmartVoicemail /
     │
     ├── greeting/
     │      greeting.mp3
     │
     ├── voicemails/
     │      vm_20260304_1015.mp3
     │      vm_20260304_1130.mp3
     │
     └── voicemails.json (metadata)
```

---

## 📜 Required Permissions

| Permission | Purpose |
|---|---|
| `ANSWER_PHONE_CALLS` | Auto-answer incoming calls |
| `READ_PHONE_STATE` | Detect incoming call state |
| `RECORD_AUDIO` | Record voicemail messages & greetings |
| `READ_CALL_LOG` | Access caller information |
| `READ_CONTACTS` | Display caller names |
| `CALL_PHONE` | Required for default dialer role |
| `FOREGROUND_SERVICE` | Keep voicemail recording active |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- JDK 17
- Physical Android device (Android 9+)

### Build & Run
1. Clone this repository
2. Open the project in Android Studio
3. Sync Gradle dependencies
4. Connect a physical Android device
5. Run the app
6. Grant all requested permissions
7. Set Smart Voicemail as your default phone app when prompted

> ⚠️ **Important:** The app must be set as the default dialer to access InCallService APIs. Emulators have limited support for telephony features — use a physical device for testing.

---

## 🗺️ Future Roadmap

- [ ] Voicemail transcription (Speech-to-Text)
- [ ] Spam call detection
- [ ] Cloud backup & sync
- [ ] New voicemail push notifications
- [ ] Call screening integration
- [ ] Widget for quick access

---

## 📄 License

This project is licensed under the MIT License.

---

## 👤 Author

Built with ❤️ for Android

---

*Smart Voicemail — Your phone, your rules. Never miss a message.*
