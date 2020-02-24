import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.net.*;

public class ThreadProxy implements Runnable {

	/*
	 * Variables for this thread. browserSocket: the socket connected to the client
	 * passed by the Proxy Server clientReader: a buffered reader to read from the
	 * client clientWriter: a buffered writer to write to the client
	 * clientToServerHttpsTransmitterThread: a seperate thread to allow transmission
	 * from client to server while this thread is transmitting from server to client
	 */
	private Socket browserSocket;
	private BufferedReader clientReader;
	private BufferedWriter clientWriter;
	private Thread clientToServerHttpsTransmitterThread;

	/*
	 * Constants for this class. To identify connection types and file types.
	 */
	private static final String CONNECT = "CONNECT";
	private static final String GET = "GET";
	private static final String JPG = ".jpg";
	private static final String JPEG = ".jpeg";
	private static final String PNG = ".png";
	private static final String GIF = ".gif";

	public ThreadProxy(Socket browserSocket) {
		this.browserSocket = browserSocket;
		try {
			this.browserSocket.setSoTimeout(2000);
			clientReader = new BufferedReader(new InputStreamReader(browserSocket.getInputStream()));
			clientWriter = new BufferedWriter(new OutputStreamWriter(browserSocket.getOutputStream()));
		} catch (IOException e) {
			System.out.println("Error initializing new thread.");
		}
	}

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
					sendNonCachedToClient(requestUrl);
				} else {
					System.out.println("HTTP request for : " + requestUrl + ". Cached page found.");
					sendCachedPageToClient(file);
				}
				break;
		}
	}

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

	private void sendCachedPageToClient(File cachedFile) {
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

	private void sendNonCachedToClient(String requestUrl) {
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
			BufferedWriter fileToCacheBW = null;

			try {
				fileToCache = new File("cached/" + fileName);
				if (!fileToCache.exists()) {
					fileToCache.createNewFile();
				}
				fileToCacheBW = new BufferedWriter(new FileWriter(fileToCache));
			} catch (IOException e) {
				System.out.println("Error trying to cache " + fileName);
				caching = false;
			} catch (NullPointerException e) {
				System.out.println("Null pointer opening file " + fileName);
			}
			if ((fileExtension.contains(PNG)) || fileExtension.contains(JPG) || fileExtension.contains(JPEG)
					|| fileExtension.contains(GIF)) {
				URL remoteURL = new URL(requestUrl);
				BufferedImage image = ImageIO.read(remoteURL);

				if (image != null) {
					ImageIO.write(image, fileExtension.substring(1), fileToCache);
					String line = getResponse(200, false);
					clientWriter.write(line);
					clientWriter.flush();

					ImageIO.write(image, fileExtension.substring(1), browserSocket.getOutputStream());

				} else {
					System.out.println("Sending 404 to client as image wasn't received from server" + fileName);
					String error = getResponse(404, false);
					clientWriter.write(error);
					clientWriter.flush();
					return;
				}
			} else {
				URL remoteURL = new URL(requestUrl);
				HttpURLConnection proxyToServerCon = (HttpURLConnection) remoteURL.openConnection();
				proxyToServerCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				proxyToServerCon.setRequestProperty("Content-Language", "en-US");
				proxyToServerCon.setUseCaches(false);
				proxyToServerCon.setDoOutput(true);
				BufferedReader proxyToServerBR = new BufferedReader(
						new InputStreamReader(proxyToServerCon.getInputStream()));
				String line = getResponse(404, false);
				clientWriter.write(line);
				while ((line = proxyToServerBR.readLine()) != null) {
					clientWriter.write(line);
					if (caching) {
						fileToCacheBW.write(line);
					}
				}
				clientWriter.flush();
				if (proxyToServerBR != null) {
					proxyToServerBR.close();
				}
			}

			if (caching) {
				fileToCacheBW.flush();
				ProxyMultiThread.addCachedPage(requestUrl, fileToCache);
			}

			if (fileToCacheBW != null) {
				fileToCacheBW.close();
			}

			if (clientWriter != null) {
				clientWriter.close();
			}
		}
		catch (Exception e) {
			System.out.println("Error sending non cached page to client");
		}
	}

	private void handleHTTPSRequest(String requestUrl) {
		String url = requestUrl.substring(7);
		String pieces[] = url.split(":");
		url = pieces[0];
		int port = Integer.valueOf(pieces[1]);

		try {
			for (int i = 0; i < 5; i++) {
				clientReader.readLine();
			}

			InetAddress address = InetAddress.getByName(url);

			Socket proxyToServerSocket = new Socket(address, port);
			proxyToServerSocket.setSoTimeout(5000);

			String line = getResponse(200, true);
			clientWriter.write(line);
			clientWriter.flush();

			BufferedWriter proxyToServerBW = new BufferedWriter(
					new OutputStreamWriter(proxyToServerSocket.getOutputStream()));
			BufferedReader proxyToServerBR = new BufferedReader(
					new InputStreamReader(proxyToServerSocket.getInputStream()));
			ClientToServerHttpsTransmitter clientToServerHttps = new ClientToServerHttpsTransmitter(
					browserSocket.getInputStream(), proxyToServerSocket.getOutputStream());

			clientToServerHttpsTransmitterThread = new Thread(clientToServerHttps);
			clientToServerHttpsTransmitterThread.start();
			try {
				byte[] buffer = new byte[4096];
				int read;
				do {
					read = proxyToServerSocket.getInputStream().read(buffer);
					if (read > 0) {
						browserSocket.getOutputStream().write(buffer, 0, read);
						if (proxyToServerSocket.getInputStream().available() < 1) {
							browserSocket.getOutputStream().flush();
						}
					}
				} while (read >= 0);
			} catch (SocketTimeoutException e) {
				System.out.println("Socket timeout during HTTPs connection");
			} catch (IOException e) {
				System.out.println("Error handling HTTPs connection");
			}

			if (proxyToServerSocket != null) {
				proxyToServerSocket.close();
			}

			if (proxyToServerBR != null) {
				proxyToServerBR.close();
			}

			if (proxyToServerBW != null) {
				proxyToServerBW.close();
			}

			if (clientWriter != null) {
				clientWriter.close();
			}

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

	private boolean isImage(String fileExtension) {
		return fileExtension.contains(PNG) || fileExtension.contains(JPG) || fileExtension.contains(JPEG)
				|| fileExtension.contains(GIF);
	}

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

	class ClientToServerHttpsTransmitter implements Runnable {

		InputStream proxyToClientIS;
		OutputStream proxyToServerOS;

		public ClientToServerHttpsTransmitter(InputStream proxyToClientIS, OutputStream proxyToServerOS) {
			this.proxyToClientIS = proxyToClientIS;
			this.proxyToServerOS = proxyToServerOS;
		}

		@Override
		public void run() {
			try {
				byte[] buffer = new byte[4096];
				int read;
				do {
					read = proxyToClientIS.read(buffer);
					if (read > 0) {
						proxyToServerOS.write(buffer, 0, read);
						if (proxyToClientIS.available() < 1) {
							proxyToServerOS.flush();
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