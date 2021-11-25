package remote.common.media.mirror.stream.video;

import android.annotation.SuppressLint;
import android.media.MediaRecorder;
import android.util.Base64;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import remote.common.media.mirror.rtp.H264Packetizer;

public class H264Stream extends VideoStream {

	public final static String TAG = "H264Stream";

	private Semaphore mLock = new Semaphore(0);
	private MP4Config mConfig;

	/**
	 * Constructs the H.264 stream.
	 * @throws IOException
	 */
	public H264Stream() {
		super();
		mMimeType = "video/avc";
		mVideoEncoder = MediaRecorder.VideoEncoder.H264;
		mPacketizer = new H264Packetizer();
	}

	/**
	 * Returns a description of the stream using SDP. It can then be included in an SDP file.
	 */
	public synchronized String getSessionDescription() throws IllegalStateException {
		if (mConfig == null) throw new IllegalStateException("You need to call configure() first !");
		return "m=video "+String.valueOf(getDestinationPorts()[0])+" RTP/AVP 96\r\n" +
		"a=rtpmap:96 H264/90000\r\n" +
		"a=fmtp:96 packetization-mode=1;profile-level-id="+mConfig.getProfileLevel()+";sprop-parameter-sets="+mConfig.getB64SPS()+","+mConfig.getB64PPS()+";\r\n";
	}

	/**
	 * Starts the stream.
	 */
	public synchronized void start() throws IllegalStateException, IOException {
		if (!mStreaming) {
			configure();
			byte[] pps = Base64.decode(mConfig.getB64PPS(), Base64.NO_WRAP);
			byte[] sps = Base64.decode(mConfig.getB64SPS(), Base64.NO_WRAP);
			((H264Packetizer)mPacketizer).setStreamParameters(pps, sps);
			super.start();
		}
	}

	/**
	 * Configures the stream. You need to call this before calling {@link #getSessionDescription()} to apply
	 * your configuration of the stream.
	 */
	public synchronized void configure() throws IllegalStateException, IOException {
		super.configure();
		mMode = mRequestedMode;
		mQuality = mRequestedQuality.clone();
		mConfig = testMediaCodecAPI();
	}
	
	@SuppressLint("NewApi")
	private MP4Config testMediaCodecAPI() throws RuntimeException, IOException {
		EncoderDebugger debugger = EncoderDebugger.debug(mSettings, mQuality.resX, mQuality.resY);
		return new MP4Config(debugger.getB64SPS(), debugger.getB64PPS());
	}
}
