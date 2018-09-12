package edu.cmu.cs.lti.script.annotators.stats;

import com.google.common.collect.HashBasedTable;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/15/14
 * Time: 5:39 PM
 */
public class ArgumentStatisticsCounter extends AbstractLoggingAnnotator {
    private static int numEvents = 0;

    private static HashBasedTable<String, String, Integer> argumentCounts = HashBasedTable.create();

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        logger.info(progressInfo(aJCas));

        for (EventMention evm : JCasUtil.select(aJCas, EventMention.class)) {
            numEvents++;
            Map<String, String> roles = new HashMap<>();

            for (EventMentionArgumentLink link : FSCollectionFactory.create(evm.getArguments(), EventMentionArgumentLink.class)) {
                String roleType = link.getArgumentRole();
                String componentId = link.getComponentId();

                //in this way, each role type only being counted once
                roles.put(roleType, componentId);
            }

            for (Map.Entry<String, String> roleEntry : roles.entrySet()) {
                int count = 1;
                String roleType = roleEntry.getKey();
                String componentId = roleEntry.getValue();
                if (argumentCounts.contains(roleType, componentId)) {
                    count += argumentCounts.get(roleType, componentId);
                }
                argumentCounts.put(roleType, componentId, count);
            }
        }
    }


    public static void main(String[] args) throws UIMAException, IOException {
        // Parameters for the reader
        String parentInput = "data";
        String baseInput = "02_event_tuples";
        // Parameters for the writer
        String paramTypeSystemDescriptor = "TypeSystem";
        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createTimeSortedGzipXmiReader(typeSystemDescription, parentInput, baseInput);

        AnalysisEngineDescription counter = AnalysisEngineFactory.createEngineDescription(
                ArgumentStatisticsCounter.class, typeSystemDescription);

        SimplePipeline.runPipeline(reader, counter);

        System.out.println("#Event_Mention :\t" + numEvents);

        for (String rowKey : argumentCounts.rowKeySet()) {
            String row = "";
            int totalCount = 0;
            for (Map.Entry<String, Integer> cell : argumentCounts.row(rowKey).entrySet()) {
                row += cell.getKey() + "\t" + cell.getValue() + "\t";
                totalCount += cell.getValue();
            }
            System.out.println(String.format("%s : %d\t%s", rowKey, totalCount, row));
        }
    }
}