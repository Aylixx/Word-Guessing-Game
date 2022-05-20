/*
 * Authors: Stefan Stojsic, Colton Aylix
 *
 * Client.java
 *
 * The purpose of this module is to act as the client side of a java RMI implemented phrase guessing game.
 * This module uses the localhost to connect to the network. Once rmiregistry and the server are running, the client
 * module asks the user to type in a username, this is to keep track of multiple players at one time. The User will then receive a prompt
 * asking them to type "start" followed by two integers representing number of words and number of tries respectively (start i f).
 * The user may also decide to quit the game at any time by typing '.' or to simply restart the current game by typing '*'.
 * This module then invokes methods from GameHandler remotely to handle the user input.
 *
 */
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class Client {

    public static void main(String[] args) throws Exception {

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Welcome to Phrase Guessing. Enter the username:");
        String player = inFromUser.readLine();

        String host = "localhost";

        String userInput = "";
        String serverResponse;
        String[] checkResponse;
        PhraseGuessingGameServer remote;
        boolean playingGame = true;
        boolean gameStarted = false;

        try {
            remote = (PhraseGuessingGameServer) Naming.lookup("rmi://" + host + "/PhraseGuessingGameServer");

            while (!remote.initializePlayer(player)) {
                System.out.println("Username exists. Choose a different username:");
                player = inFromUser.readLine();
            }

            System.out.println("CONNECTED TO THE GAME SERVER\n" + '\n');
            System.out.println("TO START A GAME ENTER: 'start i f'\n" + "i - number of words to guess\n"
                    + "f - number of tries factor\n" + "type * to start a new game at any time\n"
                    + "type . to exit the game session\n");

            while (playingGame == true) {

                System.out.print("FROM USER: ");

                userInput = "";

                while (!userInput.equals("*")) { // '*' indicates that the game should restart

                    userInput = inFromUser.readLine();
                    String[] clientMessage = userInput.split("\\s+");

                    // GAME TERMINATION REQUESTED
                    if (userInput.equals(".")) {
                        serverResponse = remote.endGame(player);
                        serverMessage(serverResponse);
                        playingGame = false;
                        userInput = "*";
                    }
                    // START OF NEW GAME
                    else if (!gameStarted) {
                        if (validateInput(clientMessage)) {
                            serverResponse = remote.startGame(player, clientMessage);
                            serverMessageWithReply(serverResponse);
                            gameStarted = true;
                        } else {
                            System.out.println("Error. Follow the format 'start i f' to start the game. \n");
                            userInput = "*";
                            gameStarted = false;
                        }
                    }
                    // GAME RESTART REQUESTED
                    else if (userInput.equals("*")) {
                        serverResponse = remote.restartGame(player);
                        serverMessage(serverResponse);
                        gameStarted = false;
                    }
                    // GUESSED ONE LETTER
                    else if (userInput.length() == 1 && userInput != "*") {
                        serverResponse = remote.guessLetter(player, userInput);
                        checkResponse = serverResponse.split("\\s+");
                        // Checks if the client won or lost, and restarts the game if they have.
                        if (checkResponse[0].equals("Winner!") || checkResponse[0].equals("Lost")) {
                            userInput = "*";
                            gameStarted = false;
                            serverMessage(serverResponse);
                        } else
                            serverMessageWithReply(serverResponse);
                    }
                    // GUESSED WHOLE PHRASE
                    else {
                        serverResponse = remote.guessPhrase(player, userInput);
                        checkResponse = serverResponse.split("\\s+");
                        // Checks if the client won or lost, and restarts the game if they have.
                        if (checkResponse[0].equals("Winner!") || checkResponse[0].equals("Lost")) {
                            userInput = "*";
                            gameStarted = false;
                            serverMessage(serverResponse);
                        } else
                            serverMessageWithReply(serverResponse);
                    }
                }
            }
        } catch (RemoteException re) {
            System.out.println("Connection could not be established. Try again.\n");
            System.exit(1);
        } catch (NotBoundException nbe) {
            System.out.println("Connection could not be established. Try again.\n");
            System.exit(1);
        }
    }

    /**************************************************************************
     * serverMessage
     *
     * Just a prompt to remove duplicate code.
     **************************************************************************/
    public static void serverMessage(String response) {
        System.out.println("FROM SERVER: " + response);
        System.out.println("Start a new game by typing start i f \n");
    }

    /**************************************************************************
     * serverMessageWithReply
     *
     * Just a prompt to remove duplicate code. Includes a reply from user section.
     **************************************************************************/
    public static void serverMessageWithReply(String response) {
        System.out.println("FROM SERVER: " + response + "\n");
        System.out.print("FROM USER: ");
    }

    /**************************************************************************
     * validateInput
     *
     * checks if the initial client input is of format 'start i f' as
     * described earlier. Returns true if the format is correct.
     **************************************************************************/
    public static boolean validateInput(String[] in) {

        try {
            if (in.length == 3)
                if (in[0].equals("start") && Integer.parseInt(in[1]) > 0 && Integer.parseInt(in[2]) > 0)
                    return true;
        } catch (Exception e) {
        }
        return false;
    }

}