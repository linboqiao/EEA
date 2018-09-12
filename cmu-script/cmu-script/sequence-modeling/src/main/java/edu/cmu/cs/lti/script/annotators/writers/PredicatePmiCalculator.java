package edu.cmu.cs.lti.script.annotators.writers;

import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.utils.DataPool;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.AbstractCustomizedTextWriterAnalysisEngine;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import edu.cmu.cs.lti.utils.BitUtils;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.File;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/28/14
 * Time: 2:50 PM
 */
public class PredicatePmiCalculator extends AbstractCustomizedTextWriterAnalysisEngine {
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


    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws Exception {
        String className = PredicatePmiCalculator.class.getSimpleName();

        System.out.println(className + " started...");

        Configuration config = new Configuration(new File(args[0]));

        String inputDir = config.get("edu.cmu.cs.lti.cds.event_tuple.path"); //"data/02_event_tuples";
        String blackListFile = config.get("edu.cmu.cs.lti.cds.blacklist"); //"duplicate.count.tail"

        DataPool.readBlackList(new File(blackListFile));
        DataPool.loadHeadStatistics(config, true);

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(typeSystemDescription, inputDir, false);

        AnalysisEngineDescription pmiCounter = AnalysisEngineFactory.createEngineDescription(
                PredicatePmiCalculator.class, typeSystemDescription,
                PredicatePmiCalculator.PARAM_PARENT_OUTPUT_DIR_PATH, "data",
                PredicatePmiCalculator.PARAM_BASE_OUTPUT_DIR_NAME, "predicate_pmi",
                PredicatePmiCalculator.PARAM_OUTPUT_STEP_NUMBER, 1
        );

        SimplePipeline.runPipeline(reader, pmiCounter);
        System.out.println(className + " completed.");
    }

}
