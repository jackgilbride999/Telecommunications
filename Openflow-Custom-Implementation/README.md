# OpenFlow-Custom-Implementation
Custom implementation of the OpenFlow Software Defined Networking protocol.

The Terminal class, the constructor of Node and the Listener class were provided to assist with user I/O and threading. The remainder of the Node class, the Controller, the Switch and the EndNode were written to create an OpenFlow network.

By default the network created when the code is run is a fixed network, containing eight switches, one controller and four end nodes. The end nodes become aware of how to contact the network once contacted by their closest switch, but have no overall view of the network.

USAGE:
- Run the program containing all classes in the repository.
- A seperate terminal window will open for each node in the network. Each terminal provides the user with output about that node. The terminals for EndNodes also take user input.
- The EndNodes will prompt the user to wait for incoming messages or send messages.
- Enter WAIT on whichever EndNode you would like to wait for a message. EndNodes can only receive a message when they are in the waiting state.
- Enter SEND on whichever EndNode you would like to send a message from. Enter the letter associated with the destination node. Enter the message to be sent. If an EndNode tries to send a message to itself it will be dropped by the network. Otherwise, provided that the destination EndNode is waiting, the message will be received and visually outputted at the destination.
- Information about the routing of the packet through the network will also be visible in the terminal window of any Switch that the packet is routed through.
