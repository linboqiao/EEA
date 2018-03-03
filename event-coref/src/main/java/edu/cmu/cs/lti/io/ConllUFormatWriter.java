package edu.cmu.cs.lti.io;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import edu.cmu.cs.lti.script.type.Dependency;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.descriptor.ConfigurationParameter;

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

        try {
            writer = new BufferedWriter(new FileWriter(outputFile));
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        JCas goldView = JCasUtil.getView(jCas, goldStandardViewName, jCas);

        int sentId = 0;
        for (StanfordCorenlpSentence sentence : JCasUtil.select(jCas, StanfordCorenlpSentence.class)) {

            writeLine(String.valueOf(sentId++));

            List<Pair<Integer, String>> deps = getDeps(sentence);
            List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);

            for (int tokenId = 0; tokenId < tokens.size(); tokenId++) {
                StanfordCorenlpToken token = tokens.get(tokenId);
                List<String> conllFields = new ArrayList<>();
                conllFields.add(String.valueOf(tokenId));
                conllFields.addAll(getWordFieds(token));

                Pair<Integer, String> dep = deps.get(tokenId);
                conllFields.add(String.valueOf(dep.getLeft()));
                conllFields.add(dep.getRight());

                token.getHeadDependencyRelations();
                writeLine(conllFields);
            }
        }
    }

    private List<Pair<Integer, String>> getDeps(StanfordCorenlpSentence sentence) {
        List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence);
        List<Pair<Integer, String>> heads = new ArrayList<>();

        Map<StanfordCorenlpToken, Integer> tokenIds = new HashMap<>();
        for (int tokenId = 0; tokenId < tokens.size(); tokenId++) {
            tokenIds.put(tokens.get(tokenId), tokenId);
        }

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
}
