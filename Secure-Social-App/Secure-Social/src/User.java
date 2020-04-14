
public class User {

	protected String username;
	protected String password;
	protected String privateKey;
	protected String publicKey;
	protected boolean admin;
	
	public User(String username, String password) {
		this.username = username;
		this.password = password;
		this.admin = false;
	}
	
	private String encrypt(String message) {
		return null;
	}
	
	private String decrypt(String message) {
		return null;
	}
	
	
	public boolean isAdmin() {
		return this.admin;
	}
	
	public void writePost() {
		
	}
}
