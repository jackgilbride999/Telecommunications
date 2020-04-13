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
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;


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
		this.users = database.getCollection("Users");
		this.groups = database.getCollection("Groups");
		this.posts = database.getCollection("Posts");
	}
	
	public boolean validateLogin(String userName, String password) {
	    Document user = users.find(Filters.and(Filters.eq("username", userName), Filters.eq("password", password))).first();
	    if(user == null) {
	    	return false;
	    } else {
	    	return true;
	    }
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
	    Document group = groups.find(Filters.eq("groupname", groupName)).first();

	    if(group == null) {
	    	System.out.println("This group does not exist");
	    } else {
	    	System.out.println("Please enter the username of the user that you want to add to the group");
			String userName = inputScanner.nextLine();
		    Document user = users.find(Filters.eq("username", userName)).first();
		    if(user == null) {
		    	System.out.println("This user does not exist.");
		    } else {
		    	groups.updateOne(Filters.eq("groupname", groupName), Updates.addToSet("users", userName));
		    	System.out.println("User added successfully.");
		    }
	    }
	}
	
	private MongoCollection<Document> getUsers(){
		return this.users;
	}
	
	private MongoCollection<Document> getGroups(){
		return this.users;
	}
	
	private MongoCollection<Document> getPosts(){
		return this.posts;
	}

}
