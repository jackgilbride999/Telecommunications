import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
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

	/*
	 * Take a message and a public key in textual form and encode 
	 * the message with the key using RSA.
	 */
	protected String encryptRSA(String message, String publicKey) {
		try {
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKey.getBytes()));
			PublicKey keyObject = KeyFactory.getInstance("RSA").generatePublic(keySpec);
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, keyObject);
			String encrypted = new String(cipher.doFinal(message.getBytes()));
			return encrypted;
		} catch (Exception e) {
			return null;
		}
	}
}
