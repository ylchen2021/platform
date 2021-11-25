package remote.common.media.mirror.stream.audio;

import android.media.MediaRecorder;

import java.io.IOException;
import java.lang.reflect.Field;

import remote.common.media.mirror.rtp.AMRNBPacketizer;


public class AMRNBStream extends AudioStream {

	public AMRNBStream() {
		super();

		mPacketizer = new AMRNBPacketizer();

		setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		
		try {
			// RAW_AMR was deprecated in API level 16.
			Field deprecatedName = MediaRecorder.OutputFormat.class.getField("RAW_AMR");
			setOutputFormat(deprecatedName.getInt(null));
		} catch (Exception e) {
			setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
		}
		
		setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		
	}

	/**
	 * Starts the stream.
	 */
	public synchronized void start() throws IllegalStateException, IOException {
		if (!mStreaming) {
			configure();
			super.start();
		}
	}

	public synchronized void configure() throws IllegalStateException, IOException {
		super.configure();
		mMode = MODE_MEDIACODEC_API;
		mQuality = mRequestedQuality.clone();
	}

	/**
	 * Returns a description of the stream using SDP. It can then be included in an SDP file.
	 */	
	public String getSessionDescription() {
		return "m=audio "+String.valueOf(getDestinationPorts()[0])+" RTP/AVP 96\r\n" +
				"a=rtpmap:96 AMR/8000\r\n" +
				"a=fmtp:96 octet-align=1;\r\n";
	}

	@Override
	protected void encodeWithMediaCodec() throws IOException {
		super.encodeWithMediaCodec();
	}

}
