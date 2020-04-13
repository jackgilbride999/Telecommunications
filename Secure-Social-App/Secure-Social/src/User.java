
public class User {

	protected String username;
	protected String password;
	protected boolean isAdmin;
	
	public User(String username, String password) {
		this.username = username;
		this.password = password;
		this.isAdmin = false;
	}
	
	public void writePost() {
		
	}
}
