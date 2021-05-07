/***************
 * JoinrMessage
 * Editors: Jack Zemlanicky, Harrison Dominique, Dylan Irwin, Henok Ketela, Bryan Sullivan
 * Spring 21: CSC340
 * This is the Join Player message
 * Used to transmit a join message.
 ***************/

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