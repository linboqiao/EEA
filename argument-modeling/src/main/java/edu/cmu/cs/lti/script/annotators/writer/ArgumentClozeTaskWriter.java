package edu.cmu.cs.lti.script.annotators.writer;

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

        StringBuilder sb = new StringBuilder();
        sb.append("#" + UimaConvenience.getArticleName(aJCas) + "\n");

        Collection<StanfordCorenlpToken> allTokens = JCasUtil.select(aJCas, StanfordCorenlpToken.class);
        String[] lemmas = new String[allTokens.size()];
        int i = 0;
        for (StanfordCorenlpToken token : allTokens) {
            lemmas[i] = token.getLemma().toLowerCase();
            token.setIndex(i);
            i++;
        }

        for (EventMention eventMention : JCasUtil.select(aJCas, EventMention.class)) {
            List<Word> complements = new ArrayList<>();

            boolean isFrameEvent = eventMention.getComponentId().equals(FrameBasedEventDetector.class.getSimpleName());

            String predicate_text;
            if (isFrameEvent) {
                predicate_text = eventMention.getHeadWord().getLemma();
            } else {
                predicate_text = UimaNlpUtils.getPredicate(eventMention.getHeadWord(), complements);
            }

            String predicate_context = getContext(lemmas, (StanfordCorenlpToken) eventMention.getHeadWord());

            sb.append(predicate_text);
            sb.append("\t").append(predicate_context);
            String frame = eventMention.getFrameName();
            sb.append("\t").append(frame == null ? "NA" : frame);

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

                sb.append("\t");
                sb.append(role).append("\t").append(fe).append("\t").append(entityId).append(":").append(argText)
                        .append("\t").append(notSingleton);
            }
            sb.append("\n");
        }

        sb.append("\n");

        try {
            writer.write(sb.toString());
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
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

        StringBuilder sb = new StringBuilder();

        for (int i = left; i < index; i++) {
            sb.append(lemmas[i]).append(" ");
        }

        // Indicate the context center.
        sb.append("_");

        for (int i = index + 1; i < right; i++) {
            sb.append(" ").append(lemmas[i]);
        }

        return sb.toString();
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
        String xmiOut = args[2];
        String outputFile = args[3];

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(
                        typeSystemDescription, workingDir, inputBase
                );

        AnalysisEngineDescription verbEvents = AnalysisEngineFactory.createEngineDescription(
                VerbBasedEventDetector.class, typeSystemDescription
        );

        AnalysisEngineDescription frameEvents = AnalysisEngineFactory.createEngineDescription(
                FrameBasedEventDetector.class, typeSystemDescription,
                FrameBasedEventDetector.PARAM_FRAME_RELATION, "../data/resources/fndata-1.7/frRelation.xml",
                FrameBasedEventDetector.PARAM_IGNORE_BARE_FRAME, true
        );

        AnalysisEngineDescription eventExtractor = AnalysisEngineFactory.createEngineDescription(
                ArgumentClozeTaskWriter.class, typeSystemDescription,
                ArgumentClozeTaskWriter.PARAM_OUTPUT_FILE, outputFile
        );

        // Run the pipeline.
        new BasicPipeline(reader, true, true, 7, workingDir, xmiOut, true,
                frameEvents, verbEvents, eventExtractor).run();
    }
}
