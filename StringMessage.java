/***************
 * StringMessage
 * Editors: Jack Zemlanicky, Harrison Dominique, Dylan Irwin, Henok Ketela, Bryan Sullivan
 * Spring 21: CSC340
 * This is the String message.
 *Used to transmit a String message.
 ***************/
import java.io.Serializable;
import java.awt.Color;
//Simple string message used to pass various messages between server and client
public class StringMessage extends Message implements Serializable {
    String message;

    public StringMessage(String message) {
        this.message = message;


    }
}