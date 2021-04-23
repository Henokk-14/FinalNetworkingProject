import java.io.Serializable;
import java.awt.Color;
//Server send to the client ID and name
public class JoinResponseMessage extends Message implements Serializable {
    String name;
    int playerID;
    public JoinResponseMessage(String name, int id) {

        this.name = name;
        this.playerID = id;

    }

}