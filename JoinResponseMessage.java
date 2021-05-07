/***************
 * JoinResponseMessage
 * Editors: Jack Zemlanicky, Harrison Dominique, Dylan Irwin, Henok Ketela, Bryan Sullivan
 * Spring 21: CSC340
 * This is the Join Response message
 *Used to transmit a Join Response message.
 ***************/

import java.io.Serializable;
import java.awt.Color;
//Server sends a JoinResponseMessage back to client after client
//requests a player name and color via a JoinMessage
public class JoinResponseMessage extends Message implements Serializable {
    String name;
    int playerID;
    public JoinResponseMessage(String name, int id) {

        this.name = name;
        this.playerID = id;

    }

}