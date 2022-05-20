/*
 * Authors: Stefan Stojsic, Colton Aylix
 *
 * GameHandler.java
 *
 * This module implements the remote interface to interpret user input from the client module for the phrase guessing game. This module also
 * has several helper methods to aid the remotely called methods. The server loads a list of words from a text file
 * words.txt, and based on clients number of words chosen, it generates a hidden phrase to be guessed by the user.
 * As the user makes guesses, letters will be revealed, if they were guessed right. The user can keep guessing
 * until all the letters are revealed or they run out of tries. The number of tries are displayed to the user
 * beside the hidden phrase, as well as the total score which keeps track of how many games have been won. If
 * the user loses, however, the total score is decremented by one. The client can also guess the entire phrase,
 * if they guess correctly they win that round, but it has to match entirely or the number of tries decrements by one.
 * Fields such as, number of tries, random phrase/hidden phrase, and total score are all stored in a player class,
 * so that these fields can remain separate and unaffected by other players.
 *
 */

import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

@SuppressWarnings("serial")
public class GameHandler extends UnicastRemoteObject implements PhraseGuessingGameServer {

    public class Player {
        private String randomPhrase;
        private String hiddenPhrase;
        private int numberOfTries;
        private int totalScore;

        public Player(String player) {
            this.randomPhrase = "";
            this.hiddenPhrase = "";
            this.numberOfTries = 0;
            this.totalScore = 0;
        }
    }

    private Map<String, Player> gameplayer = new HashMap<>();
    private List<String> words;

    public GameHandler() throws RemoteException {
        super();
        words = loadWords();
    }

    /**************************************************************************
    * initializePlayer
    *
    * This method receives a user requested username and checks to see if it can be used to create a new player.
    * Returns true if the player does not already exist and a new player is created.
    * Returns false if a player of that name already exists.
    *
    **************************************************************************/
    @Override
    public synchronized boolean initializePlayer(String player) throws RemoteException {
        if (!gameplayer.containsKey(player)) {
            gameplayer.put(player, new Player(player));
            System.out.println("Player " + player + " joined the server.");
            return true;
        } else
            return false;
    }

    /**************************************************************************
     * startGame
     *
     * This method receives the players username as well as the user input "start i f" to determine
     * how many words and guesses the user will get. The number of words and attempts are then multiplied
     * together to determine the total number of guesses. The method than calls on two helper functions,
     * one to create a random phrase with the desired number of words and one that hides the letters of
     * the phrase. The hidden phrase, number of tries and total score is then returned to the Client
     * module as one single String.
     **************************************************************************/
    @Override
    public synchronized String startGame(String player, String[] clientMessage) throws RemoteException {

        int numberOfWords = Integer.parseInt(clientMessage[1]);
        int attemptsPerWord = Integer.parseInt(clientMessage[2]);
        int numofTries = gameplayer.get(player).numberOfTries = numberOfWords * attemptsPerWord;

        // generate specified number of randomly chosen words into a single string
        gameplayer.get(player).randomPhrase = getRandomWords(words, numberOfWords);
        System.out.println("The phrase for " + player + " is: " + gameplayer.get(player).randomPhrase + '\n');

        // generates a string of dashes corresponding to the randomPhrase
        String hidden = gameplayer.get(player).hiddenPhrase = hideWords(gameplayer.get(player).randomPhrase);

        int score = gameplayer.get(player).totalScore;
        return (hidden + " Tries: " + numofTries + " Total Score: " + score + '\n');
    }

    /**************************************************************************
     * guessLetter
     *
     * This method receives the player name and the letter they guessed. It uses the helper method checkLetter
     * to determine whether the guessed letter was in the random phrase. It also keeps track of the number of tries;
     * if the number of tries hit zero because they did not guess a correct letter, a String is returned that informs
     * the user they have lost and shows the correct phrase and their current score. If all the hidden letters have
     * been revealed, then the hidden phrase will match the random phrase and a victory message will be returned to
     * the Client module including their score. If the game is not over, the hidden phrase will be returned
     * with the number of tries remaining and the total score thus far.
     **************************************************************************/
    @Override
    public synchronized String guessLetter(String player, String letter) throws RemoteException {
        int score;
        Pair guess = checkLetter(letter, gameplayer.get(player).randomPhrase, gameplayer.get(player).hiddenPhrase,
                gameplayer.get(player).numberOfTries);

        gameplayer.get(player).hiddenPhrase = guess.getKey();
        int tries = gameplayer.get(player).numberOfTries = guess.getValue();

        // if the client is out of attempts, the round is lost.
        if (tries == 0) {
            gameplayer.get(player).totalScore--;
            score = gameplayer.get(player).totalScore;

            return ("Lost this round! The phrase was: " + gameplayer.get(player).randomPhrase + " Total Score: " + score
                    + '\n');
        }
        // if the client completed the phrase, the round is won.
        else if (gameplayer.get(player).hiddenPhrase.equals(gameplayer.get(player).randomPhrase)) {
            gameplayer.get(player).totalScore++;
            score = gameplayer.get(player).totalScore;

            return ("Winner! The phrase was: " + gameplayer.get(player).randomPhrase + " Total Score: " + score + '\n');
        }
        // Client has neither lost nor won, game continues.
        else {
            score = gameplayer.get(player).totalScore;
            return (gameplayer.get(player).hiddenPhrase + " Tries: " + tries + " Total Score: " + score + '\n');
        }
    }

    /**************************************************************************
     * gussPhrase
     *
     * This method receives the guessed phrase and relies on the helper method checkPhrase, which compares the users guess with the
     * random phrase, if they match then the client wins and a victory message is returned to the Client
     * module along with the total score. If they did not match, this method checks to see if the player
     * has any more tries left, if not, a losing message is returned. If they do have more than one attempt
     * the number of tries is decremented by one and the hidden phrase is returned along with the remaining attempts
     * and total score.
     **************************************************************************/
    @Override
    public synchronized String guessPhrase(String player, String phrase) throws RemoteException {

        String result;
        int score;

        // if client guessed correctly
        if (checkPhrase(phrase, gameplayer.get(player).randomPhrase)) {
            gameplayer.get(player).totalScore++;
            score = gameplayer.get(player).totalScore;

            result = "Winner! The phrase was: " + gameplayer.get(player).randomPhrase + " Total Score: " + score + '\n';

            gameplayer.get(player).randomPhrase = "";

            // if client guessed wrong, checks if there are any tries left
            // so that if client guessed wrong with only one try left
            // it does the "game over" process
        } else {
            if (gameplayer.get(player).numberOfTries > 1) {
                gameplayer.get(player).numberOfTries--;
                result = gameplayer.get(player).hiddenPhrase + " Tries: " + gameplayer.get(player).numberOfTries
                        + " Total Score: " + gameplayer.get(player).totalScore + '\n';
            } else {
                gameplayer.get(player).totalScore--;

                result = "Lost this round! The phrase was: " + gameplayer.get(player).randomPhrase + " Total Score: "
                        + gameplayer.get(player).totalScore + '\n';
                gameplayer.get(player).randomPhrase = "";
            }
        }
        return result;
    }

    /**************************************************************************
     * endGame
     *
     * This method is to simply end the game, it receives the players name and removes that player. It also
     * returns a "Game ended." message back to the Client.
     **************************************************************************/
    @Override
    public synchronized String endGame(String player) throws RemoteException {
        System.out.println("Player " + player + " ended game.\n");
        gameplayer.remove(player);
        return ("Game ended.\n");
    }

    /**************************************************************************
     * restartGame
     *
     * This method is to simply restart the game, it resets the players score back to zero and sets the
     * random phrase to an empty string. The method then returns a game restarted message back to the Client.
     **************************************************************************/
    @Override
    public synchronized String restartGame(String player) throws RemoteException {
        gameplayer.get(player).totalScore = 0;
        gameplayer.get(player).randomPhrase = "";

        return ("New Game Requested.\n");
    }

    /**************************************************************************
     * checkLetter
     *
     * This method takes the client's single character guess, the generated phrase
     * to guess, the hidden phrase, and the current number of tries left as input
     * parameters. It returns a Pair - basically a (possibly) updated hiddenPhrase
     * if the client guessed correctly, or if the client typed a '*' (in which case
     * the returned string is empty); and possibly decremented number of tries, if
     * the client guessed wrong.
     **************************************************************************/
    public synchronized static Pair checkLetter(String clientGuess, String guessPhrase, String hiddenPhrase,
            int tries) {
        StringBuilder sb = new StringBuilder();
        boolean correct = false;

        if (clientGuess.charAt(0) == '*')
            return new Pair("", 0);

        for (int i = 0; i < guessPhrase.length(); i++) {
            char c = guessPhrase.charAt(i);
            if (c == clientGuess.charAt(0)) {
                sb.append(c);
                correct = true;
            } else {
                sb.append(hiddenPhrase.charAt(i));
            }
        }

        if (!correct)
            tries--;
        hiddenPhrase = sb.toString();

        return new Pair(hiddenPhrase, tries);
    }

    /**************************************************************************
     * checkPhrase
     *
     * This method compares two strings and returns true if they are equal/ false if
     * not equal. Here, this method is used when comparing the whole phrase guessed
     * by the client to the guess phrase, or when comparing if the hidden string had
     * all it's fields are revealed.
     **************************************************************************/
    public static boolean checkPhrase(String clientGuess, String guessPhrase) {
        if (clientGuess.contentEquals(guessPhrase))
            return true;
        else
            return false;
    }


    /**************************************************************************
     * hideWords
     *
     * Takes the guess string and turns it into a char Array, so that it
     * can traverse through it and for each non-whitespace character replace it with
     * a '-'. Returns the "dashed" string.
     **************************************************************************/
    public static String hideWords(String words) {
        StringBuilder sb = new StringBuilder();

        for (char c : words.toCharArray()) {
            if (c == ' ')
                sb.append(' ');
            else
                sb.append("-");
        }
        return sb.toString();
    }

    /**************************************************************************
     * loadWords
     *
     * Opens the words.txt file and adds each word from the file to the
     * list (ArrayList) of words. The words.txt is a file of strings delimited by
     * newline, so the delimiter for splitting the file is the newline character.
     * Returns the loaded word list.
     **************************************************************************/
    public static List<String> loadWords() {
        List<String> wordList = new ArrayList<String>();

        try {
            InputStream file = Server.class.getResourceAsStream("words.txt");
            Scanner sc = new Scanner(file);

            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                String word = line.split("\n")[0];
                wordList.add(word);
            }
            sc.close();

        } catch (NullPointerException e) {
            System.out.println("FILE IO ERROR: " + e.getMessage());
        }

        return wordList;
    }

    /**************************************************************************
     * getRandomWords
     *
     * Takes the word list, and the number of words that the client
     * specified via input. The loop invokes the Random method for n number of words
     * and each word is appended to the string with whitespace inbetween. Returns
     * the randomly generated phrase from the word list.
     **************************************************************************/
    public static String getRandomWords(List<String> wordList, int numberOfWords) {
        int index = 0;
        String randomWords = "";

        for (int i = 0; i < numberOfWords; i++) {
            index = new Random().nextInt(wordList.size());
            randomWords += wordList.get(index) + " ";
        }
        return randomWords.trim().toLowerCase();
    }
}