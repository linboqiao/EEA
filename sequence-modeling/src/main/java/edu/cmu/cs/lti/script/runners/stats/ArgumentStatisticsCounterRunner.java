package edu.cmu.cs.lti.script.runners.stats;

import com.google.common.collect.HashBasedTable;
import edu.cmu.cs.lti.script.annotators.stats.ArgumentStatisticsCounter;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/15/14
 * Time: 5:47 PM
 */
public class ArgumentStatisticsCounterRunner {
    public static int numEvents = 0;

    public static HashBasedTable<String, String, Integer> argumentCounts = HashBasedTable.create();


    public static void main(String[] args) throws UIMAException, IOException {
        // Parameters for the reader
        String paramInputDir = "data/02_event_tuples";
        // Parameters for the writer
        String paramTypeSystemDescriptor = "TypeSystem";
        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createTimeSortedGzipXmiReader(typeSystemDescription, paramInputDir, false);

        AnalysisEngineDescription counter = CustomAnalysisEngineFactory.createAnalysisEngine(
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
