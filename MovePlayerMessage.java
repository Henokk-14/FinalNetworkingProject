/***************
 * MovePlayerMessage
 * Editors: Jack Zemlanicky, Harrison Dominique, Dylan Irwin, Henok Ketela, Bryan Sullivan
 * Spring 21: CSC340
 * This is the Move Player message
 * Used to transmit a Move player message.
 ***************/
import java.io.Serializable;

public class MovePlayerMessage extends Message implements Serializable {
    double playerDX;
    double playerDY;
    public MovePlayerMessage(double playerDX, double playerDY) {
        this.playerDX= playerDX;
        this.playerDY = playerDY;

    }

}