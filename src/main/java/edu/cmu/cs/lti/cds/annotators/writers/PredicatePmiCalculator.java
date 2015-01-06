package edu.cmu.cs.lti.cds.annotators.writers;

import edu.cmu.cs.lti.cds.utils.DataPool;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.io.writer.AbstractCustomizedTextWriterAnalsysisEngine;
import edu.cmu.cs.lti.utils.BitUtils;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/28/14
 * Time: 2:50 PM
 */
public class PredicatePmiCalculator extends AbstractCustomizedTextWriterAnalsysisEngine {
    private static final double log2Val = Math.log(2);

    double[][] pmiTable;
    List<String> uniqPredicates;

    @Override
    public String getTextToPrint(JCas aJCas) {
        if (DataPool.isBlackList(aJCas, logger)) {
            return null;
        }

        List<EventMention> allMentions = new ArrayList<>(JCasUtil.select(aJCas, EventMention.class));
        computePmiTable(allMentions, aJCas);

        StringBuffer outStr = new StringBuffer();

        for (String predicate : uniqPredicates) {
            outStr.append(predicate).append("\n");
        }

        for (int i = 0; i < pmiTable.length; i++) {
            String colSep = "";
            for (int j = 0; j < pmiTable[i].length; j++) {
                outStr.append(colSep).append(pmiTable[i][j]);
                colSep = "\t";
            }
            outStr.append("\n");
        }

        return outStr.toString();
    }

    private void computePmiTable(List<EventMention> allMentions, JCas aJCas) {
        TokenAlignmentHelper alignmentHelper = new TokenAlignmentHelper();
        alignmentHelper.loadWord2Stanford(aJCas);

        Set<String> predicates = new HashSet<>();

        for (int i = 0; i < allMentions.size(); i++) {
            EventMention mention = allMentions.get(i);
            String predicate = alignmentHelper.getLowercaseWordLemma(mention.getHeadWord());
            predicates.add(predicate);
        }

        uniqPredicates = new ArrayList<>(predicates);

        pmiTable = new double[uniqPredicates.size()][uniqPredicates.size()];

        for (int i = 0; i < uniqPredicates.size(); i++) {
            String predicate1 = uniqPredicates.get(i);
            for (int j = 0; j < uniqPredicates.size(); j++) {
                if (i != j) {
                    String predicate2 = uniqPredicates.get(j);
                    pmiTable[i][j] = getPmi(predicate1, predicate2);
                } else {
                    pmiTable[i][j] = 0;
                }
            }
        }
//        printPmiTable(uniqPredicates, pmiTable);
    }

    private double getPmi(String predicate1, String predicate2) {
        //log_2 P(w1,w2) / P(w1) P(w2)
        // = log_2 C(w1,w2)/N / ( C(w1)  C(w2) / N^2 )
        // = log_2 C(w1,w2) * N / (C(w1) C(w2))

        int id1 = DataPool.headIdMap.get(predicate1);
        int id2 = DataPool.headIdMap.get(predicate2);

        double jointCount = DataPool.headPairMap.get(BitUtils.store2Int(id1, id2));
        jointCount += DataPool.headPairMap.get(BitUtils.store2Int(id2, id1));

        double count1 = DataPool.getPredicateFreq(id1);
        double count2 = DataPool.getPredicateFreq(id2);

        return log2(jointCount * DataPool.predicateTotalCount / (count1 * count2));
    }


    private void printPmiTable(List<String> uniqPredicates, double[][] pmiTable) {
        double[] relatednessScores = new double[uniqPredicates.size()];

        for (int i = 0; i < pmiTable.length; i++) {
            for (int j = 0; j < pmiTable[i].length; j++) {
                relatednessScores[i] += pmiTable[i][j];
            }
            relatednessScores[i] /= pmiTable.length;
        }

        Arrays.sort(relatednessScores);

        for (int i = 0; i < relatednessScores.length; i++) {
            System.err.println(i + " " + uniqPredicates.get(i) + " " + relatednessScores[i]);
        }

        for (int i = 0; i < uniqPredicates.size(); i++) {
            System.err.print("\t" + i + "   ");
        }

        System.err.println();

        for (int i = 0; i < uniqPredicates.size(); i++) {
            System.err.print(i);
            for (int j = 0; j < uniqPredicates.size(); j++) {
                System.err.print(String.format("\t%.2f", pmiTable[i][j]));
            }
            System.err.println();
        }
    }

    private double log2(double n) {
        return Math.log(n) / log2Val;
    }


}
