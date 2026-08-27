package com.RIKAPLAY.zhirpem_app.webrtc

import com.google.firebase.firestore.FirebaseFirestore
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class FirestoreSignalingClient(
    private val callId: String,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : SignalingClient {

    override var onOfferReceived: ((SessionDescription) -> Unit)? = null
    override var onAnswerReceived: ((SessionDescription) -> Unit)? = null
    override var onIceCandidateReceived: ((IceCandidate) -> Unit)? = null

    init {
        listenForSignaling()
    }

    private fun listenForSignaling() {
        db.collection("calls").document(callId).addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            
            val data = snapshot.data ?: return@addSnapshotListener
            
            if (data.containsKey("offer")) {
                val offerMap = data["offer"] as Map<String, String>
                onOfferReceived?.invoke(SessionDescription(
                    SessionDescription.Type.OFFER,
                    offerMap["sdp"] ?: ""
                ))
            }
            
            if (data.containsKey("answer")) {
                val answerMap = data["answer"] as Map<String, String>
                onAnswerReceived?.invoke(SessionDescription(
                    SessionDescription.Type.ANSWER,
                    answerMap["sdp"] ?: ""
                ))
            }
        }

        db.collection("calls").document(callId).collection("candidates")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                for (change in snapshot.documentChanges) {
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val data = change.document.data
                        onIceCandidateReceived?.invoke(IceCandidate(
                            data["sdpMid"] as String,
                            (data["sdpMLineIndex"] as Long).toInt(),
                            data["sdp"] as String
                        ))
                    }
                }
            }
    }

    override fun sendOffer(sdp: SessionDescription) {
        val offer = mapOf("type" to "offer", "sdp" to sdp.description)
        db.collection("calls").document(callId).set(mapOf("offer" to offer))
    }

    override fun sendAnswer(sdp: SessionDescription) {
        val answer = mapOf("type" to "answer", "sdp" to sdp.description)
        db.collection("calls").document(callId).update(mapOf("answer" to answer))
    }

    override fun sendIceCandidate(candidate: IceCandidate) {
        val candidateData = mapOf(
            "sdpMid" to candidate.sdpMid,
            "sdpMLineIndex" to candidate.sdpMLineIndex,
            "sdp" to candidate.sdp
        )
        db.collection("calls").document(callId).collection("candidates").add(candidateData)
    }

    override fun clear() {
        db.collection("calls").document(callId).delete()
    }
}
