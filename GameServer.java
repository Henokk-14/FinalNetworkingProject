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
        PrintWriter out;
        BufferedReader in;
        boolean done;
        String name;

        public Connection(Socket socket, String name){
            this.socket = socket;done = false; this.name = name;
        }

        public void run(){
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                while (!done) {
                    String line = in.readLine();
                    if(line == null){
                        printMessage(1,"Line terminated finished");// level 1
                        done = true;
                    } else {
                        processline(line);
                    }

                }
            }catch(IOException e){
                printMessage(1,"I/O error while communicating with Clinet"); //lvl 1
                printMessage(1," Message: " + e.getMessage()); //lvl 1
            }

            try{
                printMessage(1,"Clinet is closing down");
                if(in != null) in.close();
                if(out != null) out.close();
                if(socket != null) socket.close();
            } catch (IOException e) {

            }
        }

        private void processline(String line) {
            // process the line
            printMessage(1,"Proccesing line: "+line);
        }

        // process a message
        /*private void processMessage(Object message) {
            debug.println(3, "[ " + name + " ] message " + message);
            if (message instanceof JoinMessage) {
                processJoinMessage((JoinMessage) message);
            }
            else {
                debug.println(3, "[ " + name + " ] Unrecognized message: " + message);
            }
        }*/
        //print message
        public void printMessage(int lvl,String m) {
            debug.println(lvl,"["+ name +"]:" + m);
        }
    }



    // process the joinmessage

/*    private void processJoinMessage(JoinMessage message) {
    //     this.name = message.name;
    //    this.color = message.color;
            transmitMessage( new JoinResponseMessage(this.name, this.playerID));
    }


    // transmit a message
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

    }*/
}
