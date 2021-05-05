import java.awt.Color;
import java.io.Serializable;

public class MovePlayerMessage extends Message implements Serializable {
    double playerDX;
    double playerDY;
    public MovePlayerMessage(double playerDX, double playerDY) {
        this.playerDX= playerDX;
        this.playerDY = playerDY;

    }

}