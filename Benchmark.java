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
    public static final String TARGET_DIR = "LeePincombeWelsh";
    public static final String ENCODING = "UTF-8";
    public static final String DELIMTER = " ";
    private static final long MS_TO_NS = 1000000;

    static class TestResult {
        double x;
        double y;
        long elalpsedTime;

        public TestResult(double rating, double humanRating, long elapsedTime){
            this.x = rating;
            this.y = humanRating;
            this.elalpsedTime = elapsedTime;
        }
    }

    private static TestResult benchmark(String filePath1, String filePath2, double humanRating, TestMethod rodent) {
        // FILE READING 
        
        Path path1 = Paths.get(filePath1);
        Path path2 = Paths.get(filePath2);

        List<String> words1, words2, tmp;
        try {
            words1 = Arrays.asList(new String(Files.readAllBytes(path1), ENCODING).split(DELIMTER));
            words2 = Arrays.asList(new String(Files.readAllBytes(path2), ENCODING).split(DELIMTER));   

            if(words1.size() < words2.size()){
                // words1 should be the longer text
                tmp = words1;
                words1 = words2;
                words2 = tmp;
            }
        }
        catch(Exception ex){
            System.out.println("[ERROR] " + ex.getMessage());
            return null;
        }

        long t1, t2;

        t1 = System.nanoTime();
        double rating = rodent.rate(words1, words2);
        t2 = System.nanoTime();
        long msElapsed = (t2 - t1) / MS_TO_NS;

        return new TestResult(rating, humanRating, msElapsed);
    } 

    private static double roundUp(double number){
        return Math.round(number * 100.0) / 100.0;
    }

    private static double getPearsonCorrelation(List<TestResult> sample) {
        double meanX = 0.0, meanY = 0.0;
        double xySum = 0.0, xSum = 0.0, ySum = 0.0;

        for(TestResult point : sample){
            meanX += point.x;
            meanY += point.y;
        }
        meanX /= sample.size();
        meanY /= sample.size();

        for(TestResult point : sample){
            xySum += (point.x - meanX) * (point.y - meanY);
            xSum += Math.pow(point.x - meanX, 2);
            ySum += Math.pow(point.y - meanY, 2);
        }

        return xySum / (Math.sqrt(xSum) * Math.sqrt(ySum));
    }

    public static void main(String[] args) {          
        TestMethod rodent = new SSR(Paths.get("SSR/index"));
        List<TestResult> results = new ArrayList<TestResult>();

        // ARGUMENTS 
        List<List<String>> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(TARGET_DIR + "/LeePincombeWelshData.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] cellValues = line.split(",");
                lines.add(Arrays.asList(cellValues));
            }
        }
        catch(Exception e) {
            System.out.println("[ERROR] Could not read csv-file!");
        }

        Object[][] table = new Object[lines.size() + 1][4];
        table[0] = new Object[] {"DOCUMENT PERMUTATION", "ALGORITHMIC OPINION", "HUMAN OPINION", "TIME"};

        int row = 0;
        for(List<String> cells : lines) {
            String filePath1 = TARGET_DIR + "/LeePincombeWelshDocuments_" + cells.get(1) + ".txt";
            String filePath2 = TARGET_DIR + "/LeePincombeWelshDocuments_" + cells.get(2) + ".txt";
            double humanRating;

            try {
                humanRating = Double.parseDouble(cells.get(3)) / 5.0;
            }
            catch(NumberFormatException e) {
                continue;
            }           

            TestResult result = benchmark(filePath1, filePath2, humanRating, rodent);
            results.add(result);

            table[++row] = new Object[] { 
                String.format("%67s", Paths.get(filePath1).getFileName() + " | " + Paths.get(filePath2).getFileName()), 
                roundUp(result.x), 
                roundUp(result.y),
                result.elalpsedTime + "ms"
            };
        }

        final int tableCellSize = 50;
        String tableFormat = "%" + tableCellSize + "s%" + tableCellSize + "s%" + tableCellSize + "s%" + tableCellSize + "s\n"; 
        System.out.print("\033[1;6m");
        System.out.format(tableFormat, table[0]);
        System.out.print("\033[0m");
        System.out.println("");

        for(int i = 1; i < lines.size(); i++){
            System.out.format(tableFormat, table[i]);
        }

        System.out.print("\033[1;6m");
        System.out.println("\nPEARSON CORRELATION = " + roundUp(getPearsonCorrelation(results)));
        System.out.print("\033[0m");

        System.out.print("\033[0m");
        System.out.println("\n[INFO] " + lines.size() + " tests completed!\n");
    }
}
