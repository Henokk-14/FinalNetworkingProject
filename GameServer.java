/***************
 * GameServer
 * Author: Christian Duncan
 * Spring 21: CSC340
 * 
 * This is the Server for the Petrio game.
 * It is essentially inspired quite largely by Agar.io
 * And is designed to be a simple game to convert to a Networking game.
 * This just handles game connections and communication.
 ***************/
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

public class GameServer implements Runnable {
    public static final int DEFAULT_PORT = 1340;

    Set<Connection> connection; // Set of client connections
    GameEngine gameEngine;
    Debug debug;
    int port;
    boolean done;

    public GameServer() {
        this(DEFAULT_PORT);
    }
    public GameServer(int port) {
        this.port = port;
        this.gameEngine = new GameEngine();
        this.debug = Debug.getInstance();
        this.connection = new HashSet<>();
    }
    /**
     * Run the main communication server... just listen for and create connections
     **/
    public void run() {
        debug.println(1,"[Game Server] WELCOME!  Starting up...");
        try {
            // Create a server socket bound to the given port
            ServerSocket serverSocket = new ServerSocket(port);

            while (!done) {
                // Wait for a client request, establish new thread, and repeat
                Socket clientSocket = serverSocket.accept();
                addConnection(clientSocket);
            }
        } catch (Exception e) {
            debug.println(0,"[GameServer] ABORTING: An error occurred while creating server socket. " +
                    e.getMessage());
            System.exit(1);
        }
    }

    public void addConnection(Socket clientSocket){
        String name = clientSocket.getInetAddress().toString();
        System.out.println("Inventory Server: Connecting to client: "+ name );
        Connection c = new Connection(clientSocket,name);
        connection.add(c);
        c.start(); //start thread
    }
    /**
     * The main entry point.  It just processes the command line argument
     * and starts an instance of the InventoryServer running.
     **/
    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        // Set the port if specified
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Usage: java GameServer [PORT]");
                System.err.println("       PORT must be an integer.");
                System.exit(1);
            }
        }

        // Create and start the server
        GameServer s = new GameServer(port);
        s.run();
    }
    class Connection extends Thread{
        Socket socket;
        ObjectOutputStream out;
        ObjectInputStream in;
        boolean done;
        String name;
        int playerID;
        Color color;

        public Connection(Socket socket, String name){
            this.socket = socket;
            done = false;
            this.name = name;
        }

        public void run(){
            try {
                //first get i/o streams for communication to and from the server
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                while (!done) {
                    Object message = in.readObject();
                    if(message == null){
                        printMessage(1,"Line terminated finished");// level 1
                        done = true;
                    } else {
                        processMessage(message);
                    }

                }
            }catch(IOException e){
                printMessage(1,"I/O error while communicating with Client"); //lvl 1
                printMessage(1," Message: " + e.getMessage()); //lvl 1
            }
            catch (ClassNotFoundException e) {
                debug.println(1, " Coding Error: Client transmitted unrecognized Object");
            }

            try{
                printMessage(1,"Client is closing down");
                if(in != null) in.close();
                if(out != null) out.close();
                if(socket != null) socket.close();
            } catch (IOException e) {

            }
        }
        //processes a message that has been sent through this connection
        private void processMessage(Object message) {
            // process the line
            // create if statements with instance of different types of
            // messages (similar to in the client)
            if(message instanceof JoinMessage){
                processJoinMessage((JoinMessage)message);
            }
            else{
                printMessage(3, "Unrecognized message: "+message);
            }
            printMessage(1,"Proccesing line: "+message);
        }
        //process a request from the client to join
        private void processJoinMessage(JoinMessage message) {
             this.name=message.name;
             this.color=message.color;
             this.playerID= gameEngine.addPlayer(this.name,this.color); 
             //let the client know that it has been registered in the server  
             transmitMessage(new JoinResponseMessage(this.name, this.playerID));
            }
        
        
        //print message
        public void printMessage(int lvl,String m) {
            debug.println(lvl,"["+ name +"]:" + m);
        }
        public void transmitMessage(Object message) {
            try {
                   synchronized(out) {
                 out.writeObject(message);
                 out.flush();
                  }
   
            }
            catch (IOException e) {
                debug.println(3, "Error transmitting message");
            }
   
       }
    }



    // process the joinmessage
/*
    
*/

    // transmit a message
     
}
