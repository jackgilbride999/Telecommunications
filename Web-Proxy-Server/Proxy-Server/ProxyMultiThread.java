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
	private static final String BLOCK = "BLOCK";
	private static final String BLOCKED = "BLOCKED";
	private static final String CACHED = "CACHED";
	private static final String CLOSE = "CLOSE";
	private static final String HELP = "HELP";

	/*
	 * Static methods.
	 */
	public static File getCachedPage(String url) {
		return cache.get(url);
	}

	public static void addCachedPage(String urlString, File fileToCache) {
		cache.put(urlString, fileToCache);
	}

	public static boolean isBlocked(String url) {
		if (blockedSites.get(url) != null) {
			return true;
		} else {
			return false;
		}
	}

	public static void main(String[] args) {
		ProxyMultiThread proxy = new ProxyMultiThread();
		proxy.listen();
	}

	private ServerSocket browserListener;
	private volatile boolean running = true;
	static HashMap<String, File> cache;
	static HashMap<String, String> blockedSites;
	static ArrayList<Thread> servicingThreads;
	int browserPort;

	public ProxyMultiThread() {
		cache = new HashMap<>();
		blockedSites = new HashMap<>();
		servicingThreads = new ArrayList<>();
		browserPort = 9999;

		new Thread(this).start();

		try {
			File cachedSites = new File("cachedSites.txt");
			if (!cachedSites.exists()) {
				System.out.println("No cached sites found - creating new file");
				cachedSites.createNewFile();
			} else {
				FileInputStream fileInputStream = new FileInputStream(cachedSites);
				ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
				cache = (HashMap<String, File>) objectInputStream.readObject();
				fileInputStream.close();
				objectInputStream.close();
			}

			File blockedSitesTxtFile = new File("blockedSites.txt");
			if (!blockedSitesTxtFile.exists()) {
				System.out.println("No blocked sites found - creating new file");
				blockedSitesTxtFile.createNewFile();
			} else {
				FileInputStream fileInputStream = new FileInputStream(blockedSitesTxtFile);
				ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
				blockedSites = (HashMap<String, String>) objectInputStream.readObject();
				fileInputStream.close();
				objectInputStream.close();
			}
		} catch (IOException e) {
			System.out.println("Error loading previously cached sites file");
		} catch (ClassNotFoundException e) {
			System.out.println("Class not found loading in preivously cached sites file");
		}

		try {
			browserListener = new ServerSocket(browserPort);
			System.out.println("Waiting for client on port " + browserListener.getLocalPort() + "..");
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

	public void listen() {
		while (running) {
			try {
				Socket socket = browserListener.accept();
				Thread thread = new Thread(new ThreadProxy(socket));
				servicingThreads.add(thread);
				thread.start();
			} catch (SocketException e) {
				System.out.println("Server closed");
			} catch (IOException e) {
				System.out.println("Error creating new Thread from ServerSocket.");
			}
		}
	}

	private void closeServer() {
		System.out.println("Closing server");
		running = false;
		try {
			FileOutputStream cachedFileStream = new FileOutputStream("cachedSites.txt");
			ObjectOutputStream cachedObjectStream = new ObjectOutputStream(cachedFileStream);

			cachedObjectStream.writeObject(cache);
			cachedObjectStream.close();
			cachedFileStream.close();
			System.out.println("Cached sites written");

			FileOutputStream blockedFileStream = new FileOutputStream("blockedSites.txt");
			ObjectOutputStream blockedObjectStream = new ObjectOutputStream(blockedFileStream);
			blockedObjectStream.writeObject(blockedSites);
			blockedObjectStream.close();
			blockedFileStream.close();
			System.out.println("Blocked site list saved");
			try {
				for (Thread thread : servicingThreads) {
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

	@Override
	public void run() {
		Scanner terminalScanner = new Scanner(System.in);
		String userInput;
		while (running) {
			System.out.println(
					"Please enter a command. Enter HELP to see the list of commands.");
			userInput = terminalScanner.nextLine().toUpperCase();

			switch (userInput) {
				case BLOCKED:
					System.out.println("\nCurrently Blocked Sites");
					for (String key : blockedSites.keySet()) {
						System.out.println(key);
					}
					System.out.println();
					break;
				case CACHED:
					System.out.println("\nCurrently Cached Sites");
					for (String key : cache.keySet()) {
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
					blockedSites.put(userInput.toLowerCase(), userInput.toLowerCase());
					System.out.println("\n" + userInput + " blocked successfully \n");
					break;
			}
		}
		terminalScanner.close();
	}
}