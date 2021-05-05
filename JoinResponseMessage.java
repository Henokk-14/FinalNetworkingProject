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