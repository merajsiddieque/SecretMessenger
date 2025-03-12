//package com.app.secretmessenger
//
//import android.Manifest
//import android.os.Bundle
//import android.view.View
//import android.widget.Button
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import io.socket.client.IO
//import io.socket.client.Socket
//import org.json.JSONObject
//import org.webrtc.*
//
//class MeetingActivity : AppCompatActivity() {
//    private lateinit var factory: PeerConnectionFactory
//    private var peerConnection: PeerConnection? = null
//    private lateinit var eglBase: EglBase
//    private lateinit var localView: SurfaceViewRenderer
//    private lateinit var remoteView: SurfaceViewRenderer
//    private var localStream: MediaStream? = null
//    private lateinit var socket: Socket
//    private var isVideoCall: Boolean = true
//    private lateinit var meetingId: String
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_meeting)
//
//        localView = findViewById(R.id.localView)
//        remoteView = findViewById(R.id.remoteView)
//        val tvMeetingId: TextView = findViewById(R.id.tvMeetingId)
//        val btnJoin: Button = findViewById(R.id.btnJoin)
//
//        // Get intent extras
//        intent.extras?.let { extras ->
//            extras.getString("token") ?: return
//            meetingId = extras.getString("meetingId") ?: return
//            val participantName = extras.getString("participantName") ?: "User"
//            val friendUsername = extras.getString("friendUsername") ?: "Friend"
//            isVideoCall = extras.getBoolean("isVideoCall", true)
//
//            tvMeetingId.text = "Meeting Id: $meetingId"
//        } ?: return
//
//        btnJoin.setOnClickListener { startCall() }
//
//        // Initialize WebRTC
//        PeerConnectionFactory.initialize(
//            PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions()
//        )
//        factory = PeerConnectionFactory.builder().createPeerConnectionFactory()
//        eglBase = EglBase.create()
//
//        // Setup video renderers
//        localView.init(eglBase.eglBaseContext, null)
//        remoteView.init(eglBase.eglBaseContext, null)
//        localView.setZOrderMediaOverlay(true)
//
//        // Connect to signaling server
//        try {
//            socket = IO.socket("http://your-server-url:3000") // Replace with your server URL
//            socket.connect()
//            socket.emit("join-room", meetingId)
//            setupSocketListeners()
//        } catch (e: Exception) {
//            Toast.makeText(this, "Socket error: ${e.message}", Toast.LENGTH_SHORT).show()
//        }
//
//        requestPermissions()
//    }
//
//    private fun requestPermissions() {
//        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
//        ActivityCompat.requestPermissions(this, permissions, 100)
//    }
//
//    private fun startCall() {
//        findViewById<View>(R.id.meetingLayout).visibility = View.VISIBLE
//
//        // Audio stream (always enabled)
//        val audioSource = factory.createAudioSource(MediaConstraints())
//        val audioTrack = factory.createAudioTrack("101", audioSource)
//
//        localStream = factory.createLocalMediaStream("102")
//        localStream?.addTrack(audioTrack)
//
//        // Video stream (only for video calls)
//        if (isVideoCall) {
//            val videoCapturer = createVideoCapturer()
//            videoCapturer?.let {
//                val videoSource = factory.createVideoSource(it.isScreencast)
//                it.initialize(SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext), this, videoSource.capturerObserver)
//                it.startCapture(1280, 720, 30)
//
//                val videoTrack = factory.createVideoTrack("100", videoSource)
//                localStream?.addTrack(videoTrack)
//                videoTrack.addSink(localView)
//            }
//        }
//
//        // Create peer connection
//        val config = PeerConnection.RTCConfiguration(
//            listOf(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer())
//        )
//        peerConnection = factory.createPeerConnection(config, object : PeerConnection.Observer {
//            override fun onIceCandidate(candidate: IceCandidate) {
//                socket.emit("ice-candidate", JSONObject().put("roomId", meetingId).put("candidate", candidate.sdp))
//            }
//
//            override fun onAddStream(stream: MediaStream) {
//                if (isVideoCall && stream.videoTracks.isNotEmpty()) {
//                    stream.videoTracks[0].addSink(remoteView)
//                }
//            }
//
//            override fun onSignalingChange(state: PeerConnection.SignalingState) {}
//            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {}
//            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
//            override fun onRemoveStream(stream: MediaStream) {}
//            override fun onDataChannel(dc: DataChannel) {}
//            override fun onRenegotiationNeeded() {}
//        })
//
//        peerConnection?.addStream(localStream)
//
//        // Create and send offer
//        peerConnection?.createOffer(object : SimpleSdpObserver() {
//            override fun onCreateSuccess(sdp: SessionDescription) {
//                peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)
//                socket.emit("offer", JSONObject().put("roomId", meetingId).put("offer", sdp.description))
//            }
//        }, MediaConstraints())
//    }
//
//    private fun setupSocketListeners() {
//        socket.on("offer") { args ->
//            runOnUiThread {
//                val data = args[0] as JSONObject
//                val offer = data.getString("offer")
//                peerConnection?.setRemoteDescription(SimpleSdpObserver(), SessionDescription(SessionDescription.Type.OFFER, offer))
//                peerConnection?.createAnswer(object : SimpleSdpObserver() {
//                    override fun onCreateSuccess(sdp: SessionDescription) {
//                        peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)
//                        socket.emit("answer", JSONObject().put("roomId", meetingId).put("answer", sdp.description))
//                    }
//                }, MediaConstraints())
//            }
//        }
//
//        socket.on("answer") { args ->
//            runOnUiThread {
//                val data = args[0] as JSONObject
//                val answer = data.getString("answer")
//                peerConnection?.setRemoteDescription(SimpleSdpObserver(), SessionDescription(SessionDescription.Type.ANSWER, answer))
//            }
//        }
//
//        socket.on("ice-candidate") { args ->
//            runOnUiThread {
//                val data = args[0] as JSONObject
//                peerConnection?.addIceCandidate(IceCandidate("", 0, data.getString("candidate")))
//            }
//        }
//    }
//
//    private fun createVideoCapturer(): VideoCapturer? {
//        val enumerator = Camera2Enumerator(this)
//        for (device in enumerator.deviceNames) {
//            if (enumerator.isFrontFacing(device)) {
//                return enumerator.createCapturer(device, null)
//            }
//        }
//        return null
//    }
//
//    override fun onDestroy() {
//        peerConnection?.close()
//        socket.disconnect()
//        localView.release()
//        remoteView.release()
//        super.onDestroy()
//    }
//
//    private open class SimpleSdpObserver : SdpObserver {
//        override fun onCreateSuccess(sdp: SessionDescription) {}
//        override fun onSetSuccess() {}
//        override fun onCreateFailure(error: String) {}
//        override fun onSetFailure(error: String) {}
//    }
//}