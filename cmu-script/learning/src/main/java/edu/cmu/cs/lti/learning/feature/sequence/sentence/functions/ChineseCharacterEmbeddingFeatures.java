package edu.cmu.cs.lti.learning.feature.sequence.sentence.functions;

import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.sequence.base.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.learning.model.MentionKey;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.FileUtils;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.jcas.JCas;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/31/16
 * Time: 3:12 PM
 *
 * @author Zhengzhong Liu
 */
public class ChineseCharacterEmbeddingFeatures extends SequenceFeatureWithFocus<Word> {

    private WordVectors wordVectors;

    public ChineseCharacterEmbeddingFeatures(Configuration generalConfig, Configuration featureConfig) throws
            IOException {
        super(generalConfig, featureConfig);

        String embeddingPath = FileUtils.joinPaths(
                generalConfig.get("edu.cmu.cs.lti.resource.dir"),
                featureConfig.get(featureConfigKey("path"))
        );

        wordVectors = WordVectorSerializer.loadGoogleModel(new File(embeddingPath), true);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {

    }

    @Override
    public void resetWorkspace(JCas aJCas) {

    }

    @Override
    public void extractGlobal(List<Word> sequence, int focus, TObjectDoubleMap<String> globalFeatures,
                              List<MentionKey> knownStates, MentionKey currentState) {
    }

    @Override
    public void extract(List<Word> sequence, int focus, TObjectDoubleMap<String> nodeFeatures,
                        Table<Pair<Integer, Integer>, String, Double> edgeFeatures) {
        if (focus > 0 && focus < sequence.size()) {
            String lemma = sequence.get(focus).getLemma();
            double[] vector = wordVectors.getWordVector(lemma);

            for (int i = 0; i < vector.length; i++) {
                double v = vector[i];
                addToFeatures(nodeFeatures, String.format("WV_giga_300_dim=%d", i), v);
            }
        }
    }
}
