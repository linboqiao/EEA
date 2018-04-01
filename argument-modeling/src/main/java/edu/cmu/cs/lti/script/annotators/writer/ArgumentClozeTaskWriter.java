package edu.cmu.cs.lti.script.annotators.writer;

import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

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
        try {
            writer.write("#" + UimaConvenience.getArticleName(aJCas) + "\n");
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }


        for (EventMention eventMention : JCasUtil.select(aJCas, EventMention.class)) {
            List<Word> complements = new ArrayList<>();
            String predicate_text = UimaNlpUtils.getPredicate(eventMention.getHeadWord(), complements);

            StringBuilder sb = new StringBuilder();
            sb.append(predicate_text);

            FSList argsFS = eventMention.getArguments();
            Collection<EventMentionArgumentLink> argLinks = FSCollectionFactory.create(argsFS,
                    EventMentionArgumentLink.class);

            for (EventMentionArgumentLink argLink : argLinks) {
                String role = argLink.getArgumentRole();
                EntityMention en = argLink.getArgument();
                String entityId = en.getReferingEntity().getId();
                String argText = en.getHead() == null ? en.getCoveredText() : en.getHead().getLemma();
                sb.append("\t");
                sb.append(role).append(entityId).append(":").append(argText);
            }

            sb.append("\n");

            try {
                writer.write(sb.toString());
            } catch (IOException e) {
                throw new AnalysisEngineProcessException(e);
            }
        }

        try {
            writer.write("\n");
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
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

    public static void main(String[] args) throws UIMAException, IOException {
        String paramTypeSystemDescriptor = "TaskEventMentionDetectionTypeSystem";

        String inputDir = args[0];
        String outputFile = args[1];

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, inputDir);

        AnalysisEngineDescription eventExtractor = AnalysisEngineFactory.createEngineDescription(
                ArgumentClozeTaskWriter.class, typeSystemDescription,
                ArgumentClozeTaskWriter.PARAM_OUTPUT_FILE, outputFile
        );

        // Run the pipeline.
        SimplePipeline.runPipeline(reader, eventExtractor);
    }
}
