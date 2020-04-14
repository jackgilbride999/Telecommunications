import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class Group {

	protected String groupname;
	SecretKey key;

	public Group(String groupname) {
		try {
			// Initialise the AES-256 Key Generator
			KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
	        keyGenerator.init(256);
	        // Generate the Key
	        SecretKey key = keyGenerator.generateKey();
	        this.key = key;
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
