import java.io.*;
import java.net.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.ArrayList;

class ThreadProxy implements Runnable {

    // Socket connected to client passed by Proxy server
    private Socket clientSocket;
    private BufferedReader clientReader;
    private BufferedWriter clientWriter;
    private Thread clientToServerHttpsTransmitterThread;

    ThreadProxy(Socket clientSocket) {
        try {
            this.clientSocket = clientSocket;
            this.clientSocket.setSoTimeout(2000);
            clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            clientWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            /*
             * - Get request from client - Parse out the request type - Parse out URL: the
             * data between the first and second spaces
             */
            String requestLine = clientReader.readLine();
            String requestType = requestLine.substring(0, requestLine.indexOf(' '));
            String requestUrl = requestLine.substring(requestLine.indexOf(' ') + 1);
            requestUrl = requestUrl.substring(0, requestUrl.indexOf(' '));
            if(!requestUrl.startsWith("http")){
                requestUrl = "http://" + requestUrl;
            }

            if (requestType.equals("CONNECT")) {
                System.out.println("CONNECT request for " + requestUrl);
                handleHTTPSRequest(requestUrl);
            } else if (requestType.equals("GET")) {
                handleGetRequest(requestUrl);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleGetRequest(String requestUrl) {
        try {
            String fileName = requestUrl.substring(requestUrl.indexOf('.') + 1, requestUrl.lastIndexOf("."));
            String extension = requestUrl.substring(requestUrl.lastIndexOf("."), requestUrl.length());

            fileName = fileName.replace("/", "__");
            fileName = fileName.replace('.', '_');

            if (extension.contains("/")) {
                extension = extension.replace("/", "__");
                extension = extension.replace('.', '_');
                extension += ".html";
            }

            fileName = fileName + extension;

            boolean caching = true;
            File file = null;
            BufferedWriter fileBW = null;

            try {
                file = new File("cache/" + fileName);

                if (!file.exists()) {
                    file.createNewFile();
                }

                fileBW = new BufferedWriter(new FileWriter(file));
            } catch (IOException e) {
                e.printStackTrace();
                caching = false;
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

            if (extension.toLowerCase().contains(".jpg") || extension.toLowerCase().contains(".jpeg")
                    || extension.toLowerCase().contains(".png") || extension.toLowerCase().contains(".gif")) {
                BufferedImage image = ImageIO.read(new URL(requestUrl));
                if (image != null) {
                    ImageIO.write(image, extension.substring(1), file);
                    String response = "HTTP/1.0 200 OK\nProxy-agent: ProxyServer/1.0\n\r\n";
                    clientWriter.write(response);
                    clientWriter.flush();
                    ImageIO.write(image, extension.substring(1), clientSocket.getOutputStream());
                } else {
                    String error = "HTTP/1.0 404 NOT FOUND\nProxy-agent: ProxyServer/1.0\n\r\n";
                    clientWriter.write(error);
                    clientWriter.flush();
                    return;
                }
            }

            else {
                HttpURLConnection serverConnection = (HttpURLConnection) (new URL(requestUrl).openConnection());
                serverConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                serverConnection.setRequestProperty("Content-Language", "en-US");
                serverConnection.setUseCaches(false);
                serverConnection.setDoOutput(true);

                BufferedReader serverBR = new BufferedReader(new InputStreamReader(serverConnection.getInputStream()));

                String response = "HTTP/1.0 200 OK\nProxy-agent: ProxyServer/1.0\n\r\n";
                clientWriter.write(response);

                while ((response = serverBR.readLine()) != null) {
                    clientWriter.write(response);
                    if (caching) {
                        fileBW.write(response);
                    }
                }
                fileBW.flush();

                if (serverBR != null) {
                    serverBR.close();
                }

            }

            if (fileBW != null) {
                fileBW.close();
            }
            if (clientWriter != null) {
                clientWriter.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void handleHTTPSRequest(String requestUrl) {
        String splitUrl[] = requestUrl.split(":");
        requestUrl = splitUrl[0];
        int requestPort = Integer.parseInt(splitUrl[1]);

        try {
            // We have read the first line of the request from the reader.
            // Throw away the rest from the buffer
            for (int i = 0; i < 5; i++) {
                clientReader.readLine();
            }

            /*
             * - Get the IP of the server - Open a socket to the server - Let the client
             * know that connection was established
             */
            InetAddress serverAddress = InetAddress.getByName(requestUrl);
            Socket serverSocket = new Socket(serverAddress, requestPort);
            String establishedMessage = "HTTP/1.0 200 Connection established\r\n" + "Proxy-Agent: ProxyServer/1.0\r\n"
                    + "\r\n";
            clientWriter.write(establishedMessage);
            clientWriter.flush();

            BufferedWriter serverWriter = new BufferedWriter(new OutputStreamWriter(serverSocket.getOutputStream()));
            BufferedReader serverReader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

            // Handle the transmission from Client to Server in a seperate thread
            ClientToServerHttpsTransmitter clientToServerHttpsTransmitter = new ClientToServerHttpsTransmitter(
                    clientSocket, serverSocket);
            clientToServerHttpsTransmitterThread = new Thread(clientToServerHttpsTransmitter);
            clientToServerHttpsTransmitterThread.start();

            // Handle the transmission from Server to Client in this thread
            /*
             * - Create a data buffer - While there is data being sent by the server, add to
             * buffer - When data is not available from the server, use the time to flush to
             * the client
             */
            try {
                byte[] dataBuffer = new byte[4096];
                int read;
                read = serverSocket.getInputStream().read(dataBuffer);
                while (read >= 0) {
                    if (read > 0) {
                        clientSocket.getOutputStream().write(dataBuffer, 0, read);
                        if (serverSocket.getInputStream().available() < 1) {
                            clientSocket.getOutputStream().flush();
                        }
                    }
                    read = serverSocket.getInputStream().read(dataBuffer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            closeResources(serverSocket, serverReader, serverWriter, clientWriter);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void closeResources(Socket serverSocket, BufferedReader serverReader, BufferedWriter serverWriter,
            BufferedWriter clientWriter) {
        try {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ClientToServerHttpsTransmitter implements Runnable {

        InputStream clientStream;
        OutputStream serverStream;

        public ClientToServerHttpsTransmitter(Socket clientSocket, Socket serverSocket) {
            try {
                this.clientStream = clientSocket.getInputStream();
                this.serverStream = clientSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /*
         * - Create a data buffer - While there is data being sent by the client, add to
         * buffer - When data is not available from the client, use the time to flush to
         * the server
         */
        @Override
        public void run() {
            try {
                byte[] dataBuffer = new byte[4096];
                int read = clientStream.read(dataBuffer);
                while (read >= 0) {
                    if (read > 0) {
                        serverStream.write(dataBuffer, 0, read);
                        if (clientStream.available() < 1) {
                            serverStream.flush();
                        }
                    }
                    read = clientStream.read(dataBuffer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}