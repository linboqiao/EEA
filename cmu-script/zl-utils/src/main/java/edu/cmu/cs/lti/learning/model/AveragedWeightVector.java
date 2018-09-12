package edu.cmu.cs.lti.learning.model;

import gnu.trove.iterator.TIntDoubleIterator;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Date: 8/21/15
 * Time: 9:19 PM
 *
 * @author Zhengzhong Liu
 */
public abstract class AveragedWeightVector implements Serializable {
    private static final long serialVersionUID = 7646416117744167293L;

    public abstract void updateWeightsBy(FeatureVector fv, double multiplier);

    public abstract void updateAverageWeight();

    public void write(File outputFile) throws FileNotFoundException {
        consolidate();
        SerializationUtils.serialize(this, new FileOutputStream(outputFile));
        deconsolidate();
    }

    public double dotProd(FeatureVector fv) {
        double sum = 0;
        for (FeatureVector.FeatureIterator iter = fv.featureIterator(); iter.hasNext(); ) {
            iter.next();
            sum += getWeightAt(iter.featureIndex()) * iter.featureValue();
//            System.out.println(iter.featureIndex() + " " + getWeightAt(iter.featureIndex()) + " " + iter.featureValue());
        }
//        System.out.println(sum);
        return sum;
    }

    public double dotProdAver(FeatureVector fv) {
        double sum = 0;
        for (FeatureVector.FeatureIterator iter = fv.featureIterator(); iter.hasNext(); ) {
            iter.next();
            sum += getAverageWeightAt(iter.featureIndex()) * iter.featureValue();
        }
        return sum;
    }

    public double dotProdAverDebug(FeatureVector fv, Logger logger) {
        double sum = 0;
        for (FeatureVector.FeatureIterator iter = fv.featureIterator(); iter.hasNext(); ) {
            iter.next();
            double weight = getAverageWeightAt(iter.featureIndex());
            sum += weight * iter.featureValue();
            logger.info(fv.getAlphabet().getFeatureNameRepre(iter.featureIndex()) + " " + iter.featureValue() + " " +
                    weight);
        }
        return sum;
    }

    public String toReadableString(FeatureAlphabet alphabet) {
        StringBuilder sb = new StringBuilder();

        for (TIntDoubleIterator iter = getWeightsIterator(); iter.hasNext(); ) {
            iter.advance();
            if (iter.value() != 0) {
                sb.append(String.format("%s : %.2f\n", alphabet.getFeatureNameRepre(iter.key()), iter.value()));
            }
        }
        return sb.toString();
    }

    abstract void consolidate();

    abstract void deconsolidate();

    public abstract double getWeightAt(int i);

    public abstract double getAverageWeightAt(int i);

    public abstract int getFeatureSize();

    public abstract TIntDoubleIterator getWeightsIterator();
}
