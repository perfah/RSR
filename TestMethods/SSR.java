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
import java.util.Set;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.*;
import java.util.Iterator;
import java.util.OptionalInt;
import java.util.AbstractMap.SimpleEntry;
import java.util.function.Function;
import java.util.Objects;

class SSR extends TestMethod {
    private Index index;
    final int CONCEPT_SCOPE = 20;
    final double SIMILARITY_WEIGHT = 0.0;
    final double RELATEDNESS_WEIGHT = 10.0; 
    final double OVERALL_WEIGHT = 1.0; 
    final double MIN_SCORE = 0.2;
    final double MAX_SCORE = 1.0;

    private boolean verbose;

    public SSR(Path indexLocation, boolean verbose) {
        if(indexLocation.toFile().isDirectory()){
            index = new Index(indexLocation);
            this.verbose = verbose; 
        }
        else {
            index = null;
            System.out.println("[ERROR] No index found at: " + indexLocation);
        }
    }

    @Override
    public double rate(List<String> words1, List<String> words2){
        if(verbose){
            System.out.println("\nBAG OF WORDS 1:\n" + words1);
            System.out.println("\nBAG OF WORDS 2:\n" + words2);
        }

        // ~ Lambdas  ~
        // ============

        Function<Double, Double> closenessAsFraction = x -> x / (1.0 + Math.abs(x));
        Function<Double, Double> distanceAsFraction = x -> 1.0 / (1.0 + Math.abs(x));

        Function<List<String>, Map<String, Double>> neighboorhoodDump = words -> words.stream()
            .map(w -> WordEntry.of(w, index, false))
            .filter(Objects::nonNull)
            .flatMap(we -> IntStream
                .range(0, Math.min(we.priority.size(), CONCEPT_SCOPE))
                .mapToObj(i -> new SimpleEntry<String, Double>((String)we.priority.toArray()[i], (double)((WordEntry.of((String)we.priority.toArray()[i], index, false).occurrences + we.occurrences) * 1.0/2.0 * Math.pow(2, i))
            )))
            .collect(Collectors.groupingBy(Entry::getKey, Collectors.averagingDouble(Entry::getValue)));
        
        // ~ Semantic Similarity ~
        // =======================

        double similarity = 0.0;

        ArrayList<String> syntacticIntersect = new ArrayList<String>(words1);
        syntacticIntersect.retainAll(words2); 
        for(String word : syntacticIntersect){
            WordEntry we = WordEntry.of(word, index, false);
            if(we != null)
                similarity += distanceAsFraction.apply((double)we.documents);
        }
        
        // ~ Semantic Relatedness ~
        // ========================

        double relatedness = 0.0;

        Map<String, Double> entries1 = neighboorhoodDump.apply(words1);
        Map<String, Double> entries2 = neighboorhoodDump.apply(words2);
        
        List<String> conceptualIntersect = new ArrayList<>(entries1.keySet());
        conceptualIntersect.retainAll(entries2.keySet());
        conceptualIntersect.sort((String a, String b) -> (int)(entries1.get(a) + entries2.get(a) - entries1.get(b) - entries2.get(b)));
        conceptualIntersect = conceptualIntersect.subList(0, Math.min(CONCEPT_SCOPE, conceptualIntersect.size()));

        if(verbose)
            System.out.println("\nMOST RELEVANT CONCEPTS IN ORDER:");
        
        for(String key : conceptualIntersect){
            double avgOccurrences = (entries1.get(key) + entries2.get(key)) / 2.0;
            relatedness += distanceAsFraction.apply(avgOccurrences);
            if(verbose)
                System.out.println(key + " <-> " + avgOccurrences);
        }
        
        // ~ Scoring ~
        // ===========
// 10 corresp. CONCEPT_SCOPE
        return MIN_SCORE + (MAX_SCORE - MIN_SCORE) * closenessAsFraction.apply(OVERALL_WEIGHT * (SIMILARITY_WEIGHT * similarity + RELATEDNESS_WEIGHT * relatedness));
    }

    @Override
    public String toString(){
        return "SSR";
    }

}