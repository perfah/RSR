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

class Index implements Serializable {
    public static final String ENCODING = "UTF-8";
    public static final float MINIMUM_WEIGHT_THRESHOLD = 0.01f;

    private HashMap<String, Float> weights;
    private HashMap<String, Integer> popularity;
    private float averagePopularity;

    public Index(Path index){
        weights = new HashMap<String, Float>();
        popularity = new HashMap<String, Integer>();
        averagePopularity = 0;
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

            for(int i = 0; i < bagOfWords.size(); i++){
                if(bagOfWords.get(i).equals(word)){
                    occurrences++;
                    
                    for(int j = Math.max(0, i - comparisonSpan/2); j < Math.min(i + comparisonSpan/2, bagOfWords.size()); j++){
                        String neighboor = bagOfWords.get(j);
                    
                        if(!word.trim().equals("") || !neighboor.equals(word))
                            neighboorhood.put(neighboor, neighboorhood.getOrDefault(neighboor, 0) + 1);
                    }
                }
            }

            for(String neighboor : neighboorhood.keySet()){
                String h = hash(word, neighboor);
                if(h == null)
                    continue;

                Float w = weights.getOrDefault(h, 0f);
                w += (float)neighboorhood.get(neighboor) / (float)occurrences;
                if(averagePopularity > 0 && popularity.containsKey(word) && popularity.containsKey(neighboor))
                    w += w * ((2f*averagePopularity) / (popularity.get(word) + popularity.get(neighboor)) - 1);
                w /= 2f;
                //w = Math.min(w, 1f);
                //w = 1f / (1f + (float)Math.exp(w));
                weights.put(h, w);
            }            

            popularity.put(word, popularity.getOrDefault(word, 0) + 1);
            newAvgPopularity += occurrences;
        }

        averagePopularity = newAvgPopularity / (float)bagOfWords.size();
        System.out.println();
    }

    public void save(String filePath) {
        try {
            FileOutputStream fileOut = new FileOutputStream(filePath);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this);
            out.close();
            fileOut.close();
            System.out.println("[NOTE] Index saved!");
        }
        catch(Exception e) {
            System.out.println("[ERROR] Could not save index!");
        }
    }

    public void relevance() {
        /* 
            TODO: 
            Implement a function that calculates the relevance of each weight. 
            It should decrease as the frequency/popularity of any of the two words increases (exponential decrease?)
            It should automatically be run after indexing and be used by the comparator as an expected max value for each weight.
        */
        
        throw new UnsupportedOperationException();
    }


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
        String h = hash(word1, word2);
        if(h == null)
            return 0.5f;

        Float weight = weights.get(h);
        if(weight == null)
            return 0.5f;
        
        return weight;
    }

    public static void main(String[] args) {
        final String INDEX_PATH = "index.dat";
        
        Index index = null;
        boolean saveRequired = false;

        // LOAD INDEX
        // ==========
        try {
            if (Arrays.stream(args).anyMatch("-r"::equals))
                throw new Exception();

            FileInputStream fis = new FileInputStream(INDEX_PATH);
            ObjectInputStream ois = new ObjectInputStream(fis);

            index = (Index) ois.readObject();
            System.out.println("[NOTE] Reusing index at: " + INDEX_PATH);
        }
        catch(Exception e) {
            index = new Index(Paths.get(INDEX_PATH));
            System.out.println("[WARNING] Initializing new index at: " + INDEX_PATH);
        }

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
                saveRequired = true;
            }
            else
                System.out.println("[ERROR] No comparison span!");
        }
        
        if (Arrays.stream(args).anyMatch("-p"::equals)){
            index.purge();
            saveRequired = true;
        }


        if (Arrays.stream(args).anyMatch("-l"::equals)){
            index.list();
        }
        
        if(saveRequired)
            index.save(INDEX_PATH);
    }
}