import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Broker extends Node {
	private Terminal terminal;
	/** Map topic names to a list of its Subscribers in case the system needs to
	 * be expanded to multiple Subscribers. */
	private Map<String, ArrayList<InetSocketAddress>> subscriberMap;
	/** Map topic numbers to topic names, map agreed with Publisher. */
	private Map<Integer, String> topicNumbers;

	Broker(Terminal terminal) {
		this.terminal = terminal;
		try {
			socket = new DatagramSocket(BKR_PORT);
			listener.go();
		} catch (

		java.lang.Exception e) {
			e.printStackTrace();
		}
		subscriberMap = new HashMap<String, ArrayList<InetSocketAddress>>();
		topicNumbers = new HashMap<Integer, String>();
	}

	public static void main(String[] args) {
		try {
			Terminal terminal = new Terminal("Broker");
			(new Broker(terminal)).start();
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}

	private boolean createTopic(byte[] data) {
		ArrayList<InetSocketAddress> socketNumbers = new ArrayList<InetSocketAddress>();
		String topicName = getMessage(data);
		if (!subscriberMap.containsKey(topicName)) {
			subscriberMap.put(topicName, socketNumbers);
			int topicNumber = getTopicNumber(data);
			topicNumbers.put(topicNumber, topicName);
			terminal.println("Topic " + topicName + " was created.");
			return true;
		}
		return false;
	}

	private boolean publish(byte[] data) {
		int topicNumber = getTopicNumber(data);
		setType(data, PUBLICATION);
		if (topicNumbers.containsKey(topicNumber)) {
			String topicName = topicNumbers.get(topicNumber);
			ArrayList<InetSocketAddress> dstAddresses = subscriberMap.get(topicName);
			if (!dstAddresses.isEmpty()) {
				for (int i = 0; i < dstAddresses.size(); i++) {
					DatagramPacket publication = new DatagramPacket(data, data.length, dstAddresses.get(i));
					try {
						socket.send(publication);
						terminal.println("Topic " + topicName + " was published.");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return true;
		}
		return false;
	}

	private boolean subscribe(byte[] data, SocketAddress subscriberAddress) {
		String topicName = getMessage(data);
		if (subscriberMap.containsKey(topicName)) {
			ArrayList<InetSocketAddress> subscribers = subscriberMap.get(topicName);
			subscribers.add((InetSocketAddress) subscriberAddress);
			subscriberMap.remove(topicName);
			subscriberMap.put(topicName, subscribers);
			terminal.println("A new subscriber subscribed to " + topicName + ".");
			return true;
		}
		return false;
	}

	private boolean unsubscribe(byte[] data, SocketAddress subscriberAddress) {
		boolean unsubscribed = false;
		String topicName = getMessage(data);
		if (subscriberMap.containsKey(topicName)) {
			ArrayList<InetSocketAddress> subscribers = subscriberMap.get(topicName);
			if (!subscribers.isEmpty()) {
				for (int i = 0; i < subscribers.size(); i++) {
					if (subscribers.get(i).equals(subscriberAddress)) {
						subscribers.remove(i);
						terminal.println("A subscriber unsubscribed from " + topicName + ".");
						unsubscribed = true;
					}
				}
			}
			subscriberMap.remove(topicName);
			subscriberMap.put(topicName, subscribers);
		}
		return unsubscribed;
	}

	private void sendMessage(String message, SocketAddress socketAddress) {
		InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
		DatagramPacket packet = createPackets(MESSAGE, 0, message, inetSocketAddress)[0];
		try {
			socket.send(packet);
			terminal.println("Broker sent a message: " + message);
		} catch (IOException e) {
			e.printStackTrace();
			terminal.println("Broker failed to send a message: " + message);
		}
	}

	public synchronized void start() throws Exception {
		terminal.println("Waiting for contact");
		while (true) {
			this.wait();
		}
	}

	public synchronized void onReceipt(DatagramPacket packet) {
		try {
			this.notify();
			byte[] data = packet.getData();
			switch (getType(data)) {
			case ACK:
				terminal.println("ACK received for Sequence Number " + getSequenceNumber(data) + ".");
				break;
			case NAK:
				// No implementation due to time constraints.
				break;
			case RTS:
				// No implementation due to time constraints.
				break;
			case CTS:
				// No implementation due to time constraints.
				break;
			case CREATION:
				terminal.println("Request recieved to create a topic.");
				sendAck(packet, terminal);
				if (!createTopic(data)) {
					sendMessage("This is already a topic.", packet.getSocketAddress());
				} else {
					sendMessage("Topic creation successful.", packet.getSocketAddress());
				}
				break;
			case PUBLICATION:
				terminal.println("Request recieved to publish a message.");
				sendAck(packet, terminal);
				if (!publish(data)) {
					sendMessage("This topic does not exist.", packet.getSocketAddress());
				} else {
					sendMessage("Publication successful.", packet.getSocketAddress());
				}
				break;
			case SUBSCRIPTION:
				terminal.println("Request recieved to subscribe to a topic.");
				sendAck(packet, terminal);
				if (!subscribe(data, packet.getSocketAddress())) {
					sendMessage("This topic does not exist.", packet.getSocketAddress());
				} else {
					sendMessage("Subscription successful.", packet.getSocketAddress());
				}
				break;
			case UNSUBSCRIPTION:
				terminal.println("Request recieved to unsubscribe from a topic.");
				sendAck(packet, terminal);
				if (!unsubscribe(data, packet.getSocketAddress())) {
					sendMessage("This topic does not exist.", packet.getSocketAddress());
				} else {
					sendMessage("Unsubscription successful.", packet.getSocketAddress());
				}
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
