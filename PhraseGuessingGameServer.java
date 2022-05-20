/*
 * Authors: Stefan Stojsic, Colton Aylix
 *
 * PhraseGuessingGameServer.java
 *
 * This is the remote interface that declares a set of remote methods that will be accessed by the Client.
 */
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PhraseGuessingGameServer extends Remote {
    public String startGame(String player, String[] clientMessage) throws RemoteException;

    public String guessLetter(String player, String letter) throws RemoteException;

    public String guessPhrase(String player, String phrase) throws RemoteException;

    public String endGame(String player) throws RemoteException;

    public String restartGame(String player) throws RemoteException;

    public boolean initializePlayer(String player) throws RemoteException;
}