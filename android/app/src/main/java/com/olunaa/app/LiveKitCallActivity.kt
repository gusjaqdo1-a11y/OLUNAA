package com.olunaa.app

import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
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
    private var callFinished = false
    private var pulseAnimator: ValueAnimator? = null
    private val callTimeout = Runnable {
        if (!remoteConnected && !callFinished) {
            callFinished = true
            stopRingback()
            status.text = "No answer"
            detail.text = "The person may be offline or unable to receive this call."
            handler.removeCallbacks(timer)
        }
    }
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
        window.statusBarColor = Color.rgb(10, 8, 32)
        window.navigationBarColor = Color.rgb(10, 8, 32)
        window.setBackgroundDrawable(GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(10, 8, 32), Color.rgb(15, 18, 55), Color.rgb(7, 25, 45))))
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
                intArrayOf(Color.rgb(10, 8, 32), Color.rgb(18, 13, 57), Color.rgb(7, 22, 44))
            )
        }

        val top = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        top.addView(label("OLUNAA  ·  SECURE CALL", 11f, Color.rgb(34, 211, 238), true),
            LinearLayout.LayoutParams(-1, dp(28)))
        status = label("Calling…", 25f, Color.WHITE, true)
        top.addView(status, LinearLayout.LayoutParams(-1, dp(42)))
        detail = label("Connecting to the other person", 13f, Color.rgb(174, 169, 207))
        top.addView(detail, LinearLayout.LayoutParams(-1, dp(30)))
        root.addView(top, LinearLayout.LayoutParams(-1, dp(104)))

        val stage = FrameLayout(this).apply {
            background = panel(Color.argb(165, 31, 20, 73), 34, Color.argb(125, 139, 92, 246)).apply { setStroke(dp(1), Color.argb(115, 34, 211, 238)) }
        }
        avatar = label("O", 54f, Color.WHITE, true).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(139, 92, 246), Color.rgb(34, 211, 238))).apply { shape = GradientDrawable.OVAL }
        }
        val avatarSize = dp(if (videoMode) 160 else 184)
        stage.addView(avatar, FrameLayout.LayoutParams(avatarSize, avatarSize, Gravity.CENTER))
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.075f).apply {
            duration = 1350L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val scale = animation.animatedValue as Float
                if (avatar.visibility == View.VISIBLE) { avatar.scaleX = scale; avatar.scaleY = scale }
            }
            start()
        }
        video = SurfaceViewRenderer(this).apply { visibility = if (videoMode) View.VISIBLE else View.GONE }
        if (videoMode) stage.addView(video, FrameLayout.LayoutParams(-1, -1))
        root.addView(stage, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            topMargin = dp(12); bottomMargin = dp(22)
        })

        val name = intent.getStringExtra("otherName").orEmpty().ifBlank { "OLUNAA contact" }
        root.addView(label(name, 20f, Color.WHITE, true), LinearLayout.LayoutParams(-1, dp(34)))
        root.addView(label(if (videoMode) "Video conversation" else "Voice conversation", 12f,
            Color.rgb(174, 169, 207)), LinearLayout.LayoutParams(-1, dp(28)))

        val controls = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(20), 0, 0)
        }
        muteControl = addControl(controls, "🎙", "Mute") { toggleMute() }
        speakerControl = addControl(controls, "🔊", "Speaker") { toggleSpeaker() }
        if (videoMode) cameraControl = addControl(controls, "▣", "Camera") { toggleCamera() }
        addControl(controls, "✕", "End") { endCall() }
        root.addView(controls, LinearLayout.LayoutParams(-1, dp(104)))
        setContentView(root)
    }

    private fun addControl(row: LinearLayout, symbol: String, caption: String, action: () -> Unit): TextView {
        val item = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        val button = label(symbol, 12f, Color.WHITE, true).apply {
            background = panel(if (caption == "End") Color.rgb(220, 55, 90) else Color.argb(205, 40, 27, 83),
                24, Color.argb(115, 139, 92, 246)).apply { setStroke(dp(1), Color.argb(105, 34, 211, 238)) }
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
                            callFinished = true
                            handler.removeCallbacks(callTimeout)
                            stopRingback()
                            status.text = "Connected"
                            detail.text = "00:00"
                            handler.removeCallbacks(timer)
                            handler.post(timer)
                            if (intent.getStringExtra("mode") == "video") avatar.visibility = View.GONE
                        }
                        is RoomEvent.ParticipantDisconnected -> runOnUiThread {
                            remoteConnected = false
                            callFinished = true
                            handler.removeCallbacks(callTimeout)
                            status.text = "Call ended"
                            detail.text = "The other person left the call."
                        }
                        else -> Unit
                    }
                }
            }
            try {
                status.text = "Calling…"
                detail.text = "Connecting securely · checking availability"
                room.connect(serverUrl, token)
                room.localParticipant.setMicrophoneEnabled(true)
                if (intent.getStringExtra("mode") == "video") room.localParticipant.setCameraEnabled(true)
                roomConnected = true
                if (!remoteConnected) {
                    status.text = "Calling…"
                    detail.text = "Invitation sent · waiting for answer"
                    startRingback()
                    handler.removeCallbacks(callTimeout)
                    handler.postDelayed(callTimeout, 45000L)
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
        callFinished = true
        handler.removeCallbacks(callTimeout)
        stopRingback()
        handler.removeCallbacks(timer)
        if (::room.isInitialized) lifecycleScope.launch {
            try { room.disconnect() } catch (_: Exception) { }
            finish()
        } else finish()
    }

    override fun onDestroy() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        stopRingback()
        handler.removeCallbacks(timer)
        handler.removeCallbacks(callTimeout)
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
