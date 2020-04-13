import java.lang.*;
import java.io.*;
import java.util.*;

public class SecureSocialApp {
    public static void main(String[] args) {
        try {
            FileReader fileReader = new FileReader("readonlypassword.txt");
            Scanner fileScanner = new Scanner(fileReader);
            String readOnlyPassword = fileScanner.nextLine();
            System.out.println(readOnlyPassword);
            fileScanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}