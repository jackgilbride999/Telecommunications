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
 * 	Class used to interface with the Mongo database.
 */

public class DBClient {
	private MongoClient mongoClient;
	private MongoDatabase database;
	private MongoCollection<Document> users;
	private MongoCollection<Document> groups;
	private MongoCollection<Document> posts;

	
	DBClient(String name, String password){
		this.mongoClient = MongoClients.create("mongodb+srv://" + name + ":" + password
				+ "@secure-social-0n8uh.azure.mongodb.net/test?retryWrites=true&w=majority");
		this.database = mongoClient.getDatabase("Secure-Social");
		this.users = database.getCollection("users");
		this.groups = database.getCollection("groups");
		this.posts = database.getCollection("posts");
	}
	
	public void createUser(Scanner inputScanner) {
		System.out.println("Please enter the username of the user to create: ");
		String userName = inputScanner.nextLine();
		System.out.println("Please enter the password of the user to create: ");
		String userPassword = inputScanner.nextLine();
		Document document = new Document("username", userName).append("password", userPassword);
		users.insertOne(document);
		System.out.println("User created successfully.");
	}

	public void createGroup(Scanner inputScanner) {
		System.out.println("Please enter the name of the group to create: ");
		String groupName = inputScanner.nextLine();
		Document document = new Document("groupname", groupName).append("users", Arrays.asList());
		groups.insertOne(document);
		System.out.println("Group created successfully");
	}

	public void addToGroup(Scanner inputScanner) {
		System.out.println("Please enter the name of the group to add a member to: ");
		String groupName = inputScanner.nextLine();
	    BasicDBObject whereQuery = new BasicDBObject();
	    whereQuery.put("groupname", groupName);
	    Document group = groups.find(whereQuery).first();
	    if(group == null) {
	    	System.out.println("This group does not exist");
	    } else {
	    	System.out.println("Please enter the username of the user that you want to add to the group");
			String userName = inputScanner.nextLine();
			
	    }
	}
	
	public MongoCollection<Document> getUsers(){
		return this.users;
	}
	
	public MongoCollection<Document> getGroups(){
		return this.users;
	}
	
	public MongoCollection<Document> getPosts(){
		return this.posts;
	}

}
