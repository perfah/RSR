import net.sf.extjwnl.dictionary.Dictionary;
import java.io.BufferedReader;
import net.sf.extjwnl.JWNLException;

abstract class TextDiff {
    protected Dictionary dict;

    TextDiff() {
        try {
            this.dict = Dictionary.getDefaultResourceInstance();
        } 
        catch(JWNLException ex) {
            System.out.println("ERROR: Could not initialize dictionary!");
        }
  
    }

    public abstract double rate(BufferedReader bufText1, BufferedReader bufText2);

    @Override public abstract String toString();
}

