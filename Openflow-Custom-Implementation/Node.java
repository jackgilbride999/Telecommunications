
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public abstract class Node {
	/** OpenFlow Message Types as Constants. */
	/* Immutable messages. */
	protected static final byte OFPT_HELLO = 0;
	protected static final byte OFPT_ERROR = 1;
	protected static final byte OFPT_ECHO_REQUEST = 2;
	protected static final byte OFPT_ECHO_REPLY = 3;
	protected static final byte OFPT_EXPERIMENTER = 4;
	/* Switch configuration messages. */
	protected static final byte OFPT_FEATURES_REQUEST = 5;
	protected static final byte OFPT_FEATURES_REPLY = 6;
	protected static final byte OFPT_GET_CONFIG_REQUEST = 7;
	protected static final byte OFPT_GET_CONFIG_REPLY = 8;
	protected static final byte OFPT_SET_CONFIG = 9;
	/* Asynchronous messages. */
	protected static final byte OFPT_PACKET_IN = 10;
	protected static final byte OFPT_FLOW_REMOVED = 11;
	protected static final byte OFPT_PORT_STATUS = 12;
	protected static final byte OFPT_PACKET_OUT = 13;
	protected static final byte OFPT_FLOW_MOD = 14;
	protected static final byte OFPT_GROUP_MOD = 15;
	protected static final byte OFPT_PORT_MOD = 16;
	protected static final byte OFPT_TABLE_MOD = 17;
	/* Multipart messages. */
	protected static final byte OFPT_MULTIPART_REQUEST = 18;
	protected static final byte OFPT_MULTIPART_REPLY = 19;
	/* Barrier messages. */
	protected static final byte OFPT_BARRIER_REQUEST = 20;
	protected static final byte OFPT_BARRIER_REPLY = 21;
	/* Controller role change request messages. */
	protected static final byte OFPT_ROLE_REQUEST = 22;
	protected static final byte OFPT_ROLE_REPLY = 23;
	/* Asynchronous message configuration. */
	protected static final byte OFPT_GET_ASYNC_REQUEST = 24;
	protected static final byte OFPT_GET_ASYNC_REPLY = 25;
	protected static final byte OFPT_SET_ASYNC = 26;
	/* Meters and rate limiters configuration messages. */
	protected static final byte OFPT_METER_MOD = 27;
	/* Controller role change event messages. */
	protected static final byte OFPT_ROLE_STATUS = 28;
	/* Asynchronous messages. */
	protected static final byte OFPT_TABLE_STATUS = 29;
	/* Request forwarding by the switch. */
	protected static final byte OFPT_REQUESTFORWARD = 30;
	/* Bundle operations. */
	protected static final byte OFPT_BUNDLE_CONTROL = 31;
	protected static final byte OFPT_BUNDLE_ADD_MESSAGE = 32;
	/* Controller Status async message. */
	protected static final byte OFPT_CONTROLLER_STATUS = 33;

	/**
	 * Constants for the message types sent between end nodes and switches. The end nodes
	 * know nothing about OpenFlow, only the switch immediately connected to
	 * them and the end point. Its message types are different as it has no
	 * direct contact with the Controller.
	 */
	protected static final byte NODE_INITIALISE_SWITCH = 34;
	protected static final byte NODE_MESSAGE = 35;

	/** Other constants. */
	protected static final int PACKETSIZE = 1400;
	protected static final int BASE_PORT_NUMBER = 5000;
	protected static final String LOCALHOST = "localhost";
	protected static final int CONTROLLER_PORT = 0;
	/** The index for the terms in preconfInfo */
	protected static final int SRC_INDEX = 0;
	protected static final int DST_INDEX = 1;
	protected static final int SWITCH_INDEX = 2;
	protected static final int INPUT_INDEX = 3;
	protected static final int OUTPUT_INDEX = 4;
	// the features of the switch
	protected static final byte BASIC_FEATURES = 0;

	public static final byte NUM_SWITCHES = 8;
	public static final byte NUM_END_NODES = 4;
	DatagramSocket socket;
	Listener listener;
	CountDownLatch latch;

	Node() {
		latch = new CountDownLatch(1);
		listener = new Listener();
		listener.setDaemon(true);
		listener.start();
	}

	public abstract void onReceipt(DatagramPacket packet);

	protected byte getType(byte data[]) {
		return data[0];
	}
	
	protected void setType(byte[] data, byte type){
		data[0] = type;
	}
	
	/** The following three methods are for parsing messages between End Nodes. */
	protected byte getMessageSource(byte data[]){
		assert(getType(data)==NODE_MESSAGE);
		return data[1];
	}
	
	protected byte getMessageDest(byte data[]){
		assert(getType(data)==NODE_MESSAGE);
		return data[2];
	}
	
	protected String getMessageContent(byte data[]){
		assert(getType(data)==NODE_MESSAGE);
		byte[] content = Arrays.copyOfRange(data, 3, data.length);
		String messageContent = new String(content).trim();
		return messageContent;
	}

	/**
	 *
	 * Listener thread
	 * 
	 * Listens for incoming packets on a datagram socket and informs registered
	 * receivers about incoming packets.
	 */
	class Listener extends Thread {

		/*
		 * Telling the listener that the socket has been initialized
		 */
		public void go() {
			latch.countDown();
		}

		/*
		 * Listen for incoming packets and inform receivers
		 */
		public synchronized void run() {
			try {
				latch.await();
				// Endless loop: attempt to receive packet, notify receivers,
				// etc
				while (true) {
					DatagramPacket packet = new DatagramPacket(new byte[PACKETSIZE], PACKETSIZE);
					socket.receive(packet);
					onReceipt(packet);
				}
			} catch (Exception e) {
				if (!(e instanceof SocketException))
					e.printStackTrace();
			}
		}
	}
}