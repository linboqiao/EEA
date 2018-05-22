package edu.cmu.cs.lti.script.annotators.writer;

import com.google.common.collect.ArrayListMultimap;
import com.google.gson.Gson;
import edu.cmu.cs.lti.annotators.GoldStandardEventMentionAnnotator;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.annotators.EnglishSrlArgumentExtractor;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.script.utils.ImplicitFeaturesExtractor;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.io.FileUtils;
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
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.zip.GZIPOutputStream;


/**
 * Given dependency parses and coreference chains, create argument
 * cloze tasks.
 * Date: 3/24/18
 * Time: 4:32 PM
 *
 * @author Zhengzhong Liu
 */
public class ArgumentClozeTaskWriter extends AbstractLoggingAnnotator {
    public static final String PARAM_OUTPUT_FILE = "outputFile";
    @ConfigurationParameter(name = PARAM_OUTPUT_FILE)
    private String outputFile;

    public static final String PARAM_CONTEXT_WINDOW = "contextWindow";
    @ConfigurationParameter(name = PARAM_CONTEXT_WINDOW, defaultValue = "5")
    private int contextWindowSize;

    public static final String PARAM_FRAME_MAPPINGS = "frameMappings";
    @ConfigurationParameter(name = PARAM_FRAME_MAPPINGS, mandatory = false)
    private File frameMappingFile;

    public static final String PARAM_ADD_EVENT_COREF = "addEventCoref";
    @ConfigurationParameter(name = PARAM_ADD_EVENT_COREF, defaultValue = "false")
    private boolean addEventCoref;

    private OutputStreamWriter writer;

    private Gson gson = new Gson();

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        try {
            writer = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outputFile + ".gz")));
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    static class ClozeDoc {
        String docid;
        List<String> sentences;
        List<ClozeEvent> events;
        List<ClozeEntity> entities;
        List<CorefCluster> eventCorefClusters;
    }

    static class CorefCluster {
        List<Integer> elementIds;
    }


    static class ClozeEntity {
        int entityId;
        double[] entityFeatures;
        String[] featureNames;
        String representEntityHead;
    }

    static class ClozeEvent {
        String predicate;
        String context;
        int sentenceId;
        int predicateStart;
        int predicateEnd;
        String frame;
        List<ClozeArgument> arguments;

        int eventId;

        static class ClozeArgument {
            String feName;
            String dep;
            String context;
            String text;
            int entityId;

            int argStart;
            int argEnd;
        }
    }

    private void addCoref(JCas aJCas, ClozeDoc doc, TObjectIntMap<EventMention> eid2Event) {
        doc.eventCorefClusters = new ArrayList<>();

        for (Event event : JCasUtil.select(aJCas, Event.class)) {
            CorefCluster cluster = new CorefCluster();
            cluster.elementIds = new ArrayList<>();

            for (EventMention eventMention : FSCollectionFactory.create(event.getEventMentions(), EventMention.class)) {
                if (eid2Event.containsKey(eventMention)) {
                    cluster.elementIds.add(eid2Event.get(eventMention));
                }
            }
            doc.eventCorefClusters.add(cluster);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        // Assign IDs.
        int id = 0;
        for (Entity entity : JCasUtil.select(aJCas, Entity.class)) {
            entity.setId(String.valueOf(id));
            entity.setIndex(id);
            id++;
        }

        ClozeDoc doc = new ClozeDoc();
        doc.events = new ArrayList<>();
        doc.docid = UimaConvenience.getArticleName(aJCas);
        doc.sentences = new ArrayList<>();

        Collection<StanfordCorenlpToken> allTokens = JCasUtil.select(aJCas, StanfordCorenlpToken.class);
        String[] lemmas = new String[allTokens.size()];
        int tIndex = 0;
        for (StanfordCorenlpToken token : allTokens) {
            lemmas[tIndex] = token.getLemma().toLowerCase();
            token.setIndex(tIndex);
            tIndex++;
        }

        List<StanfordCorenlpSentence> sentences = new ArrayList<>(
                JCasUtil.select(aJCas, StanfordCorenlpSentence.class));

        ArrayListMultimap<EntityMention, ClozeEvent.ClozeArgument> argumentMap = ArrayListMultimap.create();

        TObjectIntMap<EventMention> eid2Event = new TObjectIntHashMap<>();

        int eventId = 0;
        for (int sentId = 0; sentId < sentences.size(); sentId++) {
            StanfordCorenlpSentence sentence = sentences.get(sentId);

            doc.sentences.add(sentence.getCoveredText());

            for (EventMention eventMention : JCasUtil.selectCovered(EventMention.class, sentence)) {
                if (eventMention.getHeadWord() == null) {
                    eventMention.setHeadWord(UimaNlpUtils.findHeadFromStanfordAnnotation(eventMention));
                }


                ClozeEvent ce = new ClozeEvent();
                ce.sentenceId = sentId;

                List<Word> complements = new ArrayList<>();

                String predicate_text = UimaNlpUtils.getPredicate(eventMention.getHeadWord(), complements, false);

                String frame = eventMention.getFrameName();
                if (frame == null) {
                    frame = "NA";
                }

                String predicate_context = getContext(lemmas, (StanfordCorenlpToken) eventMention.getHeadWord());

                ce.predicate = predicate_text;
                ce.context = predicate_context;
                ce.predicateStart = eventMention.getBegin() - sentence.getBegin();
                ce.predicateEnd = eventMention.getEnd() - sentence.getBegin();
                ce.frame = frame;
                ce.eventId = eventId++;

                eid2Event.put(eventMention, ce.eventId);

                FSList argsFS = eventMention.getArguments();
                Collection<EventMentionArgumentLink> argLinks = FSCollectionFactory.create(argsFS,
                        EventMentionArgumentLink.class);

                List<ClozeEvent.ClozeArgument> clozeArguments = new ArrayList<>();
                for (EventMentionArgumentLink argLink : argLinks) {
                    ClozeEvent.ClozeArgument ca = new ClozeEvent.ClozeArgument();

                    String role = argLink.getArgumentRole();
                    if (role == null) {
                        role = "NA";
                    }
                    String fe = argLink.getFrameElementName();
                    if (fe == null) {
                        fe = "NA";
                    }
                    EntityMention en = argLink.getArgument();
                    Word argHead = en.getHead();

                    String argText = en.getHead().getLemma();
                    argText = onlySpace(argText);

                    String argumentContext = getContext(lemmas, (StanfordCorenlpToken) argHead);

                    ca.feName = fe;
                    ca.dep = role;
                    ca.context = argumentContext;
                    ca.entityId = en.getReferingEntity().getIndex();
                    ca.text = onlySpace(argText);

                    ca.argStart = en.getBegin() - sentence.getBegin();
                    ca.argEnd = en.getEnd() - sentence.getBegin();

                    clozeArguments.add(ca);

                    argumentMap.put(en, ca);
                }
                ce.arguments = clozeArguments;
                doc.events.add(ce);
            }
        }

        if (addEventCoref) {
            addCoref(aJCas, doc, eid2Event);
        }

        Map<Entity, SortedMap<String, Double>> implicitFeatures = ImplicitFeaturesExtractor.getArgumentFeatures(aJCas);


        List<ClozeEntity> clozeEntities = new ArrayList<>();

        for (Map.Entry<Entity, SortedMap<String, Double>> entityFeatures : implicitFeatures.entrySet()) {
            Entity entity = entityFeatures.getKey();
            Map<String, Double> features = entityFeatures.getValue();

            ClozeEntity clozeEntity = new ClozeEntity();
            clozeEntity.representEntityHead = onlySpace(entity.getRepresentativeMention()
                    .getHead().getLemma().toLowerCase());

            double[] featureArray = new double[features.size()];
            String[] featureNameArray = new String[features.size()];
            int index = 0;
            for (Map.Entry<String, Double> feature : features.entrySet()) {
                featureArray[index] = feature.getValue();
                featureNameArray[index] = feature.getKey();
                index++;
            }
            clozeEntity.entityFeatures = featureArray;
            clozeEntity.featureNames = featureNameArray;
            clozeEntity.entityId = entity.getIndex();

            clozeEntities.add(clozeEntity);
        }

        doc.entities = clozeEntities;

        try {
            writer.write(gson.toJson(doc) + "\n");
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private String onlySpace(String text) {
        return text.trim().replaceAll("\t", " ").replaceAll("\n", " ");
    }

    private String getContext(String[] lemmas, StanfordCorenlpToken token) {
        int index = token.getIndex();

        int left = index - contextWindowSize;
        if (left < 0) {
            left = 0;
        }

        int right = index + contextWindowSize;
        if (right > lemmas.length - 1) {
            right = lemmas.length - 1;
        }

        StringBuilder sb = new StringBuilder();

        for (int i = left; i < index; i++) {
            sb.append(lemmas[i]).append(" ");
        }

        // Indicate the context center.
        sb.append("___");

        for (int i = index + 1; i <= right; i++) {
            sb.append(" ").append(lemmas[i]);
        }

        return onlySpace(sb.toString());
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        try {
            writer.close();
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private void loadFrameMaps() throws IOException {
        for (String line : FileUtils.readLines(frameMappingFile)) {
            String[] parts = line.split("\t");
            String framePart = parts[0];
            String argPart = parts[1];
        }
    }

    public static void main(String[] args) throws UIMAException, IOException, CpeDescriptorException, SAXException {
        String paramTypeSystemDescriptor = "TaskEventMentionDetectionTypeSystem";

        String workingDir = args[0];
        String inputBase = args[1];
        String outputFile = args[2];

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        // Reader and extractors for unsupervised events.
//        CollectionReaderDescription reader = CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(
//                typeSystemDescription, workingDir, inputBase
//        );
//        AnalysisEngineDescription remover = AnalysisEngineFactory.createEngineDescription(EventMentionRemover.class);
//
//        AnalysisEngineDescription verbEvents = AnalysisEngineFactory.createEngineDescription(
//                VerbBasedEventDetector.class, typeSystemDescription
//        );
//
//        AnalysisEngineDescription frameEvents = AnalysisEngineFactory.createEngineDescription(
//                FrameBasedEventDetector.class, typeSystemDescription,
//                FrameBasedEventDetector.PARAM_FRAME_RELATION, "../data/resources/fndata-1.7/frRelation.xml",
//                FrameBasedEventDetector.PARAM_IGNORE_BARE_FRAME, true
//        );

        // Reader and extractors for existing mentions.
        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(
                typeSystemDescription, workingDir, inputBase
        );

        AnalysisEngineDescription goldAnnotator = AnalysisEngineFactory.createEngineDescription(
                GoldStandardEventMentionAnnotator.class, typeSystemDescription,
                GoldStandardEventMentionAnnotator.PARAM_TARGET_VIEWS,
                new String[]{CAS.NAME_DEFAULT_SOFA, UimaConst.inputViewName},
                GoldStandardEventMentionAnnotator.PARAM_COPY_MENTION_TYPE, true,
                GoldStandardEventMentionAnnotator.PARAM_COPY_REALIS, true,
                GoldStandardEventMentionAnnotator.PARAM_COPY_CLUSTER, true,
                GoldStandardEventMentionAnnotator.PARAM_COPY_RELATIONS, true
        );

        AnalysisEngineDescription arguments = AnalysisEngineFactory.createEngineDescription(
                EnglishSrlArgumentExtractor.class, typeSystemDescription,
                EnglishSrlArgumentExtractor.PARAM_ADD_SEMAFOR, true,
                EnglishSrlArgumentExtractor.PARAM_ADD_FANSE, false,
                EnglishSrlArgumentExtractor.PARAM_ADD_DEPENDENCY, true
        );

        AnalysisEngineDescription clozeExtractor = AnalysisEngineFactory.createEngineDescription(
                ArgumentClozeTaskWriter.class, typeSystemDescription,
                ArgumentClozeTaskWriter.PARAM_OUTPUT_FILE, outputFile,
                ArgumentClozeTaskWriter.PARAM_ADD_EVENT_COREF, true
        );


        // Write only clozes.
        new BasicPipeline(reader, false, true, 7, goldAnnotator, arguments, clozeExtractor).run();

    }
}
