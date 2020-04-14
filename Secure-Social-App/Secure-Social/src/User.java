import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class User {

	protected String username;
	protected String password;
	protected String privateKey;
	protected String publicKey;
	protected Map groupKeys;
	protected boolean admin;

	public User(String username, String password) {
		this.username = username;
		this.password = password;
		this.admin = false;
		this.groupKeys = new HashMap<String, String>();
	}

	/*
	 * Encrypt a message with AES using a secret key. Returns a string which is the
	 * initialization vector appended to the encoded text.
	 */
	protected String encryptAES(String message, SecretKey key) {
		try {
			// Convert the message to bytes for us to work with
			byte[] plainText = message.getBytes();
			// Initialize the cipher using the secret key
			SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, keySpec);
			// Encrypt the message
			byte[] encrypted = cipher.doFinal(plainText);
			// Append the initialisation vector to the start of the payload
			byte[] iv = cipher.getIV();
			byte[] payload = new byte[iv.length + encrypted.length];
			System.arraycopy(iv, 0, payload, 0, iv.length);
			System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
			// Return the payload
			String cipherText = new String(payload);
			return cipherText;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	protected String decryptAES(String message, SecretKey key) {
		try {
			// Convert the message to bytes for us to work with
			byte[] payload = message.getBytes();
			// Take off the initialization vector which was used to encrypt
	        byte[] iv = new byte[16];
			byte[] cipherText = new byte[payload.length - iv.length];
	        System.arraycopy(payload, 0, iv, 0, 16);
	        System.arraycopy(payload, iv.length, cipherText, 0, cipherText.length);
			// Decrypt the message now that we know the initialization vector
	        SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
			// Convert to the string form that we want and return
			String decryptedText = new String(cipher.doFinal(cipherText));
			return decryptedText;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public boolean isAdmin() {
		return this.admin;
	}

	public void writePost() {

	}

	public String getUsername() {
		return this.username;
	}
}
