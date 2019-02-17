# Publish-Subscribe
Java implementation of a Publish Subscribe protocol. Uses Datagrams to communicate between a subscriber, a broker and a publisher.

The code from Terminal.java as well as the Constructor and Listener class in Node.java were provided to give assistance with I/O and threading. The remaining code was written by me to build the Publish-Subscribe protocol.

USAGE:
- Open a Java project containing all of the classes in the repository.
- Run the Broker, the Publisher and the Subscriber seperately.
- The Broker, Publisher and Subscriber will each open up their terminal windows. Each window displays output to the user. The windows for the Publisher and Subscriber also take input.
- Enter CREATE in the Publisher window to create a topic. Enter the name of the topic you wish to create. Do this one or more times. The names of these topics are now stored in the Broker.
- Enter SUBSCRIBE in the Subscriber window. Enter the name of one of the topics that you have created. This subscription is noted by the Broker.
- The Subscriber is now in a waiting state, where it can receive publications.
- Enter PUBLISH in the publisher window to publish a message for a topic. Enter the name of the topic. Enter the message that you would like to publish. Do this one or more times.
- The Broker will look at these messages and see if their topics match any Subscribers. If a Subscriber is subscribed to the topic, they will receive that message and output it on their terminal.

KNOWN LIMITATIONS:
- In the current implementation of the protocol only one Broker, Subscriber and Publisher may exist at one time. Trying to run multiple instances of any node will bind two nodes to the same DatagramSocket address and cause a runtime error.
- Currently the Subscriber has two states. The first state is before the user has subscribed to a topic, where the terminal can take user input. The second state is after the user has subcribed to a topic, where the Subscriber waits for published messages in DatagramPackets. This means that the user is limited to subscribing to one topic and not being able to enter any more input. The system may be extended to allow multiple subscriptions and unsubscriptions, as the Broker already contains functionality to allow unsubscriptions.
