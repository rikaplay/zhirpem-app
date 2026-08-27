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

    private val _isCallActive = MutableStateFlow(false)
    val isCallActive = _isCallActive.asStateFlow()

    private val _showIncomingOverlay = MutableStateFlow(false)
    val showIncomingOverlay = _showIncomingOverlay.asStateFlow()

    private val _connectionState = MutableStateFlow(PeerConnection.IceConnectionState.NEW)
    val connectionState = _connectionState.asStateFlow()

    private val _isVideoEnabled = MutableStateFlow(true)
    val isVideoEnabled = _isVideoEnabled.asStateFlow()

    private val _isAudioEnabled = MutableStateFlow(true)
    val isAudioEnabled = _isAudioEnabled.asStateFlow()

    private val observer = object : PeerConnection.Observer {
        override fun onSignalingChange(state: PeerConnection.SignalingState?) {
            android.util.Log.d("CallViewModel", "Signaling State: $state")
        }
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
            android.util.Log.d("CallViewModel", "ICE Connection State: $state")
            state?.let { _connectionState.value = it }
        }
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

    private var pendingRemoteSdp: SessionDescription? = null

    private fun setupSignaling() {
        signalingClient.onOfferReceived = { sdp ->
            if (!_isCallActive.value) {
                pendingRemoteSdp = sdp
                _isCallActive.value = true
                _showIncomingOverlay.value = true
                // We don't start capture or answer yet, waiting for user to click Accept
            }
        }
        signalingClient.onAnswerReceived = { sdp ->
            android.util.Log.d("CallViewModel", "Received Answer from signaling")
            webRtcManager.onRemoteSessionDescription(sdp)
        }
        signalingClient.onIceCandidateReceived = { candidate ->
            android.util.Log.d("CallViewModel", "Received ICE candidate from signaling")
            webRtcManager.addIceCandidate(candidate)
        }
    }

    fun acceptCall() {
        _showIncomingOverlay.value = false
        webRtcManager.startLocalCapture()
        webRtcManager.createPeerConnection(observer)
        pendingRemoteSdp?.let {
            webRtcManager.onRemoteSessionDescription(it)
            webRtcManager.answer()
        }
        pendingRemoteSdp = null
    }

    fun rejectCall() {
        hangup()
    }

    fun initLocalSurface(renderer: SurfaceViewRenderer) {
        webRtcManager.initLocalSurface(renderer)
        // Ensure capture is started, then add sink
        webRtcManager.startLocalCapture()
        webRtcManager.addLocalSink(renderer)
    }

    fun initRemoteSurface(renderer: SurfaceViewRenderer) {
        webRtcManager.initRemoteSurface(renderer)
        webRtcManager.addRemoteSink(renderer)
    }

    fun startCall() {
        _isCallActive.value = true
        // Start capture BEFORE creating peer connection
        webRtcManager.startLocalCapture()
        webRtcManager.createPeerConnection(observer)
        webRtcManager.call()
    }

    fun toggleVideo() {
        _isVideoEnabled.value = !_isVideoEnabled.value
        webRtcManager.toggleVideo(_isVideoEnabled.value)
    }

    fun toggleAudio() {
        _isAudioEnabled.value = !_isAudioEnabled.value
        webRtcManager.toggleAudio(_isAudioEnabled.value)
    }

    fun hangup() {
        _isCallActive.value = false
        webRtcManager.dispose()
        signalingClient.clear()
        _remoteVideoTrack.value = null
        _localVideoTrack.value = null
    }

    override fun onCleared() {
        super.onCleared()
        webRtcManager.release()
    }
}
