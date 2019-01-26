# OpenFlow-Custom-Implementation
Custom implementation of the OpenFlow Software Defined Networking protocol.

The Terminal class, the constructor of Node and the Listener class were provided to assist with user I/O and threading. The remainder of the Node class, the Controller, the Switch and the EndNode were written to create an OpenFlow network.

By default the network created when the code is run is a fixed network, containing eight switches, one controller and four end nodes. The end nodes become aware of how to contact the network once contacted by their closest switch, but have no overall view of the network.
