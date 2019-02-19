import java.io.BufferedReader;
import java.lang.UnsupportedOperationException;

public class Levenshtein {
    private static final int LONGEST_WORD_LENGTH = 45;

    private static int[][] distances = new int[LONGEST_WORD_LENGTH + 1][LONGEST_WORD_LENGTH + 1];

    public static int getLD(String wordA, String wordB) {
        return getLD(wordA, wordB, 1, wordA.length(), wordB.length());
    }

    public static int getLD(String word1, String word2, int startAt, int w1len, int w2len) {
        if(w1len == 0) 
            return w2len;
        
        if(w2len == 0) 
            return w1len;
        
        for(int i = 0; i <= w1len; i++) 
            distances[i][0] = i;
        
        for(int k = startAt; k <= w2len; k++) 
            distances[0][k] = k;
  
        for(int i = 1; i <= w1len; i++) {
            
            for(int k = startAt; k <= w2len; k++) {
                distances[i][k] = Math.min(
                        Math.min(
                                distances[i-1][k], 
                                distances[i][k-1]
                        ) + 1,
                        distances[i-1][k-1] + (word1.charAt(i - 1) == word2.charAt(k - 1) ? 0 : 1)
                );
  
            }
        }
  
        return distances[w1len][w2len];
  }
}