package com.olunaa.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.renderer.SurfaceViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class LiveKitCallActivity : AppCompatActivity() {
    private lateinit var room: Room
    private lateinit var status: TextView
    private lateinit var detail: TextView
    private lateinit var avatar: TextView
    private lateinit var video: SurfaceViewRenderer
    private lateinit var muteControl: TextView
    private lateinit var speakerControl: TextView
    private lateinit var cameraControl: TextView
    private lateinit var audioManager: AudioManager
    private var tone: ToneGenerator? = null
    private var muted = false
    private var cameraOff = false
    private var speakerOn = false
    private var roomConnected = false
    private var remoteConnected = false
    private var seconds = 0
    private val handler = Handler(Looper.getMainLooper())
    private var previousAudioMode = AudioManager.MODE_NORMAL
    private var previousSpeakerState = false
    private val timer = object : Runnable {
        override fun run() {
            if (remoteConnected) {
                seconds++
                detail.text = String.format("%02d:%02d", seconds / 60, seconds % 60)
                handler.postDelayed(this, 1000)
            }
        }
    }
    private val ringback = object : Runnable {
        override fun run() {
            if (!remoteConnected && roomConnected) {
                try { tone?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 2000) } catch (_: Exception) { }
                handler.postDelayed(this, 3500)
            }
        }
    }

    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val audioGranted = grants[Manifest.permission.RECORD_AUDIO] == true
        val cameraNeeded = intent.getStringExtra("mode") == "video"
        val cameraGranted = !cameraNeeded || grants[Manifest.permission.CAMERA] == true
        if (audioGranted && cameraGranted) connectCall() else {
            Toast.makeText(this, "Microphone/camera permission is required for this call.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        previousAudioMode = audioManager.mode
        previousSpeakerState = audioManager.isSpeakerphoneOn
        window.statusBarColor = Color.rgb(5, 9, 22)
        window.navigationBarColor = Color.rgb(5, 9, 22)
        buildUi()
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (intent.getStringExtra("mode") == "video") needed.add(Manifest.permission.CAMERA)
        val missing = needed.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) connectCall() else permissions.launch(missing.toTypedArray())
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun panel(color: Int, radius: Int, stroke: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != 0) setStroke(dp(1), stroke)
        }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun buildUi() {
        val videoMode = intent.getStringExtra("mode") == "video"
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(22), dp(28), dp(22), dp(24))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(8, 15, 35), Color.rgb(7, 24, 48), Color.rgb(10, 10, 28))
            )
        }

        val top = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        top.addView(label("OLUNAA  ·  SECURE CALL", 11f, Color.rgb(130, 169, 255), true),
            LinearLayout.LayoutParams(-1, dp(28)))
        status = label("Calling…", 25f, Color.WHITE, true)
        top.addView(status, LinearLayout.LayoutParams(-1, dp(42)))
        detail = label("Connecting to the other person", 13f, Color.rgb(158, 174, 205))
        top.addView(detail, LinearLayout.LayoutParams(-1, dp(30)))
        root.addView(top, LinearLayout.LayoutParams(-1, dp(104)))

        val stage = FrameLayout(this).apply {
            background = panel(Color.argb(95, 25, 42, 78), 34, Color.argb(90, 95, 139, 220))
        }
        avatar = label("O", 54f, Color.WHITE, true).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(45, 111, 255), Color.rgb(111, 75, 245))).apply { shape = GradientDrawable.OVAL }
        }
        val avatarSize = dp(if (videoMode) 160 else 184)
        stage.addView(avatar, FrameLayout.LayoutParams(avatarSize, avatarSize, Gravity.CENTER))
        video = SurfaceViewRenderer(this).apply { visibility = if (videoMode) View.VISIBLE else View.GONE }
        if (videoMode) stage.addView(video, FrameLayout.LayoutParams(-1, -1))
        root.addView(stage, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            topMargin = dp(12); bottomMargin = dp(22)
        })

        val name = intent.getStringExtra("otherName").orEmpty().ifBlank { "OLUNAA contact" }
        root.addView(label(name, 20f, Color.WHITE, true), LinearLayout.LayoutParams(-1, dp(34)))
        root.addView(label(if (videoMode) "Video conversation" else "Voice conversation", 12f,
            Color.rgb(145, 164, 199)), LinearLayout.LayoutParams(-1, dp(28)))

        val controls = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(20), 0, 0)
        }
        muteControl = addControl(controls, "Mic", "Mute") { toggleMute() }
        speakerControl = addControl(controls, "Speaker", "Speaker") { toggleSpeaker() }
        if (videoMode) cameraControl = addControl(controls, "Video", "Camera") { toggleCamera() }
        addControl(controls, "End", "End") { endCall() }
        root.addView(controls, LinearLayout.LayoutParams(-1, dp(104)))
        setContentView(root)
    }

    private fun addControl(row: LinearLayout, symbol: String, caption: String, action: () -> Unit): TextView {
        val item = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        val button = label(symbol, 12f, Color.WHITE, true).apply {
            background = panel(if (caption == "End") Color.rgb(224, 54, 77) else Color.rgb(35, 52, 83),
                24, Color.argb(70, 140, 170, 230))
            setOnClickListener { action() }
        }
        item.addView(button, LinearLayout.LayoutParams(dp(62), dp(58)))
        item.addView(label(caption, 11f, Color.rgb(185, 199, 224)), LinearLayout.LayoutParams(dp(76), dp(28)))
        row.addView(item, LinearLayout.LayoutParams(0, -1, 1f))
        return button
    }

    private fun connectCall() {
        val serverUrl = intent.getStringExtra("serverUrl").orEmpty()
        val token = intent.getStringExtra("participantToken").orEmpty()
        if (serverUrl.isBlank() || token.isBlank()) {
            status.text = "Call unavailable"
            detail.text = "The calling service is not configured."
            return
        }
        room = LiveKit.create(applicationContext)
        room.initVideoRenderer(video)
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        lifecycleScope.launch {
            launch {
                @Suppress("UNCHECKED_CAST")
                val eventFlow = room.events as Flow<RoomEvent>
                eventFlow.collect { event ->
                    when (event) {
                        is RoomEvent.TrackSubscribed -> {
                            val track = event.track
                            if (track is VideoTrack) track.addRenderer(video)
                        }
                        is RoomEvent.ParticipantConnected -> runOnUiThread {
                            remoteConnected = true
                            stopRingback()
                            status.text = "Connected"
                            detail.text = "00:00"
                            handler.removeCallbacks(timer)
                            handler.post(timer)
                            if (intent.getStringExtra("mode") == "video") avatar.visibility = View.GONE
                        }
                        is RoomEvent.ParticipantDisconnected -> runOnUiThread {
                            remoteConnected = false
                            status.text = "Call ended"
                            detail.text = "The other person left the call."
                        }
                        else -> Unit
                    }
                }
            }
            try {
                status.text = "Calling…"
                detail.text = "Waiting for the other person"
                room.connect(serverUrl, token)
                room.localParticipant.setMicrophoneEnabled(true)
                if (intent.getStringExtra("mode") == "video") room.localParticipant.setCameraEnabled(true)
                roomConnected = true
                if (!remoteConnected) {
                    status.text = "Ringing…"
                    detail.text = "Waiting for the other person to answer"
                    startRingback()
                }
            } catch (e: Exception) {
                stopRingback()
                status.text = "Could not connect"
                detail.text = e.message ?: "Check your connection and try again."
            }
        }
    }

    private fun startRingback() {
        stopRingback()
        try {
            tone = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 70)
            handler.removeCallbacks(ringback)
            handler.post(ringback)
        } catch (_: Exception) { }
    }

    private fun stopRingback() {
        handler.removeCallbacks(ringback)
        try { tone?.stopTone(); tone?.release() } catch (_: Exception) { }
        tone = null
    }

    private fun toggleMute() {
        if (!roomConnected) return
        muted = !muted
        muteControl.text = if (muted) "Muted" else "Mic"
        lifecycleScope.launch { try { room.localParticipant.setMicrophoneEnabled(!muted) } catch (_: Exception) { } }
    }

    private fun toggleSpeaker() {
        speakerOn = !speakerOn
        audioManager.isSpeakerphoneOn = speakerOn
        speakerControl.text = if (speakerOn) "On" else "Speaker"
    }

    private fun toggleCamera() {
        if (!roomConnected) return
        cameraOff = !cameraOff
        cameraControl.text = if (cameraOff) "Off" else "Video"
        lifecycleScope.launch { try { room.localParticipant.setCameraEnabled(!cameraOff) } catch (_: Exception) { } }
    }

    private fun endCall() {
        stopRingback()
        handler.removeCallbacks(timer)
        if (::room.isInitialized) lifecycleScope.launch {
            try { room.disconnect() } catch (_: Exception) { }
            finish()
        } else finish()
    }

    override fun onDestroy() {
        stopRingback()
        handler.removeCallbacks(timer)
        try {
            audioManager.isSpeakerphoneOn = previousSpeakerState
            audioManager.mode = previousAudioMode
        } catch (_: Exception) { }
        if (::room.isInitialized && roomConnected) lifecycleScope.launch {
            try { room.disconnect() } catch (_: Exception) { }
        }
        super.onDestroy()
    }
}
