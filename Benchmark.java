import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.util.*;
import javax.management.timer.Timer;
import java.io.FileReader;
import java.io.IOException;

class Benchmark {

    public static final double K1 = 1.0;
    public static final double K2 = 1.0;
    public static final double K3 = 1.0;

    public static void main(String[] args) throws IOException {  
        // ARGUMENTS 

        String filePath1, filePath2;   
        double humanScore;
        try { 
            filePath1 = args[0];
            filePath2 = args[1];
            humanScore = Double.parseDouble(args[2]);
        }
        catch(Exception ex){
            System.out.println("[ERROR] Argument requirements not satisfied: " + ex.getMessage());
            return;
        }

        // FILE READING 

        BufferedReader bufText1, bufText2;
        try {
            bufText1 = new BufferedReader(new FileReader(filePath1));
            bufText2 = new BufferedReader(new FileReader(filePath2));
        }
        catch(FileNotFoundException ex){
            System.out.println("[ERROR] " + ex.getMessage());
            return;
        }

        // BENCHMARK 

        Levenshtein levenshtein = new Levenshtein();
        ArrayList<TextDiff> synTests = new ArrayList<TextDiff>(); 
        long t1, t2;

        for(TextDiff test : synTests) {
            bufText1.reset();
            bufText2.reset();

            // Edit distance:
            double editDistance = levenshtein.rate(bufText1, bufText2);

            bufText1.reset();
            bufText2.reset();

            // Test of synonym implementation:
            t1 = System.nanoTime();
            double score = test.rate(bufText1, bufText2);
            t2 = System.nanoTime();
            long msElapsed = (t2 - t1) / 1000000;
            
            double result = K1 * editDistance + K2 * Math.abs(humanScore - score) + K3 * msElapsed;
            System.out.println(test.toString() + result);
        }

        bufText1.close();
        bufText2.close();

        System.out.println("[INFO] " + synTests.size() + " tests completed!");
    }
}