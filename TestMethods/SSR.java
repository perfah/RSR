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

class SSR extends TestMethod {
    Index index = null;

    public SSR(String indexPath) {
        try {
            FileInputStream fis = new FileInputStream(indexPath);
            ObjectInputStream ois = new ObjectInputStream(fis);

            index = (Index) ois.readObject();
        }
        catch(Exception e) {
            System.out.println(e.getMessage());
            System.out.println("[ERROR] No index found at: " + indexPath);
        }

    }

    @Override
    public double rate(List<String> words1, List<String> words2){
        float sum = 0f;
        float maxSum = 0f; //words1.size() + words2.size() / 2;

        for(String w1 : words1) {
            for(String w2 : words2) {
                if(w1.equals(w2))
                    continue;

                sum += index.getWeight(w1, w2);
                maxSum += 1.0;
                // TODO: maxSum = 1.0 * relevance.get(w1, w2)
            }
        }
            
        return sum / maxSum;
    }

    @Override
    public String toString(){
        return "SSR";
    }

}