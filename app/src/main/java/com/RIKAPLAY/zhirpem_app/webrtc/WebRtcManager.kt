package com.RIKAPLAY.zhirpem_app.webrtc

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
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
    private var localStream: MediaStream? = null

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )

    init {
        initPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val encoderFactory = DefaultVideoEncoderFactory(
            rootEglBase.eglBaseContext, true, true
        )
        val decoderFactory = DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)

        val audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setAudioDeviceModule(audioDeviceModule)
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = false
                disableNetworkMonitor = false
            })
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

    fun startLocalCapture(localRenderer: SurfaceViewRenderer) {
        val videoSource = peerConnectionFactory?.createVideoSource(false)
        videoCapturer = createVideoCapturer()
        
        // 1080p capture request: 1920x1080 at 30fps
        videoCapturer?.startCapture(1920, 1080, 30)

        localVideoTrack = peerConnectionFactory?.createVideoTrack("VIDEO_TRACK_ID", videoSource)
        localVideoTrack?.addSink(localRenderer)

        val audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory?.createAudioTrack("AUDIO_TRACK_ID", audioSource)
        
        // Bitrate tuning for Opus is usually handled by the stack, but we can configure audio source constraints if needed.
        // For HD video, we'll tune the RtpSender later.
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        return enumerator.createCapturer(deviceNames[0], null)
    }

    fun createPeerConnection(observer: PeerConnection.Observer) {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, observer)
        
        localVideoTrack?.let { peerConnection?.addTrack(it) }
        localAudioTrack?.let { peerConnection?.addTrack(it) }
    }

    fun call() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserverAdapter() {
                    override fun onSetSuccess() {
                        desc?.let { signalingClient.sendOffer(it) }
                        // Tune bitrate after setting local description
                        setVideoBitrate(2500, 8000)
                    }
                }, desc)
            }
        }, constraints)
    }

    fun answer() {
        val constraints = MediaConstraints()
        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserverAdapter() {
                    override fun onSetSuccess() {
                        desc?.let { signalingClient.sendAnswer(it) }
                        setVideoBitrate(2500, 8000)
                    }
                }, desc)
            }
        }, constraints)
    }

    fun onRemoteSessionDescription(desc: SessionDescription) {
        peerConnection?.setRemoteDescription(SdpObserverAdapter(), desc)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    /**
     * Bitrate Tuning: Sets the target bitrate for video between min and max in kbps.
     */
    private fun setVideoBitrate(minKbps: Int, maxKbps: Int) {
        val senders = peerConnection?.senders ?: return
        for (sender in senders) {
            if (sender.track()?.kind() == "video") {
                val parameters = sender.parameters
                for (encoding in parameters.encodings) {
                    encoding.minBitrateBps = minKbps * 1000
                    encoding.maxBitrateBps = maxKbps * 1000
                }
                sender.parameters = parameters
            }
        }
    }

    fun dispose() {
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        peerConnection?.dispose()
        peerConnectionFactory?.dispose()
        rootEglBase.release()
    }

    open class SdpObserverAdapter : SdpObserver {
        override fun onCreateSuccess(desc: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String?) { Log.e("WebRtcManager", "SDP Create Failure: $error") }
        override fun onSetFailure(error: String?) { Log.e("WebRtcManager", "SDP Set Failure: $error") }
    }
}
