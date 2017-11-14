package edu.cmu.cs.lti.script.annotators.argument;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.*;

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

    public static final String PARAM_FRAME_FILE = "frameFile";
    @ConfigurationParameter(name = PARAM_FRAME_FILE)
    private File frameFile;

    public static final String PARAM_SENTENCE_DISTANCE_THRESHOLD = "sentenceDistanceThreshold";
    @ConfigurationParameter(name = PARAM_SENTENCE_DISTANCE_THRESHOLD)
    private int sentenceDistance;

    private Set<String> targetLexicals;
    private Set<String> targetFrames;

    private BufferedWriter outputWriter;

    private Set<String> ignoreVerbs;


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

        try {
            parseFileFile(frameFile);
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

        ignoreVerbs = new HashSet<>();

        ignoreVerbs.addAll(Arrays.asList("appear", "be", "become", "do", "have", "seem", "do", "get", "give", "go",
                "have", "keep", "make", "put", "set", "take", "argue", "claim", "say", "suggest", "tell"));
    }

    private void parseFileFile(File frameFile) throws IOException {
        targetLexicals = new HashSet<>();
        targetFrames = new HashSet<>();
        for (String line : FileUtils.readLines(frameFile)) {
            String[] parts = line.split("\t");
            targetLexicals.add(parts[0]);
            targetFrames.add(parts[2]);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        // TODO:
        // 1. Only close sentences.
        // 2. Filter some verbs based on type. /Done
        // 3. Filter some verbs based on size.

        ArrayListMultimap<String, Pair<EntityMention, StanfordCorenlpSentence>> argumentByFrame =
                ArrayListMultimap.create();

        int index = 0;
        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            for (EventMention eventMention : JCasUtil.selectCovered(EventMention.class, sentence)) {
                String eventHead = getHeadLemma(eventMention);

                if (ignoreVerbs.contains(eventHead)) {
                    continue;
                }

                FSList argumentsFS = eventMention.getArguments();
                for (EventMentionArgumentLink argumentLink : FSCollectionFactory.create(argumentsFS,
                        EventMentionArgumentLink.class)) {
                    // TODO ignore arguments that match the head itself.
                    String role = argumentLink.getSuperFrameElementRoleName();
                    argumentByFrame.put(eventHead + "|" + eventMention.getFrameName() + "|" + role,
                            Pair.of(argumentLink.getArgument(), sentence));
                }
            }
            sentence.setIndex(index);
            index++;
        }

        List<String> lines = new ArrayList<>();

        for (Map.Entry<String, Collection<Pair<EntityMention, StanfordCorenlpSentence>>> framedArguments :
                argumentByFrame.asMap().entrySet()) {
            String[] frameElementKey = framedArguments.getKey().split("\\|");
            List<Pair<EntityMention, StanfordCorenlpSentence>> arguments = new ArrayList<>(framedArguments.getValue());

            List<Pair<String, String>> argPairs = new ArrayList<>();
            List<Pair<String, String>> provenances = new ArrayList<>();

            getArgumentPairs(arguments, argPairs, provenances);

            if (argPairs.size() > 0) {
                String event = frameElementKey[0];
                String frame = frameElementKey[1];
                String role = frameElementKey[2];

                for (int i = 0; i < argPairs.size(); i++) {
                    List<String> parts = new ArrayList<>();

                    parts.add(event);
                    parts.add(frame);
                    parts.add(role);

                    Pair<String, String> paireArg = argPairs.get(i);
                    Pair<String, String> provenance = provenances.get(i);

                    parts.add("[" + paireArg.getLeft() + "]");
                    parts.add("[" + paireArg.getRight() + "]");

                    parts.add(provenance.getLeft());
                    parts.add(provenance.getRight());

                    lines.add(Joiner.on("\t").join(parts));
                }
            }
        }

        try {
            if (!lines.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("#").append(UimaConvenience.getArticleName(aJCas)).append("\n");
                for (String line : lines) {
                    sb.append(line).append("\n");
                }
                outputWriter.write(sb.append("\n").toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        try {
            outputWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getArgumentPairs(List<Pair<EntityMention, StanfordCorenlpSentence>> arguments,
                                  List<Pair<String, String>> argPairs,
                                  List<Pair<String, String>> provenances) {
        for (int i = 0; i < arguments.size() - 1; i++) {
            Pair<EntityMention, StanfordCorenlpSentence> argument1 = arguments.get(i);
            String argument1Text = argument1.getKey().getCoveredText();
            StanfordCorenlpToken argument1Head = UimaNlpUtils.findHeadFromStanfordAnnotation(argument1.getKey());
            if (argument1Head.getPos().startsWith("PR")) {
                continue;
            }

            for (int j = i + 1; j < arguments.size(); j++) {
                Pair<EntityMention, StanfordCorenlpSentence> argument2 = arguments.get(j);
                String argument2Text = cleanWhite(argument2.getKey().getCoveredText());

                StanfordCorenlpToken argument2Head = UimaNlpUtils.findHeadFromStanfordAnnotation(argument2.getKey());
                if (argument2Head.getPos().startsWith("PR")) {
                    continue;
                }

                if (!argument1Text.equals(argument2Text)) {
                    int sentIndex1 = argument1.getValue().getIndex();
                    int sentIndex2 = argument2.getValue().getIndex();

                    if (Math.abs(sentIndex1 - sentIndex2) <= sentenceDistance) {
                        String sent1 = cleanWhite(argument1.getValue().getCoveredText());
                        String sent2 = cleanWhite(argument2.getValue().getCoveredText());

                        if (!sent1.equals(sent2)) {
                            argPairs.add(Pair.of(argument1Text, argument2Text));
                            provenances.add(Pair.of("[" + sent1 + "]", "[" + sent2 + "]"));
                        }
                    }
                }
            }
        }
    }

    private String cleanWhite(String str) {
        return str.replace("\t", " ").replace("\n", " ").replace("\t", " ").replace("|", "_");
    }

    private String getHeadLemma(ComponentAnnotation anno) {
        StanfordCorenlpToken head = UimaNlpUtils.findHeadFromStanfordAnnotation(anno);
        if (head != null) {
            return head.getLemma().toLowerCase();
        } else {
            return cleanWhite(anno.getCoveredText()).toLowerCase();
        }
    }

    public static void main(String[] argv) throws UIMAException, SAXException, CpeDescriptorException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");
        String workingDir = argv[0];
        String inputDir = argv[1];
        String outputFile = argv[2];
        String frameFile = argv[3];

        int sentThreshold = 2;
        if (argv.length > 4) {
            sentThreshold = Integer.parseInt(argv[4]);
        }

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(
                typeSystemDescription, new File(workingDir, inputDir).getPath());

        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
                CompatibleArgumentMiner.class, typeSystemDescription,
                CompatibleArgumentMiner.PARAM_OUTPUT_FILE, new File(workingDir, outputFile),
                CompatibleArgumentMiner.PARAM_SENTENCE_DISTANCE_THRESHOLD, sentThreshold,
                CompatibleArgumentMiner.PARAM_FRAME_FILE, frameFile
        );

        new BasicPipeline(reader, true, true, 7, writer).run();
    }

}
