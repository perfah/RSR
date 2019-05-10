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
    final int CONCEPT_SCOPE = 20;
    final double SIMILARITY_WEIGHT = 0.0;
    final double RELATEDNESS_WEIGHT = 0.01; 
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

        Function<List<String>, Map<String, Double>> neighboorhoodDump = words -> words
            .stream()
            .map(w -> WordEntry.of(w, index, false))
            .filter(Objects::nonNull)
            .flatMap(we1 -> IntStream
                .range(0, Math.min(we1.concepts.size(), CONCEPT_SCOPE))
                .mapToObj(i -> {
                    WordEntry we2 = WordEntry.of((String)we1.concepts.toArray()[i], index, false);
                    return new SimpleEntry<String, Double>(we2.word, WordEntry.closeness(we1, we2, index));
                })
            )
            .collect(Collectors.groupingBy(Entry::getKey, Collectors.summingDouble(Entry::getValue)));
        
        // ~ Semantic Similarity ~
        // =======================
        
        double similarity = 0.0;

        if(SIMILARITY_WEIGHT > 0.0){
            ArrayList<String> syntacticIntersect = new ArrayList<String>(words1);
            syntacticIntersect.retainAll(words2); 
            for(String word : syntacticIntersect){
                WordEntry we = WordEntry.of(word, index, false);
                if(we != null)
                    similarity += distanceAsFraction.apply((double)we.documents.size()); 
            }
        }
        
        // ~ Semantic Relatedness ~
        // ========================

        double relatedness = 0.0;

        //if(RELATEDNESS_WEIGHT > 0.0){
            Map<String, Double> entries1 = neighboorhoodDump.apply(words1);
            Map<String, Double> entries2 = neighboorhoodDump.apply(words2);

            List<String> conceptualIntersect = new ArrayList<String>(entries1.keySet());
            conceptualIntersect.retainAll(entries2.keySet());
        
            HashMap<String, Double> rank = new HashMap<String, Double>();
            for(String concept : conceptualIntersect) 
                rank.put(concept, (entries1.get(concept) + entries2.get(concept))); // * distanceAsFraction.apply(Math.abs(entries1.get(concept) - entries2.get(concept))));
            
            
            conceptualIntersect.sort(Comparator.comparingDouble(x -> rank.get(x)).reversed());
            conceptualIntersect = conceptualIntersect.subList(0, Math.min(CONCEPT_SCOPE, conceptualIntersect.size()));

            int x = 0;
            if(verbose)
                System.out.println("\nMOST RELEVANT CONCEPTS IN ORDER:");
            
            double abSum = 0.0;
            double aSum = 0.0;
            double bSum = 0.0;
            double sMax = 0.0;

            for(String concept : conceptualIntersect){
                double A = entries1.get(concept); 
                double B = entries2.get(concept); 
                
                aSum += Math.pow(A, 2);
                bSum += Math.pow(B, 2);
                abSum += A*B;

                double max = Math.abs(A + B);
                double val = (max - Math.abs(A - B));

                sMax += max;
                relatedness += rank.get(concept); //val;
                if(verbose){
                    x++;
                    System.out.println(x + ". " + concept + " <-> " + A + " : " + B);
                }
                
            }

            //double sim = abSum / (Math.sqrt(aSum) * Math.sqrt(bSum));
            //double ratio = relatedness / sMax;

            
       // }
        
        // ~ Scoring ~  
        // ===========

        return MIN_SCORE + (MAX_SCORE - MIN_SCORE) * closenessAsFraction.apply(OVERALL_WEIGHT * (SIMILARITY_WEIGHT * similarity + RELATEDNESS_WEIGHT * relatedness));
    }

    @Override
    public String toString(){
        return "SSR";
    }

}