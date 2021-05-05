/***********
 * Game State
 * Author: Christian Duncan
 *
 * This application stores the state of the game.
 *   It includes a list of players
 *   And a list of food. 
 *   It is not designed to be efficient - a different task altogether!
 ***********/
import java.util.ArrayList;
import java.util.Random;
import java.awt.Color;
import java.io.PrintStream;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.io.Serializable;

public class GameState implements Cloneable, Serializable {
    public static final long serialVersionUID=3402L;
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
        public void computeCollision(Cell other) {
            if (this == other) return;  // Same cell - ignore
            if (this.r == 0 || other.r == 0) return;  // Cell is non-existent

            double dx = this.x - other.x;
            double dy = this.y - other.y;
            double distSq = dx*dx + dy*dy;
            double distCollision = this.r + other.r;  // The radii of the two cells
            if (distSq < distCollision*distCollision) {
                // They collide - larger cell absorbs smaller cell
                double newR = Math.sqrt(this.r*this.r + other.r*other.r);
                if (this.r > other.r) {
                    this.r = newR;
                    other.r = 0;
                } else if (other.r > this.r) {
                    other.r = newR;
                    this.r = 0;
                } else {
                    if (rand.nextDouble() < 0.5) {
                        // This one wins
                        this.r = newR;
                        other.r = 0;
                    } else {
                        // The other one wins
                        other.r = newR;
                        this.r = 0;
                    }
                }
            }
        }
    }
    
    // Inner class: Just a player, with name and their list of cells
    class Player implements Cloneable, Serializable {
        public static final long serialVersionUID=3402L;
        String name;  // Name to display
        Color appearance;  // The appearance of this player
        ArrayList<Cell> cell; // The various cells associated with this player
        double dx;   // The direction the player is currently moving in
        double dy;
        
        public Player(String n, double initX,  double initY, double initR, Color appearance) {
            this.name = n;
            cell = new ArrayList<>();
            cell.add(new Cell(initX, initY, initR));
            this.appearance = appearance;
        }

        public Object clone() throws CloneNotSupportedException {
            Player p = (Player) super.clone();
            p.cell = new ArrayList<>(cell.size());
            for (int i = 0; i < cell.size(); i++) {
                p.cell.add((Cell) cell.get(i).clone());
            }
            return p;
        }

        public ArrayList<Cell> getCells() { return cell; }
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
            // Normalize speed
            double mag = Math.sqrt(dx*dx + dy*dy);
            if (mag < 1e-10) return;  // No movement at all.
            dx = delta * dx / mag;
            dy = delta * dy / mag;

            for (Cell c: cell) {
                // Compute the "speed", update the x and y, and check the bounds of the arena
                double speed = 1.0/(c.r*c.r);  // Area is PI*r^2 but PI is just a constant anyway
                c.x += speed*dx;
                if (c.x < 0) {
                    c.x = 0;
                } else if (c.x > maxX) {
                    c.x = maxX;
                }
                c.y += speed*dy;
                if (c.y < 0) {
                    c.y = 0;
                } else if (c.y >= maxY) {
                    c.y = maxY;
                }
            }
        }

        /**
         * Split all the cells for this player by the given fraction amount
         * @param fraction The fraction amount of mass to eject (up to 50%)
         *   The newly spawned cell will be tangential to the original cell -- so moves a bit forward.
         *   Within boundaries... which could force a merge again!
         *   Cells are not allowed to get too small so there is a minimum per cell
         *   If either resulting cell gets too small then the split is not allowed for that cell.
         *   Splits are not allowed once the number of cells is above a capacity as well.
         *   This is just to prevent really SLOW processing since the collisions are done inefficiently!
         **/
        public void split(double fraction) {
            if (fraction <= 0) return;  // No split
            if (fraction >= 0.5) fraction = 0.5;  // Capped at 50/50 split

            // Normalize speed
            double mag = Math.sqrt(dx*dx + dy*dy);
            if (mag < 1e-10) return;  // No movement at all.
            dx /= mag;
            dy /= mag;

            // Compute the fractional adjustments in radii (accounting for mass)
            double fracNew = Math.sqrt(fraction);   // Radius factor for the "smaller" ejected portion
            double fracOld = Math.sqrt(1-fraction); // Radius factor for the "larger" remaining portion

            // Iterate through each cell and split!
            int size = cell.size();
            for (int i = 0; i < size; i++) {
                if (cell.size() >= maxCells) return;  // No more splits allowed!
                
                Cell c = cell.get(i);

                // Compute the radius and position of the new cell (and the old one)
                double newRad = c.r * fracNew;
                double oldRad = c.r * fracOld;
                if (newRad >= minR && oldRad >= minR) {
                    // Resulting cells are large enough.  Technically, only need to check newRad here.
                    double dist = c.r + newRad;  // The distance for the new cell
                    double newX = c.x + dist*dx;
                    if (newX < 0) newX = 0;
                    else if (newX > maxX) newX = maxX;
                    double newY = c.y + dist*dy;
                    if (newY < 0) newY = 0;
                    else if (newY > maxY) newY = maxY;
                    cell.add(new Cell(newX, newY, newRad));
                    c.r = oldRad;  // Update the radius of the remaining portion
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
            collisions(other.cell);
        }

        /**
         * Determine any collisions between this player and a group of cells (player or food)
         * @param cell The list of cells
         * Two cells that collide cause the larger to absorb the smaller and the smaller to disappear.
         * If multiple cells collide at same time -- it'll depend on the order checked
         * Just for coding simplicity... this check is INEFFICIENT brute force!
         **/
        public void collisions(ArrayList<Cell> cell) {
            for (Cell thisC: this.cell) {
                for (Cell otherC: cell) {
                    if (thisC.r == 0) break;  // This cell lost, it can stop checking.
                    thisC.computeCollision(otherC);
                }
            }
        }

        /**
         * Purge cells in player list that have radius 0 -- "dead cells"
         **/
        public void purge() {
            // The simplest way is just to create a new arraylist of only those cells to keep.
            // Can be done more efficiently by keeping the old array list but minor time constraint here.
            ArrayList<Cell> newCell = new ArrayList<>(cell.size());
            for (Cell c: cell) {
                if (c.r > 0) newCell.add(c);
            }
            cell = newCell;
        }
        
        /**
         * Generate a string representation of the given player
         * For DEBUGGING purposes mainly
         **/
        @Override
        public String toString() {
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
    Player food;   // A "player" that represents unmoving food
    Color foodColor = new Color(0xF5F5DC);

    double maxX;   // The range of the game state (loops around if it gets too close)
    double maxY;
    double minR;   // The smallest that any cell can get (except for "food" particles)
    int maxCells;  // The maximum number of cells allowed for any player
    Random rand;   // Random number generator
    
    public GameState() {
        player = new ArrayList<Player>(2);  // Initial size
        maxX = 1000.0;
        maxY = 1000.0;
        minR = 1.0;
        maxCells = 10;
        rand = new Random();
        Point2D.Double p = randomPosition();
        double size = minR*(rand.nextDouble()*0.4 + 0.5);
        food = new Player(null, p.x, p.y, size, foodColor);
    }

    public Object clone() throws CloneNotSupportedException {
        GameState gs = (GameState) super.clone();
        gs.food = (Player) food.clone();
        gs.player = new ArrayList<>(player.size());
        for (int i = 0; i < player.size(); i++) {
            gs.player.add((Player) player.get(i).clone());
        }
        return gs;
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
    public int addPlayer(String name, Color color) {
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
    public void setPlayerDirection(int p, double dx, double dy) {
        Player pl = player.get(p);  // Get the Player object
        pl.setDirection(dx, dy);
    }

    /**
     * Split all cells for this player by the given fraction amount in their moving direction
     * See Player.split
     **/
    public void splitCells(int p, double fraction) {
        Player pl = player.get(p);  // Get the Player object
        pl.split(fraction);
    }

    // Returns the list of players.  Probably safer to have some way to iterate through them and the cells
    // So we can control access.  But this will be a network so this actually will be a local copy of the GameState anyway!
    public ArrayList<Player> getPlayers() {
        return player;
    }

    public Player getFood() {
        return food;
    }
    
    /** 
     * Add a piece of random food on the board - anywhere 
     **/
    public void addRandomFood() {
        Point2D.Double p = randomPosition();
        double size = minR*(rand.nextDouble()*0.4 + 0.5);
        food.addCell(p.x, p.y, size);
    }

    /**
     * Move all players by the given delta speed
     **/
    public void moveAllPlayers(double delta) {
        for (Player p: player) {
            p.move(delta);
        }
    }
    
    // Get the bounding box of the given player's cells
    public Rectangle2D.Double getBoundingBox(int p) {
        double cellMinX = maxX;
        double cellMaxX = 0;
        double cellMinY = maxY;
        double cellMaxY = 0;
        Player pl = player.get(p);
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
    public void display(PrintStream out) {
        out.println("============ State =================");
        for (Player p: player) {
            out.println("  " + p);
        }
        out.println("====================================");
    }
}
