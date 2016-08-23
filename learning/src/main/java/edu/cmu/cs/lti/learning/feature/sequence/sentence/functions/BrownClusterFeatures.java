package edu.cmu.cs.lti.learning.feature.sequence.sentence.functions;

import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.feature.sequence.base.SequenceFeatureWithFocus;
import edu.cmu.cs.lti.learning.model.MentionKey;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.jcas.JCas;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/6/15
 * Time: 9:09 PM
 *
 * @author Zhengzhong Liu
 */
public class BrownClusterFeatures extends SequenceFeatureWithFocus<StanfordCorenlpToken> {
    private int[] brownClusterPrefix;

//    private ArrayListMultimap<String, String> brownClusters;

    private Map<String, String> brownClusters;

    private String brownClusteringPath;

    public BrownClusterFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
        brownClusteringPath = edu.cmu.cs.lti.utils.FileUtils.joinPaths(
                generalConfig.get("edu.cmu.cs.lti.resource.dir"),
                generalConfig.get("edu.cmu.cs.lti.brown_cluster.path"));
        brownClusters = new HashMap<>();
        try {
            for (String line : FileUtils.readLines(new File(brownClusteringPath))) {
                String[] parts = line.split("\t");
                if (parts.length > 2) {
                    brownClusters.put(parts[1], parts[0]);
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        brownClusterPrefix = featureConfig.getIntList(this.getClass().getSimpleName() + ".length");
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
    }

    @Override
    public void resetWorkspace(JCas aJCas) {
    }

    @Override
    public void extract(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String> nodeFeatures,
                        Table<Pair<Integer, Integer>, String, Double> edgeFeatures) {
        String lemma = operateWithOutsideLowerCase(sequence, StanfordCorenlpToken::getLemma, focus);

        if (brownClusters.containsKey(lemma)) {
            String fullClusterId = brownClusters.get(lemma);
            for (int prefixLength : brownClusterPrefix) {
                if (prefixLength <= fullClusterId.length()) {
                    String brownClusterLabel = fullClusterId.substring(0, prefixLength);
                    addToFeatures(nodeFeatures, String.format("HeadLemmaBrown@%d=%s", prefixLength,
                            brownClusterLabel), 1);
                }
            }
            addToFeatures(nodeFeatures, String.format("HeadLemmaBrownFull=%s", fullClusterId), 1);
        }
    }

    @Override
    public void extractGlobal(List<StanfordCorenlpToken> sequence, int focus, TObjectDoubleMap<String>
            globalFeatures, List<MentionKey> knownStates, MentionKey currentState) {

    }
}
