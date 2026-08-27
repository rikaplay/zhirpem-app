package com.RIKAPLAY.zhirpem_app.webrtc

import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

/**
 * Interface for signaling call data (SDP and ICE candidates).
 * In a real app, this would be implemented using Firestore, WebSockets, or Firebase Realtime Database.
 */
interface SignalingClient {
    fun sendOffer(sdp: SessionDescription)
    fun sendAnswer(sdp: SessionDescription)
    fun sendIceCandidate(candidate: IceCandidate)
    
    var onOfferReceived: ((SessionDescription) -> Unit)?
    var onAnswerReceived: ((SessionDescription) -> Unit)?
    var onIceCandidateReceived: ((IceCandidate) -> Unit)?
    
    // For cleaning up
    fun clear()
}
