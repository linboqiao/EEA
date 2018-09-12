package edu.cmu.cs.lti.emd.annotators.lexical;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.ling.WordNetSearcher;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.TCollections;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Identify and hopefully disambiguate key senses from the corpus based on the distribution
 *
 * @author Zhengzhong Liu
 */
public class KeySenseExtractor extends AbstractAnnotator {

    private WordNetSearcher wns;

    public static final String PARAM_WORDNET_DICT_PATH = "wordNetDictionaryPath";
    @ConfigurationParameter(name = PARAM_WORDNET_DICT_PATH)
    private String wnDictPath;

    public static final String PARAM_STAT_OUTPUT_DIR = "statisticOutputPath";
    @ConfigurationParameter(name = PARAM_STAT_OUTPUT_DIR)
    private String statOutputDir;

    private TObjectIntMap<String> senseTfCounter = TCollections.synchronizedMap(new TObjectIntHashMap<>());
    private TObjectIntMap<String> senseDfCounter = TCollections.synchronizedMap(new TObjectIntHashMap<>());

    private PmiCollector triggerPmi = new PmiCollector();
    private PmiCollector sentencePmi = new PmiCollector();
    private PmiCollector depPmi = new PmiCollector();

    private AtomicInteger docCount = new AtomicInteger();
    private AtomicInteger sentenceCount = new AtomicInteger();

    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        try {
            wns = new WordNetSearcher(wnDictPath, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class PmiCollector {
        private TObjectIntMap<String> jointSenseTypeCounter = TCollections.synchronizedMap(new TObjectIntHashMap<>());
        private TObjectIntMap<String> typeCounter = TCollections.synchronizedMap(new TObjectIntHashMap<>());
        private TObjectIntMap<String> senseCounter = TCollections.synchronizedMap(new TObjectIntHashMap<>());

        private Table<String, String, Double> senseTypePmi;
        private Table<String, String, Double> senseTypeNPmi;
        private Table<String, String, Double> senseTypePmi2;

        private boolean calculated = false;

        public void calculatePmi() {
            if (calculated) {
                return;
            }
            // Store the sense-type PMI value.
            senseTypePmi = HashBasedTable.create();
            // Store the sense-type Normalized PMI value.
            senseTypeNPmi = HashBasedTable.create();
            // Store the PMI^2.
            senseTypePmi2 = HashBasedTable.create();

            // Compute type sense PMI.
            typeCounter.forEachEntry((type, typeCount) -> {
                senseCounter.forEachEntry((sense, senseCount) -> {
                    int jointCount = jointSenseTypeCounter.get(join(type, sense));
                    double numSent = 1.0 * sentenceCount.get();
                    double pJoint = jointCount / numSent;
                    double pType = typeCount / numSent;
                    double pSense = senseCount / numSent;
                    double preLogPmi = pJoint / (pType * pSense);
                    if (preLogPmi > 0) {
                        double pmi = Math.log(preLogPmi);
                        double npmi = pmi / (-Math.log(pJoint));
                        double pmi2 = pmi + Math.log(pJoint);
                        senseTypePmi.put(sense, type, pmi);
                        senseTypeNPmi.put(sense, type, npmi);
                        senseTypePmi2.put(sense, type, pmi2);
                    }
                    return true;
                });
                return true;
            });
        }

        public void observeSense(String sense) {
            senseCounter.adjustOrPutValue(sense, 1, 1);
        }

        public void observeType(String type) {
            typeCounter.adjustOrPutValue(type, 1, 1);
        }

        public void observeBoth(String sense, String type) {
            jointSenseTypeCounter.adjustOrPutValue(join(type, sense), 1, 1);
        }

        private String join(String sense, String type) {
            return String.format("%s::%s", sense, type);
        }

        public double getPmi(String sense, String type) {
            return senseTypePmi.get(sense, type);
        }

        public double getNormalizedPmi(String sense, String type) {
            return senseTypeNPmi.get(sense, type);
        }

        public double getPmi2(String sense, String type) {
            return senseTypePmi2.get(sense, type);
        }

        public Set<String> getTypes(){
            return typeCounter.keySet();
        }
    }

    // TODO : Make sentence based, argument based, head word based PMI, separated.

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas);
        // Go over all mentions, find the senses near these mentions (or just all mentions?)
        // Collect those that appear more.
        Set<String> appearedHypers = Collections.synchronizedSet(new HashSet<>());

        final JCas goldView = JCasUtil.getView(aJCas, goldStandardViewName, false);


        JCasUtil.select(aJCas, StanfordCorenlpSentence.class).forEach(sent -> {
                    Set<String> senseAppearedInSentence = new HashSet<>();
                    Set<String> typeAppearedInSentence = new HashSet<>();

                    // Collect the dependent words of the head word.
                    Map<EventMention, StanfordCorenlpToken> mention2DepNouns = new HashMap<>();
                    for (EventMention mention : JCasUtil.selectCovered(goldView, EventMention.class,
                            sent.getBegin(), sent.getEnd())) {
                        typeAppearedInSentence.add(mention.getEventType());

                        List<StanfordCorenlpToken> dependentWords = UimaNlpUtils.getDependentWords(
                                (StanfordCorenlpToken) mention.getHeadWord(), StanfordCorenlpToken.class);

                        for (StanfordCorenlpToken word : dependentWords) {
                            if (word.getPos().startsWith("N")) {
                                mention2DepNouns.put(mention, word);
                            }
                        }
                    }

                    for (Map.Entry<EventMention, StanfordCorenlpToken> m2Deps : mention2DepNouns.entrySet()) {
                        EventMention mention = m2Deps.getKey();
                        StanfordCorenlpToken depToken = m2Deps.getValue();

                        StanfordCorenlpToken headword = (StanfordCorenlpToken) mention.getHeadWord();

                        Set<String> allNounHypernyms = wns.getFirstWordForAllHypernymsForAllSense(
                                depToken.getLemma().toLowerCase());

                        Set<String> headWordSenses = wns.getFirstWordForAllHypernymsForAllSense(
                                headword.getLemma().toLowerCase());

                        String eventType = mention.getEventType();

                        for (String sense : allNounHypernyms) {
                            triggerPmi.observeBoth(sense, eventType);
                            triggerPmi.observeSense(sense);
                            triggerPmi.observeType(eventType);
                        }

                        for (String sense : headWordSenses) {
                            depPmi.observeType(eventType);
                            depPmi.observeSense(sense);
                            depPmi.observeBoth(sense, eventType);
                        }
                    }

                    // Seems that you cannot do JCas query inside a parallel JCas iterator.
                    JCasUtil.selectCovered(StanfordCorenlpToken.class, sent).stream().parallel()
                            .filter(token -> token.getPos().startsWith("N")).forEach(token -> {
                        Set<String> allNounHypernyms = wns.getFirstWordForAllHypernymsForAllSense(token.getLemma());
                        for (String hyper : allNounHypernyms) {
                            senseTfCounter.adjustOrPutValue(hyper, 1, 1);
                            appearedHypers.add(hyper);
                            senseAppearedInSentence.add(hyper);

                            depPmi.observeSense(hyper);
                        }
                    });


                    for (String sense : senseAppearedInSentence) {
                        sentencePmi.observeSense(sense);
                    }

                    for (String type : typeAppearedInSentence) {
                        sentencePmi.observeType(type);
                        for (String sense : senseAppearedInSentence) {
                            sentencePmi.observeBoth(type, sense);
                        }
                    }

                    sentenceCount.incrementAndGet();
                }
        );

        for (String appearedHyper : appearedHypers) {
            senseDfCounter.adjustOrPutValue(appearedHyper, 1, 1);
        }

        docCount.incrementAndGet();
    }

    private String join(String sense, String type) {
        return String.format("%s::%s", sense, type);
    }

    public void collectionProcessComplete() {

        // Store the sense-type PMI value.
        Table<String, String, Double> senseTypePmi = HashBasedTable.create();
        // Store the sense-type Normalized PMI value.
        Table<String, String, Double> senseTypeNPmi = HashBasedTable.create();
        // Store the PMI^2.
        Table<String, String, Double> senseTypePmi2 = HashBasedTable.create();

        Stream<String> sortedTypeNames = sentencePmi.getTypes().stream().sorted();

        // Write out the top PMI senses associated with a particular type.
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(statOutputDir, "typePmi.txt")))) {
            sortedTypeNames.forEach(t -> {
                String topPmi = getTopCells(senseTypePmi.column(t));
                String topNpmi = getTopCells(senseTypeNPmi.column(t));
                String topPmi2 = getTopCells(senseTypePmi2.column(t));

                try {
                    writer.write(String.format("%s\t%s\t%s\t%s\n", t, topPmi, topNpmi, topPmi2));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Write out the statistics related to a sense, sorted alphabetically.
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(statOutputDir, "senseCount.txt")))) {
            senseTfCounter.keySet().stream().sorted().forEach(s -> {
                int tf = senseTfCounter.get(s);
                int df = senseDfCounter.get(s);
                double tfIdf = tf * Math.log(docCount.get() / df);

                String topPmi = getTopCells(senseTypePmi.row(s));
                String topNpmi = getTopCells(senseTypeNPmi.row(s));
                String topPmi2 = getTopCells(senseTypePmi2.row(s));

                try {
                    writer.write(String.format(
                            "%s\t%d\t%d\t%.4f\t%s\t%s\t%s\n", s, tf, df, tfIdf, topPmi, topNpmi, topPmi2));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getTopCells(Map<String, Double> row) {
        int limit = 3;
        for (int i = row.size(); i < limit; i++) {
            row.put(i + "-", 0.0);
        }

        return row.entrySet().stream().sorted(
                (a, b) -> Double.compare(b.getValue(), a.getValue())
        ).limit(limit).map(d -> String.format("%s\t%.4f", d.getKey(), d.getValue()))
                .collect(Collectors.joining("\t"));
    }

    public static void main(String[] argv) throws UIMAException, IOException {
        Configuration taskConfig = new Configuration(argv[0]);

        String paramTypeSystemDescriptor = "TaskEventMentionDetectionTypeSystem";
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);
        String trainingWorkingDir = taskConfig.get("edu.cmu.cs.lti.training.working.dir");
        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, trainingWorkingDir, "preprocessed");

        AnalysisEngineDescription runner = AnalysisEngineFactory.createEngineDescription(
                KeySenseExtractor.class, typeSystemDescription,
                KeySenseExtractor.PARAM_STAT_OUTPUT_DIR,
                edu.cmu.cs.lti.utils.FileUtils.joinPaths(taskConfig.get("edu.cmu.cs.lti.stats.dir"),
                        "kbp/LDC2015E73"),
                KeySenseExtractor.PARAM_WORDNET_DICT_PATH,
                edu.cmu.cs.lti.utils.FileUtils.joinPaths(taskConfig.get("edu.cmu.cs.lti.resource.dir"),
                        taskConfig.get("edu.cmu.cs.lti.wndict.path"))
        );

        SimplePipeline.runPipeline(reader, runner);
    }
}
