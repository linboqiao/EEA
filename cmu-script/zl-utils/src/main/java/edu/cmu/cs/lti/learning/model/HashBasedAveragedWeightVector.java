package edu.cmu.cs.lti.learning.model;

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/21/15
 * Time: 9:19 PM
 *
 * @author Zhengzhong Liu
 */
public class HashBasedAveragedWeightVector extends AveragedWeightVector {
    private static final long serialVersionUID = 7646416117744167293L;

    // Individual update count of this vector.
    private int averageUpdateCount;

    private TIntDoubleMap weights;
    private TIntDoubleMap averagedWeights;

    private boolean consolidated;

    public HashBasedAveragedWeightVector() {
        this(0);
    }

    public HashBasedAveragedWeightVector(int initialAverageUpdateCount) {
        weights = new TIntDoubleHashMap();
        averagedWeights = new TIntDoubleHashMap();
        consolidated = false;
        averageUpdateCount = initialAverageUpdateCount;
    }

    @Override
    public void updateWeightsBy(FeatureVector fv, double multiplier) {
        for (FeatureVector.FeatureIterator iter = fv.featureIterator(); iter.hasNext(); ) {
            iter.next();
            int index = iter.featureIndex();
            double updateAmount = iter.featureValue() * multiplier;
            weights.adjustOrPutValue(index, updateAmount, updateAmount);
        }
    }

    @Override
    public void updateAverageWeight() {
        for (TIntDoubleIterator iter = weights.iterator(); iter.hasNext(); ) {
            iter.advance();
            int index = iter.key();
            double value = iter.value();
            averagedWeights.adjustOrPutValue(index, value, value);
        }
        averageUpdateCount++;
    }

//    @Override
//    public void write(File outputFile) throws FileNotFoundException {
//        consolidate();
//        SerializationUtils.serialize(this, new FileOutputStream(outputFile));
//        deconsolidate();
//    }
//
//    @Override
//    public double dotProd(FeatureVector fv) {
//        double sum = 0;
//        for (FeatureVector.FeatureIterator iter = fv.featureIterator(); iter.hasNext(); ) {
//            iter.next();
//            sum += weights.get(iter.featureIndex()) * iter.featureValue();
//        }
//        return sum;
//    }
//
//    @Override
//    public double dotProdAver(FeatureVector fv) {
//        double sum = 0;
//        for (FeatureVector.FeatureIterator iter = fv.featureIterator(); iter.hasNext(); ) {
//            iter.next();
//            sum += averagedWeights.get(iter.featureIndex()) * iter.featureValue();
//        }
//        return sum;
//    }

    @Override
    void consolidate() {
        if (!consolidated) {
            if (averageUpdateCount != 0) {
                for (TIntDoubleIterator iter = averagedWeights.iterator(); iter.hasNext(); ) {
                    iter.advance();
                    if (iter.value() == 0) {
                        iter.remove();
                    } else {
                        iter.setValue(iter.value() / averageUpdateCount);

                    }
                }
            }
            consolidated = true;
        }
    }

    @Override
    void deconsolidate() {
        if (consolidated) {
            if (averageUpdateCount != 0) {
                for (TIntDoubleIterator iter = averagedWeights.iterator(); iter.hasNext(); ) {
                    iter.advance();
                    iter.setValue(iter.value() * averageUpdateCount);
                }
            }
            consolidated = false;
        }
    }

    @Override
    public double getWeightAt(int i) {
        return weights.get(i);
    }

    @Override
    public double getAverageWeightAt(int i) {
        return averagedWeights.get(i);
    }

    @Override
    public int getFeatureSize() {
        return weights.size();
    }

    public TIntDoubleIterator getWeightsIterator() {
        return weights.iterator();
    }
}
