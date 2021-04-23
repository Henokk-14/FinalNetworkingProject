import java.io.Serializable;
import java.awt.Color;

public class StringMessage extends Message implements Serializable {
    String message;

    public StringMessage(String message) {
        this.message = message;


    }
}