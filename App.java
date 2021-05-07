/***************
 * App
 * Author: Christian Duncan
 * Spring 21: CSC340
 *
 * This is the Main GUI interface to the Petrio game.
 * It is essentially inspired quite largely by Agar.io
 * And is designed to be a simple game to convert to a Networking game.
 ***************/
import java.awt.*;        // import statements to make necessary classes available
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Deque;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

public class App extends JFrame {
    /**
     * The main entry point that sets up the window and basic functionality
     */
    public static void main(String[] args) {
        App frame = new App();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setResizable(true);
        frame.setVisible(true);
    }

    static final double MIN_WIDTH = 50; // Minimum dimensions of the screen for cells.
    static final double MIN_HEIGHT = 50;

    private GameEngine gameEngine;
    private GameState gameState;
    private VisPanel visPane;
    private Debug debug = Debug.getInstance();
    JDialog debugWindow = null;
    int playerID = -1;
    private String hostname = "127.0.0.1";
    private int port = GameServer.DEFAULT_PORT;
    private Connection connection = null;
    private String name;

    /* Constructor: Sets up the initial look-and-feel */
    public App() {
        JLabel label;  // Temporary variable for a label
        JButton button; // Temporary variable for a button

        // Set up the initial size and layout of the frame
        // For this we will keep it to a simple BoxLayout
        setLocation(100, 100);
        setPreferredSize(new Dimension(800, 800));
        setTitle("NetWorm");
        Container mainPane = getContentPane();
        mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.Y_AXIS));
        mainPane.setPreferredSize(new Dimension(1000, 500));

        // Create the Visualization Panel
        visPane = new VisPanel();
        mainPane.add(visPane);

        // Set up the debug window
        setupDebugWindow();

        // Setup the menubar
        setupMenuBar();

        // Create animation
        Timer animationTimer;  // A Timer that will emit events to force redrawing of game state
        animationTimer = new Timer(16, new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    visPane.repaint();
                }
            });
        animationTimer.start();
        //Commented this out as we are no longer running the game locally from the app class
        //causes a lot of errors in the app when client connects to server because of this
        //startServer();
    }

    // Basically a scrollable text area that shows contents of the debug output
    // stream.  Which we will also create here.
    private void setupDebugWindow() {
        debugWindow = new JDialog(this, "Debug Output (Level " + debug.getLevel() + ")");
        JTextArea debugTextArea;
        debugTextArea  = new JTextArea(50,60);
        debugTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(debugTextArea);
        debugWindow.add(scrollPane);
        debugWindow.setVisible(false);
        debugWindow.pack();
        debugWindow.setResizable(true);
        debug.setStream(new PrintStream(new TextStreamer(debugTextArea)));
    }

    private void setupMenuBar() {
        JMenuBar mbar = new JMenuBar();
        JMenu menu;
        JMenuItem menuItem;
        Action menuAction;

        menu = new JMenu("Connection");
        // Menu item to change server IP address (or hostname really)
        menuAction = new AbstractAction("Change Server ") {
            public void actionPerformed(ActionEvent e) {
                String newHostName = JOptionPane.showInputDialog("Please enter a server IP/Hostname.\nThis only takes effect after the next connection attempt.\nCurrent server address: " + hostname);
                if (newHostName != null && newHostName.length() > 0)
                    hostname = newHostName;
            }
        };
        menuAction.putValue(Action.SHORT_DESCRIPTION, "Change server host name.");
        menuItem = new JMenuItem(menuAction);
        menu.add(menuItem);

        // Menu item to change the port to use
        menuAction = new AbstractAction("Change Server PORT") {
            public void actionPerformed(ActionEvent e) {
                String portName = JOptionPane.showInputDialog("Please enter a server PORT.\nThis only takes effect after the next connection attempt.\nCurrent port: " + port);
                if (portName != null && portName.length() > 0) {
                    try {
                        int p = Integer.parseInt(portName);
                        if (p < 0 || p > 65535) {
                            JOptionPane.showMessageDialog(null, "The port [" + portName + "] must be in the range 0 to 65535.", "Invalid Port Number", JOptionPane.ERROR_MESSAGE);
                        } else {
                            port = p;  // Valid.  Update the port
                        }
                    } catch (NumberFormatException ignore) {
                        JOptionPane.showMessageDialog(null, "The port [" + portName + "] must be an integer.", "Number Format Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };
        menuAction.putValue(Action.SHORT_DESCRIPTION, "Change server PORT.");
        menuItem = new JMenuItem(menuAction);
        menu.add(menuItem);

        // Menu item to create a connection
        menuAction = new AbstractAction("Join Server") {
            public void actionPerformed(ActionEvent e) {

                establishConnection();
                // Add yourself to the game and start the game running
                // First get the name
                boolean flag = true;
                //  while loop until the user enters a name
                while( flag == true) {
                    name = JOptionPane.showInputDialog("Please enter your name.");

                    // if length is not 0 then they entered a name.
                    if(name.length() != 0) {
                        flag = false;
                    }
                }


                // And the color
                Color color = JColorChooser.showDialog(App.this,
                        "Select your color!",
                        Color.BLUE);

                // "Register" the player
                // passes color and name to the server
                registerPlayer(color,name);
            }
        };
        menuAction.putValue(Action.SHORT_DESCRIPTION, "Connect to Server.");
        menuItem = new JMenuItem(menuAction);
        menu.add(menuItem);
        mbar.add(menu);

        menu = new JMenu("Monitor");
        menuAction = new AbstractAction("Debug Console") {
            public void actionPerformed(ActionEvent event) {
                debugWindow.setLocationRelativeTo(debugWindow.getParent());
                debugWindow.setVisible(true);
            }
        };
        menuAction.putValue(Action.SHORT_DESCRIPTION, "Show debug console");
        menuItem = new JMenuItem(menuAction);
        menu.add(menuItem);
        menuAction = new AbstractAction("Debug Level") {
            public void actionPerformed(ActionEvent e) {
                String debugLevel = JOptionPane.showInputDialog("Please enter an integer to select debug level.");
                if (debugLevel != null && debugLevel.length() > 0) {
                    try {
                        int dl = Integer.parseInt(debugLevel);
                        debug.setLevel(dl);
                        debugWindow.setTitle("Debug Output (Level " + debug.getLevel() + ")");
                    } catch (NumberFormatException ignore) {
                        JOptionPane.showMessageDialog(null, "The debug level [" + debugLevel + "] must be an integer.", "Number Format Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };
        menuAction.putValue(Action.SHORT_DESCRIPTION, "Set the Debug level");
        menuItem = new JMenuItem(menuAction);
        menu.add(menuItem);
        mbar.add(menu);
        setJMenuBar(mbar);
    }

    public void establishConnection() {
        try {

        // Establish connection with the Inventory Server
        Socket socket = new Socket(hostname, port);
        connection = new Connection(socket);
        connection.start();

        } catch (UnknownHostException e) {
                printMessage("Unknown host: " + hostname);
                printMessage("             " + e.getMessage());
        } catch (IOException e) {
                printMessage("IO Error: Error establishing communication with server.");
                printMessage("          " + e.getMessage());
        }
    }

    private void registerPlayer(Color color, String name)  {
       if (connection.out != null ){
           JoinMessage message = new JoinMessage(name,color);
           connection.transmitMessage(message);
       } else {
           debug.println(3, "No server connection has been established, registerPlayer failed");
       }
    }
// A connection to handle incomming communcation from the server
class Connection extends Thread {
    Socket socket;
    ObjectOutputStream out;
    ObjectInputStream in;
    boolean done;
    //Constructor
    public Connection(Socket socket){
        this.socket=socket;
        done=false;
    }
    public void run( ) {
           try {
               //First make the streams to get and send information to and from the client via this thread
               out=new ObjectOutputStream(socket.getOutputStream());
               in=new ObjectInputStream(socket.getInputStream());
               while(!done){
                    Object message = in.readObject();
                    //if the message is null, that mean the stream is done
                    if(message==null){
                        debug.println(1, "Line terminated. Ending connection.");
                        done=true;
                    }
                    //otherwise, the stream is NOT finished so we can process the message
                    else{
                        processMessage(message);
                    }
               }
          }
          catch (ClassNotFoundException e) {
                debug.println(1, " Coding Error: Server transmitted unrecognized Object");
            }
          catch (IOException e) {
                printMessage("IO Error: Error establishing communication with server.");
                printMessage("          " + e.getMessage());
            }
          try {
            //Close the socket
            printMessage("Client is closing down");
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
             }
          catch (IOException e) {
            printMessage("Error trying to close the socket." +e.getMessage());
        }
    }
        private void processMessage(Object message) {
            debug.println(3, "[ Connection ] Processing line: "  + message);
            // protocol for the server passing the playerID to client
            //see if the received message is an instance of one of our several message types
            if(message instanceof JoinResponseMessage) {
                processJoinResponseMessage((JoinResponseMessage) message);
            }
            else if(message instanceof GameState){
                processGameStateMessage((GameState) message);
            }
            else if(message instanceof StringMessage){
                //processStringMessage method
            }
            else debug.println(5, "Incoming message not recognized by any message instance");

        }
        //process an incoming game state
        private void processGameStateMessage(GameState state){
            gameState=state;
            debug.println(3, "Successfully processed a gameStateMessage.");
            state.display(debug.getStream());
        }
        private void processJoinResponseMessage(JoinResponseMessage message) {
            //name = message.name;
            playerID = message.playerID;
            debug.println(3, "Successfully processed a JoinResponseMessage, player " + name + " is registered with ID: " + playerID);
        }
        public void transmitMessage(Object message)  {
            try {
                synchronized (out) {
                    out.writeObject(message);
                    out.flush();
                }
            }
            catch(IOException e) {
                debug.println(3, "Error");
            }
        }
    }
    // **End of the Connection class**

    private void printMessage( String message) {
        debug.println(3, "[Connection]: "+message);
    }


    public class VisPanel extends JPanel {
        Graphics2D g2;
        double viewportSize = 100.0;

        public VisPanel() {
            setPreferredSize(new Dimension(1000,1000) ); // Set size of drawing area, in pixels.
            MouseInputAdapter mouseInputAdapter = new MouseInputAdapter() {
                    public void mouseDragged(MouseEvent e) {
                        Point p = e.getPoint();  // Get point relative to this component
                        //debug.println(5, "App.VP: Mouse dragged to position " + p);
                        updateDirection(p);
                    }

                    public void mouseMoved(MouseEvent e) {
                        Point p = e.getPoint();  // Get point relative to this component
                        //debug.println(5, "App.VP: Mouse moved to position " + p);
                        updateDirection(p);
                    }

                    public void mousePressed(MouseEvent e) {
                         // If this mouse is pressed (held), then speed up
                         GameState gameState = new GameState();
                         debug.println(3, "App.vP.mIA: Mouse pressed.  Feature Pending!");
                         updateSpeed(2*GameState.MIN_SPEED);

                     }

                     public void mouseReleased(MouseEvent e) {
                         // If this mouse is released, go to normal speed
                         GameState gameState = new GameState();
                         debug.println(3, "App.vP.mIA: Mouse released.  Feature Pending!");
                         updateSpeed(GameState.MIN_SPEED);
                     }

                    private void updateDirection(Point p) {
                        if (playerID == -1) return;  // No player to update
                        double centerX = getWidth()/2.0;
                        double centerY = getHeight()/2.0;
                        double playerDX = p.x - centerX;
                        double playerDY = centerY - p.y;
                        if(gameEngine != null){
                            gameEngine.setPlayerDirection(playerID, playerDX, playerDY);
                        }
                        else{
                            //transmit movement message to the server (if playing online)
                            connection.transmitMessage(new MovePlayerMessage(playerDX, playerDY));
                        }
                    }

                    private void updateSpeed(double s) {
                        if (playerID == -1) return;  // No player to update
                        //if offline
                        if(gameEngine!=null){
                            gameEngine.setPlayerSpeed(playerID, s);
                        }
                        else{
                            //transmit a speed update message to server
                            connection.transmitMessage(new BoostPlayerMessage(s));
                        }

                    }
                };
            addMouseMotionListener(mouseInputAdapter);
            addMouseListener(mouseInputAdapter);
            App.this.addMouseMotionListener(mouseInputAdapter);
            App.this.addMouseListener(mouseInputAdapter);
        }

        /* Used for drawing the network */
        protected void paintComponent(Graphics g) {
            g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new Color(200, 200, 220));
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (gameEngine != null) {
                GameState gameState = gameEngine.getGameState();
            }

            // Compute the dimensions of the world
            if (gameState == null) return;  // Nothing to draw yet anyway

            // if(gameState != null)return;
            Rectangle2D.Double bounds = null;
            //was if(playerID==-1)
            if (playerID == -1 || gameState.getPlayers().size()<=playerID) {
                //default (unzoomed) bounding box
                bounds = new Rectangle2D.Double(0, 0, gameState.maxX, gameState.maxY);
            } else {
                // Get some nice bounds around the player's cells
                bounds = gameState.getBoundingBox(playerID);

                // Add a little buffer (5% of width/height) on all sides
                // Or increase size to fit a minimum
                double bufferX = bounds.width*0.05;
                if (bounds.width*1.1 < MIN_WIDTH) {
                    // Too small even with buffer
                    bufferX = (MIN_WIDTH - bounds.width)*0.5;
                }
                double bufferY = bounds.height*0.05;
                if (bounds.height*1.1 < MIN_HEIGHT) {
                    // Too small even with buffer
                    bufferY = (MIN_HEIGHT - bounds.height)*0.5;
                }
                bounds.x -= bufferX;
                bounds.y -= bufferY;
                bounds.width += bufferX*2;
                bounds.height += bufferY*2;
                // To Do: Should keep it within the 0,0 to maxX, maxY range as well!
            }
            setupViewport(bounds.x, bounds.x + bounds.width, bounds.y, bounds.y + bounds.height);

            drawGameState(gameState);
        }

        /**
         * Set up the viewport so dimensions of window are in range (left to right) and (bottom to top)
         * ... roughly, aspect ratio is still preserved.
         * @param left left edge of drawing area
         * @param right right edge of drawing area
         * @param bottom bottom edge of drawing area
         * @param top top edge of drawing area
         */
        private void setupViewport(double left, double right, double bottom, double top) {
            // Get width and height in pixels of panel.
            int width = getWidth();
            int height = getHeight();

            // Correct viewport dimensions to preserve aspect ratio
            double panelAspect = Math.abs((double)height / width);
            double viewAspect = Math.abs(( bottom-top ) / ( right-left ));
            if (panelAspect > viewAspect) {
                // Expand the viewport vertically.
                double padding = (bottom-top)*(panelAspect/viewAspect - 1)/2;
                bottom += padding;
                top -= padding;
            }
            else {
                // Expand the viewport horizontally
                double padding = (right-left)*(viewAspect/panelAspect - 1)/2;
                right += padding;
                left -= padding;
            }

            g2.scale(width/(right-left), height/(bottom-top));
            g2.translate(-left, -top);
        }

        private void drawGameState(GameState gameState) {
            if (gameState == null) return;   // No game to display yet!

            if (cellFont == null) {
                // Create the cell font
                cellFont = new Font("Serif", Font.BOLD, 18);
            }
            g2.setColor(Color.BLACK);
            g2.drawRect(-1, -1, (int)gameState.maxX+2, (int)gameState.maxY+2);

            // Not sure if it changes as screen size changes for example. So getting it each redisplay
            cellFontMetrics = g2.getFontMetrics(cellFont);

            // Draw the snacks first
            drawSnacks(gameState);

            // Iterate through all of the players and all of the cells in the game
            // Again, not done super efficiently - could crop ones that are not visible!
            ArrayList<GameState.Player> player = gameState.getPlayers();
            for (GameState.Player p: player) {
                drawPlayer(p);
            }
        }

        // Draw the cells for this player
        private void drawPlayer(GameState.Player p) {
            Deque<GameState.Cell> cell = p.getCells();
            String name = p.getName();
            Color appearance = p.getAppearance();
            for (GameState.Cell c: cell) {
                drawCell(c.x, c.y, c.r, null, appearance);
            }
            GameState.Cell head = cell.peekFirst();
            drawCell(head.x, head.y, head.r, name, appearance);
        }

        // Draw the cells for the snacks
         private void drawSnacks(GameState gameState) {
             ArrayList<GameState.Cell> snacks = gameState.getSnacks();
             for (GameState.Cell s: snacks) {
                 drawCell(s.x, s.y, s.r, null, gameState.snackColor);
             }
         }

        // Could make it configurable but why...
        private Font cellFont = null;
        private FontMetrics cellFontMetrics = null;

        private void drawCell(double cx, double cy, double radius, String text, Color color) {
            AffineTransform cs = g2.getTransform();
            g2.translate(cx, cy);   // Make cx,cy the center... easier to think about.

            g2.setPaint(color);
            double diam = radius*2;
            Ellipse2D circ = new Ellipse2D.Double(-radius, -radius, diam, diam);
            g2.fill(circ);

            if (text != null) {
                // Compute the starting position of text so it is centered at cx,cy
                // Determine the X coordinate for the text
                // double x = cx - metrics.stringWidth(text) / 2.0;
                // double y = cy + (metrics.getHeight()-metrics.getLeading()) / 2.0
                Rectangle2D rec = cellFontMetrics.getStringBounds(text, g2);
                double x = -rec.getCenterX();
                double y = -rec.getCenterY();  // Adding since y increases downward but still drops upward
                double scaleFactor = radius*1.8/rec.getWidth();
                g2.scale(scaleFactor, -scaleFactor);

                // Set the font and draw String
                g2.setColor(Color.BLACK);
                g2.setFont(cellFont);
                g2.drawString(text, (float) x, (float) y);
            }

            g2.setTransform(cs);
        }
    }

    private class TextStreamer extends OutputStream {
        JTextArea txt;
        StringBuilder buffer;
        static final int MAX_LENGTH = 100000;   // Maximum length of text area (in characters)
        public TextStreamer(JTextArea txt) {
            this.txt = txt;
            this.buffer = new StringBuilder(256);
        }

        @Override
        public void write(int b) throws IOException {
            char c = (char) b;
            this.buffer.append(c);
            if (c == '\n') {
                // Flush the buffer
                flush();
            }
        }

        @Override
        public void flush() throws IOException {
            if (this.buffer.length() > 0) {
                // Transfer buffer to the TextArea, clear buffer, truncate if needed, and move caret
                this.txt.append(this.buffer.toString());
                this.buffer.setLength(0);  // Clear the buffer
                String fullText = this.txt.getText();
                int fullLen = fullText.length();
                if (fullLen > MAX_LENGTH) {
                    // Truncate (trim about half the text)
                    String trunc = "..." + fullText.substring(fullLen - MAX_LENGTH/2);
                    this.txt.setText(trunc);
                    fullLen = trunc.length();
                }
                this.txt.setCaretPosition(fullLen);
            }
        }
    }
}
