# OpenFlow-Custom-Implementation
Custom implementation of the OpenFlow Software Defined Networking protocol. The full report [here](OpenFlow-Report.pdf), details the entire technical specification of the project.

By default the network created when the code is run is a fixed network, containing eight switches, one controller and four end nodes. The end nodes become aware of how to contact the network once contacted by their closest switch, but have no overall view of the network.

## Usage
1. Compile the project in this directory. If using the command line, use `javac Controller.java`.
2. Run the project. If using the command line, use `java Controller`.
3. A seperate terminal window will open for each node in the network. Each terminal provides the user with output about that node. The terminals for *EndNodes* also take user input.
4. The *EndNodes* will prompt the user to wait for incoming messages or send messages.
5. Enter `WAIT` on whichever *EndNode* you would like to wait for a message. *EndNodes* can only receive a message when they are in the waiting state.
6. Enter `SEND` on whichever *EndNode* you would like to send a message from. Enter the letter associated with the destination node. Enter the message to be sent. If an *EndNode* tries to send a message to itself it will be dropped by the network. Otherwise, provided that the destination *EndNode* is waiting, the message will be received and visually outputted at the destination.
7. Information about the routing of the packet through the network will also be visible in the terminal window of any *Switch* that the packet is routed through.