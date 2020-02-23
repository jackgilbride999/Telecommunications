
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class ProxyMultiThread implements Runnable {

    // A server socket to wait for requests over the network
    private ServerSocket browserListener;
    // Tells us whether the thread is running or not
    private volatile boolean running = true;
    // ArrayList to keep track of running threads
    static ArrayList<Thread> threads;
    // List of blocked websites
    static ArrayList<String> blockedList;
    // The cache mapping the URL to the cached file
    static HashMap<String, File> proxyCache;
    // The port number that the browser will find this program on
    int browserPort;

    // Constants for user input
    private static final String BLOCK = "BLOCK";
    private static final String BLOCKED = "BLOCKED";
    private static final String CACHED = "CACHED";
    private static final String CLOSE = "CLOSE";
    private static final String HELP = "HELP";

    /*
        Static methods.
    */
    public static File getFromCache(String pageUrl){
        return proxyCache.get(pageUrl);
    }

    public static void addToCache(String pageUrl, File file){
        proxyCache.put(pageUrl, file);
    }

    public static boolean isBlocked(String pageUrl){
        if(blockedList.contains(pageUrl)){
            return true;
        }
        return false;
    }
    /*
     * Create an instance of the ProxyMultiThread and listen for connections
     */
    public static void main(String[] args) {
        ProxyMultiThread proxy = new ProxyMultiThread();
        proxy.listen();
    }

    public ProxyMultiThread() {
        // Intiialize variables
        proxyCache = new HashMap<>();
        blockedList = new ArrayList<>();
        threads = new ArrayList<>();
        browserPort = 9999;

        // Start the management console thread
        new Thread(this).start();

        try {
            // If a cache file exists, populate the hashmap. If not, create a cache file.
            File cacheFile = new File("files/cacheFile.txt");
            if (cacheFile.exists()) {
                System.out.println("Found cache file.");
                FileInputStream fileInputStream = new FileInputStream(cacheFile);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                proxyCache = (HashMap<String, File>) objectInputStream.readObject();
                fileInputStream.close();
                objectInputStream.close();
            } else {
                System.out.println("Didn't find cache file.");
                cacheFile.createNewFile();
            }

            // And do the same for the list of blocked sites
            File blockedFile = new File("files/blockedFile.txt");
            if (blockedFile.exists()) {
                System.out.println("Found blocked file.");
                FileInputStream fileInputStream = new FileInputStream(blockedFile);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                blockedList = (ArrayList<String>) objectInputStream.readObject();
                fileInputStream.close();
                objectInputStream.close();
            } else {
                System.out.println("Didn't find blocked file.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassCastException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e){
            e.printStackTrace();
        }

        try {
            // listen out to a connection to the program from the browser
            browserListener = new ServerSocket(browserPort);
            running = true;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Listen for a new connection to the program from the browser. Once a new
     * connection is made, a socket is created. A new ThreadProxy is made to handle
     * the socket connection.
     */
    public void listen() {
        try {
            Socket browserSocket = browserListener.accept();
            Thread connection = new Thread(new ThreadProxy(browserSocket));
            threads.add(connection);
            connection.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Scan for user input on the terminal and carry out the appropriate action.
     */
    @Override
    public void run() {
        Scanner terminalScanner = new Scanner(System.in);
        while (running) {
            System.out.println("Please enter your command, or HELP to see the list of possible commands.");
            String userInput = terminalScanner.nextLine().toUpperCase();
            switch (userInput) {
                case BLOCK:
                    String url = userInput.substring(userInput.indexOf(' '));
                    if (isValidUrl(url)) {
                        blockedList.add(url);
                    } else {
                        System.out.println("Tried to block an invalid URL.");
                    }
                    break;
                case BLOCKED:
                    System.out.println("Printing list of blocked sites.");
                    for (String site : blockedList) {
                        System.out.println(site);
                    }
                    break;
                case CACHED:
                    System.out.println("Printing list of cached sites.");
                    for (String site : proxyCache.keySet()) {
                        System.out.println(site);
                    }
                    break;
                case CLOSE:
                    closeServer();
                    break;
                case HELP:
                    System.out.println("Enter BLOCK followed by a URL to block a new URL.");
                    System.out.println("Enter BLOCKED to view the list of blocked URLs.");
                    System.out.println("Enter CACHED to view the list of caches webpages");
                    System.out.println("Enter CLOSE to close the proxy server.");
                    System.out.println("Enter HELP to see the list of possible commands");
                    break;
                default:
                    System.out.println("Invalid command.");
                    System.out.println("Enter HELP to see the list of possible commands");
                    break;
            }
        }
        terminalScanner.close();

    }

    private boolean isValidUrl(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void closeServer() {
        running = false;
        try {
            FileOutputStream fileOutputStream = new FileOutputStream("files/cacheFile.txt");
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(proxyCache);
            objectOutputStream.close();
            fileOutputStream.close();
            System.out.println("Cached sites successfully written to file.");

            fileOutputStream = new FileOutputStream("files/blockedFile.txt");
            objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(blockedList);
            objectOutputStream.close();
            fileOutputStream.close();
            System.out.println("Blocked sites successfully written to file.");

            for (Thread thread : threads) {
                if (thread.isAlive()) {
                    thread.join();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            browserListener.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}