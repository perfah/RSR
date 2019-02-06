import net.sf.extjwnl.dictionary.Dictionary;
import java.io.BufferedReader;
import net.sf.extjwnl.JWNLException;
import java.util.Iterator;

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

    public abstract double rate(Iterator<String> iter1, Iterator<String> iter2);

    @Override public abstract String toString();
}

