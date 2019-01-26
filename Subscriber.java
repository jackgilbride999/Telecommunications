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

	public static void main(String[] args) {
		try {
			Terminal terminal = new Terminal("Subscriber");
			(new Subscriber(terminal)).start();
		} catch (java.lang.Exception e) {
		}
	}

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
