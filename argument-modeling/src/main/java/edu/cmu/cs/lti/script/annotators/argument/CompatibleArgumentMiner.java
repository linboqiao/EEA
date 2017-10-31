package edu.cmu.cs.lti.script.annotators.argument;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.type.ComponentAnnotation;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.EventMentionArgumentLink;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.GzippedXmiCollectionReader;
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
import org.apache.uima.fit.factory.CollectionReaderFactory;
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
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 3/26/16
 * Time: 6:49 PM
 *
 * @author Zhengzhong Liu
 */
public class CompatibleArgumentMiner extends AbstractLoggingAnnotator {

    public static final String PARAM_OUTPUT_FILE = "outputFile";
    @ConfigurationParameter(name = PARAM_OUTPUT_FILE)
    private File outputFile;
    private BufferedWriter outputWriter;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        File parent = outputFile.getParentFile();

        if (!parent.exists()) {
            parent.mkdirs();
        }

        try {
            outputWriter = new BufferedWriter(new FileWriter(outputFile));
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        // TODO:
        // 1. Only close sentences.
        // 2. Filter some verbs based on type.
        // 3. Filter some verbs based on size.

        StringBuilder sb = new StringBuilder();
        sb.append("#").append(UimaConvenience.getDocId(aJCas)).append("\n");

        ArrayListMultimap<String, String> argumentByFrame = ArrayListMultimap.create();

        for (EventMention eventMention : JCasUtil.select(aJCas, EventMention.class)) {
            String eventHead = getHeadLemma(eventMention);
            FSList argumentsFS = eventMention.getArguments();
            for (EventMentionArgumentLink argumentLink : FSCollectionFactory.create(argumentsFS,
                    EventMentionArgumentLink.class)) {
                String role = argumentLink.getSuperFrameElementRoleName();
                String argumentText = argumentLink.getArgument().getCoveredText().replace("\t", " ").replace("\n", " ");
                argumentByFrame.put(eventHead + "|" + eventMention.getFrameName() + "|" + role, argumentText);
            }
        }

        for (Map.Entry<String, Collection<String>> framedArguments : argumentByFrame.asMap().entrySet()) {
            String[] frameElementKey = framedArguments.getKey().split("\\|");
            Collection<String> arguments = framedArguments.getValue();

            List<String> parts = new ArrayList<>();
            if (arguments.size() > 1) {
                String event = frameElementKey[0];
                String frame = frameElementKey[1];
                String role = frameElementKey[2];

                parts.add(event);
                parts.add(frame);
                parts.add(role);

                for (String argument : arguments) {
                    parts.add(argument);
                }

                sb.append(Joiner.on("\t").join(parts)).append("\n");
            }
        }

        try {
            outputWriter.write(sb.append("\n").toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getHeadLemma(ComponentAnnotation anno) {
        StanfordCorenlpToken head = UimaNlpUtils.findHeadFromStanfordAnnotation(anno);
        if (head != null) {
            return head.getLemma().toLowerCase();
        } else {
            return anno.getCoveredText().replace("\t", " ").replace("\n", " ").toLowerCase();
        }
    }

    public static void main(String[] argv) throws UIMAException, SAXException, CpeDescriptorException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");
        String workingDir = argv[0];
        String inputDir = argv[1];
        String outputFile = argv[2];

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, workingDir,
                GzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, inputDir,
                GzippedXmiCollectionReader.PARAM_EXTENSION, ".xmi.gz",
                GzippedXmiCollectionReader.PARAM_RECURSIVE, true
        );

        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
                CompatibleArgumentMiner.class, typeSystemDescription,
                CompatibleArgumentMiner.PARAM_OUTPUT_FILE, new File(workingDir, outputFile)
        );

        new BasicPipeline(reader, true, true, 7, writer).run();
    }

}
