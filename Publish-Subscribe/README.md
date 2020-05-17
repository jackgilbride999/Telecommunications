# Publish-Subscribe
Java implementation of a Publish-Subscribe protocol, which uses datagrams to communicate between a Subscriber, a Broker and a Publisher. The full report, [here](Publish-Subscribe-Report.pdf), details the entire technical specification of the project.

## Usage
1. Open a Java project containing all of the classes in the repository.
2. Run the *Broker*, the *Publisher* and the *Subscriber* seperately.
3. The *Broker*, *Publisher* and *Subscriber* will each open up their terminal windows. Each window displays output to the user. The windows for the *Publisher* and *Subscriber* also take input.
4. Enter `CREATE` in the *Publisher* window to create a topic. Enter the name of the topic you wish to create. Do this one or more times. The names of these topics are now stored in the *Broker*.
5. Enter `SUBSCRIBE` in the *Subscriber* window. Enter the name of one of the topics that you have created. This subscription is noted by the *Broker*.
6. The *Subscriber* is now in a waiting state, where it can receive publications.
7. Enter `PUBLISH` in the publisher window to publish a message for a topic. Enter the name of the topic. Enter the message that you would like to publish. Do this one or more times.
8. The *Broker* will look at these messages and see if their topics match any *Subscribers*. If a *Subscriber* is subscribed to the topic, they will receive that message and output it on their terminal.

## Known Limitations
- In the current implementation of the protocol only one *Broker*, *Subscriber* and *Publisher* may exist at one time. Trying to run multiple instances of any node will bind two nodes to the same DatagramSocket address and cause a runtime error.
- Currently the *Subscriber* has two states. The first state is before the user has subscribed to a topic, where the terminal can take user input. The second state is after the user has subcribed to a topic, where the *Subscriber* waits for published messages in DatagramPackets. This means that the user is limited to subscribing to one topic and not being able to enter any more input. The system may be extended to allow multiple subscriptions and unsubscriptions, as the *Broker* already contains functionality to allow unsubscriptions.
