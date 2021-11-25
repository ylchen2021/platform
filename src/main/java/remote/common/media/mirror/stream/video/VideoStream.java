package remote.common.media.mirror.stream.video;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;

import remote.common.media.mirror.rtp.MediaCodecInputStream;
import remote.common.media.mirror.stream.MediaStream;

/** 
 * Don't use this class directly.
 */
public abstract class VideoStream extends MediaStream {

	protected final static String TAG = "VideoStream";

	protected VideoQuality mRequestedQuality = VideoQuality.DEFAULT_VIDEO_QUALITY.clone();
	protected VideoQuality mQuality = mRequestedQuality.clone();
	protected SharedPreferences mSettings = null;
	protected int mVideoEncoder;
	protected int mRequestedOrientation = 0, mOrientation = 0;
	protected boolean mUpdated = false;
		protected String mMimeType;

	private MediaProjection mMediaProjection;
	private VirtualDisplay mVirtualDisplay;
	/** 
	 * Don't use this class directly.
	 * Uses CAMERA_FACING_BACK by default.
	 */
	public VideoStream() {
		super();
	}	

	/**
	 * Sets the orientation of the preview.
	 * @param orientation The orientation of the preview
	 */
	public void setPreviewOrientation(int orientation) {
		mRequestedOrientation = orientation;
		mUpdated = false;
	}
	
	/** 
	 * Sets the configuration of the stream. You can call this method at any time 
	 * and changes will take effect next time you call {@link #configure()}.
	 * @param videoQuality Quality of the stream
	 */
	public void setVideoQuality(VideoQuality videoQuality) {
		if (!mRequestedQuality.equals(videoQuality)) {
			mRequestedQuality = videoQuality.clone();
			mUpdated = false;
		}
	}

	public void setMediaProjection(MediaProjection mediaProjection) {
		mMediaProjection = mediaProjection;
	}

	/** 
	 * Returns the quality of the stream.  
	 */
	public VideoQuality getVideoQuality() {
		return mRequestedQuality;
	}

	/**
	 * Some data (SPS and PPS params) needs to be stored when {@link #getSessionDescription()} is called 
	 * @param prefs The SharedPreferences that will be used to save SPS and PPS parameters
	 */
	public void setPreferences(SharedPreferences prefs) {
		mSettings = prefs;
	}

	/**
	 * Configures the stream. You need to call this before calling {@link #getSessionDescription()} 
	 * to apply your configuration of the stream.
	 */
	public synchronized void configure() throws IllegalStateException, IOException {
		super.configure();
		mOrientation = mRequestedOrientation;
	}	
	
	public synchronized void start() throws IllegalStateException, IOException {
		super.start();
		Log.d(TAG,"Stream configuration: FPS: "+mQuality.framerate+" Width: "+mQuality.resX+" Height: "+mQuality.resY);
	}

	/** Stops the stream. */
	public synchronized void stop() {
		super.stop();
	}

	/**
	 * Video encoding is done by a MediaCodec.
	 */
	protected void encodeWithMediaCodec() throws RuntimeException, IOException {
		// Uses the method MediaCodec.createInputSurface to feed the encoder
		encodeWithMediaCodecMethod2();
	}

	/**
	 * Video encoding is done by a MediaCodec.
	 * But here we will use the buffer-to-surface method
	 */
	@SuppressLint({ "InlinedApi", "NewApi" })	
	protected void encodeWithMediaCodecMethod2() throws RuntimeException, IOException {
		Log.d(TAG,"Video encoded using the MediaCodec API with a surface");
		createVirtualDisplay();
		// Estimates the frame rate of the camera
		measureFramerate();

		EncoderDebugger debugger = EncoderDebugger.debug(mSettings, mQuality.resX, mQuality.resY);

		mMediaCodec = MediaCodec.createByCodecName(debugger.getEncoderName());
		MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mQuality.resX, mQuality.resY);
		mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mQuality.bitrate);
		mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mQuality.framerate);
		mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
		mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
		mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
		mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		Surface surface = mMediaCodec.createInputSurface();
		mVirtualDisplay.setSurface(surface);
		mMediaCodec.start();

		// The packetizer encapsulates the bit stream in an RTP stream and send it over the network
		mPacketizer.setInputStream(new MediaCodecInputStream(mMediaCodec));
		mPacketizer.start();
		mStreaming = true;

	}

	public abstract String getSessionDescription() throws IllegalStateException;

	protected synchronized void createVirtualDisplay() {
		if (mVirtualDisplay == null) {
			mVirtualDisplay = mMediaProjection.createVirtualDisplay("ScreenRecorder-display0",
					mQuality.resX, mQuality.resY, 1 /*dpi*/,
					DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
					null /*surface*/, null, null);
		} else {
			// resize if size not matched
			Point size = new Point();
			mVirtualDisplay.getDisplay().getSize(size);
			if (size.x != mQuality.resX || size.y != mQuality.resY) {
				mVirtualDisplay.resize(mQuality.resX, mQuality.resY, 1);
			}
		}
	}

	/**
	 * Computes the average frame rate at which the preview callback is called.
	 * We will then use this average frame rate with the MediaCodec.  
	 * Blocks the thread in which this function is called.
	 */
	private void measureFramerate() {
		Log.d(TAG,"Actual framerate: "+mQuality.framerate);
		if (mSettings != null) {
			Editor editor = mSettings.edit();
			editor.putInt(PREF_PREFIX+"fps"+mRequestedQuality.framerate+","+mRequestedQuality.resX+mRequestedQuality.resY, mQuality.framerate);
			editor.commit();
		}
	}
}
