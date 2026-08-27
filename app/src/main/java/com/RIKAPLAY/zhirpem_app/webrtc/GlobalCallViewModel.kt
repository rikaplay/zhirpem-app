package com.RIKAPLAY.zhirpem_app.webrtc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.*

class GlobalCallViewModel(
    private val application: Application
) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private var signalingClient: SignalingClient? = null
    private var webRtcManager: WebRtcManager? = null

    private val _isCallActive = MutableStateFlow(false)
    val isCallActive = _isCallActive.asStateFlow()

    private val _showIncomingOverlay = MutableStateFlow(false)
    val showIncomingOverlay = _showIncomingOverlay.asStateFlow()

    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId = _currentChatId.asStateFlow()

    private val _peerName = MutableStateFlow("Чат")
    val peerName = _peerName.asStateFlow()

    private val _connectionState = MutableStateFlow(PeerConnection.IceConnectionState.NEW)
    val connectionState = _connectionState.asStateFlow()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack = _remoteVideoTrack.asStateFlow()

    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack = _localVideoTrack.asStateFlow()

    private val _isVideoEnabled = MutableStateFlow(true)
    val isVideoEnabled = _isVideoEnabled.asStateFlow()

    private val _isAudioEnabled = MutableStateFlow(true)
    val isAudioEnabled = _isAudioEnabled.asStateFlow()

    private var pendingRemoteSdp: SessionDescription? = null
    private var myUsername: String? = null

    private val observer = object : PeerConnection.Observer {
        override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
            state?.let { _connectionState.value = it }
        }
        override fun onIceConnectionReceivingChange(receiving: Boolean) {}
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
        override fun onIceCandidate(candidate: IceCandidate?) {
            candidate?.let { signalingClient?.sendIceCandidate(it) }
        }
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
        override fun onAddStream(stream: MediaStream?) {
            stream?.videoTracks?.firstOrNull()?.let { _remoteVideoTrack.value = it }
        }
        override fun onRemoveStream(stream: MediaStream?) {}
        override fun onDataChannel(channel: DataChannel?) {}
        override fun onRenegotiationNeeded() {}
        override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
            receiver?.track()?.let { if (it is VideoTrack) _remoteVideoTrack.value = it }
        }
    }

    fun init(username: String) {
        if (myUsername == username) return
        myUsername = username
        listenForIncomingCalls()
    }

    private fun listenForIncomingCalls() {
        val username = myUsername ?: return
        db.collection("calls")
            .whereEqualTo("recipientId", username)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                
                // Only take the newest call if multiple exist
                val doc = snapshot.documents.maxByOrNull { it.getLong("timestamp") ?: 0L }
                if (doc != null) {
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    // Only react to calls started in the last 30 seconds
                    if (System.currentTimeMillis() - timestamp < 30000 && !_isCallActive.value) {
                        val chatId = doc.id
                        val offerMap = doc.get("offer") as? Map<*, *>
                        val sdp = offerMap?.get("sdp") as? String
                        
                        if (sdp != null) {
                            startIncomingCall(chatId, sdp)
                        }
                    }
                }
            }
    }

    private fun startIncomingCall(chatId: String, sdp: String) {
        _currentChatId.value = chatId
        val peerId = chatId.split("_").firstOrNull { it != myUsername } ?: ""
        db.collection("users").document(peerId).get().addOnSuccessListener {
            _peerName.value = it.getString("name") ?: peerId
        }

        signalingClient = FirestoreSignalingClient(chatId, myUsername!!)
        webRtcManager = WebRtcManager(application, signalingClient!!)
        
        pendingRemoteSdp = SessionDescription(SessionDescription.Type.OFFER, sdp)
        _isCallActive.value = true
        _showIncomingOverlay.value = true
        
        // Setup other signaling listeners (answer, candidates) via SignalingClient
        signalingClient?.onAnswerReceived = { webRtcManager?.onRemoteSessionDescription(it) }
        signalingClient?.onIceCandidateReceived = { webRtcManager?.addIceCandidate(it) }
    }

    fun startOutgoingCall(chatId: String) {
        _currentChatId.value = chatId
        val peerId = chatId.split("_").firstOrNull { it != myUsername } ?: ""
        db.collection("users").document(peerId).get().addOnSuccessListener {
            _peerName.value = it.getString("name") ?: peerId
        }

        _isCallActive.value = true
        _showIncomingOverlay.value = false
        
        signalingClient = FirestoreSignalingClient(chatId, myUsername!!)
        webRtcManager = WebRtcManager(application, signalingClient!!)
        
        webRtcManager?.startLocalCapture()
        webRtcManager?.createPeerConnection(observer)
        webRtcManager?.call()

        signalingClient?.onAnswerReceived = { webRtcManager?.onRemoteSessionDescription(it) }
        signalingClient?.onIceCandidateReceived = { webRtcManager?.addIceCandidate(it) }
    }

    fun acceptCall() {
        _showIncomingOverlay.value = false
        webRtcManager?.startLocalCapture()
        webRtcManager?.createPeerConnection(observer)
        pendingRemoteSdp?.let {
            webRtcManager?.onRemoteSessionDescription(it)
            webRtcManager?.answer()
        }
        pendingRemoteSdp = null
    }

    fun rejectCall() {
        hangup()
    }

    fun hangup() {
        _isCallActive.value = false
        _showIncomingOverlay.value = false
        webRtcManager?.dispose()
        signalingClient?.clear()
        webRtcManager = null
        signalingClient = null
        _remoteVideoTrack.value = null
        _localVideoTrack.value = null
        _currentChatId.value = null
    }

    fun initLocalSurface(renderer: SurfaceViewRenderer) {
        webRtcManager?.initLocalSurface(renderer)
        webRtcManager?.startLocalCapture()
        webRtcManager?.addLocalSink(renderer)
    }

    fun initRemoteSurface(renderer: SurfaceViewRenderer) {
        webRtcManager?.initRemoteSurface(renderer)
        webRtcManager?.addRemoteSink(renderer)
    }

    fun toggleVideo() {
        _isVideoEnabled.value = !_isVideoEnabled.value
        webRtcManager?.toggleVideo(_isVideoEnabled.value)
    }

    fun toggleAudio() {
        _isAudioEnabled.value = !_isAudioEnabled.value
        webRtcManager?.toggleAudio(_isAudioEnabled.value)
    }

    override fun onCleared() {
        super.onCleared()
        webRtcManager?.release()
    }
}
