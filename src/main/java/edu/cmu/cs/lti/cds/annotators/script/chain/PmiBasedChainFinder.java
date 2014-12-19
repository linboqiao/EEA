package edu.cmu.cs.lti.cds.annotators.script.chain;

import edu.cmu.cs.lti.cds.utils.DataPool;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.BitUtils;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import edu.cmu.cs.lti.utils.Utils;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TLongLongMap;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/28/14
 * Time: 2:50 PM
 */
public class PmiBasedChainFinder extends AbstractLoggingAnnotator {
    TLongLongMap eventHeadPairCount;
    TIntLongMap eventHeadTfDf;

    private static double log2Val = Math.log(2);

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        if (DataPool.isBlackList(aJCas, logger)) {
            return;
        }
        List<EventMention> allMentions = new ArrayList<>(JCasUtil.select(aJCas, EventMention.class));
        filterMentions(allMentions, aJCas);
        Utils.pause();
    }

    private void filterMentions(List<EventMention> allMentions, JCas aJCas) {
        TokenAlignmentHelper alignmentHelper = new TokenAlignmentHelper();
        alignmentHelper.loadWord2Stanford(aJCas);

        double[][] pmiTable = new double[allMentions.size()][allMentions.size()];

        for (int i = 0; i < allMentions.size(); i++) {
            EventMention mention1 = allMentions.get(i);
            String predicate1 = alignmentHelper.getLowercaseWordLemma(mention1.getHeadWord());
            for (int j = 0; j < allMentions.size(); j++) {
                EventMention mention2 = allMentions.get(j);
                String predicate2 = alignmentHelper.getLowercaseWordLemma(mention2.getHeadWord());

                if (predicate1.equals(predicate2)) {
                    pmiTable[i][j] = 0; //set it to zero to avoid repeating words get rewards
                } else {
                    pmiTable[i][j] = getPmi(predicate1, predicate2);
                }
            }
        }

        double[] relatednessScores = new double[allMentions.size()];

        for (int i = 0; i < pmiTable.length; i++) {
            for (int j = 0; j < pmiTable[i].length; j++) {
                relatednessScores[i] += pmiTable[i][j];
            }
            relatednessScores[i] /= pmiTable.length;

            System.err.println(allMentions.get(i).getCoveredText() + " " + relatednessScores[i]);
        }
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

    private double log2(double n) {
        return Math.log(n) / log2Val;
    }


}
