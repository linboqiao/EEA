package edu.cmu.cs.lti.learning.model;

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

/**
 * Represent a real valued feature vector, implemented with hash map. It does not support negative or zero feature
 * values for performance benefits.
 *
 * @author Zhengzhong Liu
 */
public class RealValueHashFeatureVector extends FeatureVector {

    private static final long serialVersionUID = -8434459870299460601L;

    TIntDoubleMap fv;

    public RealValueHashFeatureVector(FeatureAlphabet alphabet) {
        super(alphabet);
        fv = new TIntDoubleHashMap();
    }

    @Override
    public FeatureVector newFeatureVector() {
        return new RealValueHashFeatureVector(alphabet);
    }

    @Override
    protected boolean addFeatureInternal(int featureIndex, double featureValue) {
        boolean isNewFeature = !fv.containsKey(featureIndex);
        double adjustedValue = fv.adjustOrPutValue(featureIndex, featureValue, featureValue);
        return isNewFeature;
    }

    @Override
    public double getFeatureValue(int featureIndex) {
        // Note that the no entry default of trove is 0.
        return fv.get(featureIndex);
    }

    public FeatureVector newVector() {
        return new RealValueHashFeatureVector(alphabet);
    }

    @Override
    public FeatureIterator featureIterator() {
        TIntDoubleIterator iter = fv.iterator();
        return new FeatureIterator() {
            @Override
            public int featureIndex() {
                return iter.key();
            }

            @Override
            public double featureValue() {
                return iter.value();
            }

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public void next() {
                iter.advance();
            }
        };
    }
}
