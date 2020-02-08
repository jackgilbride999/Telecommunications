/** Controller class for custom implementation of OpenFlow Software Defined Network.
    Contains information about a hard-coded network layout, which is sent out to each
    switch to define the network. @author: Jack Gilbride.
*/

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class Controller extends Node {

	public static final byte R1 = 1;
	public static final byte R2 = 2;
	public static final byte R3 = 3;
	public static final byte R4 = 4;
	public static final byte R5 = 5;
	public static final byte R6 = 6;
	public static final byte R7 = 7;
	public static final byte R8 = 8;
	public static final byte E1 = 9;
	public static final byte E2 = 10;
	public static final byte E3 = 11;
	public static final byte E4 = 12;

	private static Terminal terminal;

	/**
	 * 2D Array storing the preconfiguration information of the controller. Column
	 * 0 = final destination port number, column 1 = original source port number, 
	 * column 2 = current router number, column 3 = port number of previous hop, 
	 * column 4 = port number of next hop.
	 */
	private static final byte[][] PRECONF_INFO = { { E1, E2, R1, E1, R4 }, { E1, E2, R4, R1, R7 },
			{ E1, E2, R7, R4, R8 }, { E1, E2, R8, R7, E2 }, { E1, E3, R1, E1, R2 }, { E1, E3, R2, R1, R5 },
			{ E1, E3, R5, R2, E3 }, { E1, E4, R1, E1, R3 }, { E1, E4, R3, R1, E4 }, { E2, E1, R8, E2, R7 },
			{ E2, E1, R7, R8, R4 }, { E2, E1, R4, R7, R1 }, { E2, E1, R1, R4, E1 }, { E2, E3, R8, E2, R7 },
			{ E2, E3, R7, R8, R5 }, { E2, E3, R5, R7, E3 }, { E2, E4, R8, E4, R6 }, { E2, E4, R6, R8, R3 },
			{ E2, E4, R3, R6, E4 }, { E3, E1, R5, E3, R2 }, { E3, E1, R2, R5, R1 }, { E3, E1, R1, R2, E1 },
			{ E3, E2, R5, E3, R7 }, { E3, E2, R7, R5, R8 }, { E3, E2, R8, R7, E2 }, { E3, E4, R5, E3, R2 },
			{ E3, E4, R2, R5, R1 }, { E3, E4, R1, R2, R3 }, { E3, E4, R3, R1, E4 }, { E4, E1, R3, E4, R1 },
			{ E4, E1, R1, R3, E1 }, { E4, E2, R3, E4, R6 }, { E4, E2, R6, R3, R8 }, { E4, E2, R8, R6, E2 },
			{ E4, E3, R3, E4, R6 }, { E4, E3, R6, R3, R4 }, { E4, E3, R4, R6, R7 }, { E4, E3, R7, R4, R5 },
			{ E4, E3, R5, R7, E3 } };

	/** Arrays to store the other nodes once they are initialised.*/
	private Switch[] switches;
	private EndNode[] endNodes;

	/* Mainline. Contruct a new Controller and start its functionality. */
	public static void main(String[] args) {
		try {
			terminal = new Terminal("Controller");
			(new Controller(terminal)).start();
		} catch (java.lang.Exception e) {
			e.printStackTrace();
		}
	}

	/* Controller constructor. Initialises the Controller as well as each of
	*  the switches and end nodes in the network. Passed the terminal that it
	*  will use for output.
	*/
	Controller(Terminal terminal) throws SocketException {
		// Initialise Controller
		Controller.terminal = terminal;
		this.socket = new DatagramSocket(BASE_PORT_NUMBER + CONTROLLER_PORT);
		listener.go();
		// Initialise Switches
		switches = new Switch[NUM_SWITCHES + 1];
		for (byte i = R1; i <= NUM_SWITCHES; i++) {
			switches[i] = new Switch(i);
		}
		// Initialise EndNodes
		endNodes = new EndNode[NUM_END_NODES];
		for (byte j = 0; j < NUM_END_NODES; j++) {
			endNodes[j] = new EndNode((byte) (j + NUM_SWITCHES + 1));
		}
	}

	/* Start the initial switch. Once the first switch has finished setup the second
	*  switch will start, and so on.
	*/
	public synchronized void start() throws Exception {
		startSwitch(R1);
	}

	/* Call the start method of the next switch, beginning its setup sequence
	*  where the switch will send a hello packet.
	*/
	private synchronized void startSwitch(int routerNumber) {
		Switch s = switches[routerNumber];
		s.start();
	}

	/* Send the relevant flow table to the relevant switch. Parses out the relevant
	*  information from the preconfiguration information into a two-dimensional
	*  array. Converts this into a one-dimensional array which can be encapsulated
	*  into a Datagram Packet to send to the switch.
	*/
	private synchronized void sendTable(byte switchNumber) {
		// create a new flow mod table; the row corresponds to the switch number
		byte[] table = new byte[PRECONF_INFO.length * PRECONF_INFO[0].length];
		for (int i = 0; i < PRECONF_INFO.length; i++) {
			if (switchNumber == PRECONF_INFO[i][SWITCH_INDEX]) {
				int j = 0;
				while (table[j] != 0) {
					j++; // set j to an index where the value is zero
				}
				for (int k = 0; k <= OUTPUT_INDEX; k++) {
					table[j] = PRECONF_INFO[i][k];
					j++;
				}
			}
		}
		// now we have a 2D array. The first row is full of zeros. The other
		// rows correspond to switch numbers and contain the tables that should
		// be sent in a packet.
		byte[] flowTable = new byte[table.length + 1];
		// set the first byte of the flowTable to be the type, a FLOW_MOD
		// packet
		for (int i = 0; i < table.length; i++) {
			flowTable[i + 1] = table[i];
		}
		flowTable[0] = OFPT_FLOW_MOD;
		DatagramPacket packet = new DatagramPacket(flowTable, flowTable.length);
		InetSocketAddress dstAddress = new InetSocketAddress(LOCALHOST, BASE_PORT_NUMBER + switchNumber);
		packet.setSocketAddress(dstAddress);
		try {
			this.socket.send(packet); // try to send the flow table to the switch
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* Send a packet of type OPFT_HELLO when passed the destination address.
	*/
	private synchronized void sendHello(InetSocketAddress dstAddress) {
		byte[] data = { OFPT_HELLO };
		DatagramPacket hello = new DatagramPacket(data, data.length);
		hello.setSocketAddress(dstAddress);
		try {
			socket.send(hello);
			int port = dstAddress.getPort() - BASE_PORT_NUMBER;
			terminal.println("Hello packet sent to switch number " + port + ".");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* Send a packet of type OPFT_FEATURES_REQUEST when passed the destination address.
	*/
	private synchronized void sendFeaturesRequest(InetSocketAddress dstAddress) {
		byte[] data = { OFPT_FEATURES_REQUEST };
		DatagramPacket featuresRequest = new DatagramPacket(data, data.length);
		featuresRequest.setSocketAddress(dstAddress);
		try {
			socket.send(featuresRequest);
			int port = dstAddress.getPort() - BASE_PORT_NUMBER;
			terminal.println("Features request send to switch number  " + port + ".");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* Implementation of the abstract class in Node.java which handles received Datagram Packets.
	*/
	@Override
	public synchronized void onReceipt(DatagramPacket packet) {
		byte[] data = packet.getData();
		int port = packet.getPort() - BASE_PORT_NUMBER;
		byte type = getType(data);
		switch (type) {
		// Handle a Hello packet by replying with a Hello and a Features Request.		
		case OFPT_HELLO:
			terminal.println("Hello packet received from switch number " + port + ".");
			sendHello((InetSocketAddress) packet.getSocketAddress());
			sendFeaturesRequest((InetSocketAddress) packet.getSocketAddress());
			break;
		// Handle a Features Reply by printing the features to the terminal.
		case OFPT_FEATURES_REPLY:
			terminal.println("Features reply received from switch number " + port + ".");
			if (data[1] == BASIC_FEATURES) {
				terminal.println("Switch " + port + " has basic features.");
			} else {
				terminal.println("Features of switch " + port + " unknown.");
			}
			sendTable((byte) port);
			break;
		// Handle a confirmation of completion of flow mod as the end of the setup sequence
		// for that switch. Start the next swtich.
		case OFPT_FLOW_MOD:
			terminal.println("Flow mod complete for switch number " + port + ".");
			if (port < NUM_SWITCHES) {
				startSwitch(port + 1); // flow mod is complete, start the next switch
			}
			break;
		// Handle an unrecognised packet forwarded by a switch by telling that switch to 
		// drop the packet.
		case OFPT_PACKET_IN:
			setType(data, OFPT_FLOW_REMOVED);
			packet.setData(data);
			packet.setSocketAddress(packet.getSocketAddress());
			try {
				socket.send(packet);
				terminal.println("Told switch " + port + " to drop packet.");
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		}
	}

}
