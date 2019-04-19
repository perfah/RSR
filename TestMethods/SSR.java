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
    final double SIMILARITY_WEIGHT = 0.1;
    final double RELATEDNESS_WEIGHT = 0.1; 
    final double OVERALL_WEIGHT = 0.5; 

    public SSR(Path indexLocation) {
        if(indexLocation.toFile().isDirectory()){
            index = new Index(indexLocation);
        }
        else {
            index = null;
            System.out.println("[ERROR] No index found at: " + indexLocation);
        }

    }

    @Override
    public double rate(List<String> words1, List<String> words2){
        // ~ Lambdas  ~
        // ============

        Function<Double, Double> closenessAsFraction = x -> x / (1.0 + Math.abs(x));
        Function<Double, Double> distanceAsFraction = x -> 1.0 / (1.0 + Math.abs(x));

        Function<List<String>, Map<String, Double>> neighboorhoodDump = words -> words.stream()
            .map(w -> WordEntry.of(w, index))
            .filter(Objects::nonNull)
            .flatMap(we -> IntStream
                .range(0, Math.min(we.priority.size(), CONCEPT_SCOPE))
                .mapToObj(i -> new SimpleEntry<String, Double>((String)we.priority.toArray()[i], (double)(we.occurrences * i)
            )))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> (a + b) / 2.0));
        
        // ~ Semantic Similarity ~
        // =======================

        double similarity = 0.0;

        ArrayList<String> syntacticIntersect = new ArrayList<String>(words1);
        syntacticIntersect.retainAll(words2); 
        for(String word : syntacticIntersect){
            WordEntry we = WordEntry.of(word, index);
            if(we != null)
                similarity += distanceAsFraction.apply((double)we.occurrences);
        }
        
        // ~ Semantic Relatedness ~
        // ========================

        double relatedness = 0.0;

        Map<String, Double> entries1 = neighboorhoodDump.apply(words1);
        Map<String, Double> entries2 = neighboorhoodDump.apply(words2);

        Set<String> conceptualIntersect = entries1.keySet();
        conceptualIntersect.retainAll(entries2.keySet());
        
        //System.out.println("=================================");
        for(String key : conceptualIntersect){
            double contribution = distanceAsFraction.apply((entries1.get(key) + entries2.get(key)) / 2.0);
            relatedness += contribution;
            //System.out.println(key + " <-> " + contribution);
        }
        
        // ~ Scoring ~
        // ===========

        return closenessAsFraction.apply(OVERALL_WEIGHT * (SIMILARITY_WEIGHT * similarity + RELATEDNESS_WEIGHT * relatedness));
    }

    @Override
    public String toString(){
        return "SSR";
    }

}