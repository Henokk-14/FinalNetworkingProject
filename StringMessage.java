/***************
 * StringMessage
 * Editors: Jack Zemlanicky, Harrison Dominique, Dylan Irwin, Henok Ketela, Bryan Sullivan
 * Spring 21: CSC340
 * This is the String message.
 * Used to transmit a String message.
 * It is not actually used in our game, but it is here for future use in case we expand upon the game.
 ***************/
import java.io.Serializable;
//Simple string message used to pass various messages between server and client
public class StringMessage extends Message implements Serializable {
    String message;

    public StringMessage(String message) {
        this.message = message;


    }
}