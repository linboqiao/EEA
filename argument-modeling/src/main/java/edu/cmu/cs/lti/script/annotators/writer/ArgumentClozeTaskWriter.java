package edu.cmu.cs.lti.script.annotators.writer;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import edu.cmu.cs.lti.annotators.EventMentionRemover;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.annotators.FrameBasedEventDetector;
import edu.cmu.cs.lti.script.annotators.VerbBasedEventDetector;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
    private File outputFile;

    public static final String PARAM_CONTEXT_WINDOW = "contextWindow";
    @ConfigurationParameter(name = PARAM_CONTEXT_WINDOW, defaultValue = "5")
    private int contextWindowSize;

    private BufferedWriter writer;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        try {
            writer = new BufferedWriter(new FileWriter(outputFile));
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        // Assign IDs.
        int id = 0;
        for (Entity entity : JCasUtil.select(aJCas, Entity.class)) {
            entity.setId(String.valueOf(id++));
        }

        // The fields:
        // predicate_head, predicate_context, frame_name,
        //   [arg_role, frame_element, entity_id, arg_text, more_than_one] * Number_Roles

        StringBuffer sb = new StringBuffer();
        sb.append("#").append(UimaConvenience.getArticleName(aJCas)).append("\n");

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

        for (int sentId = 0; sentId < sentences.size(); sentId++) {
            StanfordCorenlpSentence sentence = sentences.get(sentId);

            for (EventMention eventMention : JCasUtil.selectCovered(EventMention.class, sentence)) {
                List<Word> complements = new ArrayList<>();

                String predicate_text = UimaNlpUtils.getPredicate(eventMention.getHeadWord(), complements, false);

                List<String> fields = new ArrayList<>();
                String frame = eventMention.getFrameName();

                String predicate_context = getContext(lemmas, (StanfordCorenlpToken) eventMention.getHeadWord());

                fields.add(predicate_text);
                fields.add(predicate_context);
                fields.add(frame == null ? "NA" : frame);

                FSList argsFS = eventMention.getArguments();
                Collection<EventMentionArgumentLink> argLinks = FSCollectionFactory.create(argsFS,
                        EventMentionArgumentLink.class);

                for (EventMentionArgumentLink argLink : argLinks) {
                    String role = argLink.getArgumentRole();
                    if (role == null) {
                        role = "NA";
                    }
                    String fe = argLink.getFrameElementName();
                    if (fe == null) {
                        fe = "NA";
                    }
                    EntityMention en = argLink.getArgument();
                    Entity cluster = en.getReferingEntity();
                    String entityId = cluster.getId();

                    int notSingleton = cluster.getEntityMentions().size() == 1 ? 0 : 1;

                    String argText = en.getHead() == null ? en.getCoveredText() : en.getHead().getLemma();
                    argText = onlySpace(argText);

                    fields.add(role);
                    fields.add(fe);
                    fields.add(entityId);
                    fields.add(argText);
                    fields.add(String.valueOf(notSingleton));
                }

                fields.add(String.valueOf(sentId));

                sb.append(Joiner.on("\t").join(fields.stream().map((Function<String, String>) this::onlySpace)
                        .collect(Collectors.toList())));

                sb.append("\n");
            }
        }


        sb.append("\n");

        try {
            writer.write(sb.toString());
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
        if (right > lemmas.length) {
            right = lemmas.length;
        }

        StringBuffer sb = new StringBuffer();

        for (int i = left; i < index; i++) {
            sb.append(lemmas[i]).append(" ");
        }

        // Indicate the context center.
        sb.append("___");

        for (int i = index + 1; i < right; i++) {
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

    public static void main(String[] args) throws UIMAException, IOException, CpeDescriptorException, SAXException {
        String paramTypeSystemDescriptor = "TaskEventMentionDetectionTypeSystem";

        String workingDir = args[0];
        String inputBase = args[1];
        String outputFile = args[2];
        String xmiOut = args[3];


        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(
                        typeSystemDescription, workingDir, inputBase
                );

        AnalysisEngineDescription remover = AnalysisEngineFactory.createEngineDescription(EventMentionRemover.class);

        AnalysisEngineDescription verbEvents = AnalysisEngineFactory.createEngineDescription(
                VerbBasedEventDetector.class, typeSystemDescription
        );

        AnalysisEngineDescription frameEvents = AnalysisEngineFactory.createEngineDescription(
                FrameBasedEventDetector.class, typeSystemDescription,
                FrameBasedEventDetector.PARAM_FRAME_RELATION, "../data/resources/fndata-1.7/frRelation.xml",
                FrameBasedEventDetector.PARAM_IGNORE_BARE_FRAME, true
        );

        AnalysisEngineDescription clozeExtractor = AnalysisEngineFactory.createEngineDescription(
                ArgumentClozeTaskWriter.class, typeSystemDescription,
                ArgumentClozeTaskWriter.PARAM_OUTPUT_FILE, outputFile
        );

//        StepBasedDirGzippedXmiWriter.dirSegFunction = IOUtils::indexBasedSegFunc;

        // Run the pipeline.
        new BasicPipeline(reader, true, true, 7, workingDir, xmiOut, true,
                remover, frameEvents, verbEvents, clozeExtractor).run();

//        new BasicPipeline(reader, true, true, 7, clozeExtractor).run();

    }
}
