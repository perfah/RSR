import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.io.FileInputStream;
import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.*;
import java.util.Iterator;
import java.lang.*;
import java.util.Optional;

/*
    TODO
    - Word popularity measure or lexicon to account for the purposeless words 

*/

class Index {
    public static final String ENCODING = "UTF-8";
    public static final float MINIMUM_WEIGHT_THRESHOLD = 0.01f;

    public HashMap<Integer, WordEntry> entries;
    private HashMap<String, Integer> accuracy;
    private HashMap<String, Integer> popularity;
    public File resource;
    private float averagePopularity;

    public Index(Path fsLocation){
        entries = new HashMap<Integer, WordEntry>();
        popularity = new HashMap<String, Integer>();
        averagePopularity = 0;

        resource = fsLocation.toFile();
        if(resource.isDirectory()){
            System.out.println("[NOTE] Reusing index at: " + fsLocation.toAbsolutePath());
        }
        else{
            resource.mkdirs();
            System.out.println("[WARNING] Initializing new index at: " + fsLocation.toAbsolutePath());
        }

    }

    public void calibrate(File docPath, int comparisonSpan) {
        String docName = docPath.getName();

        if(docPath.isDirectory())
        {   
            File[] children = docPath.listFiles();
            
            for(File child : children)
                System.out.println("[NOTE] " + "Calibration against document queued: " + child.getName());
            
            for(File child : children)
                calibrate(child, comparisonSpan);
            
            return;
        }

        List<String> bagOfWords;

        try {
            String raw = new String(Files.readAllBytes(docPath.toPath()), ENCODING).replaceAll("[^A-Za-z ]+", " ");
            bagOfWords = Arrays.asList(raw.split(" "));
        }
        catch(Exception ex){
            System.out.println("[ERROR] " + ex.getMessage());
            return;
        }
        
        HashSet<String> wordsLeft = new HashSet<String>(bagOfWords);
        float newAvgPopularity = 0;
        
        // Syntactic sugar
        int wordsProcessed = 0;
        int wordsTotal = wordsLeft.size();
        final int progressBarLen = 50;
        String header = "Progress for '" + docName + "' -> [";
        System.out.print(String.format("%70s", header));
        for(int i = 0; i < progressBarLen; i++)
            System.out.print(" ");
        System.out.print("]");
        System.out.print("\033[" + (progressBarLen+1) + "D");

        for(String word : wordsLeft){
            wordsProcessed++;

            float progress = (float)wordsProcessed / (float)wordsTotal;
            int chunksFilled = (int)((float)progressBarLen * progress);
            
            for(int i = 0; i < chunksFilled; i++)
                System.out.print("#");
            System.out.print("\033[" + (progressBarLen-chunksFilled+1) + "C");
            System.out.print(" " + String.format("%3d", (int)(100f * progress)) + "%");
            System.out.print("\033[" + (progressBarLen+6) + "D");

            int occurrences = 0;
            HashMap<String, Integer> neighboorhood = new HashMap<String, Integer>();
            
            WordEntry x = WordEntry.of(word, this);
            for(int i = 0; i < bagOfWords.size(); i++){
                if(bagOfWords.get(i).equals(word)){
                    x.record();
                    occurrences++;
                    
                    for(int j = Math.max(0, i - comparisonSpan/2); j < Math.min(i + comparisonSpan/2, bagOfWords.size()); j++){
                        String neighboor = bagOfWords.get(j);
                        
                        if(!word.trim().equals("") || !neighboor.equals(word)){
                            WordEntry y = WordEntry.of(neighboor, this);
                            WordEntry.join(x, y);

                            //neighboorhood.put(neighboor, neighboorhood.getOrDefault(neighboor, 0) + 1);
                        }
                    }
                }
            }

            for(String neighboor : neighboorhood.keySet()){

                /*
                String h = hash(word, neighboor);
                if(h == null)
                    continue;

                Float w = weights.getOrDefault(h, 0f);
                Integer n = accuracy.getOrDefault(h, 0);
                
                // Invariant: [ x1 + x2 + ... xn+1 ] / n+1 = [ (x1 + ... xn) * n + xn+1 ] / n+1
                w *= n;
                w += (float)neighboorhood.get(neighboor) / (float)occurrences;
                n++;
                w /= n;
                accuracy.put(h, n);

                if(averagePopularity > 0 && popularity.containsKey(word) && popularity.containsKey(neighboor))
                    w += w * ((2f*averagePopularity) / (popularity.get(word) + popularity.get(neighboor)) - 1);
                
                weights.put(h, w);
                */
            }            

            popularity.put(word, popularity.getOrDefault(word, 0) + 1);
            newAvgPopularity += occurrences;
        }

        averagePopularity = newAvgPopularity / (float)bagOfWords.size();
        System.out.println();
    }

    public void save(String filePath) {
        System.out.println(entries.size());
        for(WordEntry entry : entries.values())
            entry.close(resource.toPath());
    }

    /*
    public void purge() {
        Iterator<String> iter = weights.keySet().iterator();

        while(iter.hasNext()) {
            String key = iter.next();

            if (weights.get(key) < MINIMUM_WEIGHT_THRESHOLD)
                iter.remove();
        }

        System.out.println("[NOTE] Purge complete!");
    }

    public void list() {
        weights.entrySet().stream()
            .sorted(Entry.comparingByValue())
            .forEach(System.out::println);    

        System.out.println("Average popularity: " + averagePopularity);
        System.out.println("[NOTE] Listing complete!");
    }
    */

    private boolean isValidWordPermutation(String wordA, String wordB) {
        return !wordA.equals(wordB) && !wordA.equals("") && !wordB.equals("");
    }

    public String hash(String wordA, String wordB) {
        if(wordA.equals("") || wordB.equals("") || wordA.equals(wordB))
            return null;

        if(wordA.compareTo(wordB) > 0)
            return wordA + ":" + wordB;
        else
            return wordB + ":" + wordA;
    }

    
    public float getWeight(String word1, String word2) {
        return WordEntry.similarity(WordEntry.of(word1, this), WordEntry.of(word2, this));
    }

    public static void main(String[] args) {
        final String INDEX_PATH = "index";
        
        Index index = new Index(Paths.get(INDEX_PATH));
        boolean saveRequired = false;

        // CALIBRATION
        // ===========
        Optional<String> filePath = Arrays.stream(args).filter(s -> !s.startsWith("-")).findFirst();
        if(filePath.isPresent()){
            Optional<Integer> comparisonSpan = Arrays.stream(args).flatMap(s -> {
                try { 
                    return Stream.of(Integer.valueOf(s));
                }
                catch(Exception e) {
                    return Stream.empty();
                }
            }).findFirst();

            if(comparisonSpan.isPresent()){
                index.calibrate(new File(filePath.get()), comparisonSpan.get());
                System.out.println("[NOTE] Calibration complete!");
                saveRequired = true;
            }
            else
                System.out.println("[ERROR] No comparison span!");
        }
        
        if (Arrays.stream(args).anyMatch("-p"::equals)){
            //index.purge();
            saveRequired = true;
        }


        if (Arrays.stream(args).anyMatch("-l"::equals)){
            //index.list();
        }
        
        if(saveRequired)
            index.save(INDEX_PATH);
    }
}