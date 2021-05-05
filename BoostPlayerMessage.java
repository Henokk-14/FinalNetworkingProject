import java.awt.Color;
import java.io.Serializable;

public class BoostPlayerMessage extends Message implements Serializable {
    double playerDX;
    double playerDY;
    public BoostPlayerMessage(double playerDX, double playerDY) {
        this.playerDX= playerDX;
        this.playerDY = playerDY;
        //need to implement sending a boost message (when holding down space bar)
    }

}