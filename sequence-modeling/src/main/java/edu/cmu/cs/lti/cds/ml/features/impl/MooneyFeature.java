package edu.cmu.cs.lti.cds.ml.features.impl;

import edu.cmu.cs.lti.cds.ml.features.PairwiseFeature;
import edu.cmu.cs.lti.script.annotators.learn.train.KarlMooneyScriptCounter;
import edu.cmu.cs.lti.script.model.ContextElement;
import org.mapdb.Fun;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/30/14
 * Time: 5:36 PM
 */
public class MooneyFeature extends PairwiseFeature {
    @Override
    public Map<String, Double> getFeature(ContextElement elementLeft, ContextElement elementRight, int skip) {
        Map<String, Double> features = new HashMap<>();

        if (skip >= 3) {
            //for skip gram further than 3, use LongMooneyFeature
            return features;
        }

        Fun.Tuple2<Fun.Tuple4<String, Integer, Integer, Integer>, Fun.Tuple4<String, Integer, Integer, Integer>> substitutedForm = KarlMooneyScriptCounter.
                firstBasedSubstitution(elementLeft.getMention(), elementRight.getMention());

        int[] arg1s = getLast3IntFromTuple(substitutedForm.a);
        int[] arg2s = getLast3IntFromTuple(substitutedForm.b);


        String featureName = "m_arg" + "_" + asArgumentStr(arg1s) + "_" + asArgumentStr(arg2s);

//        if (elementRight.getMention().getMentionHead().equals("say") && arg2s[0] == -1
//                && arg2s[1] == 0 && arg2s[2] == -1 && elementLeft.getMention().getMentionHead().equals("understand")) {
//            System.err.println("Substituting " + elementLeft.getMention() + "  " + elementRight.getMention());
//            System.err.println("Substituted form : " + substitutedForm.a + " " + substitutedForm.b);
//            System.err.println(featureName);
//            Utils.pause();
//        }

        features.put(featureName, 1.0);
        return features;
    }

    @Override
    public boolean isLexicalized() {
        return true;
    }

    private int[] getLast3IntFromTuple(Fun.Tuple4<String, Integer, Integer, Integer> t) {
        int[] a = new int[3];
        a[0] = t.b;
        a[1] = t.c;
        a[2] = t.d;
        return a;
    }

    private String asArgumentStr(int[] args) {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        for (int arg : args) {
            sb.append(sep).append(arg);
            sep = "_";
        }
        return sb.toString();
    }
}