/** Switch class for custom implementation of OpenFlow Software Defined Network.
    Finds out what other nodes it is connected to via initialisation messages from
    the controller, then will forward messages from end nodes and other switches
    based on its flow table. @author: Jack Gilbride.
*/

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;

public class Switch extends Node {
	private Terminal terminal;
	private int routerNumber;
	private byte[][] flowTable;
	private InetSocketAddress controllerAddress;
	private InetSocketAddress endNodeAddress;

	/** Constructor of switch. Initialises the terminal, datagram socket and listener.
	*/
	Switch(byte routerNumber) throws SocketException {
		this.routerNumber = BASE_PORT_NUMBER + routerNumber;
		socket = new DatagramSocket(this.routerNumber);
		this.terminal = new Terminal("Switch " + routerNumber);
		controllerAddress = new InetSocketAddress(LOCALHOST, BASE_PORT_NUMBER + CONTROLLER_PORT);
		endNodeAddress = null;
		listener.go();
	}

	/* Start the switch by sending a Hello packet to the controller.
	*/
	public synchronized void start() {
		sendHello();
	}

	/* Implementation of the abstract function in Node.java. Hands over to
	 * another function based on the source address.
	 */
	@Override
	public synchronized void onReceipt(DatagramPacket packet) {
		terminal.print("Got a packet: ");
		if (packet.getSocketAddress().equals(controllerAddress)) {
			handleControllerPacket(packet);
		} else {
			handleEndNodePacket(packet);
		}
	}

	/* Handles a packet sent from a controller.
	*/
	private synchronized void handleControllerPacket(DatagramPacket packet) {
		byte[] data = packet.getData();
		switch (getType(data)) {
		// If the packet is a hello, print to the terminal.
		case OFPT_HELLO:
			terminal.println("Hello packet received from controller.");
			break;
		// If the packet is a features request, reply that the switch has basic
		// features in a features reply.
		case OFPT_FEATURES_REQUEST:
			byte[] reply = { OFPT_FEATURES_REPLY, BASIC_FEATURES };
			DatagramPacket featuresReply = new DatagramPacket(reply, reply.length);
			featuresReply.setSocketAddress(controllerAddress);
			try {
				socket.send(featuresReply);
				terminal.println("Sent a features reply to controller.");
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		// If the packet is a flow mod packet, parse the switch's flow table from
		// the packet into a two-dimensional array.
		case OFPT_FLOW_MOD:
			terminal.println("Flow mod packet received from controller.");
			byte[] flatTable = Arrays.copyOfRange(data, 1, data.length);
			updateFlowtable(flatTable);
			for (int j = 0; j < flowTable.length; j++) {
				for (int k = 0; k < flowTable[0].length; k++) {
					terminal.print("" + flowTable[j][k] + " ");
				}
				terminal.println("");
			}
			packet.setSocketAddress(controllerAddress);
			try {
				socket.send(packet);
				terminal.println("Flow mod confirmation sent back to controller.");
			} catch (IOException e) {
				e.printStackTrace();
			}
			setEndNodeAddress();
			break;
		// If the packet tells the switch to drop the packet, do nothing.
		case OFPT_FLOW_REMOVED:
			terminal.println("Packet from end node dropped at instruction of controller.");
		}
	}

	/* Update the switch's flowtable given a one-dimensional array 
	 * representing it.
	 */
	private synchronized void updateFlowtable(byte[] flatFlowtable) {
		int rowCount;
		for (rowCount = 0; flatFlowtable[rowCount * 5] != 0; rowCount++)
			;
		flowTable = new byte[rowCount][OUTPUT_INDEX + 1];
		int i = 0;
		for (int j = 0; j < flowTable.length; j++) {
			for (int k = 0; k < flowTable[0].length; k++) {
				flowTable[j][k] = flatFlowtable[i];
				i++;
			}
		}
	}

	/* Send a hello packet to the controller.
	*/
	private synchronized void sendHello() {
		byte[] data = { OFPT_HELLO };
		DatagramPacket hello = new DatagramPacket(data, data.length);
		hello.setSocketAddress(controllerAddress);
		try {
			socket.send(hello);
			terminal.println("Hello packet sent to controller.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method lets the switch figure out which end node it is connected to by
	 * checking its flow table. If at any point in the table its immediate input or
	 * output is beyond the switch index, then it is an end node. Each switch is
	 * connected to a maximum of one node in the system so the check can end once if
	 * the end node is found.
	 */
	private synchronized void setEndNodeAddress() {
		boolean addressSet = false;
		int i = 0;
		while (i < flowTable.length && !addressSet) {
			if (flowTable[i][INPUT_INDEX] > NUM_SWITCHES) {
				int portNumber = flowTable[i][INPUT_INDEX] + BASE_PORT_NUMBER;
				this.endNodeAddress = new InetSocketAddress(LOCALHOST, portNumber);
				addressSet = true;
				terminal.println(
						"This switch is connected to end node " + (portNumber - BASE_PORT_NUMBER - NUM_SWITCHES) + ".");
			} else if (flowTable[i][OUTPUT_INDEX] > NUM_SWITCHES) {
				int portNumber = flowTable[i][OUTPUT_INDEX] + BASE_PORT_NUMBER;
				this.endNodeAddress = new InetSocketAddress(LOCALHOST, portNumber);
				addressSet = true;
				terminal.println(
						"This switch is connected to end node " + (portNumber - BASE_PORT_NUMBER - NUM_SWITCHES) + ".");
			}
			i++;
		}
		if (!addressSet) {
			terminal.println("This switch is not connected to an end node in the network.");
		} else {
			byte[] data = { NODE_INITIALISE_SWITCH };
			DatagramPacket initialisation = new DatagramPacket(data, data.length);
			initialisation.setSocketAddress(endNodeAddress);
			try {
				socket.send(initialisation);
				terminal.println("Switch port number sent to the connected end node.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/** Handle a packet not sent by the controller. This is a message meant to be forwared
	  * based on the switch's flow table. If the source and destination are not recognised
	  * together in a row of the flow table, the packet is forwarded to the controller to
	  * find out what to do with it. Otherwise the packet is forawrded to the next hop on
	  * the flow table.
	  */
	private synchronized void handleEndNodePacket(DatagramPacket packet) {
		byte[] data = packet.getData();
		if (getType(data) == NODE_MESSAGE) {
			terminal.println("Message from end node received.");
			byte nextHop = checkFlowtable(data, packet.getPort());
			if (nextHop == CONTROLLER_PORT) {
				terminal.println("Next hop not in flow table.");
				byte[] unrecognised = new byte[data.length];
				setType(unrecognised, OFPT_PACKET_IN);
				for (int i = 1; i < unrecognised.length; i++) {
					unrecognised[i] = data[i - 1];
				}
				DatagramPacket packetIn = new DatagramPacket(unrecognised, unrecognised.length);
				packetIn.setSocketAddress(controllerAddress);
				try {
					socket.send(packetIn);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				InetSocketAddress outputAddress = new InetSocketAddress(LOCALHOST, BASE_PORT_NUMBER + nextHop);
				packet.setSocketAddress(outputAddress);
				try {
					socket.send(packet);
					if (nextHop <= NUM_SWITCHES) {
						terminal.println(
								"Packet forwarded to switch " + nextHop + " on port " + outputAddress.getPort() + ".");
					} else {
						terminal.println("Packet forwarded to end node " + nextHop % NUM_SWITCHES + " on port "
								+ outputAddress.getPort() + ".");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/* Takes the data from a message and the port number of the previous hop of the packet.
	 * Checks the flowtable for a row with the correct previous hop, source address and 
	 * destination address to return the next hop in the table. If no next hop is found,
	 * the next hop is set as the controller port so that the controller can decide what to
	 * do with the packet.
	 */
	private synchronized byte checkFlowtable(byte[] data, int port) {
		assert (getType(data) == NODE_MESSAGE);
		byte src = getMessageSource(data);
		terminal.println("Source Port Number = " + (src + BASE_PORT_NUMBER) + ".");
		byte dst = getMessageDest(data);
		terminal.println("Destination Port Number = " + (dst + BASE_PORT_NUMBER) + ".");
		byte prev = (byte) (port - BASE_PORT_NUMBER);
		terminal.println("Previous Port Number = " + (prev + BASE_PORT_NUMBER) + ".");
		int nextHop = CONTROLLER_PORT;
		int i = 0;
		while (i < flowTable.length && nextHop == CONTROLLER_PORT) {
			if (src == flowTable[i][SRC_INDEX] && dst == flowTable[i][DST_INDEX] && prev == flowTable[i][INPUT_INDEX]) {
				nextHop = flowTable[i][OUTPUT_INDEX];
			}
			i++;
		}
		return (byte) nextHop;
	}
}
