/*
 * Authors: Stefan Stojsic, Colton Aylix
 *
 * Pair.java
 *
 * Pair is a class used to return two values from a function. We use it for checkLetter function in the
 * GameHandler class. In checkLetter, the return values are the modified hidden phrase string
 * and the decremented number of tries. (Just a helper class)
 */
public class Pair {
private String key;
private int value;

public Pair(String newKey, int newValue) {
this.key = newKey;
this.value = newValue;
}

public String getKey() {
return key;
}

public int getValue() {
return value;
}

}