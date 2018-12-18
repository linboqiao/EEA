package edu.cmu.cs.lti.emd.stat;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.ZparTreeAnnotation;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/1/16
 * Time: 10:13 AM
 *
 * @author Zhengzhong Liu
 */
public class ChineseMentionStats extends AbstractLoggingAnnotator {

    public final static String PARAM_OUTPUT_PATH = "outputPath";
    @ConfigurationParameter(name=PARAM_OUTPUT_PATH)
    File outputPath;

    Table<String, String, Integer> surfaceCount = HashBasedTable.create();
    Table<String, String, Integer> headcharacterCount = HashBasedTable.create();
    Table<String, String, Integer> nounCharacterCount = HashBasedTable.create();
    Table<String, String, Integer> verbCharacterCount = HashBasedTable.create();

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        for (EventMention mention : JCasUtil.select(aJCas, EventMention.class)) {
            String type = mention.getEventType();
            increment(surfaceCount, mention.getCoveredText(), type);
            increment(headcharacterCount, UimaNlpUtils.findHeadCharacterFromZparAnnotation(mention).getCoveredText(),
                    type);

            List<String> nounChars = new ArrayList<>();
            List<String> verbChars = new ArrayList<>();

            for (ZparTreeAnnotation tree : JCasUtil.selectCovered(ZparTreeAnnotation.class, mention)) {
                String charLabel = tree.getAdditionalCharacterLabel();
                if (!tree.getIsLeaf() && charLabel != null) {
                    if (charLabel.equals("i") || charLabel.equals("b")) {
                        String posLabel = tree.getPennTreeLabel();
                        if (posLabel.startsWith("N")) {
                            nounChars.add(tree.getCoveredText());
                        } else if (posLabel.startsWith("V")) {
                            verbChars.add(tree.getCoveredText());
                        }
                    }
                }
            }

            for (String nounChar : nounChars) {
                increment(nounCharacterCount, nounChar, type);
            }

            for (String verbChar : verbChars) {
                increment(verbCharacterCount, verbChar, type);
            }
        }
    }

    private void increment(Table<String, String, Integer> counts, String surface, String type) {
        if (counts.contains(surface, type)) {
            counts.put(surface, type, counts.get(surface, type) + 1);
        } else {
            counts.put(surface, type, 1);
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();

        logger.info("Output path is " + outputPath);

        printMap(new File(outputPath, "full"), surfaceCount);
        printMap(new File(outputPath, "head"), headcharacterCount);
        printMap(new File(outputPath, "noun"), nounCharacterCount);
        printMap(new File(outputPath, "verb"), verbCharacterCount);
    }

    private void printMap(File outFile, Table<String, String, Integer> counts)  {
        List<Table.Cell<String, String, Integer>> sortedCounts = counts.cellSet().stream().sorted((o1, o2) -> new
                CompareToBuilder().append(o1.getValue(), o2.getValue()).append(o1.getRowKey(), o2.getRowKey()).append
                (o1.getColumnKey(), o2.getColumnKey()).toComparison()).collect(Collectors
                .toList());


        List<String> content = new ArrayList<>();
        for (Table.Cell<String, String, Integer> sortedCount : sortedCounts) {
            content.add(String.format("%s\t%s\t%s", sortedCount.getRowKey(), sortedCount.getColumnKey(),
                    sortedCount.getValue()));
        }

        try {
            FileUtils.writeLines(outFile, content);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
