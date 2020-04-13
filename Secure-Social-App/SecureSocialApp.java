import java.lang.*;
import java.io.*;
import java.util.*;

public class SecureSocialApp {
    public static void main(String[] args) {
        String[] passwords = getPasswords();
        String readOnlyPassword = passwords[0];
        String readWritePassword = passwords[1];
    }

    /*
        Return the passwords to the data base in a String array.
        index 0 contains the read-only password
        index 1 contains the read-write password
    */
    private static String[] getPasswords(){
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