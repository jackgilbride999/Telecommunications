/** Subscriber class for custom Publish-Subscribe protocol. Takes user input via a
  * terminal and interacts with the broker to subscribe and unsubscribe from topics.
  * Due to problems with threading I could not figure out how to get the subscriber
  * to wait for user input to subscribe and unsubscribe from topics while also waiting
  * on incoming packets to print messages of the topics that it is subscribed to. As a 
  * result the current implementation can subscribe to exactly one topic, then going to 
  * a state where it waits for messages from that topic. @author: Jack Gilbride
  */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class Subscriber extends Node {
	/** Constant substrings to recognize user input. */
	private static final String SUBSCRIBE = "SUB";
	public static final String UNSUBSCRIBE = "UNSUB";

	private Terminal terminal;
	private InetSocketAddress dstAddress;
	private boolean invalidInput;

	/* Subscriber constructor. Initialises the terminal, datagram socket and listener.
	*/
	Subscriber(Terminal terminal) {
		invalidInput = true;
		try {
			this.terminal = terminal;
			dstAddress = new InetSocketAddress(DEFAULT_DST, BKR_PORT);
			socket = new DatagramSocket(SUB_PORT);
			listener.go();
		} catch (java.lang.Exception e) {
		}
	}

	/* Start method of subscriber. The initial loop runs while the subscriber has not successfully subscribed to a
	 * topic. invalidInput may be set to false in the onReceipt method so that the initial loop is broken and the
	 * subscriber enters a state of permanently waiting for messages.
	 */
	public synchronized void start() throws Exception {
		while (invalidInput == true) {
			String startingString = terminal
					.read("Enter SUBSCRIBE to subscribe to a topic or UNSUBSCRIBE to unsubscribe from a topic: ");
			terminal.println("Enter SUBSCRIBE to subscribe to a topic or\nUNSUBSCRIBE to unsubscribe from a topic: "
					+ startingString);
			if (startingString.toUpperCase().contains(UNSUBSCRIBE)) {
				unsubscribe();
				this.wait(); // wait for ACK
				this.wait(); // wait for MESSAGE
			} else if (startingString.toUpperCase().contains(SUBSCRIBE)) {
				subscribe();
				this.wait(); // wait for ACK
				this.wait(); // wait for MESSAGE
			} else {
				terminal.println("Invalid input.");
				invalidInput = true;
			}
		}
		while (true) {
			this.wait();
		}
	}

	/* Takes user input about the name of the topic to subscribe to and sends a subscription
	 * packet to the broker.
	 */
	public synchronized void subscribe() {
		String data = terminal.read("Please enter a topic to subscribe to: ");
		terminal.println("Please enter a topic to subscribe to: " + data);
		terminal.println("Sending packet...");
		DatagramPacket packet = createPackets(SUBSCRIPTION, 0, data, dstAddress)[0];
		try {
			socket.send(packet);
		} catch (IOException e) {
		}
		terminal.println("Packet sent");
	}

	/* Takes user input about the name of the topic to unsubscribe from and sends an 
	 * unsubscription packet to the broker.
	 */
	public synchronized void unsubscribe() {
		String data = terminal.read("Please enter a topic to unsubscribe from: ");
		terminal.println("Please enter a topic to unsubscribe from: " + data);
		terminal.println("Sending packet...");
		DatagramPacket packet = createPackets(UNSUBSCRIPTION, 0, data, dstAddress)[0];
		try {
			socket.send(packet);
		} catch (IOException e) {
		}
		terminal.println("Packet sent");
	}

	/* Mainline for subscriber. Initialises the terminal, calls the constructor and start
	 * method.
	 */
	public static void main(String[] args) {
		try {
			Terminal terminal = new Terminal("Subscriber");
			(new Subscriber(terminal)).start();
		} catch (java.lang.Exception e) {
		}
	}

	/* Implementation of the abstract method in Node.java to handle incoming Datagram Packets. Prints either
	 * an ack, a message or a publication. May change the value of 'invalidInput' based on a received message
	 * to determine whether the subscriber can exit the state of looking for a topic to subscribe to.
	 */
	@Override
	public synchronized void onReceipt(DatagramPacket packet) {
		try {
			this.notify();
			byte[] data = packet.getData();
			if (getType(data) == ACK) {
				terminal.println("ACK received for Sequence Number " + getSequenceNumber(data) + ".");
			} else if (getType(data) == MESSAGE) {
				terminal.println("Message received: " + getMessage(data));
				sendAck(packet, terminal);
				if (getMessage(data).equals("This topic does not exist.")) {
					invalidInput = true;
				} else {
					invalidInput = false;
				}
			} else if (getType(data) == PUBLICATION) {
				terminal.println("New publication: " + getMessage(data));
				sendAck(packet, terminal);
			}
		} catch (Exception e) {
		}
	}
}
