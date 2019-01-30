import java.util.*;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.data.PointerType;
import net.sf.extjwnl.data.PointerUtils;
import net.sf.extjwnl.data.list.PointerTargetNodeList;
import net.sf.extjwnl.data.list.PointerTargetTree;
import net.sf.extjwnl.data.relationship.AsymmetricRelationship;
import net.sf.extjwnl.data.relationship.Relationship;
import net.sf.extjwnl.data.relationship.RelationshipFinder;
import net.sf.extjwnl.data.relationship.RelationshipList;
import net.sf.extjwnl.dictionary.Dictionary;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.BufferedReader;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class TextDiff {

    Dictionary dict;
    Double humanScore;
    String text1;
    String text2;

    BufferedReader br = null;
    FileReader fr = null;

    public TextDiff(text1, text2, humanScore){
      this.fileName1 = text1;
      this.fileName2 = text2;
      this.humanScore = humanScore;

      try {
          this.dict = Dictionary.getDefaultResourceInstance();
      } catch(JWNLException ex) {}

        try {
          fr = new FileReader(fileName1);
          br = new BufferedReader(fr);
          String sCurrentLine;

          while ((sCurrentLine = br.readLine()) != null) {
            System.out.println(sCurrentLine);
          }

        } catch (IOException e) {
          e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        String fileName1 = args[0];
        String fileName2 = args[1];
        Double humanScore = args[2];

        TextDiff textDiff = new TextDiff(text1, text2, humanScore, dict);

    }


}
