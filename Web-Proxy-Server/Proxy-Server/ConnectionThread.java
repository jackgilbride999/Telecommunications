import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.net.*;

public class ConnectionThread implements Runnable {

	/*
	 * Variables for this thread. A socket to connect to the client (browser), a
	 * reader and writer to the client, and a seperate thread to handle HTTPS
	 * communication from the client to the server.
	 */
	private Socket browserSocket;
	private BufferedReader clientReader;
	private BufferedWriter clientWriter;
	private Thread clientToServerHttpsThread;

	/*
	 * Constants for this class. To identify connection types and file types.
	 */
	private static final String CONNECT = "CONNECT";
	private static final String GET = "GET";
	private static final String JPG = ".jpg";
	private static final String JPEG = ".jpeg";
	private static final String PNG = ".png";
	private static final String GIF = ".gif";

	/*
	 * Constructor for this thread. Initalize the local variables.
	 */
	public ConnectionThread(Socket browserSocket) {
		this.browserSocket = browserSocket;
		try {
			this.browserSocket.setSoTimeout(2000);
			clientReader = new BufferedReader(new InputStreamReader(browserSocket.getInputStream()));
			clientWriter = new BufferedWriter(new OutputStreamWriter(browserSocket.getOutputStream()));
		} catch (IOException e) {
			System.out.println("Error initializing new thread.");
		}
	}

	/*
	 * Take the request from the client. If the site is blocked, do not fulfil the
	 * request. Identify whether it is a HTTP or HTTPS request. Handle by passing
	 * off to the appropriate method.
	 */
	@Override
	public void run() {
		String requestString, requestType, requestUrl;
		try {
			requestString = clientReader.readLine();
			String[] splitRequest = splitRequest(requestString);
			requestType = splitRequest[0];
			requestUrl = splitRequest[1];
		} catch (IOException e) {
			System.out.println("Error reading request from client.");
			return;
		}

		if (ProxyMultiThread.isBlocked(requestUrl)) {
			System.out.println("Blocked site " + requestUrl + " requested.");
			blockedSiteRequested();
			return;
		}

		switch (requestType) {
			case CONNECT:
				System.out.println("HTTPS request for : " + requestUrl);
				handleHTTPSRequest(requestUrl);
				break;
			default:
				File file = ProxyMultiThread.getCachedPage(requestUrl);
				if (file == null) {
					System.out.println("HTTP request for : " + requestUrl + ". No cached page found.");
					fulfilNonCachedRequest(requestUrl);
				} else {
					System.out.println("HTTP request for : " + requestUrl + ". Cached page found.");
					fulfilCachedRequest(file);
				}
				break;
		}
	}

	// Parse a CONNECT or GET request and return an array containing the URL and
	// port number
	private String[] splitRequest(String requestString) {
		String requestType, requestUrl;
		int requestSeparatorIndex;
		requestSeparatorIndex = requestString.indexOf(' ');
		requestType = requestString.substring(0, requestSeparatorIndex);
		requestUrl = requestString.substring(requestSeparatorIndex + 1);
		requestUrl = requestUrl.substring(0, requestUrl.indexOf(' '));
		if (!requestUrl.substring(0, 4).equals("http")) {
			requestUrl = "http://" + requestUrl;
		}
		return new String[] { requestType, requestUrl };
	}

	// Fetch a page from the cache for the client
	private void fulfilCachedRequest(File cachedFile) {
		try {
			String fileExtension = cachedFile.getName().substring(cachedFile.getName().lastIndexOf('.'));
			if (isImage(fileExtension)) {
				BufferedImage image = ImageIO.read(cachedFile);
				if (image == null) {
					System.out.println("Image " + cachedFile.getName() + " was null");
					String response = getResponse(404, false);
					clientWriter.write(response);
					clientWriter.flush();
				} else {
					String response = getResponse(200, false);
					clientWriter.write(response);
					clientWriter.flush();
					ImageIO.write(image, fileExtension.substring(1), browserSocket.getOutputStream());
				}
			} else {
				BufferedReader cachedFileBufferedReader = new BufferedReader(
						new InputStreamReader(new FileInputStream(cachedFile)));
				String response = getResponse(200, false);
				clientWriter.write(response);
				clientWriter.flush();
				String line;
				while ((line = cachedFileBufferedReader.readLine()) != null) {
					clientWriter.write(line);
				}
				clientWriter.flush();

				if (cachedFileBufferedReader != null) {
					cachedFileBufferedReader.close();
				}
			}
			if (clientWriter != null) {
				clientWriter.close();
			}
		} catch (IOException e) {
			System.out.println("Error sending cached file to client");
		}
	}

	// Set up a server connection, fetch the appropriate content. Return the content
	// to the client and also add it to the cache.
	private void fulfilNonCachedRequest(String requestUrl) {
		try {
			int fileExtensionIndex = requestUrl.lastIndexOf(".");
			String fileExtension;
			fileExtension = requestUrl.substring(fileExtensionIndex, requestUrl.length());
			String fileName = requestUrl.substring(0, fileExtensionIndex);
			fileName = fileName.substring(fileName.indexOf('.') + 1);
			fileName = fileName.replace("/", "__");
			fileName = fileName.replace('.', '_');

			if (fileExtension.contains("/")) {
				fileExtension = fileExtension.replace("/", "__");
				fileExtension = fileExtension.replace('.', '_');
				fileExtension += ".html";
			}
			fileName = fileName + fileExtension;
			boolean caching = true;
			File fileToCache = null;
			BufferedWriter cacheWriter = null;

			try {
				fileToCache = new File("cache/" + fileName);
				if (!fileToCache.exists()) {
					fileToCache.createNewFile();
				}
				cacheWriter = new BufferedWriter(new FileWriter(fileToCache));
			} catch (IOException e) {
				System.out.println("Error trying to cache " + fileName);
				caching = false;
			} catch (NullPointerException e) {
				System.out.println("Null pointer opening file " + fileName);
			}
			if (isImage(fileExtension)) {
				URL remoteURL = new URL(requestUrl);
				BufferedImage image = ImageIO.read(remoteURL);

				if (image != null) {
					ImageIO.write(image, fileExtension.substring(1), fileToCache);
					String line = getResponse(200, false);
					clientWriter.write(line);
					clientWriter.flush();
					ImageIO.write(image, fileExtension.substring(1), browserSocket.getOutputStream());

				} else {
					String error = getResponse(404, false);
					clientWriter.write(error);
					clientWriter.flush();
					return;
				}
			} else {
				URL remoteURL = new URL(requestUrl);
				HttpURLConnection serverConnection = (HttpURLConnection) remoteURL.openConnection();
				serverConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				serverConnection.setRequestProperty("Content-Language", "en-US");
				serverConnection.setUseCaches(false);
				serverConnection.setDoOutput(true);
				BufferedReader proxyToServerBR = new BufferedReader(
						new InputStreamReader(serverConnection.getInputStream()));
				String line = getResponse(200, false);
				clientWriter.write(line);
				while ((line = proxyToServerBR.readLine()) != null) {
					clientWriter.write(line);
					if (caching) {
						cacheWriter.write(line);
					}
				}
				clientWriter.flush();
				if (proxyToServerBR != null) {
					proxyToServerBR.close();
				}
			}

			if (caching) {
				cacheWriter.flush();
				ProxyMultiThread.addCachedPage(requestUrl, fileToCache);
			}
			if (cacheWriter != null) {
				cacheWriter.close();
			}
			if (clientWriter != null) {
				clientWriter.close();
			}
		} catch (Exception e) {
			System.out.println("Error sending non cached page to client");
		}
	}

	// Handle a HTTPS CONNECT request
	private void handleHTTPSRequest(String requestUrl) {
		String url = requestUrl.substring(7);
		String pieces[] = url.split(":");
		url = pieces[0];
		int serverPort = Integer.valueOf(pieces[1]);

		try {
			// Only the first line of the CONNECT request has been processed. Clear the
			// rest.
			for (int i = 0; i < 5; i++) {
				clientReader.readLine();
			}
			// Create a new connection to the server
			InetAddress serverAddress = InetAddress.getByName(url);
			Socket serverSocket = new Socket(serverAddress, serverPort);
			serverSocket.setSoTimeout(5000);

			String line = getResponse(200, true);
			clientWriter.write(line);
			clientWriter.flush();

			// Create the IO tools to read from and write to the server
			BufferedWriter serverWriter = new BufferedWriter(new OutputStreamWriter(serverSocket.getOutputStream()));
			BufferedReader serverReader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

			// Spin off a seperate thread to send from the client to the server. This thread
			// will handle communication from the server to the client.
			ClientToServerHttpsTransmitter clientToServerHttps = new ClientToServerHttpsTransmitter(
					browserSocket.getInputStream(), serverSocket.getOutputStream());
			clientToServerHttpsThread = new Thread(clientToServerHttps);
			clientToServerHttpsThread.start();

			// Handle communication from the server to the client
			try {
				byte[] buffer = new byte[4096];
				int read;
				do {
					read = serverSocket.getInputStream().read(buffer);
					if (read > 0) {
						browserSocket.getOutputStream().write(buffer, 0, read);
						if (serverSocket.getInputStream().available() < 1) {
							browserSocket.getOutputStream().flush();
						}
					}
				} while (read >= 0);
			} catch (SocketTimeoutException e) {
				System.out.println("Socket timeout during HTTPs connection");
			} catch (IOException e) {
				System.out.println("Error handling HTTPs connection");
			}

			// Close the resources
			closeResources(serverSocket, serverReader, serverWriter, clientWriter);

		} catch (SocketTimeoutException e) {
			String line = getResponse(504, false);
			try {
				clientWriter.write(line);
				clientWriter.flush();
			} catch (IOException x) {
			}
		} catch (Exception e) {
			System.out.println("Error on HTTPS " + requestUrl);
		}
	}

	// Respond to the browser with a 403 Error Code
	private void blockedSiteRequested() {
		try {
			BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(browserSocket.getOutputStream()));
			String line = getResponse(403, false);
			bufferedWriter.write(line);
			bufferedWriter.flush();
		} catch (IOException e) {
			System.out.println("Error writing to client when requested a blocked site");
		}
	}

	// Check whether the file extension is that of an image
	private boolean isImage(String fileExtension) {
		return fileExtension.contains(PNG) || fileExtension.contains(JPG) || fileExtension.contains(JPEG)
				|| fileExtension.contains(GIF);
	}

	// Return certain response strings based on the error code passed. There are two
	// cases for 200, 'OK' and 'Connection established', in which case check the
	// boolean.
	private String getResponse(int code, boolean connectionEstablished) {
		String response = "";
		switch (code) {
			case 200:
				if (connectionEstablished) {
					response = "HTTP/1.0 200 Connection established\r\nProxy-Agent: ProxyServer/1.0\r\n\r\n";
				} else {
					response = "HTTP/1.0 200 OK\nProxy-agent: ProxyServer/1.0\n\r\n";
				}
				break;
			case 403:
				response = "HTTP/1.0 403 Access Forbidden \nUser-Agent: ProxyServer/1.0\n\r\n";
				break;
			case 404:
				response = "HTTP/1.0 404 NOT FOUND \nProxy-agent: ProxyServer/1.0\n\r\n";
				break;
			case 504:
				response = "HTTP/1.0 504 Timeout Occured after 10s\nUser-Agent: ProxyServer/1.0\n\r\n";
				break;
			default:
				break;
		}
		return response;
	}

	// Close the passed resources
	private void closeResources(Socket serverSocket, BufferedReader serverReader, BufferedWriter serverWriter,
			BufferedWriter clientWriter) throws IOException {
		if (serverSocket != null) {
			serverSocket.close();
		}
		if (serverReader != null) {
			serverReader.close();
		}
		if (serverWriter != null) {
			serverWriter.close();
		}
		if (clientWriter != null) {
			clientWriter.close();
		}
	}

	// Seperate class to handle HTTPS transmission from the client to the server. In
	// practise it is spun off as a seperate thread to run alongside transmission
	// from the server to the client.
	class ClientToServerHttpsTransmitter implements Runnable {

		InputStream clientStream;
		OutputStream serverStream;

		public ClientToServerHttpsTransmitter(InputStream proxyToClientIS, OutputStream proxyToServerOS) {
			this.clientStream = proxyToClientIS;
			this.serverStream = proxyToServerOS;
		}

		@Override
		public void run() {
			try {
				byte[] buffer = new byte[4096];
				int read;
				do {
					read = clientStream.read(buffer);
					if (read > 0) {
						serverStream.write(buffer, 0, read);
						if (clientStream.available() < 1) {
							serverStream.flush();
						}
					}
				} while (read >= 0);
			} catch (SocketTimeoutException e) {
			} catch (IOException e) {
				System.out.println("Proxy to client HTTPS read timed out");
			}
		}
	}
}