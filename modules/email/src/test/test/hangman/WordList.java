package test.hangman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * List of words.
 *
 * @author Kohsuke Kawaguchi
 */
public final class WordList {

    public static final List<String> words;

    /**
     * Picks up a random word.
     */
    public static String getRandomWord() {
        int index = new Random().nextInt(words.size());
        String s = words.get(index);
        // interleave with ' ' for visibility
        StringBuilder sb = new StringBuilder();
        for( int i=0; i<s.length(); i++ ) {
            if(i!=0)
                sb.append(' ');
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    static {
        try {
            List<String> list = new ArrayList<String>();

            BufferedReader in = new BufferedReader(new InputStreamReader(
                WordList.class.getResourceAsStream("words.txt")));
            String line;
            while((line=in.readLine())!=null) {
                list.add(line);
            }

            words = Collections.unmodifiableList(list);
        } catch (IOException e) {
            throw new Error(e); // treat this as a fatal error
        }
    }
}
