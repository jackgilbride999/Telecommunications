import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class Publisher extends Node {

	/** Constant substrings to recognize user input. */
	private static final String CREATE = "CRE";
	private static final String PUBLISH = "PUB";

	Terminal terminal;
	InetSocketAddress dstAddress;
	/** Map topic numbers to topic names, map agreed with Broker. */
	private Map<Integer, String> topicNumbers;

	Publisher(Terminal terminal) {
		try {
			this.terminal = terminal;
			dstAddress = new InetSocketAddress(DEFAULT_DST, BKR_PORT);
			socket = new DatagramSocket(PUB_PORT);
			listener.go();
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
		topicNumbers = new HashMap<Integer, String>();
	}

	public static void main(String[] args) {
		try {
			Terminal terminal = new Terminal("Publisher");
			(new Publisher(terminal)).start();
			terminal.println("Program completed");
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}

	private void createTopic() {
		String topic = terminal.read("Please enter a topic to create: ");
		terminal.println("Please enter a topic to create: " + topic);
		terminal.println("Sending packet...");

		DatagramPacket[] packets = createPackets(CREATION, topicNumbers.size(), topic, dstAddress);
		topicNumbers.put(topicNumbers.size(), topic);
		try {
			socket.send(packets[0]);
		} catch (IOException e) {
			e.printStackTrace();
		}
		terminal.println("Packet sent");
	}

	private boolean publishMessage() {
		String topic = terminal.read("Please enter the name of the topic you want to publish a message for: ");
		terminal.println("Please enter the name of the topic you want to publish a message for: " + topic);
		String message = terminal.read("Please enter the message that you would like to publish: ");
		terminal.println("Please enter the message that you would like to publish: " + message);
		int topicNumber = Integer.MAX_VALUE;
		for (int i = 0; i < topicNumbers.size(); i++) {
			if ((topicNumbers.get(i)).equals(topic)) {
				topicNumber = i;
			}
		}
		if (topicNumber == Integer.MAX_VALUE) {
			terminal.println("This topic does not exist.");
		} else {
			DatagramPacket[] packets = createPackets(PUBLICATION, topicNumber, message, dstAddress);
			try {
				terminal.println("Sending packet...");
				socket.send(packets[0]);
			} catch (IOException e) {
				e.printStackTrace();
			}
			terminal.println("Packet sent");
			return true;
		}
		return false;
	}

	public synchronized void start() throws Exception {
		while (true) {
			String startingString = terminal
					.read("Enter CREATE to create a new topic or PUBLISH to publish a new message: ");
			terminal.println(
					"Enter CREATE to create a new topic or \nPUBLISH to publish a new message: " + startingString);
			if (startingString.toUpperCase().contains(CREATE)) {
				createTopic();
				this.wait(); // wait for ACK
				this.wait(); // wait for MESSAGE
			} else if (startingString.toUpperCase().contains(PUBLISH)) {
				if (publishMessage()) {
					this.wait(); // wait for ACK
					this.wait(); // wait for MESSAGE
				}
			} else {
				terminal.println("Invalid input.");
			}
		}
	}

	@Override
	public synchronized void onReceipt(DatagramPacket packet) {
		this.notify();
		byte[] data = packet.getData();
		if (getType(data) == ACK) {
			terminal.println("ACK received for Sequence Number " + getSequenceNumber(data));
		} else if (getType(data) == MESSAGE) {
			terminal.println("Message received: " + getMessage(data));
			sendAck(packet, terminal);
		}
	}

}
