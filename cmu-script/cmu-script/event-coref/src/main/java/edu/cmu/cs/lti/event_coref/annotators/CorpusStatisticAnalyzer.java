package edu.cmu.cs.lti.event_coref.annotators;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.script.type.Event;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.io.reader.XmiCollectionReader;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.*;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Date: 11/8/15
 * Time: 8:34 PM
 *
 * @author Zhengzhong Liu
 */
public class CorpusStatisticAnalyzer extends JCasAnnotator_ImplBase {
    private int numberMentions = 0;

    private int numTokens = 0;

    private int numDocs = 0;

    private int numClusters = 0;

    private int numMentionsInCluster = 0;

    private int longestCluster = 0;

    private String longestClusterDoc = "";

    private TObjectIntMap<String> typeCount = new TObjectIntHashMap<>();

    private TObjectIntMap<String> surfaceCount = new TObjectIntHashMap<>();

    public static final String PARAM_OUTPUT_DIR = "subOutputDir";

    @ConfigurationParameter(name = PARAM_OUTPUT_DIR)
    private String outputDir;

    Joiner tabJoiner = Joiner.on("\t");

    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        FileUtils.ensureDirectory(outputDir);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas goldView = null;
        try {
            goldView = aJCas.getView("GoldStandard");
        } catch (CASException e) {
            e.printStackTrace();
        }

        for (EventMention mention : JCasUtil.select(goldView, EventMention.class)) {
            numberMentions++;
            typeCount.adjustOrPutValue(mention.getEventType(), 1, 1);
            surfaceCount.adjustOrPutValue(getLemmas(aJCas, mention), 1, 1);
        }

        numTokens += JCasUtil.select(aJCas, StanfordCorenlpToken.class).size();

        Collection<Event> allEvents = JCasUtil.select(goldView, Event.class);
        numClusters += allEvents.size();

        for (Event e : allEvents) {
            int clusterSize = e.getEventMentions().size();
            numMentionsInCluster += clusterSize;
            if (clusterSize > longestCluster) {
                longestCluster = clusterSize;
                longestClusterDoc = UimaConvenience.getDocId(aJCas);
            }
        }

        numDocs++;
    }

    private String getLemmas(JCas aJCas, EventMention mention) {
        StringBuilder sb = new StringBuilder();
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(aJCas, StanfordCorenlpToken.class,
                mention.getBegin(), mention.getEnd())) {
            sb.append(token.getLemma().toLowerCase());
            sb.append(" ");
        }
        return sb.toString();
    }

    public void collectionProcessComplete() {
        writeMapByline(typeCount, outputDir + "/typeCount.txt");
        writeMapByline(surfaceCount, outputDir + "/surfaceCount.txt");

        try {
            Writer summary = new BufferedWriter(new FileWriter(new File(outputDir + "/summary.txt")));
            writeByTab(summary, "Number of mentions", numberMentions + "");
            writeByTab(summary, "Number of events", numClusters + "");
            writeByTab(summary, "Number of tokens", numTokens + "");
            writeByTab(summary, "Number of documents", numDocs + "");
            writeByTab(summary, "Number of singletons", (numberMentions - numMentionsInCluster) + "");
            writeByTab(summary, "Average mention per document", numberMentions * 1.0 / numDocs + "");
            writeByTab(summary, "Average token per document", numTokens * 1.0 / numDocs + "");
            writeByTab(summary, "Average cluster size", numMentionsInCluster * 1.0 / numClusters + "");
            writeByTab(summary, "Largest cluster", longestCluster + "");
            writeByTab(summary, "Largest cluster doc id", longestClusterDoc);
            writeByTab(summary, "Number token per mention", numTokens * 1.0 / numberMentions + "");
            summary.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeByTab(Writer writer, String... strings) {
        try {
            writer.write(tabJoiner.join(strings) + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeMapByline(TObjectIntMap<String> typeCount, String outputFilePath) {
        try {
            Writer typeWriter = new BufferedWriter(new FileWriter(new File(outputFilePath)));
            for (String type : typeCount.keySet().stream().sorted().collect(Collectors.toList())) {
                typeWriter.write(String.format("%s\t%d\n", type, typeCount.get(type)));
            }
            typeWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void analyze(TypeSystemDescription typeSystemDescription, String dataset) throws
            UIMAException, IOException {
        String baseOut = "stats/kbp/";

        String trainDatasource = "data/mention/kbp/" + dataset + "/preprocessed";

        CollectionReaderDescription trainReader = CollectionReaderFactory.createReaderDescription(
                XmiCollectionReader.class, typeSystemDescription,
                XmiCollectionReader.PARAM_INPUT_DIR, trainDatasource
        );
        AnalysisEngineDescription trainAnalyzer = AnalysisEngineFactory.createEngineDescription(
                CorpusStatisticAnalyzer.class, typeSystemDescription,
                CorpusStatisticAnalyzer.PARAM_OUTPUT_DIR, baseOut + dataset);

        SimplePipeline.runPipeline(trainReader, trainAnalyzer);
    }


    public static void main(String[] args) throws IOException, UIMAException {
        Configuration commonConfig = new Configuration("settings/common.properties");
        Configuration kbpConfig = new Configuration("settings/kbp.properties");

        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");
        String baseOut = "stats/kbp/";

        TypeSystemDescription typeSystemDescription =
                TypeSystemDescriptionFactory.createTypeSystemDescription(typeSystemName);

        analyze(typeSystemDescription, "LDC2015E95");
        analyze(typeSystemDescription, "LDC2015E73");
        analyze(typeSystemDescription, "LDC2015E73_news");
        analyze(typeSystemDescription, "LDC2015E73_forum");

        String goldStandardPath = "data/mention/LDC/LDC2015R26/data/tbf/EvalEventHopper20150903.tbf";
        String plainTextPath = "data/mention/LDC/LDC2015R26/data/source";
        String tokenMapPath = "data/mention/LDC/LDC2015R26/data/tkn";

        CollectionReaderDescription goldReader = CollectionReaderFactory.createReaderDescription(
                TbfEventDataReader.class, typeSystemDescription,
                TbfEventDataReader.PARAM_GOLD_STANDARD_FILE, goldStandardPath,
                TbfEventDataReader.PARAM_SOURCE_EXT, ".txt",
                TbfEventDataReader.PARAM_SOURCE_TEXT_DIRECTORY, plainTextPath,
                TbfEventDataReader.PARAM_TOKEN_DIRECTORY, tokenMapPath,
                TbfEventDataReader.PARAM_TOKEN_EXT, ".tab",
                TbfEventDataReader.PARAM_INPUT_VIEW_NAME, UimaConst.inputViewName
        );

        AnalysisEngineDescription goldAnalyzer = AnalysisEngineFactory.createEngineDescription(
                CorpusStatisticAnalyzer.class, typeSystemDescription,
                CorpusStatisticAnalyzer.PARAM_OUTPUT_DIR, baseOut + "LDC2015R26");

        SimplePipeline.runPipeline(goldReader, goldAnalyzer);

    }

}
