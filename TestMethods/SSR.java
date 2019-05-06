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
import java.util.function.BiFunction;
import java.util.Objects;
import java.util.Comparator;

class SSR extends TestMethod {
    private Index index;
    final int CONCEPT_SCOPE = 40;
    final double SIMILARITY_WEIGHT = 0.0;
    final double RELATEDNESS_WEIGHT = 1.0; 
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

        Function<List<String>, Map<String, List<WordEntry>>> neighboorhoodDump = words -> words
            .stream()
            .map(w -> WordEntry.of(w, index, false))
            .filter(Objects::nonNull)
            .flatMap(we -> IntStream
                .range(0, Math.min(we.concepts.size(), CONCEPT_SCOPE))
                .mapToObj(i -> new SimpleEntry<String, WordEntry>(
                    (String)we.concepts.toArray()[i], 
                    we
            )))
            .collect(Collectors.groupingBy(Entry::getKey, Collectors.mapping(Entry::getValue, Collectors.toList())));
        
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

        Map<String, List<WordEntry>> entries1 = neighboorhoodDump.apply(words1);
        Map<String, List<WordEntry>> entries2 = neighboorhoodDump.apply(words2);
        
        List<String> conceptualIntersect = new ArrayList<String>(entries1.keySet());
        conceptualIntersect.retainAll(entries2.keySet());
    
        HashMap<String, Double> rank = new HashMap<String, Double>();
        for(String concept : conceptualIntersect) {
            WordEntry c = WordEntry.of(concept, index, false);

            double maxCloseness = 0.0;
            WordEntry finalA = null;
            WordEntry finalB = null;

            for(WordEntry a : entries1.get(concept)){
                for(WordEntry b : entries2.get(concept)){
                    double cand = 0.0;
                        //(a.word.equals(b.word)) ? 0.0 : (CONCEPT_SCOPE * WordEntry.closeness(a, b)) + 
                    cand += (CONCEPT_SCOPE - IntStream.range(0, a.concepts.size()).filter(i -> c.word.equals(a.concepts.toArray()[i])).findFirst().getAsInt());
                    cand += (CONCEPT_SCOPE - IntStream.range(0, b.concepts.size()).filter(i -> c.word.equals(b.concepts.toArray()[i])).findFirst().getAsInt());
                    
                    if(cand >= maxCloseness){
                        maxCloseness = cand;
                        finalA = a;
                        finalB = b;
                    }
                }
            }
            rank.put(concept, maxCloseness);
            entries1.get(concept).retainAll(Arrays.asList(finalA));
            entries2.get(concept).retainAll(Arrays.asList(finalB));
        }
        
        conceptualIntersect.sort(Comparator.comparingDouble(x -> rank.get(x)).reversed());
        conceptualIntersect = conceptualIntersect.subList(0, Math.min(CONCEPT_SCOPE, conceptualIntersect.size()));

        if(verbose)
            System.out.println("\nMOST RELEVANT CONCEPTS IN ORDER:");
        
        for(String key : conceptualIntersect){
            relatedness += rank.get(key);
            if(verbose){
                System.out.print(key + " <-> " + rank.get(key));
            }
            System.out.println(" (" + entries1.get(key).get(0).word + ", " + entries2.get(key).get(0).word + ")");
        }
        
        // ~ Scoring ~
        // ===========

        return MIN_SCORE + (MAX_SCORE - MIN_SCORE) * closenessAsFraction.apply(OVERALL_WEIGHT * (SIMILARITY_WEIGHT * similarity + RELATEDNESS_WEIGHT * relatedness));
    }

    @Override
    public String toString(){
        return "SSR";
    }

}