import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.util.*;
import javax.management.timer.Timer;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Stream;
import java.nio.file.Paths;
import java.nio.file.Files;

class Benchmark {
    public static final double AGREEABLENESS_SIGNIFICANCE = 1.0;
    public static final double PERFORMANCE_SIGNIFICANCE = 1.0;
    public static final String ENCODING = "UTF-8";
    public static final String DELIMTER = " ";

    private static final long MS_TO_NS = 1000000;

    public static void main(String[] args) {  
        // ARGUMENTS 

        String filePath1, filePath2;   
        double humanRating;
                
        try { 
            filePath1 = args[0];
            filePath2 = args[1];
            humanRating = Double.parseDouble(args[2]);
        }
        catch(Exception ex){
            System.out.println("[ERROR] Argument requirements not satisfied: " + ex.getMessage());
            return;
        }

        // FILE READING 
        
        List<String> words1, words2;
        try {
            words1 = Arrays.asList(new String(Files.readAllBytes(Paths.get(filePath1)), ENCODING).split(DELIMTER));
            words2 = Arrays.asList(new String(Files.readAllBytes(Paths.get(filePath2)), ENCODING).split(DELIMTER));   
        }
        catch(Exception ex){
            System.out.println("[ERROR] " + ex.getMessage());
            return;
        }

        // BENCHMARK 

        ArrayList<TestMethod> rodents = new ArrayList<TestMethod>(); 
        
        long t1, t2;

        for(TestMethod rodent : rodents) {
            // Test of synonym implementation:
            t1 = System.nanoTime();
            double rating = rodent.rate(words1, words2);
            t2 = System.nanoTime();
            long msElapsed = (t2 - t1) / MS_TO_NS;
            
            double result = AGREEABLENESS_SIGNIFICANCE * Math.abs(rating - humanRating) + PERFORMANCE_SIGNIFICANCE * msElapsed;
            System.out.println(rodent.toString() + result);
        }

        System.out.println("[INFO] " + rodents.size() + " tests completed!");
    }
}