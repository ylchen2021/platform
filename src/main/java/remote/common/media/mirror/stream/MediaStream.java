package remote.common.media.mirror.stream;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Random;

import remote.common.media.mirror.rtp.AbstractPacketizer;

/**
 * A MediaRecorder that streams what it records using a packetizer from the RTP package.
 * You can't use this class directly !
 */
public abstract class MediaStream implements Stream {

	protected static final String TAG = "MediaStream";

	/** A LocalSocket will be used to feed the MediaRecorder object */
	public static final byte PIPE_API_LS = 0x01;
	
	/** A ParcelFileDescriptor will be used to feed the MediaRecorder object */
	public static final byte PIPE_API_PFD = 0x02;
	
	/** Prefix that will be used for all shared preferences saved by libstreaming */
	protected static final String PREF_PREFIX = "libstreaming-";

	/** The packetizer that will read the output of the camera and send RTP packets over the networked. */
	protected AbstractPacketizer mPacketizer = null;

	/** 
	 * Starting lollipop the LocalSocket API cannot be used to feed a MediaRecorder object. 
	 * You can force what API to use to create the pipe that feeds it with this constant 
	 * by using  {@link #PIPE_API_LS} and {@link #PIPE_API_PFD}.
	 */
	protected final static byte sPipeApi;
	
	protected boolean mStreaming = false, mConfigured = false;
	protected int mRtpPort = 0, mRtcpPort = 0; 
	protected byte mChannelIdentifier = 0;
	protected OutputStream mOutputStream = null;
	protected InetAddress mDestination;
	
	protected ParcelFileDescriptor[] mParcelFileDescriptors;
	protected ParcelFileDescriptor mParcelRead;
	protected ParcelFileDescriptor mParcelWrite;
	
	protected LocalSocket mReceiver, mSender = null;
	private LocalServerSocket mLss = null;
	private int mSocketId; 
	
	private int mTTL = 64;

	protected MediaRecorder mMediaRecorder;
	protected MediaCodec mMediaCodec;
	
	static {
		// Starting lollipop, the LocalSocket API cannot be used anymore to feed 
		// a MediaRecorder object for security reasons
		sPipeApi = PIPE_API_PFD;
	}

	public MediaStream() {
	}

	/** 
	 * Sets the destination IP address of the stream.
	 * @param dest The destination address of the stream 
	 */	
	public void setDestinationAddress(InetAddress dest) {
		mDestination = dest;
	}

	/** 
	 * Sets the destination ports of the stream.
	 * If an odd number is supplied for the destination port then the next 
	 * lower even number will be used for RTP and it will be used for RTCP.
	 * If an even number is supplied, it will be used for RTP and the next odd
	 * number will be used for RTCP.
	 * @param dport The destination port
	 */
	public void setDestinationPorts(int dport) {
		if (dport % 2 == 1) {
			mRtpPort = dport-1;
			mRtcpPort = dport;
		} else {
			mRtpPort = dport;
			mRtcpPort = dport+1;
		}
	}

	/**
	 * Sets the destination ports of the stream.
	 * @param rtpPort Destination port that will be used for RTP
	 * @param rtcpPort Destination port that will be used for RTCP
	 */
	public void setDestinationPorts(int rtpPort, int rtcpPort) {
		mRtpPort = rtpPort;
		mRtcpPort = rtcpPort;
		mOutputStream = null;
	}	

	/**
	 * If a TCP is used as the transport protocol for the RTP session,
	 * the output stream to which RTP packets will be written to must
	 * be specified with this method.
	 */ 
	public void setOutputStream(OutputStream stream, byte channelIdentifier) {
		mOutputStream = stream;
		mChannelIdentifier = channelIdentifier;
	}
	
	
	/**
	 * Sets the Time To Live of packets sent over the network.
	 * @param ttl The time to live
	 * @throws IOException
	 */
	public void setTimeToLive(int ttl) throws IOException {
		mTTL = ttl;
	}

	/** 
	 * Returns a pair of destination ports, the first one is the 
	 * one used for RTP and the second one is used for RTCP. 
	 **/
	public int[] getDestinationPorts() {
		return new int[] {
				mRtpPort,
				mRtcpPort
		};
	}

	public int[] getLocalPorts() {
		return mPacketizer.getRtpSocket().getLocalPorts();
	}

	/**
	 * Returns the packetizer associated with the {@link MediaStream}.
	 * @return The packetizer
	 */
	public AbstractPacketizer getPacketizer() { 
		return mPacketizer;
	}

	/**
	 * Returns an approximation of the bit rate consumed by the stream in bit per seconde.
	 */
	public long getBitrate() {
		return !mStreaming ? 0 : mPacketizer.getRtpSocket().getBitrate(); 
	}

	/**
	 * Indicates if the {@link MediaStream} is streaming.
	 * @return A boolean indicating if the {@link MediaStream} is streaming
	 */
	public boolean isStreaming() {
		return mStreaming;
	}

	public synchronized void configure() throws IllegalStateException, IOException {
		if (mStreaming) throw new IllegalStateException("Can't be called while streaming.");
		if (mPacketizer != null) {
			mPacketizer.setDestination(mDestination, mRtpPort, mRtcpPort);
			mPacketizer.getRtpSocket().setOutputStream(mOutputStream, mChannelIdentifier);
		}
		mConfigured = true;
	}
	
	/** Starts the stream. */
	public synchronized void start() throws IllegalStateException, IOException {
		
		if (mDestination==null)
			throw new IllegalStateException("No destination ip address set for the stream !");

		if (mRtpPort<=0 || mRtcpPort<=0)
			throw new IllegalStateException("No destination ports set for the stream !");

		mPacketizer.setTimeToLive(mTTL);

		encodeWithMediaCodec();
	}

	/** Stops the stream. */
	@SuppressLint("NewApi") 
	public synchronized  void stop() {
		if (mStreaming) {
			try {
				mPacketizer.stop();
				mMediaCodec.stop();
				mMediaCodec.release();
				mMediaCodec = null;
			} catch (Exception e) {
				e.printStackTrace();
			}	
			mStreaming = false;
		}
	}
 
    protected abstract void encodeWithMediaCodec() throws IOException;
	
	public abstract String getSessionDescription();
	
	public int getSSRC() {
		return getPacketizer().getSSRC();
	}
	
	protected void createSockets() throws IOException {

		if (sPipeApi == PIPE_API_LS) {
			
			final String LOCAL_ADDR = "net.majorkernelpanic.streaming-";
	
			for (int i=0;i<10;i++) {
				try {
					mSocketId = new Random().nextInt();
					mLss = new LocalServerSocket(LOCAL_ADDR+mSocketId);
					break;
				} catch (IOException e1) {}
			}
	
			mReceiver = new LocalSocket();
			mReceiver.connect( new LocalSocketAddress(LOCAL_ADDR+mSocketId));
			mReceiver.setReceiveBufferSize(500000);
			mReceiver.setSoTimeout(3000);
			mSender = mLss.accept();
			mSender.setSendBufferSize(500000);
			
		} else {
			Log.e(TAG, "parcelFileDescriptors createPipe version = Lollipop");
			mParcelFileDescriptors = ParcelFileDescriptor.createPipe();
			mParcelRead = new ParcelFileDescriptor(mParcelFileDescriptors[0]);
			mParcelWrite = new ParcelFileDescriptor(mParcelFileDescriptors[1]);
		}
	}

	protected void closeSockets() {
		if (sPipeApi == PIPE_API_LS) {
			try {
				mReceiver.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				mSender.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				mLss.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			mLss = null;
			mSender = null;
			mReceiver = null;
			
		} else {
			try {
				if (mParcelRead != null) {
					mParcelRead.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if (mParcelWrite != null) {
					mParcelWrite.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
}