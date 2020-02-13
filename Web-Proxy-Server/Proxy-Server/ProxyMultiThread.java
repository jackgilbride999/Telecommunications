import java.io.*;
import java.net.*;

public class ProxyMultiThread {
	/*
	# Web Proxy Server
	The objective of the exercise is to implement a Web Proxy Server. 
	- A Web proxy is a local server, which fetches items from the Web on behalf of a Web client instead of the client fetching them directly. 
	- This allows for caching of pages and access control.

	The program should be able to:
	1. Respond to HTTP & HTTPS requests, and should display each request on a management console. It should forward the request to the Web server and relay the response to the browser.
	2. Handle Websocket connections.
	3. Dynamically block selected URLs via the management console.
	4. Effectively cache requests locally and thus save bandwidth. You must gather timing and bandwidth data to prove the efficiency of your proxy.
	5. Handle multiple requests simultaneously by implementing a threaded server.

	The program can be written in a programming language of your choice. However, you must ensure that you do not overuse any API or Library functionality that implements the majority of the work for you.
	*/

	public static void main(String[] args) {
		try {
            if (args.length != 3)
                throw new IllegalArgumentException("insuficient arguments");
            // and the local port that we listen for connections on
            String host = args[0];
            int remoteport = Integer.parseInt(args[1]);
            int localport = Integer.parseInt(args[2]);
            // Print a start-up message
            System.out.println("Starting proxy for " + host + ":" + remoteport
                    + " on port " + localport);
            ServerSocket server = new ServerSocket(localport);
            while (true) {
                new ThreadProxy(server.accept(), host, remoteport);
            }
        } catch (Exception e) {
            System.err.println(e);
            System.err.println("Usage: java ProxyMultiThread "
                    + "<host> <remoteport> <localport>");
        }
	}

}