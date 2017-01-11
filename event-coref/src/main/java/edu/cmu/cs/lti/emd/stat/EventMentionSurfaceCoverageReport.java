package edu.cmu.cs.lti.emd.stat;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.annotators.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.event_coref.annotators.GoldStandardEventMentionAnnotator;
import edu.cmu.cs.lti.io.EventDataReader;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.pipeline.ProcessorWrapper;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import edu.emory.mathcs.backport.java.util.Collections;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * We take some the head word lemma of those nuggets that ever appear as event nugget, and record whether all these
 * lemmas are annotated as nuggets.
 *
 * @author Zhengzhong Liu
 */
public class EventMentionSurfaceCoverageReport extends AbstractLoggingAnnotator {
    public final static String PARAM_EVENT_MENTION_SURFACE = "eventMentionSurface";
    @ConfigurationParameter(name = PARAM_EVENT_MENTION_SURFACE)
    private File surfaceFile;

    public final static String PARAM_REPORT_OUTPUT = "reportOutputDir";
    @ConfigurationParameter(name = PARAM_REPORT_OUTPUT)
    private File reportOutput;

    public final static String PARAM_DATA_SET_NAME = "datasetName";
    @ConfigurationParameter(name = PARAM_DATA_SET_NAME)
    private String datasetName;

    public final static String PARAM_LANGUAGE = "language";
    @ConfigurationParameter(name = PARAM_LANGUAGE, defaultValue = "en")
    private String language;

    private List<String> surfaces = new ArrayList<>();

    private TObjectIntMap<String> surfaceCounts;
    private TObjectIntMap<String> surfaceMisses;

    private ArrayListMultimap<String, String> missedContext;
    private ArrayListMultimap<String, String> annotatedContext;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        try {
            for (String s : FileUtils.readLines(surfaceFile)) {
                String[] parts = s.split("\t");
                if (parts.length == 2) {
                    surfaces.add(parts[0]);
                }
            }
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

        surfaceCounts = new TObjectIntHashMap<>();
        surfaceMisses = new TObjectIntHashMap<>();

        missedContext = ArrayListMultimap.create();
        annotatedContext = ArrayListMultimap.create();

        logger.info("Producing report for " + datasetName);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        String docId = UimaConvenience.getDocId(aJCas);

        TIntObjectMap<EventMention> eventMentionOccurrences = new TIntObjectHashMap<>();

        Collection<EventMention> mentions = JCasUtil.select(aJCas, EventMention.class);
        Collection<StanfordCorenlpSentence> sentences = JCasUtil.select(aJCas, StanfordCorenlpSentence.class);

        for (EventMention eventMention : mentions) {
            for (int i = eventMention.getBegin(); i < eventMention.getEnd(); i++) {
                eventMentionOccurrences.put(i, eventMention);
            }
        }

        for (StanfordCorenlpSentence sentence : sentences) {
            String sentenceStr = sentence.getCoveredText();

            //We want to match something ignoring the white spaces, so we use a whitespace ignoring pattern.
            for (String surface : surfaces) {
                String surfacePattern = whiteSpaceAgnosticPattern(surface);
                Pattern p = Pattern.compile(surfacePattern);
                Matcher m = p.matcher(sentenceStr);
                int count = 0;

                while (m.find()) {
//                for (int index = 0; (index = sentenceStr.indexOf(surface, index)) >= 0; index++) {
                    int index = m.start();

                    // Find the occurrence, check if a mention is here.
                    int docIndex = sentence.getBegin() + index;
                    int end = index + m.group().length();

//                    logger.info("Text is " + sentenceStr);
//                    logger.info("Matching " + surfacePattern);
//                    logger.info("Match found at " + index + ", end at " + end);

                    if (eventMentionOccurrences.containsKey(docIndex)) {
                        // This appearance is annotated.
                        annotatedContext.put(surface, getAnnotatedContenxt(sentenceStr,
                                eventMentionOccurrences.get(docIndex), index, end, docId));
                    } else {
                        // No annotation found here.
                        surfaceMisses.adjustOrPutValue(surface, 1, 1);
                        missedContext.put(surface, getMissedContext(sentenceStr, index, end, docId));
                    }

                    count++;
                }
                surfaceCounts.adjustOrPutValue(surface, count, count);
            }
        }
    }

    private String whiteSpaceAgnosticPattern(String surface) {
        StringBuilder sb = new StringBuilder();

        String split = "";
        for (char c : surface.toCharArray()) {
            sb.append(split);
            sb.append(c);
            split = "\\s*";
        }

        return sb.toString();
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();

        File datasetReportOutput = new File(reportOutput, datasetName);

        if (!datasetReportOutput.isDirectory()) {
            datasetReportOutput.mkdirs();
        }

        logger.info("Reports will be available at " + datasetReportOutput);

        File surfaceCountOutput = new File(datasetReportOutput, "surface_stats.txt");
        File missedContextOutDir = new File(datasetReportOutput, "missed_contexts");
        File annotatedContextDir = new File(datasetReportOutput, "annotates");

        if (!missedContextOutDir.isDirectory()) {
            missedContextOutDir.mkdirs();
        }

        if (!annotatedContextDir.isDirectory()) {
            annotatedContextDir.mkdirs();
        }

        List<Pair<Pair<String, Double>, Pair<Integer, Integer>>> aggregateCounts = new ArrayList<>();

        for (TObjectIntIterator<String> iter = surfaceCounts.iterator(); iter.hasNext(); ) {
            iter.advance();
            String surface = iter.key();
            int surfaceCount = iter.value();
            int missedCount = surfaceMisses.get(surface);

            if (surfaceCount > 0) {
                double percentage = 1 - 1.0 * missedCount / surfaceCount;
                aggregateCounts.add(Pair.of(Pair.of(iter.key(), percentage), Pair.of(missedCount, surfaceCount)));
            }
        }

        Collections.sort(aggregateCounts, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                Pair<Pair<String, Double>, Pair<Integer, Integer>> c1 = (Pair) o1;
                Pair<Pair<String, Double>, Pair<Integer, Integer>> c2 = (Pair) o2;

                return new CompareToBuilder().append(c2.getKey().getValue(), c1.getKey().getValue())
                        .append(c1.getKey().getKey(), c2.getKey().getKey()).toComparison();
            }
        });

        StringBuilder sb = new StringBuilder();
        sb.append("Surface\tCoverage\tNot Annotated\tAppearance\n");
        for (Pair<Pair<String, Double>, Pair<Integer, Integer>> aggregateCount : aggregateCounts) {
            sb.append(String.format("%s\t%.2f\t%d\t%d\n", aggregateCount.getKey().getKey(),
                    aggregateCount.getKey().getValue(), aggregateCount.getValue().getKey(),
                    aggregateCount.getValue().getValue()));
        }
        String countsStr = sb.toString();


        try {
            FileUtils.write(surfaceCountOutput, countsStr);

            for (String s : missedContext.keySet()) {
                File missedContextOutput = new File(missedContextOutDir, s);
                FileUtils.writeLines(missedContextOutput, missedContext.get(s));
            }
            for (String s : annotatedContext.keySet()) {
                File annotatedContextOutput = new File(annotatedContextDir, s);
                FileUtils.writeLines(annotatedContextOutput, annotatedContext.get(s));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String getMissedContext(String sentence, int begin, int end, String docId) {
        String targetText = sentence.substring(begin, end);
        String inContext = sentence.substring(0, begin) + "[" + targetText + "]" + sentence.substring(end);
        String nicerText = language.equals("zh") ? inContext.replaceAll("\\s", "") : inContext.replaceAll("\n", " ");
        return String.format("%s\t%s\t%s", targetText, docId, nicerText);
    }

    private String getAnnotatedContenxt(String sentence, EventMention mention, int begin, int end, String docId) {
        String targetText = sentence.substring(begin, end);
        String inContext = sentence.substring(0, begin) + "[" + targetText + "]" + sentence.substring(end);
        String nicerText = language.equals("zh") ? inContext.replaceAll("\\s", "") : inContext.replaceAll("\n", " ");

        return String.format("%s\t%s\t%s\t%s\t%s", targetText, mention.getEventType(), mention.getRealisType(), docId,
                nicerText);
    }


    public static void main(String[] args) throws UIMAException, IOException, CpeDescriptorException, SAXException {
        // We focused on a couple Chinese data:
        //
        // Rich ERE:
        // LDC2014E114_DEFT_ERE_Chinese_and_English_Parallel_Annotation
        // LDC2015E105_DEFT_Rich_ERE_Chinese_Training_Annotation
        // LDC2015E78_DEFT_Rich_ERE_Chinese_and_English_Parallel_Annotation_V2
        // LDC2015E112_DEFT_Rich_ERE_Chinese_Training_Annotation_R2
        //
        // ACE:
        // ACE2005-TrainingData-V4.0/Chinese

        if (args.length < 2) {
            System.out.println("Please provide where to write stats.");
            System.exit(1);
        }

        // "../data/stats/kbp/chinese_mention_surfaces.tsv"
        String mentionSurfaceFile = args[0];

        // "../data/stats/kbp/chinese_mention_coverage_report"
        String statOutputDir = args[1];

        // A place to put intermediate files.
        String workingDir = args[2];

        Configuration commonConfig = new Configuration("settings/common.properties");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(typeSystemName);

        String[] datasetNames = {"LDC2014E114", "LDC2015E105", "LDC2015E78", "LDC2015E112_R2", "ACE2005_Chinese"};

        EventDataReader dataReader = new EventDataReader(workingDir, "all", false);

        for (String datasetName : datasetNames) {
            Configuration datasetConfig = new Configuration(
                    new File(commonConfig.get("edu.cmu.cs.lti.dataset.settings.path"), datasetName + ".properties"));
            dataReader.readData(datasetConfig, typeSystemDescription);
        }

        CollectionReaderDescription reader = dataReader.getReader();

        AnalysisEngineDescription goldAnnotator = AnalysisEngineFactory.createEngineDescription(
                GoldStandardEventMentionAnnotator.class, typeSystemDescription,
                GoldStandardEventMentionAnnotator.PARAM_TARGET_VIEWS,
                new String[]{CAS.NAME_DEFAULT_SOFA, UimaConst.inputViewName},
                GoldStandardEventMentionAnnotator.PARAM_COPY_MENTION_TYPE, true,
                GoldStandardEventMentionAnnotator.PARAM_COPY_REALIS, true,
                GoldStandardEventMentionAnnotator.PARAM_COPY_CLUSTER, true,
                GoldStandardEventMentionAnnotator.PARAM_COPY_RELATIONS, true
        );

        AnalysisEngineDescription surfaceCollector = AnalysisEngineFactory
                .createEngineDescription(EventMentionSurfaceCollector.class,
                        EventMentionSurfaceCollector.PARAM_OUTPUT_FILE_PATH, mentionSurfaceFile
                );

        SimplePipeline.runPipeline(reader, goldAnnotator, surfaceCollector);

        for (int i = 0; i < datasetNames.length; i++) {
            EventDataReader datasetEventReader = new EventDataReader(workingDir, datasetNames[i], false);

            String datasetName = datasetNames[i];

            CollectionReaderDescription dataSetReader = datasetEventReader.getReader();

            AnalysisEngineDescription stanfordProcessor = AnalysisEngineFactory.createEngineDescription(
                    StanfordCoreNlpAnnotator.class, typeSystemDescription,
                    StanfordCoreNlpAnnotator.PARAM_LANGUAGE, "zh",
                    StanfordCoreNlpAnnotator.PARAM_SPLIT_ONLY, true
            );

            AnalysisEngineDescription coverageReport = AnalysisEngineFactory.createEngineDescription(
                    EventMentionSurfaceCoverageReport.class,
                    EventMentionSurfaceCoverageReport.PARAM_EVENT_MENTION_SURFACE, mentionSurfaceFile,
                    EventMentionSurfaceCoverageReport.PARAM_REPORT_OUTPUT, statOutputDir,
                    EventMentionSurfaceCoverageReport.PARAM_DATA_SET_NAME, datasetName
            );


            new BasicPipeline(new ProcessorWrapper() {
                @Override
                public CollectionReaderDescription getCollectionReader() throws ResourceInitializationException {
                    return dataSetReader;
                }

                @Override
                public AnalysisEngineDescription[] getProcessors() throws ResourceInitializationException {
                    return new AnalysisEngineDescription[]{goldAnnotator, stanfordProcessor, coverageReport};
                }
            }).run();
        }
    }
}
