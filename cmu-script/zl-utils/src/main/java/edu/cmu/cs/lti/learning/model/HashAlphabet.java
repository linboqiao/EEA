package edu.cmu.cs.lti.learning.model;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import gnu.trove.TCollections;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * A hash based implementation of the feature alphabet. Because the use of hashing, the lookup of feature index is
 * easily thread-safe. Feature counters are made thread-safe by employing synchronized Trove maps.
 *
 * @author Zhengzhong Liu
 */
public class HashAlphabet extends FeatureAlphabet {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final long serialVersionUID = 8684276781666018103L;

    private final HashFunction hasher = Hashing.murmur3_32();

    private final TObjectIntMap<String>[] featureCounters;

    private final boolean storeReadable;

    private final int alphabetSize;

    private final int hashMask;

    /**
     * Create a alphabet that also stores all feature names to integer id count. This will make the training about
     * 25% slower.
     *
     * @param alphabetBits  The power of 2 of this is the alphabet size, i.e. number of bits for feature.
     * @param storeReadable If this is true, it will create a alphabet that also stores all feature names to integer id
     *                      count. This will make the training about25% slower.
     */
    public HashAlphabet(int alphabetBits, boolean storeReadable) {
        super();
        if (alphabetBits >= 31) {
            throw new IllegalArgumentException("Alphabet size exceed the power of current Murmur");
        }

        int alphabetSize = (int) Math.pow(2, alphabetBits);

        this.storeReadable = storeReadable;
        this.alphabetSize = alphabetSize;
        this.hashMask = alphabetSize - 1;
        logger.info(String.format("Feature Alphabet initialized with size %d", alphabetSize));
        logger.info(String.format("Feature Mask is %s", Integer.toBinaryString(hashMask)));

        featureCounters = new TObjectIntMap[alphabetSize];
        if (storeReadable) {
            logger.info("Alphabet will store feature name to hash value mappings. " +
                    "This may take additional memory and make the process slower.");
        }
    }

    private int hash(String feature) {
        int hashVal = plainHash(feature);

        if (storeReadable) {
            TObjectIntMap<String> counter = featureCounters[hashVal];
            if (counter == null) {
                counter = TCollections.synchronizedMap(new TObjectIntHashMap<>());
                featureCounters[hashVal] = counter;
            }
            counter.adjustOrPutValue("[" + feature + "]", 1, 1);
        }
        return hashVal;
    }

    private int plainHash(String feature) {
        // It is murmur32, which can produce a maximum 4 byte element.
        return hasher.hashString(feature, Charsets.UTF_8).asInt() & hashMask;
    }

    public boolean storeFeatureName(int featureIndex){
        return storeReadable && featureCounters[featureIndex] != null;
    }

    public String getMappedFeatureCounters(int featureIndex) {
        if (storeReadable) {
            TObjectIntMap counter = featureCounters[featureIndex];
            if (counter == null) {
                return "<NOT_FOUND>";
            }
            return counter.toString();
        } else {
            return "<UNSAVED>";
        }
    }

    private String[] getMappedFeatureNames(int featureIndex) {
        if (storeReadable) {
            TObjectIntMap<String> counter = featureCounters[featureIndex];
            if (featureCounters[featureIndex] == null) {
                return null;
            }
            return counter.keys(new String[counter.size()]);
        } else {
            return null;
        }
    }

    public void computeConflictRates() {
        if (storeReadable) {
            int actualFeatures = 0;
            int occupied = 0;
            for (TObjectIntMap featureCounter : featureCounters) {
                if (featureCounter != null) {
                    actualFeatures += featureCounter.size();
                    occupied += 1;
                }
            }
            logger.info(String.format("Actual features : %d, actual occupied,: %d, alphabet size : %d",
                    actualFeatures, occupied, alphabetSize));
        } else {
            throw new NotImplementedException();
        }
    }

    public boolean isStoreReadable() {
        return storeReadable;
    }

    @Override
    public int getFeatureId(String featureName) {
        return hash(featureName);
    }

    @Override
    public String[] getFeatureNames(int featureIndex) {
        return getMappedFeatureNames(featureIndex);
    }

    @Override
    public String getFeatureNameRepre(int featureIndex) {
        return getMappedFeatureCounters(featureIndex);
    }

    public int getAlphabetSize() {
        return alphabetSize;
    }
}