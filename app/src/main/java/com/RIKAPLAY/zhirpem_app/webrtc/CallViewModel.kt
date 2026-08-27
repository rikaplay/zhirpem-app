package com.RIKAPLAY.zhirpem_app.webrtc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.*

class CallViewModel(
    application: Application,
    private val signalingClient: SignalingClient
) : AndroidViewModel(application) {

    private val webRtcManager = WebRtcManager(application, signalingClient)
    
    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack = _remoteVideoTrack.asStateFlow()

    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack = _localVideoTrack.asStateFlow()

    private val observer = object : PeerConnection.Observer {
        override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
        override fun onIceConnectionReceivingChange(receiving: Boolean) {}
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
        override fun onIceCandidate(candidate: IceCandidate?) {
            candidate?.let { signalingClient.sendIceCandidate(it) }
        }
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
        override fun onAddStream(stream: MediaStream?) {
            stream?.videoTracks?.firstOrNull()?.let {
                _remoteVideoTrack.value = it
            }
        }
        override fun onRemoveStream(stream: MediaStream?) {}
        override fun onDataChannel(channel: DataChannel?) {}
        override fun onRenegotiationNeeded() {}
        override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
            receiver?.track()?.let { track ->
                if (track is VideoTrack) {
                    _remoteVideoTrack.value = track
                }
            }
        }
    }

    init {
        setupSignaling()
    }

    private fun setupSignaling() {
        signalingClient.onOfferReceived = { sdp ->
            webRtcManager.onRemoteSessionDescription(sdp)
            webRtcManager.answer()
        }
        signalingClient.onAnswerReceived = { sdp ->
            webRtcManager.onRemoteSessionDescription(sdp)
        }
        signalingClient.onIceCandidateReceived = { candidate ->
            webRtcManager.addIceCandidate(candidate)
        }
    }

    fun initLocalSurface(renderer: SurfaceViewRenderer) {
        webRtcManager.initLocalSurface(renderer)
        webRtcManager.startLocalCapture(renderer)
    }

    fun initRemoteSurface(renderer: SurfaceViewRenderer) {
        webRtcManager.initRemoteSurface(renderer)
    }

    fun startCall() {
        webRtcManager.createPeerConnection(observer)
        webRtcManager.call()
    }

    fun hangup() {
        webRtcManager.dispose()
        signalingClient.clear()
    }

    override fun onCleared() {
        super.onCleared()
        webRtcManager.dispose()
    }
}
