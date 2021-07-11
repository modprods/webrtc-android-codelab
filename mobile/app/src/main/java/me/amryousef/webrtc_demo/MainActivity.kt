package me.amryousef.webrtc_demo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Context
import androidx.core.view.isGone
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_main.call_button
//import kotlinx.android.synthetic.main.activity_main.local_view
import kotlinx.android.synthetic.main.activity_main.remote_view
import kotlinx.android.synthetic.main.activity_main.remote_view_loading
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import org.webrtc.AudioTrack
import android.media.AudioManager

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
class MainActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    }

    private lateinit var rtcClient: RTCClient
    private lateinit var signallingClient: SignallingClient
    private lateinit var remoteAudioTrack : AudioTrack
    private var currVolume = 0

    private val sdpObserver = object : AppSdpObserver() {
        override fun onCreateSuccess(p0: SessionDescription?) {
            super.onCreateSuccess(p0)
            signallingClient.send(p0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startNegotiation()
    }


    private fun openSpeaker() {
        try {
            val audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
             audioManager.mode = AudioManager.MODE_IN_CALL
            currVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
//            if (!audioManager.isSpeakerphoneOn) {
            //setSpeakerphoneOn() only work when audio mode set to MODE_IN_CALL.
            audioManager.isSpeakerphoneOn = true
            audioManager.isMicrophoneMute = true
//                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
//                        audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
//                        AudioManager.STREAM_VOICE_CALL)
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                10,
                AudioManager.STREAM_VOICE_CALL)
//            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    private fun closeSpeaker() {
        try {
            val audioManager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_IN_CALL
            if (audioManager != null) {
//                if (audioManager.isSpeakerphoneOn) {
                audioManager.isSpeakerphoneOn = false
//                    audioManager.isMicrophoneMute = false
                audioManager.setStreamVolume(
                    AudioManager.STREAM_VOICE_CALL, currVolume,
                    AudioManager.STREAM_VOICE_CALL
                )
//                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission()
        } else {
            startNegotiation()
        }
    }

    private fun startNegotiation() {
        rtcClient = RTCClient(
            application,
            object : PeerConnectionObserver() {
                override fun onIceCandidate(p0: IceCandidate?) {
                    Log.v(this@MainActivity.javaClass.simpleName, "onIceCandidate: ${p0.toString()}")

                    super.onIceCandidate(p0)
                    signallingClient.send(p0)
                    rtcClient.addIceCandidate(p0)
                }

                override fun onAddStream(p0: MediaStream?) {
                    super.onAddStream(p0)
                    Log.v(this@MainActivity.javaClass.simpleName, "onAddStream: ${p0.toString()}")
                    p0?.videoTracks?.get(0)?.addSink(remote_view)
                    Log.v(this@MainActivity.javaClass.simpleName, "addSink: $remote_view ")
                    if (p0?.audioTracks!!.isNotEmpty()) {
                        openSpeaker()
                        remoteAudioTrack = p0?.audioTracks!!.get(0)
                    }
                }
            }
        )
        rtcClient.initSurfaceView(remote_view)
//        rtcClient.initSurfaceView(local_view)
//        rtcClient.startLocalVideoCapture(local_view)
        signallingClient = SignallingClient(createSignallingClientListener())
        call_button.setOnClickListener { rtcClient.call(sdpObserver) }
    }

    private fun createSignallingClientListener() = object : SignallingClientListener {
        override fun onConnectionEstablished() {
            call_button.isClickable = true
        }

        override fun onOfferReceived(description: SessionDescription) {
            rtcClient.onRemoteSessionReceived(description)
            rtcClient.answer(sdpObserver)
            remote_view_loading.isGone = true
        }

        override fun onAnswerReceived(description: SessionDescription) {
            rtcClient.onRemoteSessionReceived(description)
            remote_view_loading.isGone = true
        }

        override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
            rtcClient.addIceCandidate(iceCandidate)
        }
    }

    private fun requestCameraPermission(dialogShown: Boolean = false) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA_PERMISSION) && !dialogShown) {
            showPermissionRationaleDialog()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(CAMERA_PERMISSION), CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Camera Permission Required")
            .setMessage("This app need the camera to function")
            .setPositiveButton("Grant") { dialog, _ ->
                dialog.dismiss()
                requestCameraPermission(true)
            }
            .setNegativeButton("Deny") { dialog, _ ->
                dialog.dismiss()
                onCameraPermissionDenied()
            }
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startNegotiation()
        } else {
            onCameraPermissionDenied()
        }
    }

    private fun onCameraPermissionDenied() {
        Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        closeSpeaker()
        signallingClient.destroy()
        super.onDestroy()
    }
}
