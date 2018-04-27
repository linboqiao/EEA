package edu.cmu.cs.lti.salience.utils;

import edu.cmu.cs.lti.utils.BitUtils;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.collections.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/19/17
 * Time: 4:52 PM
 *
 * @author Zhengzhong Liu
 */
public class LookupTable {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private TObjectIntMap<String> indexLookup;
    private double[][] embeddingLookup;

    private double[] unknownVector;

    private AtomicInteger index;

    public LookupTable(int vocabSize, int embeddingSize) {
        indexLookup = new TObjectIntHashMap<>();
        embeddingLookup = new double[vocabSize][embeddingSize];
        unknownVector = new double[embeddingSize];
        index = new AtomicInteger();
        logger.info(String.format("Lookup table initialized with %d words.", vocabSize));
    }

    private int readWordIndex(String word) {
        if (indexLookup.containsKey(word)) {
            return indexLookup.get(word);
        } else {
            return -1;
        }
    }

    private int getWordIndex(String word) {
        if (indexLookup.containsKey(word)) {
            return indexLookup.get(word);
        } else {
            indexLookup.put(word, index.intValue());
            return index.getAndIncrement();
        }
    }

    public void setEmbedding(String word, double[] embedding) {
        int index = getWordIndex(word);
        System.arraycopy(embedding, 0, embeddingLookup[index], 0, embedding.length);
    }


    public void setEmbedding(int index, int channel, double value) {
        embeddingLookup[index][channel] = value;
    }

    public double[] getEmbedding(String word) {
        if (indexLookup.containsKey(word)) {
            return embeddingLookup[indexLookup.get(word)];
        } else {
            return unknownVector;
        }
    }

    public double[] getEmbedding(int wordIndex) {
        return embeddingLookup[wordIndex];
    }

    public static class SimCalculator {
        private LookupTable table;

        private Map<Long, Double> cachedSim;

        public SimCalculator(LookupTable table) {
            this.table = table;
            cachedSim = Collections.synchronizedMap(new LRUMap(10000));
        }

        public double getSimilarity(String word1, String word2) {
            String first;
            String second;

            if (word1.compareTo(word2) > 0) {
                first = word1;
                second = word2;
            } else {
                first = word2;
                second = word1;
            }

            int indexFirst = table.readWordIndex(first);
            int indexSecond = table.readWordIndex(second);

            if (indexFirst == -1 || indexSecond == -1) {
                // There are still embeddings not included?
                return 0;
            }

            long key = BitUtils.store2Int(indexFirst, indexSecond);

            double score = getCachedScore(key);

            if (score == -1) {
                score = cosine(table.getEmbedding(first), table.getEmbedding(second));
                cachedSim.put(key, score);
            }
            return score;
        }

        private synchronized double getCachedScore(long key) {
            if (cachedSim.containsKey(key)) {
                try {
                    return cachedSim.get(key);
                }catch (NullPointerException e){
                    return -1;
                }
            } else {
                return -1;
            }
        }

        private double cosine(double[] emb1, double[] emb2) {
            double length1 = Math.sqrt(dotProd(emb1, emb1));
            double length2 = Math.sqrt(dotProd(emb2, emb2));

            if (length1 == 0 || length2 == 0) {
                return 0;
            } else {
                return dotProd(emb1, emb2) / (length1 * length2);
            }
        }

        private double dotProd(double[] emb1, double[] emb2) {
            double score = 0;
            for (int i = 0; i < emb1.length; i++) {
                score += emb1[i] * emb2[i];
            }
            return score;
        }
    }
}
