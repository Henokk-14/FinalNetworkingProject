/***************
 * GameEngine
 * Author: Christian Duncan
 * Editor: Dylan Irwin
 * Spring 21: CSC340
 * This is the Game Engine for the Networm game
 ***************/
import java.io.IOException;
import java.util.ArrayList;
import java.awt.Color;
import java.io.*;

public class GameEngine implements Runnable {
    public static final int DEFAULT_PORT = 1340;
    GameState gameState;
    Debug debug;
    boolean done; // is game done
    double snackDensity;

    public GameEngine() {
        this.gameState = new GameState();
        this.debug = Debug.getInstance();
        this.done = false;
        this.snackDensity = 0.01;
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
     //returns the gameState in a byte array form
     public byte[] getGameStateBinary() {
       try {
         ByteArrayOutputStream bout = new ByteArrayOutputStream();
         ObjectOutputStream out = new ObjectOutputStream(bout);
         synchronized(gameState) {
           out.writeObject(gameState);
         }
         out.close();
         bout.close();
         return bout.toByteArray();
       } catch (IOException e) {
         debug.println(1, "[GameEngine.gGSB]: Coding Error!");
         return null;
       }
     }

    public synchronized int addPlayer(String name, Color color) {
        
      int i = gameState.addPlayer(name, color);
      System.out.println("adding player now");
      gameState.display(System.out);
      return i;
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
         for (int i = 0; i < 100; i++)
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
             if(gameState.getSnacks().size()<gameState.maxX*gameState.maxY*snackDensity){
                synchronized (this) {
                gameState.addRandomSnack();
            }
             }
             

             // Detect all collisions
             detectCollisions();

             try {
                 Thread.sleep(16);
             } catch (Exception e) { }
         }
     }

     private synchronized void detectCollisions() {
             ArrayList<GameState.Player> player = gameState.getPlayers();
             ArrayList<GameState.Cell> snacks =  gameState.getSnacks();

             // First check for collisions with food
             for (GameState.Player p: player) p.collisions(snacks, true);
             gameState.purgeSnacks();

             // Now check for collisions with all the players (not themselves)
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
}
