package me.amryousef.webrtc_demo

import android.util.Log
import io.ktor.util.*
import org.webrtc.*

open class PeerConnectionObserver : PeerConnection.Observer {
    override fun onIceCandidate(p0: IceCandidate?) {
        Log.v(this@PeerConnectionObserver.javaClass.simpleName, "onIceCandidate: ${p0.toString()}")

    }

    override fun onDataChannel(p0: DataChannel?) {
        Log.v(this@PeerConnectionObserver.javaClass.simpleName, "onDataChannel: ${p0.toString()}")

    }

    override fun onTrack(transceiver: RtpTransceiver?) {
        super.onTrack(transceiver)
        Log.v(this@PeerConnectionObserver.javaClass.simpleName, "onTrack: ${transceiver.toString()}")
    }

    override fun onIceConnectionReceivingChange(p0: Boolean) {
        Log.v(this@PeerConnectionObserver.javaClass.simpleName, "onIceConnectionReceivingChange: ${p0.toString()}")

    }

    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
        Log.i(this@PeerConnectionObserver.javaClass.simpleName, "onIceConnectionChange: ${p0.toString()}")
        if (p0 == PeerConnection.IceConnectionState.COMPLETED) {
        }
    }

    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
        Log.v(this@PeerConnectionObserver.javaClass.simpleName, "onIceGatheringChange: ${p0.toString()}")

    }

    override fun onAddStream(p0: MediaStream?) {
        Log.v(this@PeerConnectionObserver.javaClass.simpleName, "onAddStream: ${p0.toString()}")

    }

    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
        Log.v(this@PeerConnectionObserver.javaClass.simpleName, "onSignallingChange: ${p0.toString()}")
    }

    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
        Log.v(this@PeerConnectionObserver.javaClass.simpleName, "onIceCandidatesRemoved: ${p0.toString()}")
    }

    override fun onRemoveStream(p0: MediaStream?) {
        Log.v(this@PeerConnectionObserver.javaClass.simpleName, "onRemoveStream: ${p0.toString()}")

    }

    override fun onRenegotiationNeeded() {
        Log.v(this@PeerConnectionObserver.javaClass.simpleName, "onRenegotiationNeeded")

    }

    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
        Log.v(this@PeerConnectionObserver.javaClass.simpleName, "onAddTrack: ${p0.toString()} ${p1.toString()}")

    }
}