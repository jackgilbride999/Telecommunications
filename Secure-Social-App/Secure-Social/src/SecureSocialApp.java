import java.lang.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.MongoClientSettings;

/*
 * 	The aim of this project is to develop a secure social media application for Facebook,
 *  Twitter, WhatsApp etc., or for your own social networking app. For example, your 
 *  application will secure the Facebook Wall, such that only people that are part of 
 *  your “Secure Facebook Group” will be able to decrypt each other’s posts. To all other
 *  users of the system the post will appear as ciphertext. You are required to design 
 *  and implement a suitable key management system for your application that allows any 
 *  member of the group to share social media messages securely, and allows you to add 
 *  or remove people from a group. You are free to implement your application for a 
 *  desktop or mobile platform and make use of any open source cryptographic libraries.
 */

public class SecureSocialApp {
	public static void main(String[] args) {
		Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
		mongoLogger.setLevel(Level.SEVERE); // e.g. or Log.WARNING, etc.
		System.out.println("Welcome to Secure Social.");
		Scanner inputScanner = new Scanner(System.in);
		String[] passwords = getPasswords();
		String readOnlyPassword = passwords[0];
		String readWritePassword = passwords[1];
		DBClient mongo = new DBClient("readwrite", readWritePassword);
	}


	/*
	 * Return the passwords to the data base in a String array. index 0 contains the
	 * read-only password index 1 contains the read-write password
	 */
	private static String[] getPasswords() {
		try {
			String[] passwords = new String[2];

			FileReader fileReader = new FileReader("readonlypassword.txt");
			Scanner fileScanner = new Scanner(fileReader);
			passwords[0] = fileScanner.nextLine();
			fileReader.close();
			fileScanner.close();

			fileReader = new FileReader("readwritepassword.txt");
			fileScanner = new Scanner(fileReader);
			passwords[1] = fileScanner.nextLine();
			fileReader.close();
			fileScanner.close();
			return passwords;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}