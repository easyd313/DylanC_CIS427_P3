//Author: Dylan Crompton
//Date Modified: 4/17/2022
//Date Created: 4/12/2022
//Purpose: Geometry Solver Client
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.Scanner;

// ClientHandler class
class ClientHandler implements Runnable {

    Scanner scn = new Scanner(System.in);
    final DataInputStream dis;
    final DataOutputStream dos;
    Socket s;
    ServerSocket ss;
    String loginState;
    Server.LoginInfo[] loginList = new Server.LoginInfo[4];
    boolean serverUp;

    // constructor
    public ClientHandler(Socket s, DataInputStream dis, DataOutputStream dos, Server.LoginInfo[] loginList, ServerSocket ss) {
        this.dis = dis;
        this.dos = dos;
        this.s = s;
        this.loginState = "";
        this.loginList = loginList;
        this.serverUp = true;
        this.ss = ss;
    }

    @Override
    public void run() {

        try {
            while (serverUp) {
                String strReceived = dis.readUTF();
                System.out.println("CLIENT COMMAND RECIEVED: " + strReceived);
                String[] strSplit = strReceived.split("\\s+");

                if (!loginState.isEmpty()) {
                    switch (strSplit[0]) {
                        case "SOLVE":
                            SolveGeo(strSplit, dos);
                            break;
                        case "LIST":
                            if (strSplit.length > 1) {
                                ListAllSolutions(strSplit, dos);
                            } else {
                                dos.writeUTF(ListSolutions(loginState));
                            }
                            break;
                        case "SHUTDOWN":
                            if (strSplit.length > 1) {
                                System.out.println("Unknown command received: " + strReceived);
                                dos.writeUTF("301 message format error");
                                break;
                            } else {
                                System.out.println("Shutting down server...");
                                dos.writeUTF("200 OK");
                                for (ClientHandler mc : Server.ar) {
                                    mc.dos.writeUTF("200 OK ");
                                }
                                ss.close();
                                serverUp = false;
                            }
                            break;
                        case "LOGOUT":
                            if (strSplit.length > 1) {
                                System.out.println("Unknown command received: " + strReceived);
                                dos.writeUTF("301 message format error");
                                break;
                            } else {
                                System.out.println("LOGGING OUT USER: " + loginState);
                                dos.writeUTF("200 OK");
                                loginState = "";
                            }
                            break;
                        case "MESSAGE":
                            if (strSplit.length < 3) {
                                System.out.println("Unknown command received: " + strReceived);
                                dos.writeUTF("301 message format error");
                                break;
                            } else {
                                System.out.println("S: " + "Message from client:");
                                String message = "";
                                for (int i = 2; i < strSplit.length; ++i) {
                                    message += " " + strSplit[i];
                                }
                                System.out.println(message);
                                if (loginState.equals("root") && strSplit[1].equals("-all")) {
                                    for (ClientHandler mc : Server.ar) {
                                        if (!mc.loginState.equals("root")) { // writes to all logged in user's output stream except root
                                            System.out.println("Sending to " + mc.loginState);
                                            mc.dos.writeUTF("Message from " + this.loginState + ":\n" + message);
                                        }
                                    }
                                    dos.writeUTF("Message sent.");
                                } else {

                                    System.out.println("Sending to " + strSplit[1]);
                                    boolean userExists = false;

                                    for (int i = 0; i < 4; ++i) {
                                        if (loginList[i].username.equals(strSplit[1])) { // checks is recipient exists
                                            userExists = true;
                                        }
                                    }

                                    if (!userExists) {
                                        System.out.println("User " + strSplit[1] + " doesn't exist");
                                        System.out.println("Informing client.");
                                        dos.writeUTF("User " + strSplit[1] + " does not exist");
                                    } else {
                                        boolean messageSent = false;
                                        for (ClientHandler mc : Server.ar) {
                                            // if the recipient is found, write on its
                                            // output stream
                                            if (mc.loginState.equals(strSplit[1])) {
                                                mc.dos.writeUTF("Message from " + this.loginState + ":\n" + message);
                                                dos.writeUTF("Message sent.");
                                                messageSent = true;
                                                break;
                                            }
                                        }
                                        if (messageSent == false) {
                                            System.out.println("User " + strSplit[1] + " is not logged in.");
                                            System.out.println("Informing client.");
                                            dos.writeUTF("User " + strSplit[1] + " is not logged in");
                                        }
                                    }
                                }

                            }
                            break;
                        default:
                            System.out.println("Unknown command received: " + strReceived);
                            dos.writeUTF("300 invalid command");
                            break;
                    }

                } else {
                    if (strSplit[0].equals("LOGIN")) {
                        if (strSplit.length == 3) {                 //check the client input format
                            LoginUser(strSplit[1], strSplit[2], dos);
                        } else {
                            System.out.println("Unknown command received: " + strReceived);
                            dos.writeUTF("FAILURE: Please provide correct username and password. Try again.");
                        }
                    } else {
                        System.out.println("Unknown command received: " + strReceived);
                        dos.writeUTF("300 invalid command");
                    }
                }
            }//end server loop

            // search for the recipient in the connected devices list.
            // ar is the vector storing client of active users
        } catch (IOException e) {

            e.printStackTrace();
        }

        try {
            // closing resources
            this.dis.close();
            this.dos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void LoginUser(String user, String pass, DataOutputStream outputToClient) {
        try {
            for (int i = 0; i < loginList.length; ++i) {     // search for user info in login list
                if (loginList[i].username.equals(user)) {
                    if (loginList[i].password.equals(pass)) {
                        loginState = user;
                        System.out.println("SUCCESS");
                        outputToClient.writeUTF("SUCCESS");
                    }
                }
            }

            if (loginState.equals("")) {   //checks if the login attempt was successful
                System.out.println("FAILURE: Unidentified login credentials.");
                outputToClient.writeUTF("FAILURE: Please provide correct username and password. Try again.");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }//end try-catch

    }

    public void SolveGeo(String[] commands, DataOutputStream outputToClient) {
        DecimalFormat df = new DecimalFormat("#.##");
        double area;
        double perimeter;
        double mes1;
        double mes2;

        try {
            FileWriter myWriter = new FileWriter(loginState + "_solutions.txt", true);

            if (commands.length == 1) {  //
                System.out.println("Error:  301 message format error");
                outputToClient.writeUTF("Error:  301 message format error");
                myWriter.write("Error:  301 message format error\n");
            } else if (commands.length == 2) {  //no sides or radius found
                if (commands[1].equals("-c")) {
                    System.out.println("Error:  No radius found");
                    outputToClient.writeUTF("Error:  No radius found");
                    myWriter.write("Error:  No radius found\n");
                } else if (commands[1].equals("-r")) {
                    System.out.println("Error:  No sides found");
                    outputToClient.writeUTF("Error:  No sides found");
                    myWriter.write("Error:  No sides found\n");
                } else {
                    System.out.println("Error:  301 message format error");
                    outputToClient.writeUTF("Error:  301 message format error");
                    myWriter.write("Error:  301 message format error\n");
                }
            } else if (commands.length > 4) {  //input was incorrect
                if (commands[1].equals("-c")) {
                    System.out.println("Error:  301 message format error");
                    outputToClient.writeUTF("Error:  301 message format error");
                    myWriter.write("Error:  301 message format error\n");
                } else if (commands[1].equals("-r")) {
                    System.out.println("Error:  301 message format error");
                    outputToClient.writeUTF("Error:  301 message format error");
                    myWriter.write("Error:  301 message format error\n");
                } else {
                    System.out.println("Error:  301 message format error");
                    outputToClient.writeUTF("Error:  301 message format error");
                    myWriter.write("Error:  301 message format error\n");
                }
            } else if (commands.length == 4 && commands[1].equals("-c")) {    //input was incorrect based on type
                System.out.println("Error:  301 message format error");
                outputToClient.writeUTF("Error:  301 message format error");
                myWriter.write("Error:  301 message format error\n");
            } else {
                if (commands[1].equals("-c")) { //this solves correctly formatted circle area
                    mes1 = Integer.parseInt(commands[2]);
                    area = Math.PI * Math.pow(mes1, 2);
                    perimeter = 2 * Math.PI * mes1;
                    outputToClient.writeUTF("Circle's circumference is " + df.format(perimeter) + " and area is " + df.format(area));
                    myWriter.write("radius " + commands[2] + ":  Circle's circumference is " + df.format(perimeter) + " and area is " + df.format(area) + "\n");
                } else if (commands[1].equals("-r")) {    //this solves correctly formatted rectangle area
                    mes1 = Integer.parseInt(commands[2]);
                    if (commands.length == 4) {
                        mes2 = Integer.parseInt(commands[3]);
                        area = mes1 * mes2;
                        perimeter = 2 * (mes1 + mes2);
                        myWriter.write("sides " + commands[2] + " " + commands[3] + ":  Rectangle's perimeter is " + df.format(perimeter) + " and area is " + df.format(area) + "\n");
                    } else {  //rectangle is a square
                        mes2 = mes1;
                        area = mes1 * mes2;
                        perimeter = 2 * (mes1 + mes2);
                        myWriter.write("sides " + commands[2] + " " + commands[2] + ":  Rectangle's perimeter is " + df.format(perimeter) + " and area is " + df.format(area) + "\n");
                    }
                    area = mes1 * mes2;
                    perimeter = 2 * (mes1 + mes2);

                    outputToClient.writeUTF("Rectangle's perimeter is " + df.format(perimeter) + " and area is " + df.format(area));

                } else {
                    System.out.println("Error:  No shape type found");
                    outputToClient.writeUTF("Error:  301 message format error");
                    myWriter.write("Error:  301 message format error\n");
                }
            }

            myWriter.close();
        } catch (NumberFormatException nfe) {
            System.out.println("Error:  Shape measurements must be numeric");
            try {
                FileWriter myWriter = new FileWriter(loginState + "_solutions.txt", true);
                outputToClient.writeUTF("Error:  301 message format error");
                myWriter.write("Error:  301 message format error\n");
                myWriter.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }//end try-catch
        } catch (IOException ex) {
            ex.printStackTrace();
        }//end try-catch

    }

    public static String ListSolutions(String currUser) {
        String fileData = "";

        try {
            System.out.println(currUser);
            fileData = fileData + currUser + "\n";

            File myObj = new File(currUser + "_solutions.txt");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {  //append each line from file to return string
                String data = myReader.nextLine();
                System.out.println("\t" + data);
                fileData = fileData + "\t\t" + data + "\n";
            }

            myReader.close();
            return fileData;
        } catch (FileNotFoundException e) {
            System.out.println("\tNo interactions yet");
            return currUser + "\n" + "\t\tNo interactions yet\n";
        } catch (IOException ex) {
            ex.printStackTrace();
            return "\tError: Exception thrown\n";
        }
    }

    public void ListAllSolutions(String[] commands, DataOutputStream outputToClient) {
        String fullList = "";

        try {
            if (commands.length == 2) {
                if (commands[1].equals("-all")) {
                    if (loginState.equals("root")) {
                        for (int i = 0; i < loginList.length; ++i) {
                            fullList += ListSolutions(loginList[i].username) + "\t";
                        }
                        outputToClient.writeUTF(fullList);
                    } else {
                        System.out.println("Error: you are not the root user");
                        outputToClient.writeUTF("Error: you are not the root user");
                    }
                } else {
                    System.out.println("Error: Command not recognized");
                    outputToClient.writeUTF("301 message format error");
                }
            } else {
                System.out.println("Error: Command not recognized");
                outputToClient.writeUTF("301 message format error");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
