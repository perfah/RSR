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
import java.util.OptionalInt;

/*
    TODO
    - Word popularity measure or lexicon to account for the purposeless words 

*/

class Index implements Serializable {
    public static final String ENCODING = "UTF-8";
    public static final float MINIMUM_WEIGHT_THRESHOLD = 0.01f;

    public int documents;
    public HashMap<Integer, WordEntry> entries;
    private HashMap<String, Integer> accuracy;
    private HashMap<String, Integer> popularity;
    public File resource;
    private float averagePopularity;
    public WordEntry mostOccuringEntry;

    public Index(Path fsLocation){
        documents = 0;
        entries = new HashMap<Integer, WordEntry>();
        popularity = new HashMap<String, Integer>();
        averagePopularity = 0;
        mostOccuringEntry = null;

        resource = fsLocation.toFile();
        if(resource.isDirectory()){
            System.out.println("[NOTE] Using index at: " + fsLocation.toAbsolutePath());
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
            String raw = new String(Files.readAllBytes(docPath.toPath()), ENCODING).replaceAll("[^A-Za-z' ]+", " ");
            bagOfWords = Arrays.asList(raw.split(" "));
        }
        catch(Exception ex){
            System.out.println("[ERROR] " + ex.getMessage());
            return;
        }
        
        HashSet<String> wordsLeft = new HashSet<String>(bagOfWords);
        if(wordsLeft.size() == 0){
            return;
        }

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
            System.out.flush();

            int occurrences = 0;
            HashMap<String, Integer> neighboorhood = new HashMap<String, Integer>();
            
            WordEntry x = WordEntry.of(word, this, true);
            
            if(x == null)
                continue;

            for(int i = 0; i < bagOfWords.size(); i++){
                if(bagOfWords.get(i).equals(word)){
                    x.record(this, docName);
                    occurrences++;
                    
                    for(int j = Math.max(0, i - comparisonSpan/2); j < Math.min(i + comparisonSpan/2, bagOfWords.size()); j++){
                        String neighboor = bagOfWords.get(j);
                        
                        if(!word.trim().equals("") || !neighboor.equals(word)){
                            WordEntry y = WordEntry.of(neighboor, this, true);
                            if(y == null)
                                continue;

                            WordEntry.join(x, y);
                        }
                    }
                }
            }       

            popularity.put(word, popularity.getOrDefault(word, 0) + 1);
            newAvgPopularity += occurrences;
        }

        averagePopularity = newAvgPopularity / (float)bagOfWords.size();
        documents++;
        System.out.println();
    }

    public void save(String filePath) {
        for(WordEntry entry : entries.values())
            entry.close(resource.toPath());
    }

    
    public void purge(int quantity, boolean preview) {
        loadAllEntries();
        List<WordEntry> orderedEntries = new ArrayList<WordEntry>(entries.values());

        Collections.sort(orderedEntries, (a, b) -> b.occurrences - a.occurrences);

        System.out.println("The following words " + (preview ? "would be " : "are being ") + "purged:");
        for(int i = 0; i < Math.min(quantity, entries.size()); i++){
            String word = orderedEntries.get(i).word;

            if(!preview)
                orderedEntries.get(i).delete(this);
            System.out.println("    " + (i+1) + ". " + word);
        }
    }


    public void list() {
        loadAllEntries();
        for(WordEntry entry : entries.values())
            System.out.println(entry.word + " <=> " + entry.concepts);
    }
    

    private boolean isValidWordPermutation(String wordA, String wordB) {
        return !wordA.equals(wordB) && !wordA.equals("") && !wordB.equals("");
    }

    private void loadAllEntries() {
        File[] listOfFiles = resource.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                try {
                    WordEntry entry = WordEntry.fromFile(listOfFiles[i], this);
                    int hash = entry.word.hashCode();

                    if(!entries.containsKey(hash))
                        entries.put(hash, entry);
                }
                catch(Exception e) {
                    System.out.println("[ERROR] " + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        final String INDEX_PATH = "index";
        
        Index index = new Index(Paths.get(INDEX_PATH));
        boolean saveRequired = false;

        // CALIBRATION
        // ===========
        OptionalInt calibrationOp = IntStream.range(0, args.length).filter(i -> !args[i].startsWith("-") && !args[i].matches("[0-9]+")).findFirst();
        if(calibrationOp.isPresent()){
            try {
                int comparisonSpan = Integer.valueOf(args[calibrationOp.getAsInt() + 1]); 
                index.calibrate(new File(args[calibrationOp.getAsInt()]), comparisonSpan);
                saveRequired = true;
            }
            catch(Exception e){
                System.out.println("[ERROR] No comparison span!");
            }
        }
        
        OptionalInt purgeOp = IntStream.range(0, args.length).filter(i -> args[i].equals("--purge")).findFirst();
        if (purgeOp.isPresent()){
            try{
                boolean preview = Arrays.stream(args).anyMatch("--preview"::equals);
                int quantity = Integer.valueOf(args[purgeOp.getAsInt() + 1]); 
                index.purge(quantity, preview);
                saveRequired = !preview;
            }
            catch(Exception e){
                e.printStackTrace();
                System.out.println("[ERROR] No purge quantity!");
            }
        }


        if (Arrays.stream(args).anyMatch("--list"::equals)){
            index.list();
        }
        
        if(saveRequired)
            index.save(INDEX_PATH);
    }
}