
public class User {

	protected String username;
	protected String password;
	protected boolean admin;
	
	public User(String username, String password) {
		this.username = username;
		this.password = password;
		this.admin = false;
	}
	
	public boolean isAdmin() {
		return this.admin;
	}
	
	public void writePost() {
		
	}
}
