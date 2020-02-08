
/** EndNode class for custom implementation of OpenFlow Software Defined Network.
    Contains I/O handling and packet forwarding into the network. The end node 
    only decides the destination of the message and has no overall view of the 
    network or the OpenFlow protocol. @author: Jack Gilbride.
*/

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class EndNode extends Node {

	private Terminal terminal;
	private InetSocketAddress dstAddress;
	private final byte socketNumber;
	private static final String WAIT = "WAIT";
	private static final String SEND = "SEND";

	/** EndNode constructor. Initialises the terminal, datagram socket and listener of the end node.
	*/
	EndNode(byte socketNumber) throws SocketException {
		this.socketNumber = socketNumber;
		this.terminal = new Terminal("EndNode " + (socketNumber-NUM_SWITCHES));
		terminal.println(
				"This node can wait for incoming messages or\n send a message, depending on your input. While\n not waiting, any incoming messages will be dropped.");
		this.socket = new DatagramSocket(BASE_PORT_NUMBER + socketNumber);
		listener.go();
	}

	/** Implementation of the abstract onReceipt function in Node.java. If the datagram received is a
	  * message, it is printed to the terminal. Otherwise if it is an initialisation message from a
	  * switch, that switch's address is set as the destination address for all packets sent out.
	  */
	@Override
	public synchronized void onReceipt(DatagramPacket packet) {
		this.terminal.println("Got a packet.");
		if (getType(packet.getData()) == NODE_INITIALISE_SWITCH) {
			dstAddress = (InetSocketAddress) packet.getSocketAddress();
			terminal.println("Connected to switch " + (packet.getPort() - BASE_PORT_NUMBER) + ".");
		} else if (getType(packet.getData()) == NODE_MESSAGE) {
			byte[] data = packet.getData();
			byte src = getMessageSource(data);
			String message = getMessageContent(data);
			terminal.println("New message from end node " + src%NUM_SWITCHES + ": " + message);
		}
		this.start();
	}

	/* Start method of the end node which lets the user choose whether they would like to send a message
	 * or wait for a message.
	 */
	public synchronized void start() {
		while (true) {
			String chosenState = terminal.read("Please enter SEND or WAIT to continue: ").toUpperCase();
			terminal.println("Please enter SEND or WAIT to continue: " + chosenState);
			if (chosenState.contains(WAIT)) {
				terminal.println("Wating for messages.");
				return;
			} else if (chosenState.contains(SEND)) {
				sendMessage();
			} else {
				terminal.println("Invalid input.");
			}
		}
	}

	/* Function to send a message to another end node. Asks the user which end node to send the
	 * message to as well as the content of the message. Sends the message into the network.
	 */
	private synchronized void sendMessage() {
		String dest;
		boolean validInput = false;
		byte[] data = new byte[PACKETSIZE];
		setType(data, NODE_MESSAGE);
		setSrc(data, this.socketNumber);
		do {
			dest = terminal.read("Send to node 1, 2, 3 or 4? ");
			terminal.println("Send to node 1, 2, 3 or 4? " + dest);
			if (dest.equals("1") || dest.equals("2") || dest.equals("3") || dest.equals("4")) {
				validInput = true;
				byte finalDst = (byte) (Integer.parseInt(dest)+NUM_SWITCHES);
				setDst(data, finalDst);
			} else {
				terminal.println("Invalid input.");
			}
		} while (!validInput);
		String stringMessage = terminal.read("Please enter a message to send: ");
		terminal.println("Please enter a message to send: " + stringMessage);
		setMessage(data, stringMessage);
		DatagramPacket message = new DatagramPacket(data, data.length);
		message.setSocketAddress(dstAddress);
		try {
			socket.send(message);
			terminal.println("Message sent.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private synchronized void setSrc(byte[] data, byte src) {
		data[1] = src;
	}

	private synchronized void setDst(byte[] data, byte dst) {
		data[2] = dst;
	}

	private synchronized void setMessage(byte[] data, String message) {
		byte[] content = (byte[]) message.getBytes();
		for (int i = 0; i < content.length; i++) {
			data[i + 3] = content[i];
		}
	}

}
