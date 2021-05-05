/***************
 * GameEngine
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
import java.io.*;

public class GameEngine implements Runnable {
    public static final int DEFAULT_PORT = 1340;
    GameState gameState;
    Debug debug;
    boolean done; // is game done

    public GameEngine() {
        this.gameState = new GameState();
        this.debug = Debug.getInstance();
        this.done = false;
    }

     /**
      * Return a (deep) clone of the game state.
      * Thus any changes to the game state must be made through the game server.
      **/
     public GameState getGameState() {
       try {
         byte[] byteCopy = getGameStateBinary();
         ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(byteCopy)); // NullPointerException Occuring here and line 418 in App
         GameState copy = (GameState) (in.readObject());
         return copy;
       } catch (ClassNotFoundException | IOException e) {
         debug.println(1, "[GameEngine.gGS]: Coding Error!");
         return null;
       }
     }

     public byte[] getGameStateBinary() {
       try {
         ByteArrayOutputStream bout = new ByteArrayOutputStream();
         ObjectOutputStream out = new ObjectOutputStream(bout);
         synchronized(gameState) {
           out.writeObject(gameState);
         }
         return bout.toByteArray();
       } catch (IOException e) {
         debug.println(1, "[GameEngine.gGSB]: Coding Error!");
         return null;
       }
     }

    public synchronized int addPlayer(String name, Color color) {
        return gameState.addPlayer(name, color);
    }

    /**
     * Set a player p's direction to dx and dy.
     * This moves all cells in that direction
     * @param p The player (index) to move
     * @param dx The amount to move in the x direction
     * @param dy The amount to move in the y direction
     **/
    public synchronized void setPlayerDirection(int p, double dx, double dy) {
        gameState.setPlayerDirection(p, dx, dy);
    }

      /**
     * Set a player p's speed
     * @param p The player (index) to move
     * @param s New speed
     **/
    public synchronized void setPlayerSpeed(int p, double s) {
        gameState.setPlayerSpeed(p, s);
    }

    // main run method and runs instance of slither
    public void run() {
         // First add a lot of random food cells
         for (int i = 0; i < 1000; i++)
             gameState.addRandomSnack();

         long currentTime = System.currentTimeMillis();
         while (!gameState.isDone()) {
             debug.println(10, "(GameEngine.run) Executing...");
             // Compute elapsed time since last iteration
             long newTime = System.currentTimeMillis();
             long delta = newTime - currentTime;
             currentTime = newTime;

             // Move all of the players
             synchronized (this) {
                 gameState.moveAllPlayers(delta/1000.0);  // Speed to move in
             }

             // Add some more food.  (Could do this periodically instead but for now ALL the time)
             synchronized (this) {
                 gameState.addRandomSnack();
             }

             // Detect all collisions
             detectCollisions();

             try {
                 Thread.sleep(1);
             } catch (Exception e) { }
         }
     }

     private synchronized void detectCollisions() {
             ArrayList<GameState.Player> player = gameState.getPlayers();
             ArrayList<GameState.Cell> snacks =  gameState.getSnacks();

             // First check for collisions with food
             for (GameState.Player p: player) p.collisions(snacks, true);
             gameState.purgeSnacks();

             // Now check for collisions with all the players (including themselves)
             int size = player.size();
             for (int i = 0; i < size; i++) {
                 GameState.Player p = player.get(i);
                 for (int j = 0; j < size; j++) {
                     if (i != j) {
                       GameState.Player q = player.get(j);
                       p.collisions(q);  // Compute collisions between these two players
                     }
                 }
                 // And purge this player's dead cells at end
                 // TODO: REMOVE DEAD PLAYERS
             }
    }
    // process the joinmessage

   /* private void processJoinMessage(JoinMessage message) {
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
