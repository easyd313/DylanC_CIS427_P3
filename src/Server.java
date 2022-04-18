//Author: Dylan Crompton
//Date Modified: 4/17/2022
//Date Created: 4/12/2022
//Purpose: Geometry Solver Client

import java.io.*;
import java.util.*;
import java.net.*;

// Server class
public class Server {

    // Vector to store active clients
    static Vector<ClientHandler> ar = new Vector<>();

    
    public static class LoginInfo {
        String username = "";
        String password = "";
    }
    
    public static LoginInfo[] loginList = new LoginInfo[4];
    
    

    public static void main(String[] args) throws IOException {
        // server is listening on port 8728
        ServerSocket ss = new ServerSocket(8728);
        Socket s;
        
        RetrieveLogins();

        // running infinite loop for getting
        // client request
        while (true) {
            // Accept the incoming request
            s = ss.accept();

            System.out.println("New client request received : " + s);

            // obtain input and output streams
            DataInputStream dis = new DataInputStream(s.getInputStream());
            DataOutputStream dos = new DataOutputStream(s.getOutputStream());

            System.out.println("Creating a new handler for this client...");

            // Create a new handler object for handling this request.
            ClientHandler mtch = new ClientHandler(s, dis, dos, loginList, ss);

            // Create a new Thread with this object.
            Thread t = new Thread(mtch);

            System.out.println("Adding this client to active client list");

            // add this client to active clients list
            ar.add(mtch);

            // start the thread.
            t.start();

        }
    }
    
    public static void RetrieveLogins() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("C:/Users/easyd/OneDrive/Documents/SCHOOL/Winter 2022/CIS 427/Project1/Dylan_Crompton_p1/logins.txt"));
            String line = reader.readLine();
            
            int i = 0;
            LoginInfo currLogin = new LoginInfo();
            while (line != null) {                      //populate login list with logins from logins.txt
                String[] strSplit = line.split("\\s+");
                currLogin.username = strSplit[0];
                currLogin.password = strSplit[1];
                loginList[i] = currLogin;
                currLogin = new LoginInfo();
                line = reader.readLine();
                i++; 
            }
            reader.close();
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }//end try-catch
    }
    

    
    
}
