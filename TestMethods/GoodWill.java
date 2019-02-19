import java.io.BufferedReader;
import java.lang.UnsupportedOperationException;
import java.util.List;

public class GoodWill extends TestMethod {
    /*
        Good will span comparison method
        Time complexity: n * k
    */

    private int comparisonSpan;
    private double sumLD;
    private int tolerance;

    public GoodWill(int comparisonSpan, int tolerance) {
        super();
        this.comparisonSpan = comparisonSpan;
        this.tolerance = tolerance;
        sumLD = 0;
    }

    @Override
    public double rate(List<String> words1, List<String> words2){
        int mutualMax = Math.min(words1.size(), words2.size());

        for(int p1 = 0; p1 < mutualMax; p1++){
            int start = Math.max(0, p1 - comparisonSpan/2),
                stop = Math.min(mutualMax, p1 + comparisonSpan/2);
            
            double minLD = Integer.MAX_VALUE;
            for(int p2 = start; p2 < stop; p2++){       
                String w1 = words1.get(p1).toLowerCase(), 
                       w2 = words2.get(p2).toLowerCase();
                
                int distance = Levenshtein.getLD(w1, w2); 

                if(distance == 0.0 || synonyms(w1, w2)) {
                    minLD = 0.0;
                    break;
                }
                else if (distance < minLD) {
                    minLD = distance; 
                    // TODO: One design decision is whether the length of the words should matter: 
                    //       Math.min(w1.length(), w2.length());
                    continue;
                }
            }

            sumLD += minLD;
        }

        return Math.min(tolerance / sumLD, 1.0);
    }

    @Override
    public String toString(){
        return "GOOD WILL (" + comparisonSpan + ", " + tolerance + ")";
    }
}