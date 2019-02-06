import java.io.BufferedReader;
import java.lang.UnsupportedOperationException;
import java.util.Iterator;

class GoodWill extends TestMethod {
    int comparisonSpan;
    
    public GoodWill(int comparisonSpan) {
        this.comparisonSpan = comparisonSpan;
    }

    @Override
    public double rate(Iterator<String> iter1, Iterator<String> iter2){
        /*

            LD > 0 -> 
                is synonym -> +0 points
                not synonym -> +LD point
            else -> 
                +0 points
        */
        
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString(){
        return "Good will";
    }

    /*
        Time complexity: n * k
        
        You are good.
        Goody you are.

    */
}