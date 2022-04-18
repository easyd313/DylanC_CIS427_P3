//Author: Dylan Crompton
//Date Modified: 4/15/2022
//Date Created: 4/12/2022
//Purpose: Geometry Solver Client

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private static final int SERVER_PORT = 8728;

    public static void main(String[] args) {

        DataOutputStream toServer;
        DataInputStream fromServer;
        Scanner input
                = new Scanner(System.in);

        //attempt to connect to the server
        try {
            Socket socket
                    = new Socket("localhost", SERVER_PORT);

            //create input stream to receive data
            //from the server
            fromServer
                    = new DataInputStream(socket.getInputStream());

            toServer
                    = new DataOutputStream(socket.getOutputStream());

            // sendMessage thread
            Thread sendMessage = new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.print("C:\t");
                    while (true) {

                        // read the message to deliver.
                        String messageC = input.nextLine();

                        try {
                            // write on the output stream
                            toServer.writeUTF(messageC);
                            if (messageC.equals("SHUTDOWN")) {
                                break;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            // readMessage thread
            Thread readMessage = new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        // read the message sent to this client
                        while (true) {
                            String messageS = fromServer.readUTF();
                            System.out.print("\nS:\t" + messageS + "\nC:\t");
                            if (messageS.equals("200 OK ")) {
                                break;
                            }
                        }
                        socket.close();
                    } catch (IOException e) {

                        e.printStackTrace();
                    }

                }
            });

            sendMessage.start();
            readMessage.start();

        } catch (IOException ex) {
            ex.printStackTrace();
        }//end try-catch

    }//end main
}
