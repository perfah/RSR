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
import java.util.Set;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.*;
import java.util.Iterator;
import java.lang.*;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Comparator;

public class WordEntry implements Serializable {

    class WordComparator implements Comparator<String>, Serializable {
        public transient HashMap<String, Double> contextHandle;
        public transient Index indexHandle;

        public WordComparator(HashMap<String, Double> context, Index index){
            this.contextHandle = context;
            this.indexHandle = index;
        }

        @Override
        public int compare(String a, String b) {
            if(contextHandle == null || indexHandle == null)
            {
                
                //System.out.println("!!!!!");
                
                return 0;
            }
            else {
                double mostUsedTermOccurrences = indexHandle.mostOccuringEntry.occurrences;             // Most occuring term in all Docs.
                double indexedDocuments = indexHandle.documents;                                        // Total number of indexed Docs.

                double occursA = WordEntry.of(a, indexHandle, false).occurrences;                       // Num times where term A is used.
                double occursB = WordEntry.of(b, indexHandle, false).occurrences;                       // Num times where term B is used.

                double docOccurrencesA = WordEntry.of(a, indexHandle, false).documents;                 // Num docs where word A is used.
                double docOccurrencesB = WordEntry.of(b, indexHandle, false).documents;                 // Num docs where word B is used.

                // tf = 0.5 + 0.5 (Occurrences of "A" / Most occurrences of a term " X " in all docs)
                double tfA = 0.5 + 0.5 * (occursA / mostUsedTermOccurrences);
                double tfB = 0.5 + 0.5 * (occursB / mostUsedTermOccurrences);

                // idf = Number of Documents indexed / Number of docs where the term "A" is used
                double idfA = indexedDocuments / docOccurrencesA;
                double idfB = indexedDocuments / docOccurrencesB;

                double tfidfA = tfA * idfA;
                double tfidfB = tfB * idfB;

                return (int)(
                    tfidfA - tfidfB
                );
            }
        }
    }

    public static final int MAX_SEARCH_RADIUS = 3;
    public static final float AGNOSTIC_WEIGHT = 0.5f;

    public String word;
    private HashMap<String, Double> context;
    public PriorityQueue<String> priority;
    private WordComparator wordCmp;
    public int occurrences;
    public int documents;

    public WordEntry(String word, Index index){
        this.word = word;
        context = new HashMap<String, Double>();
        wordCmp = this.new WordComparator(context, index);
        priority = new PriorityQueue<String>(10, wordCmp);
        occurrences = 0;
        documents = 0;
    }

    public void record(Index index) {
        occurrences++;

        if(index.mostOccuringEntry == null || occurrences > index.mostOccuringEntry.occurrences)
            index.mostOccuringEntry = this;
    }

    public static File resource(String word, Path index) {
        return index.resolve(Paths.get(word.hashCode() + ".dat")).toFile();
    }

    public void close(Path index) {
        try {
            FileOutputStream fileOut = new FileOutputStream(resource(this.word, index));
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this);
            out.close();
            fileOut.close();
        }
        catch(Exception e) {
            System.out.println("[ERROR] Could not save word entry: " + e.getMessage());
        }
    }

    public static WordEntry of(String word, Index index, boolean indexing) {
        if(word.isEmpty())
            return null;

        WordEntry entry;
        Integer hash = word.hashCode();

        if(index.entries.containsKey(hash)){
            // Using cached version of word entry
            entry = index.entries.get(hash);
        }
        else {
            // Loading word entry from file or creating a new
            try {
                entry = WordEntry.fromFile(resource(word, index.resource.toPath()), index);
            }
            catch(IOException e) {
                entry = new WordEntry(word, index);  
            }
            catch(ClassNotFoundException e) {
                entry = null;
                System.out.println("[ERROR] Should not happen!");
            }
            catch(Exception e) {
                System.out.println(e.toString());
                e.printStackTrace();
                entry = new WordEntry(word, index);  }

            entry.wordCmp.contextHandle = entry.context;
            entry.wordCmp.indexHandle = index;
            
            index.entries.put(hash, entry);

            if(indexing)
                entry.documents++;
        }

        return entry;
    }

    public static WordEntry fromFile(File file, Index index) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis);

        WordEntry entry = (WordEntry) ois.readObject();
        ois.close();

        return entry;
    }

    public static void join(WordEntry entry1, WordEntry entry2) {
        if(entry1.word.equals(entry2.word))
            return;

        assert(entry1 != null && entry2 != null);

        entry1.context.put(entry2.word, entry1.context.getOrDefault(entry2.word, 0.0) + 1.0);
        if(!entry1.priority.contains(entry2.word))
            entry1.priority.add(entry2.word);

        entry2.context.put(entry1.word, entry2.context.getOrDefault(entry1.word, 0.0) + 1.0);
        if(!entry2.priority.contains(entry1.word))
            entry2.priority.add(entry1.word);
    }

    // Euclidean similarity
    public static double similarity(WordEntry entry1, WordEntry entry2) {
        double distance = 0;

        Set<String> set = Stream
            .concat(entry1.priority.stream().limit(20), entry2.priority.stream().limit(20))
            .collect(Collectors.toSet());

        //System.out.println(entry1.word + " " + set);

        for(String concept : set) {
            double a;
            if(entry1.occurrences > 0)
                a = entry1.context.getOrDefault(concept, 0.0) / (double)entry1.occurrences;
            else
                a = 0.0;

                double b;
            if(entry2.occurrences > 0)
                b = entry2.context.getOrDefault(concept, 0.0) / (double)entry2.occurrences;
            else
                b = 0.0;
        
            distance += Math.abs(a - b);
        }
        
        return 1f / (1f + distance);
    }
}

