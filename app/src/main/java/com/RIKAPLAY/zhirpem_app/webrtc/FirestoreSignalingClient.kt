package com.RIKAPLAY.zhirpem_app.webrtc

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class FirestoreSignalingClient(
    private val callId: String,
    private val currentUserId: String,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : SignalingClient {

    override var onOfferReceived: ((SessionDescription) -> Unit)? = null
    override var onAnswerReceived: ((SessionDescription) -> Unit)? = null
    override var onIceCandidateReceived: ((IceCandidate) -> Unit)? = null
    override var onCallEnded: (() -> Unit)? = null
    
    private val sessionStartTime = System.currentTimeMillis()
    private var hasBeenCreated = false
    
    private var callListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var candidatesListener: com.google.firebase.firestore.ListenerRegistration? = null

    init { listenForSignaling() }

    private fun listenForSignaling() {
        callListener = db.collection("calls").document(callId).addSnapshotListener { snap, e ->
            if (e != null) {
                android.util.Log.e("Signaling", "Error listening to call doc: ${e.message}")
                return@addSnapshotListener
            }
            
            val exists = snap?.exists() == true
            android.util.Log.d("Signaling", "Call doc exists: $exists, hasBeenCreated: $hasBeenCreated")
            
            if (!exists) {
                if (hasBeenCreated) {
                    android.util.Log.d("Signaling", "Call document disappeared, triggering onCallEnded")
                    onCallEnded?.invoke()
                }
                return@addSnapshotListener
            }

            val data = snap?.data ?: return@addSnapshotListener
            val timestamp = data["timestamp"] as? Long ?: 0L
            if (timestamp < sessionStartTime) return@addSnapshotListener

            hasBeenCreated = true

            if (data.containsKey("offer")) {
                val map = data["offer"] as? Map<*, *>
                if (map != null && map["from"] != currentUserId) {
                    onOfferReceived?.invoke(SessionDescription(SessionDescription.Type.OFFER, map["sdp"] as String))
                }
            }
            if (data.containsKey("answer")) {
                val map = data["answer"] as? Map<*, *>
                if (map != null && map["from"] != currentUserId) {
                    onAnswerReceived?.invoke(SessionDescription(SessionDescription.Type.ANSWER, map["sdp"] as String))
                }
            }
        }

        candidatesListener = db.collection("calls").document(callId).collection("candidates")
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) return@addSnapshotListener
                snap.documentChanges.filter { it.type == com.google.firebase.firestore.DocumentChange.Type.ADDED }.forEach { ch ->
                    val data = ch.document.data
                    if (data["from"] != currentUserId) {
                        onIceCandidateReceived?.invoke(IceCandidate(
                            data["sdpMid"] as String, (data["sdpMLineIndex"] as Number).toInt(), data["sdp"] as String
                        ))
                    }
                }
            }
    }

    override fun sendOffer(sdp: SessionDescription, callType: String) {
        val recipientId = callId.split("_").firstOrNull { it != currentUserId } ?: ""
        val offer = mapOf(
            "sdp" to sdp.description,
            "from" to currentUserId,
            "to" to recipientId
        )
        db.collection("calls").document(callId).set(mapOf(
            "offer" to offer,
            "timestamp" to System.currentTimeMillis(),
            "recipientId" to recipientId,
            "callType" to callType
        ))
    }

    override fun sendAnswer(sdp: SessionDescription) {
        val answer = mapOf("sdp" to sdp.description, "from" to currentUserId)
        db.collection("calls").document(callId).update(mapOf("answer" to answer, "timestamp" to System.currentTimeMillis()))
    }

    override fun sendIceCandidate(candidate: IceCandidate) {
        val data = mapOf("sdpMid" to candidate.sdpMid, "sdpMLineIndex" to candidate.sdpMLineIndex, "sdp" to candidate.sdp, "from" to currentUserId)
        db.collection("calls").document(callId).collection("candidates").add(data)
    }

    override fun clear() {
        db.collection("calls").document(callId).delete()
    }

    override fun dispose() {
        callListener?.remove()
        candidatesListener?.remove()
        callListener = null
        candidatesListener = null
    }
}
