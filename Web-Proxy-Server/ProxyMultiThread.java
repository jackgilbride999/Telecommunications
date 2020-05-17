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
import java.util.*;

public class ProxyMultiThread implements Runnable {

	// Constants for user input
	private static final String BLOCKED = "BLOCKED";
	private static final String CACHED = "CACHED";
	private static final String CLOSE = "CLOSE";
	private static final String HELP = "HELP";

	// Static local variables. Data structures to keep track of cached sites, blocked sites and threads.
	private static HashMap<String, File> cachedMap;
	private static HashMap<String, String> blockedMap;
	private static ArrayList<Thread> threadList;

	/*
	 * Static methods.
	 */
	// Get the cached page from the cache hashmap
	public static File getCachedPage(String url) {
		return cachedMap.get(url);
	}

	// Add a page to the cache hashmap
	public static void addCachedPage(String urlString, File fileToCache) {
		cachedMap.put(urlString, fileToCache);
	}

	// Go through the blocked list to check whether the site is blocked
	public static boolean isBlocked(String url) {
		for (String key : blockedMap.keySet()) {
			if (url.contains(key)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * main method. Create the proxy and start listening for a client connection.
	 */
	public static void main(String[] args) {
		ProxyMultiThread proxy = new ProxyMultiThread();
		proxy.listen();
	}

	/*
	 * Local variables. The port number for the browser to listen on, a server
	 * socket to listen to this port, a boolean to declare whether the proxy is
	 * running, and a seperate thread for the management console.
	 */
	private int browserPort;
	private ServerSocket browserListener;
	private volatile boolean running = true;
	private Thread managementConsole;

	/*
	 * Constructor for the Proxy.
	 */
	public ProxyMultiThread() {
		// initialise the data structures and client port number
		cachedMap = new HashMap<>();
		blockedMap = new HashMap<>();
		threadList = new ArrayList<>();
		browserPort = 9999;
		// Spin off a seperate thread to handle the management console
		managementConsole = new Thread(this);
		managementConsole.start();
		// Initalize the maps of cached site and blocked sites
		initializeCachedSites();
		initializeBlockedSites();
		// Start listening to the browser
		try {
			browserListener = new ServerSocket(browserPort);
			System.out.println("Waiting for client on port " + browserListener.getLocalPort());
			running = true;
		} catch (SocketException e) {
			System.out.println("Socket Exception when connecting to client");
		} catch (SocketTimeoutException e) {
			System.out.println("Timeout occured while connecting to client");
		} catch (IOException e) {
			System.out.println("IO exception when connecting to client");
		}
	}

	/*
	 * Non static methods.
	 */

	// Spin off a new thread for every new connection that the browser requests
	public void listen() {
		while (running) {
			try {
				Socket socket = browserListener.accept();
				Thread thread = new Thread(new ConnectionThread(socket));
				threadList.add(thread);
				thread.start();
			} catch (SocketException e) {
				System.out.println("Server closed");
			} catch (IOException e) {
				System.out.println("Error creating new Thread from ServerSocket.");
			}
		}
	}

	// Initalize the data structure for the cached sites by reading from the cache
	// file. If a cache file does not exist, create one
	private void initializeCachedSites() {
		try {
			File cachedSites = new File("cachedSites.txt");
			if (!cachedSites.exists()) {
				System.out.println("Creating new cache file");
				cachedSites.createNewFile();
			} else {
				FileInputStream cachedFileStream = new FileInputStream(cachedSites);
				ObjectInputStream cachedObjectStream = new ObjectInputStream(cachedFileStream);
				cachedMap = (HashMap<String, File>) cachedObjectStream.readObject();
				cachedFileStream.close();
				cachedObjectStream.close();
			}
		} catch (IOException e) {
			System.out.println("Error loading previously cached sites file");
		} catch (ClassNotFoundException e) {
			System.out.println("Class not found loading in preivously cached sites file");
		}
	}

	// Initalize the data structure for the blocked sites by reading from the
	// blocked file. If a blocked file does not exist, create one
	private void initializeBlockedSites() {
		try {
			File blockedSitesTxtFile = new File("blockedSites.txt");
			if (!blockedSitesTxtFile.exists()) {
				System.out.println("No blocked sites found - creating new file");
				blockedSitesTxtFile.createNewFile();
			} else {
				FileInputStream blockedFileStream = new FileInputStream(blockedSitesTxtFile);
				ObjectInputStream blockedObjectStream = new ObjectInputStream(blockedFileStream);
				blockedMap = (HashMap<String, String>) blockedObjectStream.readObject();
				blockedFileStream.close();
				blockedObjectStream.close();
			}
		} catch (IOException e) {
			System.out.println("Error loading previously cached sites file");
		} catch (ClassNotFoundException e) {
			System.out.println("Class not found loading in preivously cached sites file");
		}
	}

	// Close the server. Write back to the cached and blocked files. Join the
	// threads.
	private void closeServer() {
		System.out.println("Closing server");
		running = false;
		try {
			FileOutputStream cachedFileStream = new FileOutputStream("cachedSites.txt");
			ObjectOutputStream cachedObjectStream = new ObjectOutputStream(cachedFileStream);

			cachedObjectStream.writeObject(cachedMap);
			cachedObjectStream.close();
			cachedFileStream.close();
			System.out.println("Cached sites written");

			FileOutputStream blockedFileStream = new FileOutputStream("blockedSites.txt");
			ObjectOutputStream blockedObjectStream = new ObjectOutputStream(blockedFileStream);
			blockedObjectStream.writeObject(blockedMap);
			blockedObjectStream.close();
			blockedFileStream.close();
			System.out.println("Blocked site list saved");
			try {
				for (Thread thread : threadList) {
					if (thread.isAlive()) {
						thread.join();
					}
				}
			} catch (InterruptedException e) {
				System.out.println("Interrupted exception when closing server.");
			}

		} catch (IOException e) {
			System.out.println("Error saving cache/blocked sites");
		}
		try {
			System.out.println("Terminating connection");
			browserListener.close();
		} catch (Exception e) {
			System.out.println("Exception closing proxy's server socket");
			e.printStackTrace();
		}
	}

	// The functionality of the management console. Watch System.in and look out for
	// commands. If a defined command is not entered, assume that the input is a URL
	// to be blocked and block it.
	@Override
	public void run() {
		Scanner terminalScanner = new Scanner(System.in);
		String userInput;
		while (running) {
			System.out.println("Please enter a command. Enter HELP to see the list of commands.");
			userInput = terminalScanner.nextLine().toUpperCase();

			switch (userInput) {
				case BLOCKED:
					System.out.println("\nCurrently Blocked Sites");
					for (String key : blockedMap.keySet()) {
						System.out.println(key);
					}
					System.out.println();
					break;
				case CACHED:
					System.out.println("\nCurrently Cached Sites");
					for (String key : cachedMap.keySet()) {
						System.out.println(key);
					}
					System.out.println();
					break;
				case CLOSE:
					running = false;
					closeServer();
					break;
				case HELP:
					System.out.println("Enter BLOCKED to view the list of blocked URLs.");
					System.out.println("Enter CACHED to view the list of caches webpages");
					System.out.println("Enter CLOSE to close the proxy server.");
					System.out.println("Enter HELP to see the list of possible commands");
					System.out.println("Otherwise, enter a URL to add it to the blocked list.");
					break;
				default:
					blockedMap.put(userInput.toLowerCase(), userInput.toLowerCase());
					System.out.println("\n" + userInput + " blocked successfully \n");
					break;
			}
		}
		terminalScanner.close();
	}
}