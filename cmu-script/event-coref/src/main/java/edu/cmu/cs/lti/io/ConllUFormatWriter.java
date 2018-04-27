package edu.cmu.cs.lti.io;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import edu.cmu.cs.lti.learning.runners.RunnerUtils;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * See the ConllU format description: http://universaldependencies.org/docs/format.html
 */
public class ConllUFormatWriter extends AbstractLoggingAnnotator {
    public static final String PARAM_OUTPUT_FILE = "outputFile";

    @ConfigurationParameter(name = PARAM_OUTPUT_FILE)
    private File outputFile;

    private BufferedWriter writer;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        if (!outputFile.getParentFile().exists()) {
            File parent = outputFile.getParentFile();
            parent.mkdirs();
        }

        try {
            writer = new BufferedWriter(new FileWriter(outputFile));
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        int sentId = 0;

        String docid = UimaConvenience.getArticleName(jCas);

        Map<EventMention, Integer> clusterIds = new HashMap<>();

        int clusterId = 0;
        for (Event event : JCasUtil.select(jCas, Event.class)) {
            FSArray mentions = event.getEventMentions();
            if (mentions.size() > 1) {
                for (EventMention eventMention : FSCollectionFactory.create(mentions, EventMention.class)) {
                    clusterIds.put(eventMention, clusterId++);
                }
            }
        }

        writeLine("# newdoc id = " + docid);
        for (StanfordCorenlpSentence sentence : JCasUtil.select(jCas, StanfordCorenlpSentence.class)) {

            writeLine("# sent_id = " + String.valueOf(sentId++));

            List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);
            Map<StanfordCorenlpToken, Integer> tokenIds = new HashMap<>();
            for (int tokenId = 0; tokenId < tokens.size(); tokenId++) {
                tokenIds.put(tokens.get(tokenId), tokenId);
            }

            List<Pair<Integer, String>> deps = getDeps(tokens, tokenIds);

            Pair<String[], int[]> eventInfo = getEvents(sentence, tokenIds, clusterIds);
            String[] tokenEventType = eventInfo.getKey();
            int[] tokenEventId = eventInfo.getValue();

            for (int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++) {
                StanfordCorenlpToken token = tokens.get(tokenIndex);
                List<String> conllFields = new ArrayList<>();

                // Token id is one-based since there is a ROOT.
                conllFields.add(String.valueOf(tokenIndex + 1));
                conllFields.addAll(getWordFieds(token));

                Pair<Integer, String> dep = deps.get(tokenIndex);
                conllFields.add(String.valueOf(dep.getLeft()));
                conllFields.add(dep.getRight());

                // Enhanced dependency not available.
                conllFields.add("_");

                // Add event info.
                conllFields.add(tokenEventType[tokenIndex]);
                int eid = tokenEventId[tokenIndex];
                conllFields.add(eid == -1 ? "_" : String.valueOf(eid));

                // Add span info.
                conllFields.add(String.format("%d,%d", token.getBegin(), token.getEnd()));
                writeLine(conllFields);
            }
            writeLine("");
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

    private Pair<String[], int[]> getEvents(StanfordCorenlpSentence sentence,
                                            Map<StanfordCorenlpToken, Integer> tokenIds,
                                            Map<EventMention, Integer> clusterIds
    ) {
        String[] tokenMentionTypes = new String[tokenIds.size()];
        for (int i = 0; i < tokenMentionTypes.length; i++) {
            tokenMentionTypes[i] = "O";
        }

        int[] tokenEventId = new int[tokenIds.size()];

        for (EventMention mention : JCasUtil.selectCovered(EventMention.class, sentence)) {
            List<StanfordCorenlpToken> mentionTokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, mention);

            for (int i = 0; i < mentionTokens.size(); i++) {
                StanfordCorenlpToken mentionToken = mentionTokens.get(i);
                int tokenId = tokenIds.get(mentionToken);
                String prefix = "I_";
                if (i == 0) {
                    prefix = "B_";
                }
                tokenMentionTypes[tokenId] = prefix + mention.getEventType();
                tokenEventId[tokenId] = clusterIds.getOrDefault(mention, -1);
            }
        }
        return Pair.of(tokenMentionTypes, tokenEventId);
    }

    private List<Pair<Integer, String>> getDeps(List<StanfordCorenlpToken> tokens,
                                                Map<StanfordCorenlpToken, Integer> tokenIds) {
        List<Pair<Integer, String>> heads = new ArrayList<>();

        for (int tokenId = 0; tokenId < tokens.size(); tokenId++) {
            StanfordCorenlpToken token = tokens.get(tokenId);
            FSList headDepsFs = token.getHeadDependencyRelations();

            if (headDepsFs != null) {
                Collection<Dependency> dependencies = FSCollectionFactory.create(headDepsFs, Dependency.class);
                Dependency headDep = Iterables.get(dependencies, 0);
                int headId = tokenIds.get(headDep.getHead());
                String rel = headDep.getDependencyType();
                heads.add(Pair.of(headId, rel));
            } else {
                heads.add(Pair.of(0, "ROOT"));
            }
        }

        return heads;
    }

    private void writeLine(String text) throws AnalysisEngineProcessException {
        try {
            writer.write(text + "\n");
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private void writeLine(List<String> fields) throws AnalysisEngineProcessException {
        List<String> cleanFields = fields.stream().map(v -> v.replaceAll("\\s+", "")).collect(Collectors.toList());
        writeLine(Joiner.on('\t').join(cleanFields));
    }

    private List<String> getWordFieds(StanfordCorenlpToken token) {
        List<String> fields = new ArrayList<>();
        fields.add(token.getCoveredText());
        fields.add(token.getLemma());
        fields.add("_"); // Universal dependency, not available here.
        fields.add(token.getPos());
        fields.add("_"); // morphological features, not available.
        return fields;
    }

    public static void main(String[] argv) throws IOException, UIMAException {
        Configuration commonConfig = new Configuration("settings/common.properties");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(typeSystemName);

        String inputDir = argv[0];
        String outputFile = argv[1];

        CollectionReaderDescription reader = CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, inputDir);

        AnalysisEngineDescription goldAnnotator = RunnerUtils.getGoldAnnotator(true, true, true, true);

        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
                ConllUFormatWriter.class, typeSystemDescription,
                ConllUFormatWriter.PARAM_OUTPUT_FILE, outputFile
        );

        SimplePipeline.runPipeline(reader, goldAnnotator, writer);
    }

}
