/***************
 * BoostPlayerMessage
 * Editors: Jack Zemlanicky, Harrison Dominique, Dylan Irwin, Henok Ketela, Bryan Sullivan
 * Spring 21: CSC340
 * This is the Boost Player message
 *Used to transmit a boost message.
 ***************/

import java.awt.Color;
import java.io.Serializable;


public class BoostPlayerMessage extends Message implements Serializable {
    double speed;
    public BoostPlayerMessage(double speed) {
        this.speed = speed;
        //need to implement sending a boost message (when holding down space bar/click)
    }

}