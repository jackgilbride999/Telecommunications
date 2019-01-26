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

	Switch(byte routerNumber) throws SocketException {
		this.routerNumber = BASE_PORT_NUMBER + routerNumber;
		socket = new DatagramSocket(this.routerNumber);
		this.terminal = new Terminal("Switch " + routerNumber);
		controllerAddress = new InetSocketAddress(LOCALHOST, BASE_PORT_NUMBER + CONTROLLER_PORT);
		endNodeAddress = null;
		listener.go();
	}

	public synchronized void start() {
		sendHello();
	}

	@Override
	public synchronized void onReceipt(DatagramPacket packet) {
		terminal.print("Got a packet: ");
		if (packet.getSocketAddress().equals(controllerAddress)) {
			handleControllerPacket(packet);
		} else {
			handleEndNodePacket(packet);
		}
	}

	private synchronized void handleControllerPacket(DatagramPacket packet) {
		byte[] data = packet.getData();
		switch (getType(data)) {
		case OFPT_HELLO:
			terminal.println("Hello packet received from controller.");
			break;
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
		case OFPT_FLOW_REMOVED:
			// Do nothing with the packet
			terminal.println("Packet from end node dropped at instruction of controller.");
		}
	}

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
