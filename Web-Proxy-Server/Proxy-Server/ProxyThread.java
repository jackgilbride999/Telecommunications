//import java.net.Socket;
import java.net.http.WebSocket;

public class ProxyThread extends Thread {

	private WebSocket socket;
	private final String SERVER_URL;
	private final int SERVER_PORT;
	
	ProxyThread(WebSocket socket, String serverUrl, int serverPort){
		this.SERVER_URL = serverUrl;
		this.SERVER_PORT = serverPort;
		this.socket = socket;
		this.start();
	}
	
	@Override
	public void run() {
		
	}
}
