package edu.cmu.cs.lti.script.annotators.writer;

import com.google.common.collect.ArrayListMultimap;
import com.google.gson.Gson;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.script.utils.ImplicitFeaturesExtractor;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
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
    @ConfigurationParameter(name = PARAM_FRAME_MAPPINGS, defaultValue = "frameMappings")
    private File frameMappingFile;

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

        for (int sentId = 0; sentId < sentences.size(); sentId++) {
            StanfordCorenlpSentence sentence = sentences.get(sentId);

            doc.sentences.add(sentence.getCoveredText());

            for (EventMention eventMention : JCasUtil.selectCovered(EventMention.class, sentence)) {
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

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(
                typeSystemDescription, workingDir, inputBase
        );

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

        AnalysisEngineDescription clozeExtractor = AnalysisEngineFactory.createEngineDescription(
                ArgumentClozeTaskWriter.class, typeSystemDescription,
                ArgumentClozeTaskWriter.PARAM_OUTPUT_FILE, outputFile
        );

        // Extract and write as cloze.
//        String xmiOut = args[3];
//        new BasicPipeline(reader, true, true, 7, workingDir, xmiOut, true,
//                remover, frameEvents, verbEvents, clozeExtractor).run();

        // Write only clozes.
        new BasicPipeline(reader, true, true, 7, clozeExtractor).run();

    }
}
