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
        public transient HashMap<String, Integer> contextHandle;

        public WordComparator(HashMap<String, Integer> context){
            this.contextHandle = context;
        }

        @Override
        public int compare(String a, String b) {
            if(contextHandle == null)
                return 0;
            else
                return context.get(a) - context.get(b);
        }
    }

    public static final int MAX_SEARCH_RADIUS = 3;
    public static final float AGNOSTIC_WEIGHT = 0.5f;

    private String word;
    private HashMap<String, Integer> context;
    private PriorityQueue<String> priority;
    private WordComparator wordCmp;
    public int occurrences;

    public WordEntry(String word){
        this.word = word;
        context = new HashMap<String, Integer>();
        wordCmp = this.new WordComparator(context);
        priority = new PriorityQueue<String>(10, wordCmp);
        occurrences = 0;
    }

    public void record() {
        occurrences++;
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

    public static WordEntry of(String word, Index index) {
        WordEntry entry;
        Integer hash = word.hashCode();

        if(index.entries.containsKey(hash)){
            entry = index.entries.get(hash);
        }
        else {
            try {
                FileInputStream fis = new FileInputStream(resource(word, index.resource.toPath()));
                ObjectInputStream ois = new ObjectInputStream(fis);

                entry = (WordEntry) ois.readObject();
                entry.wordCmp.contextHandle = entry.context;
                ois.close();
            }
            catch(IOException e) {
                entry = new WordEntry(word);  
            }
            catch(ClassNotFoundException e) {
                entry = null;
                System.out.println("[ERROR] Should not happen!");
            }
            catch(Exception e) {
                System.out.println(e.toString());
                e.printStackTrace();
                entry = new WordEntry(word);  }

            index.entries.put(hash, entry);
        }

        return entry;
    }

    public static void join(WordEntry entry1, WordEntry entry2) {
        assert(entry1 != null && entry2 != null);

        entry1.context.put(entry2.word, entry1.context.getOrDefault(entry2.word, 0) + 1);
        if(!entry1.priority.contains(entry2.word))
            entry1.priority.add(entry2.word);

        entry2.context.put(entry1.word, entry2.context.getOrDefault(entry1.word, 0) + 1);
        if(!entry2.priority.contains(entry1.word))
            entry2.priority.add(entry1.word);
    }

    // Euclidean similarity
    public static float similarity(WordEntry entry1, WordEntry entry2) {
        float distance = 0;

        Set<String> set = Stream
            .concat(entry1.priority.stream().limit(10), entry2.priority.stream().limit(10))
            .collect(Collectors.toSet());
        
        for(String concept : set) {
            float a;
            if(entry1.occurrences > 0)
                a = (float)entry1.context.getOrDefault(concept, 0) / (float)entry1.occurrences;
            else
                a = 0f;

            float b;
            if(entry2.occurrences > 0)
                b = (float)entry2.context.getOrDefault(concept, 0) / (float)entry2.occurrences;
            else
                b = 0f;
            
            //float frequency = 1f / (1f + entry1.occurrences + entry2.occurrences);
            distance += Math.abs(a - b);
        }
        
        return 1f / (1f + distance);
    }
}

