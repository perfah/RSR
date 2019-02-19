import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.util.*;
import javax.management.timer.Timer;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Stream;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.Path;

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
        
        Path path1 = Paths.get(filePath1);
        Path path2 = Paths.get(filePath2);

        List<String> words1, words2;
        try {
            words1 = Arrays.asList(new String(Files.readAllBytes(path1), ENCODING).split(DELIMTER));
            words2 = Arrays.asList(new String(Files.readAllBytes(path2), ENCODING).split(DELIMTER));   
        }
        catch(Exception ex){
            System.out.println("[ERROR] " + ex.getMessage());
            return;
        }

        // BENCHMARK 

        System.out.println("COMPARING " + path1.getFileName().toString().toUpperCase() + " AND " + path2.getFileName().toString().toUpperCase());
        System.out.println("HUMAN SIMILARITY SCORE: " + humanRating * 100.0 + "%");

        ArrayList<TestMethod> rodents = new ArrayList<TestMethod>(); 
        for(int i = 1; i < 17; i++)
            rodents.add(new GoodWill(i, 10));
        
        final int tableCellSize = 40;
        String tableFormat = "%" + tableCellSize + "s%" + tableCellSize + "s%" + tableCellSize + "s%" + tableCellSize + "s\n"; 
        Object[][] table = new Object[rodents.size() + 1][4];
        table[0] = new Object[] { " TEST", "SIMILARITY SCORE [%]", "TIME [MS]", "OVERALL"};
        
        int row = 0;
        long t1, t2;
        for(TestMethod rodent : rodents) {
            // Test of synonym implementation:
            t1 = System.nanoTime();
            double rating = rodent.rate(words1, words2);
            t2 = System.nanoTime();
            long msElapsed = (t2 - t1) / MS_TO_NS;
            //rodent.closeDictionary();
            
            double result = AGREEABLENESS_SIGNIFICANCE * Math.abs(rating - humanRating) + PERFORMANCE_SIGNIFICANCE * msElapsed;

            table[++row] = new Object[] { 
                rodent.toString(), 
                Math.round(rating * 100.0 * 10.0) / 10.0, 
                msElapsed, 
                Math.round(result * 10.0) / 10.0
            };
        }

        System.out.format(tableFormat, table[0]);
        System.out.println("");

        for(int i = 1; i <= row; i++)
            System.out.format(tableFormat, table[i]);

        System.out.println("\n[INFO] " + rodents.size() + " tests completed!");
    }
}