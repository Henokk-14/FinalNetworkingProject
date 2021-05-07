/***********
 * Game State
 * Author: Christian Duncan
 * Editors: Dylan Irwin, Jack Zemlanicky
 * This application stores the state of the game.
 ***********/
import java.util.ArrayList;
import java.util.Random;
import java.util.Deque;
import java.util.ArrayDeque;
import java.awt.Color;
import java.io.PrintStream;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.io.Serializable;


public class GameState implements Cloneable, Serializable {
    public static final long serialVersionUID=3402L;
    public static final double MIN_SPEED = 10.0;

    // Inner class: A simple cell on the board
    class Cell implements Cloneable, Serializable {
        public static final long serialVersionUID=3402L;
        double x;  // x position
        double y;  // y position
        double r;  // radius

        public Cell(double x, double y, double r) {
            this.x = x;
            this.y = y;
            this.r = r;
        }

        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        /**
         * Determine if two cells collide and have larger absorb smaller cell
         * @param other The other cell to check for collision with this cell
         * If they collide, the two cells' radii will change (increase and go to 0)
         * Does a check to see if the two cells are the same - if so, ignore check
         * If two cells are same radii, a coin toss happens!
         **/
         public boolean computeCollision(Cell other) {
           if (this == other) return false;  // Same cell - ignore
           if (this.r == 0 || other.r == 0) return false;  // Cell is non-existent

           double dx = this.x - other.x;
           double dy = this.y - other.y;
           double distSq = dx*dx + dy*dy;
           double distCollision = this.r + other.r;  // The radii of the two cells

           return (distSq < distCollision*distCollision);
       }
    }

    // Inner class: Just a player, with name and their list of cells
    class Player implements Cloneable, Serializable {
        public static final long serialVersionUID=3402L;

        String name;  // Name to display
        Color appearance;  // The appearance of this player
        Deque<Cell> cell; // The various cells associated with this player
        double dx;   // The direction the player is currently moving in
        double dy;
        double growAmount = 2.0;   // Tracks snacks eaten by each player, to grow the player's length
        double distance;  // Distance moved from the head
        double speed;   // Speed at which the snake is moving, in units/second

        public Player(String n, double initX,  double initY, double initR, Color appearance) {
            this.name = n;
            cell = new ArrayDeque<>();
            cell.add(new Cell(initX, initY, initR));
            this.appearance = appearance;
            this.speed = MIN_SPEED;
        }

        public Deque<Cell> getCells() { return cell; }
        public String getName() { return name; }
        public Color getAppearance() { return appearance; }

        /**
         * Set the movement direction for this player
         **/
        public void setDirection(double dx, double dy) {
            this.dx = dx;
            this.dy = dy;
        }

        /**
         * Set the movement speed of this player
         **/
        public void setSpeed(double s) {
            this.speed = s;
        }

        /**
         * Add a new cell for this player
         **/
        public void addCell(double x, double y, double r) {
            cell.add(new Cell(x, y, r));
        }

        /**
         * Move all the cells for this player in the general direction dx, dy by the delta factor
         *   The actual distance moved will depend on the mass of the cell.
         *   More mass = slower movement.
         **/
         public void move(double delta) {
             distance += speed*delta;   // Calulate how far away it's moved now
             Cell head = cell.peekFirst();
             //System.out.println(this.name + ": Distance = " + distance + " x = " + head.x + " y = " + head.y);
             if (speed > MIN_SPEED) {
               //Increase speed comes at a cost, decreases the growth value at a rate of 1 cell/second
               double factor = speed/MIN_SPEED - 1.0;
               growAmount -= factor*delta;
             }
             if (distance > head.r) {
                 distance -= head.r;
                 double mag = Math.sqrt(dx*dx + dy*dy);
                 //if player is just joining
                 if(mag==0){
                    return;
                 }
                 double newX = head.x + head.r * dx / mag;
                 double newY = head.y + head.r * dy / mag;
                 if(newX<0) newX = 0;
                 else if(newX>maxX) newX = maxX;
                 if(newY<0) newY = 0;
                 else if(newY>maxY) newY = maxY;
                 if (growAmount >= 1) {
                   // Grow a new head
                   this.addCell(newX, newY, head.r);
                   growAmount--;
                 } else if (growAmount <= -1) {
                   // Remove a cell
                   if (this.cell.size() > 1) {
                     this.cell.removeLast();
                   }
                   growAmount++;
                   Cell end = this.cell.removeLast();
                   end.x = newX;
                   end.y = newY;
                   end.r = head.r;
                   this.cell.addFirst(end);
                 } else {
                   Cell end = this.cell.removeLast();
                   end.x = newX;
                   end.y = newY;
                   end.r = head.r;
                   this.cell.addFirst(end);
                 }
             }
         }

        /**
         * Determine any collisions between two groups of Players
         * @param other The other player
         * Two cells that collide cause the larger to absorb the smaller and the smaller to disappear.
         * If multiple cells collide at same time -- it'll depend on the order checked
         * Just for coding simplicity... this check is INEFFICIENT brute force!
         **/
        public void collisions(Player other) {
            collisions(other.cell, false);
        }

        /**
         * Determine any collisions between this player and a group of cells (player or food)
         * @param cell The list of cells
         * Two cells that collide cause the larger to absorb the smaller and the smaller to disappear.
         * If multiple cells collide at same time -- it'll depend on the order checked
         * Just for coding simplicity... this check is INEFFICIENT brute force!
         **/
         public void collisions(Iterable<Cell> cell, boolean isSnack) {
             Cell head = this.cell.peekFirst();
             for (Cell otherC: cell) {
                 if (otherC.r > 0 && head.computeCollision(otherC)) {
                   if(isSnack) {
                     this.growAmount+= otherC.r;   // Add cell to the player
                     otherC.r = 0;   // Get rid of snack
                     //System.out.println(this.name + ": yum yum " + this.growAmount);
                   }
                   else{
                       //player whose head collides with other's body shrinks by x factor and respawns
                       Point2D.Double p = randomPosition();
                       //TODO: iterate thru old deque to turn them into food
                       this.cell = new ArrayDeque<>();
                       this.cell.add(new Cell(p.x, p.y, minR));
                   }
                   return;
                 }
             }
         }

        /**
         * Generate a string representation of the given player
         * For DEBUGGING purposes mainly
         **/
        @Override
        public synchronized String toString() {
            StringBuilder res = new StringBuilder();
            res.append(name);
            res.append(" cells: ");
            for (Cell c: cell) {
                res.append("(");
                res.append(c.x);
                res.append(",");
                res.append(c.y);
                res.append(",");
                res.append(c.r);
                res.append(") ");
            }
            return res.toString();
        }
    }

    // The list of Players
    private ArrayList<Player> player;
    // The list of snacks to eat
    ArrayList<Cell> snacks;
    Color snackColor = new Color(0xF5F5DC);

    double maxX;   // The range of the game state (loops around if it gets too close)
    double maxY;
    double minR;   // The smallest that any cell can get (except for "food" particles)
    int maxCells;  // The maximum number of cells allowed for any player
    Random rand;   // Random number generator

    public GameState() {
        player = new ArrayList<Player>(2);  // Initial size
        maxX = 500.0;
        maxY = 500.0;
        minR = 1.0;
        maxCells = 10;
        snacks = new ArrayList<Cell>();
        rand = new Random();
    }

    /**
     * Returns if the game is done or not!
     **/
    public boolean isDone() {
        return false;  // For now, runs forever!
    }

    /**
     * @returns A random Point on the game state
     **/
    public Point2D.Double randomPosition() {
        return new Point2D.Double(rand.nextDouble()*maxX, rand.nextDouble()*maxY);
    }

    /**
     * Add a player to the Game State.  All future references to this
     * player should use the index returned.
     * @param name The name of the player
     * @param color The color of the player
     * @returns The index of this player (in the ArrayList)
     **/
    public synchronized int addPlayer(String name, Color color) {
        // Pick an initial random location for the cell
        Point2D.Double p = randomPosition();
        player.add(new Player(name, p.x, p.y, minR, color));
        return player.size()-1;
    }

    /**
     * Set a player p's direction to dx and dy.
     * This moves all cells in that direction
     * @param p The player (index) to move
     * @param dx The amount to move in the x direction
     * @param dy The amount to move in the y direction
     **/
    public synchronized void setPlayerDirection(int p, double dx, double dy) {
        Player pl = player.get(p);  // Get the Player object
        pl.setDirection(dx, dy);
    }

    /**
     * Set a player p's speed
     * @param p The player (index) to move
     * @param s Spped of player
     **/
    public synchronized void setPlayerSpeed(int p, double s) {
        Player pl = player.get(p);  // Get the Player object
        pl.setSpeed(s);
    }

    // Returns the list of players.  Probably safer to have some way to iterate through them and the cells
    // So we can control access.  But this will be a network so this actually will be a local copy of the GameState anyway!
    public ArrayList<Player> getPlayers() {
        return player;
    }

    public ArrayList<Cell> getSnacks() {
        return snacks;
    }

    /**
     * Add a piece of random snack on the board - anywhere
     **/
    public synchronized void addRandomSnack() {
        Point2D.Double p = randomPosition();
        double size = rand.nextDouble()*0.9+0.1;
        Cell snac = new Cell(p.x, p.y, size);
        snacks.add(snac);
    }

    /**
     * Purge cells in snack list that have radius 0 -- "dead cells"
     **/
    public synchronized void purgeSnacks() {
        // The simplest way is just to create a new arraylist of only those cells to keep.
        // Can be done more efficiently by keeping the old array list but minor time constraint here.
        ArrayList<Cell> newSnacks = new ArrayList<Cell>();
        for (Cell s: snacks) {
            if (s.r > 0) newSnacks.add(s);
        }
        snacks = newSnacks;
    }

    /**
     * Move all players by the given delta speed
     **/
    public synchronized void moveAllPlayers(double delta) {
        for (Player p: player) {
            p.move(delta);
        }
    }

    // Get the bounding box of the given player's cells
    public synchronized Rectangle2D.Double getBoundingBox(int p) {
        double cellMinX = maxX;
        double cellMaxX = 0;
        double cellMinY = maxY;
        double cellMaxY = 0;
        Player pl = player.get(p);
        //System.err.println("gBB: playerID = " +p+ "number of players is "+player.size());
        if (pl.cell.size() == 0) return new Rectangle2D.Double(0, 0, maxX, maxY);  // Full screen

        for (Cell c: pl.cell) {
            if (c.x-c.r < cellMinX) cellMinX = c.x - c.r;
            if (c.x+c.r > cellMaxX) cellMaxX = c.x + c.r;
            if (c.y-c.r < cellMinY) cellMinY = c.y - c.r;
            if (c.y+c.r > cellMaxY) cellMaxY = c.y + c.r;
        }

        return new Rectangle2D.Double(cellMinX, cellMinY, cellMaxX - cellMinX, cellMaxY - cellMinY);
    }

    /**
     * Display the "Game State"
     **/
    public synchronized void display(PrintStream out) {
        out.println("============ State =================");
        for (Player p: player) {
            out.println("  " + p);
        }
        out.println("====================================");

    }
}
