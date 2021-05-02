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
            return (GameState) gameState.clone();
        } catch (CloneNotSupportedException e) {
            debug.println(1, "Coding error: GameState cloning is not supported.  Why not?");
        }
        return null;
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
     * Split all cells for this player by the given fraction amount in their moving direction
     **/
    public synchronized void splitCells(int p, double fraction) {
        gameState.splitCells(p, fraction);
    }
    // main run method and runs instance of slither
    public void run() {
        // First add a lot of random food cells
        for (int i = 0; i < 1000; i++)
            gameState.addRandomFood();

        long currentTime = System.currentTimeMillis();
        while (!gameState.isDone()) {
            debug.println(10, "(GameEngine.run) Executing...");
            // Compute elapsed time since last iteration
            long newTime = System.currentTimeMillis();
            long delta = newTime - currentTime;
            currentTime = newTime;

            // Move all of the players
            synchronized (this) {
                gameState.moveAllPlayers(delta/20);  // Speed to move in
            }

            // Add some more food.  (Could do this periodically instead but for now ALL the time)
            synchronized (this) {
                gameState.addRandomFood();
            }

            // Detect all collisions
            detectCollisions();

            try {
                Thread.sleep(100);
            } catch (Exception e) { }
        }
    }

    private synchronized void detectCollisions() {
        ArrayList<GameState.Player> player = gameState.getPlayers();
        GameState.Player food = gameState.getFood();

        // First check for collisions with food
        for (GameState.Player p: player) p.collisions(food);
        food.purge();

        // Now check for collisions with all the players (including themselves)
        int size = player.size();
        for (int i = 0; i < size; i++) {
            GameState.Player p = player.get(i);
            for (int j = i; j < size; j++) {
                GameState.Player q = player.get(j);
                p.collisions(q);  // Compute collisions between these two players
            }

            // And purge this player's dead cells at end
            p.purge();
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

