import java.io.*;
import java.net.*;
import java.util.ArrayList;

class ThreadProxy extends Thread {
    private Socket sClient;
    private final String SERVER_URL;
    private final int SERVER_PORT;

    ThreadProxy(Socket sClient, String ServerUrl, int ServerPort) {
        this.sClient = sClient;         // the socket connecting this thread to its endpoint e.g. port 9999
        this.SERVER_URL = ServerUrl;    // e.g. 192.168.1.10 
        this.SERVER_PORT = ServerPort;  // e.g. 8080
        System.out.println("Created a new ThreadProxy");
        this.start();
        System.out.println("Started ThreadProxy");
    }

    @Override
    public void run() {
        try {
            final byte[] request = new byte[1024];
            byte[] reply = new byte[4096];
            final InputStream inFromClient = sClient.getInputStream();
            final OutputStream outToClient = sClient.getOutputStream();

            // Collect the browser's request into an ArrayList
            ArrayList<String> requestLines = new ArrayList<String>();
            InputStreamReader inFromClientReader = new InputStreamReader(inFromClient);
            BufferedReader in =  new BufferedReader(inFromClientReader); 
            String requestLine = "";
            while (!(requestLine = in.readLine()).equals("")) {
                requestLines.add(requestLine);
            }
            String firstLine;
            if((firstLine = requestLines.get(0)).startsWith("CONNECT")){
                int spaceIndex = firstLine.indexOf(" ");
                int colonIndex = firstLine.indexOf(":");
                int secondSpaceIndex = firstLine.indexOf(" ", colonIndex);
                String IP = firstLine.substring(spaceIndex+1, colonIndex);
                String port = firstLine.substring(colonIndex + 1, secondSpaceIndex);
                System.out.println("You want to connect to " + IP + " on port " + port + ".");
            }



            Socket client = null, server = null;
            // connects a socket to the server
            try {
                server = new Socket(SERVER_URL, SERVER_PORT);
                System.out.println("Created new socket");
            } catch (IOException e) {
                PrintWriter out = new PrintWriter(new OutputStreamWriter(
                        outToClient));
                out.flush();
                throw new RuntimeException(e);
            }
            // a new thread to manage streams from server to client (DOWNLOAD)
            final InputStream inFromServer = server.getInputStream();
            final OutputStream outToServer = server.getOutputStream();
            // a new thread for uploading to the server
            new Thread() {
                public void run() {
                    int bytes_read;
                    try {
                        while ((bytes_read = inFromClient.read(request)) != -1) {
                            outToServer.write(request, 0, bytes_read);
                            outToServer.flush();
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
                    //TODO CREATE YOUR LOGIC HERE
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (server != null)
                        server.close();
                    if (client != null)
                        client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            outToClient.close();
            sClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}