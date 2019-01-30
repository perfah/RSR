import java.io.BufferedReader;
import java.lang.UnsupportedOperationException;

class Levenshtein extends TextDiff {
    public Levenshtein() {
        super();
    }

    @Override
    public double rate(BufferedReader bufText1, BufferedReader bufText2){
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString(){
        return "Levenshtein";
    }
}