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
import java.io.*;
import java.net.*;

public class ProxyMultiThread {

	public static void main(String[] args) {
		try {

            int browserPort = 9999;  // the local port that the program will listen for connections on
            ServerSocket server = new ServerSocket(browserPort);      // create a new server socket on the local port
            while (true) {
                Socket connectionSocket = server.accept();          // Listen for a connection to be made to server and accept it. Blocks until a connection is made.
                Thread thread = new Thread(new ThreadProxy(connectionSocket));// Create a new ThreadProxy given the connection socket            }
                thread.start();
            }
        } catch (Exception e) {
            System.err.println(e);
            System.err.println("Usage: java ProxyMultiThread "
                    + "<host> <remoteport> <localport>");
        }
	}

}