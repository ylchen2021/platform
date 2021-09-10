package com.boostvision.platform.media.miracast.rtp

class RtpSession(
    private var clientIp: String,
    private var clientRtp: Int,
    private var clientRtcp: Int,
    private var transportMode: TransportMode
) {

    private var serverRtpPort = 0

    init {
        if (transportMode == TransportMode.TRANSPORT_TCP_INTERLEAVED) {
            serverRtpPort = 0;
        } else if (transportMode == TransportMode.TRANSPORT_TCP) {
            serverRtpPort = 20000;
        }
    }

    fun isStreaming(): Boolean {
        return false
    }

    fun getServerPort(): Int {
        return serverRtpPort
    }
}