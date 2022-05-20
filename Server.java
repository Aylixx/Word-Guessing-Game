/*
 * Authors: Stefan Stojsic, Colton Aylix
 *
 * Server.java
 *
 * The Server class contains the main method which initializes the RMI registry and then creates a new GameHandler
 * for remote object implementation and then binds that instance to a name in the Java RMI registry which is the
 * PhraseGuessingGameServer.
 */

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class Server {

    public static void main(String args[]) {
        try {
            try {
                LocateRegistry.getRegistry(1099).list();
            } catch (RemoteException e) {
                LocateRegistry.createRegistry(1099);
            }
            System.out.println("Server ON. Waiting for the client...");
            GameHandler local = new GameHandler();
            Naming.rebind("rmi:///PhraseGuessingGameServer", local);
        } catch (RemoteException re) {
            re.printStackTrace();
        } catch (MalformedURLException mfe) {
            mfe.printStackTrace();
        }
    }
}