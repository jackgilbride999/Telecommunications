import java.lang.*;
import java.io.*;
import java.util.*;

import org.bson.Document;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.MongoClientSettings;


public class SecureSocialApp {
    public static void main(String[] args) {
        String[] passwords = getPasswords();
        String readOnlyPassword = passwords[0];
        String readWritePassword = passwords[1];

        MongoClient mongoClient = MongoClients.create(
                "mongodb+srv://readwrite:" + readWritePassword +"@secure-social-0n8uh.azure.mongodb.net/test?retryWrites=true&w=majority");
        MongoDatabase database = mongoClient.getDatabase("Secure-Social");
        System.out.println(database.getName());
        for(Document doc : database.listCollections()) {
        	System.out.println(doc.toJson());
        }


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