import net.sf.extjwnl.dictionary.Dictionary;
import java.io.BufferedReader;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.*;
import net.sf.extjwnl.data.relationship.RelationshipFinder;
import java.util.List;

abstract class TestMethod {
    protected Dictionary dict;

    TestMethod() {
        try {
            this.dict = Dictionary.getDefaultResourceInstance();
        } 
        catch(JWNLException ex) {
            System.out.println("ERROR: Could not initialize dictionary!");
        }
  
    }

    protected boolean synonyms(String w1, String w2){
        // w1 and w2 must share the same POS to be synonyms
        
        try {
            for(IndexWord iw1 : dict.lookupAllIndexWords(w1).getIndexWordArray()){
                for(IndexWord iw2 : dict.lookupAllIndexWords(w2).getIndexWordArray()){
                    if(iw1.getPOS() == iw2.getPOS() && RelationshipFinder.getImmediateRelationship(iw1, iw2) > 0)
                        return true;          
                }
            }
        }
        catch(JWNLException ex){
            System.out.println("[ERROR] " + ex.getMessage());
        }
        
        return false;
    }

    public abstract double rate(List<String> words1, List<String> words2);

    @Override public abstract String toString();
}

