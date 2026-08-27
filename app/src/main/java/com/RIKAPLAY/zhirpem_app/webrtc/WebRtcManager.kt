package com.RIKAPLAY.zhirpem_app.webrtc

import android.content.Context
import android.util.Log
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule

class WebRtcManager(
    private val context: Context,
    private val signalingClient: SignalingClient
) {
    private val rootEglBase: EglBase = EglBase.create()
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var videoCapturer: VideoCapturer? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer()
    )

    init {
        initPeerConnectionFactory()
    }

    @Synchronized
    private fun initPeerConnectionFactory() {
        if (peerConnectionFactory != null) return

        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        
        try { PeerConnectionFactory.initialize(options) } catch (e: Exception) {}

        val encoderFactory = DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)
        val audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setAudioDeviceModule(audioDeviceModule)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    fun initLocalSurface(renderer: SurfaceViewRenderer) {
        renderer.init(rootEglBase.eglBaseContext, null)
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        renderer.setMirror(true)
    }

    fun initRemoteSurface(renderer: SurfaceViewRenderer) {
        renderer.init(rootEglBase.eglBaseContext, null)
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
    }

    fun getLocalVideoTrack(): VideoTrack? = localVideoTrack

    fun startLocalCapture() {
        initPeerConnectionFactory()
        if (localVideoTrack != null) return

        val videoSource = peerConnectionFactory?.createVideoSource(false)
        videoCapturer = createVideoCapturer()
        videoCapturer?.initialize(SurfaceTextureHelper.create("WebRtcCap", rootEglBase.eglBaseContext), context, videoSource?.capturerObserver)
        videoCapturer?.startCapture(1280, 720, 30)

        localVideoTrack = peerConnectionFactory?.createVideoTrack("VIDEO_TRACK", videoSource)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("AUDIO_TRACK", peerConnectionFactory?.createAudioSource(MediaConstraints()))
    }

    fun addLocalSink(renderer: SurfaceViewRenderer) {
        localVideoTrack?.addSink(renderer)
    }

    fun addRemoteSink(renderer: SurfaceViewRenderer) {
        peerConnection?.receivers?.forEach { (it.track() as? VideoTrack)?.addSink(renderer) }
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        return enumerator.deviceNames.find { enumerator.isFrontFacing(it) }?.let { enumerator.createCapturer(it, null) }
            ?: enumerator.deviceNames.firstOrNull()?.let { enumerator.createCapturer(it, null) }
    }

    fun createPeerConnection(observer: PeerConnection.Observer) {
        initPeerConnectionFactory()
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, observer)
        localVideoTrack?.let { peerConnection?.addTrack(it) }
        localAudioTrack?.let { peerConnection?.addTrack(it) }
    }

    fun call(callType: String = "VIDEO") {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserverAdapter() {
                    override fun onSetSuccess() {
                        desc?.let { signalingClient.sendOffer(it, callType) }
                        setVideoBitrate(2500, 8000)
                    }
                }, desc)
            }
        }, constraints)
    }

    fun answer() {
        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserverAdapter() {
                    override fun onSetSuccess() {
                        desc?.let { signalingClient.sendAnswer(it) }
                        setVideoBitrate(2500, 8000)
                    }
                }, desc)
            }
        }, MediaConstraints())
    }

    fun onRemoteSessionDescription(desc: SessionDescription) {
        peerConnection?.setRemoteDescription(SdpObserverAdapter(), desc)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    private fun setVideoBitrate(min: Int, max: Int) {
        peerConnection?.senders?.filter { it.track()?.kind() == "video" }?.forEach { sender ->
            sender.parameters = sender.parameters.apply { encodings.forEach { it.minBitrateBps = min * 1000; it.maxBitrateBps = max * 1000 } }
        }
    }

    fun toggleVideo(en: Boolean) = localVideoTrack?.setEnabled(en)
    fun toggleAudio(en: Boolean) = localAudioTrack?.setEnabled(en)

    fun flipCamera(handler: CameraVideoCapturer.CameraSwitchHandler? = null) {
        (videoCapturer as? CameraVideoCapturer)?.switchCamera(handler)
    }

    fun getStats(callback: (RTCStatsReport) -> Unit) {
        peerConnection?.getStats { callback(it) }
    }

    fun dispose() {
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        videoCapturer = null
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
        localVideoTrack = null
        localAudioTrack = null
    }

    fun release() {
        dispose()
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        rootEglBase.release()
    }

    open class SdpObserverAdapter : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(e: String?) { Log.e("WebRtc", "SDP Create Fail: $e") }
        override fun onSetFailure(e: String?) { Log.e("WebRtc", "SDP Set Fail: $e") }
    }
}
