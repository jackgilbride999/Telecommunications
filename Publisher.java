/** Publisher class for custom Publish-Subscribe protocol. Takes user input and creates
  * topics, which it can then publish messages for. Interacts with the Broker who stores
  * what topics exist as well as the list of subscribers to each topic. @author: Jack Gilbride
  */

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

	/* Constructor of the Publisher. Initialises the terminal, map of
	 * topic names and numbers, the listener and the datagram socket.
	 */
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

	/* Mainline of the publisher. Initialises the terminal, calls the contructor
	 * and the start method.
	 */
	public static void main(String[] args) {
		try {
			Terminal terminal = new Terminal("Publisher");
			(new Publisher(terminal)).start();
			terminal.println("Program completed");
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}

	/* Function to create a topic. Takes user input for the topic name and sends the packet to the
	 * broker to create the packet.
	 */
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

	/* Function to take user input and interact with the broker to publish a message to subscribers of a particular
	 * topic. Contains simple error handling to check if the topic exists before the packet is sent to the broker.
	 */
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

	/* Start function of the publisher. Takes user input to either create a topic or publish a message, then waits
	 * for an acknowledgement and a reply message. Assumes that the ack or message will not be lost across the medium.
	 */
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

	/* Implementation of the abstract method in Node.java to handle Datagram Packets. Prints either a message or
	 * an ack from the broker to the terminal, no extra processing of received packets is required.
	 */
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
