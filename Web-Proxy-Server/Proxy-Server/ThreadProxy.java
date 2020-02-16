import java.io.*;
import java.net.*;
import java.util.ArrayList;

class ThreadProxy extends Thread {
    
    // Socket connected to client passed by Proxy server
    private Socket clientSocket;

    private BufferedReader clientReader;

    private BufferedWriter clientWriter;


    ThreadProxy(Socket clientSocket) {
        try{
            this.clientSocket = clientSocket;
            clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            clientWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            this.start();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            final byte[] request = new byte[1024];
            byte[] reply = new byte[4096];
            final InputStream inFromClient = clientSocket.getInputStream();
            final OutputStream outToClient = clientSocket.getOutputStream();


            /*
                - Get request from client
                - Parse out the request type 
                - Parse out URL: the data between the first and second spaces
            */
            String requestLine = clientReader.readLine();
            String requestType = requestLine.substring(0, requestLine.indexOf(' '));
            String requestUrl = requestLine.substring(requestLine.indexOf(' ')+1);
            requestUrl = requestUrl.substring(0, requestUrl.indexOf(' '));
            if(requestType.equals("CONNECT")){
                System.out.println("CONNECT request for " + requestUrl);
                handleHTTPSRequest(requestUrl);
            }



            Socket client = null, webServer = null;
            // connects a socket to the server
           /* try {
                webServer = new Socket(url, port);
                System.out.println("Created new socket " + url + ":" + port);
            } catch (IOException e) {
                PrintWriter out = new PrintWriter(new OutputStreamWriter(
                        outToClient));
                out.flush();
                throw new RuntimeException(e);
            }
*/
            final InputStream inFromServer = webServer.getInputStream();
            final OutputStream outToServer = webServer.getOutputStream();
            // a new thread for uploading to the server
            new Thread() {
                public void run() {
                    int bytes_read;
                    try {
                        while ((bytes_read = inFromClient.read(request)) != -1) {
                            outToServer.write(request, 0, bytes_read);
                            outToServer.flush();
                            System.out.println("Forwarded to server " + request);
                            //TODO CREATE YOUR LOGIC HERE
                        }
                    } catch (IOException e) {
                    }
                    try {
                        outToServer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
            // current thread manages streams from server to client (DOWNLOAD)
            int bytes_read;
            try {
                while ((bytes_read = inFromServer.read(reply)) != -1) {
                    outToClient.write(reply, 0, bytes_read);
                    outToClient.flush();
                    System.out.println("Forwarded to client " + reply);
                    //TODO CREATE YOUR LOGIC HERE
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (webServer != null)
                        webServer.close();
                    if (client != null)
                        client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            outToClient.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleHTTPSRequest(String requestURL){
        String splitUrl = requestUrl.split(":");
        requestURL = split[0];
        int requestPort = Integer.parseInt(split[1]);

        try{
            // We have read the first line of the request from the reader.
            // Throw away the rest from the buffer
			for(int i=0;i<5;i++){
				clientReader.readLine();
            }
            
            /*
                - Get the IP of the server
                - Open a socket to the server
                - Let the client know that connection was established
            */
            InetAddress serverAddress = InetAddress.getByName(url);
            Socket serverSocket = new Socket(serverAddress, requestURL);
			String establishedMessage = "HTTP/1.0 200 Connection established\r\n" +
					"Proxy-Agent: ProxyServer/1.0\r\n" +
                    "\r\n";
            clientWriter.write(establishedMessage);
            clientWriter.flush();


            BufferedWriter severWriter = new BufferedWriter(new OutputStreamWriter(serverSocket.getOutputStream()));
            BufferedReader serverReader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));

        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}