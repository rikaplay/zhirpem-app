package com.RIKAPLAY.zhirpem_app.webrtc

import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface SignalingClient {
    fun sendOffer(sdp: SessionDescription, callType: String = "VIDEO")
    fun sendAnswer(sdp: SessionDescription)
    fun sendIceCandidate(candidate: IceCandidate)
    
    var onOfferReceived: ((SessionDescription) -> Unit)?
    var onAnswerReceived: ((SessionDescription) -> Unit)?
    var onIceCandidateReceived: ((IceCandidate) -> Unit)?
    var onCallEnded: (() -> Unit)?

    fun clear()
    fun dispose()
}
