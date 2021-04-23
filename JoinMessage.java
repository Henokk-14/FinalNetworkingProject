import java.awt.Color;
import java.io.Serializable;

public class JoinMessage extends Message implements Serializable {
    String name;
    Color color;
    public JoinMessage(String name, Color color) {

        this.name = name;
        this.color = color;

    }

}