package com.olunaa.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
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
import kotlinx.coroutines.launch

class LiveKitCallActivity : AppCompatActivity() {
    private lateinit var room: Room
    private lateinit var status: TextView
    private lateinit var video: SurfaceViewRenderer
    private var muted = false
    private var cameraOff = false
    private var connected = false
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val audioGranted = grants[Manifest.permission.RECORD_AUDIO] == true
        val cameraNeeded = intent.getStringExtra("mode") == "video"
        val cameraGranted = !cameraNeeded || grants[Manifest.permission.CAMERA] == true
        if (audioGranted && cameraGranted) connectCall()
        else {
            Toast.makeText(this, "Microphone/camera permission is required.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(5, 10, 18)
        window.navigationBarColor = Color.rgb(5, 10, 18)
        buildUi()
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (intent.getStringExtra("mode") == "video") needed.add(Manifest.permission.CAMERA)
        val missing = needed.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) connectCall() else permissions.launch(missing.toTypedArray())
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 28, 24, 24)
            setBackgroundColor(Color.rgb(5, 10, 18))
        }
        status = TextView(this).apply {
            text = "Connecting securely…"
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        root.addView(status, LinearLayout.LayoutParams(-1, 72))
        video = SurfaceViewRenderer(this)
        root.addView(video, LinearLayout.LayoutParams(-1, 0, 1f))
        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        fun control(label: String, action: () -> Unit) {
            controls.addView(Button(this).apply { text = label; setOnClickListener { action() } },
                LinearLayout.LayoutParams(0, 58, 1f).apply { setMargins(4, 12, 4, 12) })
        }
        control("Mute") { toggleMute() }
        if (intent.getStringExtra("mode") == "video") control("Camera") { toggleCamera() }
        control("End") { endCall() }
        root.addView(controls, LinearLayout.LayoutParams(-1, 76))
        setContentView(root)
    }

    private fun connectCall() {
        val serverUrl = intent.getStringExtra("serverUrl").orEmpty()
        val token = intent.getStringExtra("participantToken").orEmpty()
        if (serverUrl.isBlank() || token.isBlank()) { status.text = "Call credentials are missing."; return }
        room = LiveKit.create(applicationContext)
        room.initVideoRenderer(video)
        lifecycleScope.launch {
            launch {
                room.events.collect { event ->
                    when (event) {
                        is RoomEvent.TrackSubscribed -> { val track = event.track; if (track is VideoTrack) track.addRenderer(video) }
                        is RoomEvent.ParticipantConnected -> runOnUiThread { status.text = "Connected · " + (event.participant.name.ifBlank { "OLUNAA user" }) }
                        is RoomEvent.ParticipantDisconnected -> runOnUiThread { status.text = "Participant left the call" }
                        else -> Unit
                    }
                }
            }
            try {
                room.connect(serverUrl, token)
                room.localParticipant.setMicrophoneEnabled(true)
                if (intent.getStringExtra("mode") == "video") room.localParticipant.setCameraEnabled(true)
                connected = true
                status.text = "Call connected"
            } catch (e: Exception) { status.text = "Could not connect: " + (e.message ?: "network error") }
        }
    }

    private fun toggleMute() {
        if (!connected) return
        muted = !muted
        lifecycleScope.launch { try { room.localParticipant.setMicrophoneEnabled(!muted) } catch (_: Exception) { } }
    }

    private fun toggleCamera() {
        if (!connected) return
        cameraOff = !cameraOff
        lifecycleScope.launch { try { room.localParticipant.setCameraEnabled(!cameraOff) } catch (_: Exception) { } }
    }

    private fun endCall() {
        if (::room.isInitialized) lifecycleScope.launch { try { room.disconnect() } catch (_: Exception) { }; finish() }
        else finish()
    }

    override fun onDestroy() {
        if (::room.isInitialized && connected) lifecycleScope.launch { try { room.disconnect() } catch (_: Exception) { } }
        super.onDestroy()
    }
}
