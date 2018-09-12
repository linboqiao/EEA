package edu.cmu.cs.lti.learning.model;

import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/16/15
 * Time: 2:38 PM
 * <p>
 * This feature vector is a lazy list implementation, all features added to it are considered as unique, it does not do
 * any checks on feature index. Duplicated features will simply be used multiple times. This will create performance
 * overhead when number of duplicates is large.
 *
 * @author Zhengzhong Liu
 */
public class RealValueLazyListFeatureVector extends FeatureVector {
    private static final long serialVersionUID = 5154909346179407831L;

    List<Pair<Integer, Double>> fv;

    public RealValueLazyListFeatureVector(FeatureAlphabet alphabet) {
        super(alphabet);
        fv = new ArrayList<>();
    }

    @Override
    public FeatureVector newFeatureVector() {
        return new RealValueLazyListFeatureVector(alphabet);
    }


    @Override
    public FeatureVector newVector() {
        return new RealValueLazyListFeatureVector(alphabet);
    }

    @Override
    protected boolean addFeatureInternal(int featureIndex, double featureValue) {
        return fv.add(Pair.with(featureIndex, featureValue));
    }

    @Override
    public double getFeatureValue(int featureIndex) {
        return fv.get(featureIndex).getValue1();
    }

    @Override
    public FeatureIterator featureIterator() {

        Iterator<Pair<Integer, Double>> iter = fv.iterator();

        return new FeatureIterator() {
            Pair<Integer, Double> current = null;

            @Override
            public int featureIndex() {
                return current.getValue0();
            }

            @Override
            public double featureValue() {
                return current.getValue1();
            }

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public void next() {
                current = iter.next();
            }
        };
    }

    public RealValueHashFeatureVector getUniqifyVector() {
        RealValueHashFeatureVector consolidatedFv = new RealValueHashFeatureVector(alphabet);
        for (Pair<Integer, Double> fvElement : fv) {
            consolidatedFv.addFeature(fvElement.getValue0(), fvElement.getValue1());
        }
        return consolidatedFv;
    }
}
