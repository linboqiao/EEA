package edu.cmu.cs.lti.learning.model;

import org.apache.commons.lang3.SerializationUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/11/15
 * Time: 11:22 AM
 *
 * @author Zhengzhong Liu
 */
public abstract class FeatureAlphabet implements Serializable {
    private static final long serialVersionUID = -912015198635250229L;

    protected FeatureAlphabet() {

    }

    public abstract int getFeatureId(String featureName);

    public abstract String[] getFeatureNames(int featureIndex);

    public abstract String getFeatureNameRepre(int featureIndex);

    public abstract int getAlphabetSize();

    public void write(File outputFile) throws FileNotFoundException {
        SerializationUtils.serialize(this, new FileOutputStream(outputFile));
    }
}
