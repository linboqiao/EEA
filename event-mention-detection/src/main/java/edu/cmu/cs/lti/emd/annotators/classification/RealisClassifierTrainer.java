package edu.cmu.cs.lti.emd.annotators.classification;

import edu.cmu.cs.lti.learning.model.ClassAlphabet;
import edu.cmu.cs.lti.learning.model.FeatureAlphabet;
import edu.cmu.cs.lti.learning.training.WekaBasedTrainer;
import gnu.trove.map.TIntDoubleMap;
import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.javatuples.Pair;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LibSVM;
import weka.core.OptionHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/11/15
 * Time: 12:52 PM
 *
 * @author Zhengzhong Liu
 */
public class RealisClassifierTrainer extends WekaBasedTrainer {
    private TypeSystemDescription typeSystemDescription;
    private CollectionReaderDescription reader;

    private List<String> classifierNames;

    public RealisClassifierTrainer(TypeSystemDescription typeSystemDescription, CollectionReaderDescription reader) {
        this.typeSystemDescription = typeSystemDescription;
        this.reader = reader;
    }

    @Override
    protected Map<String, Classifier> getClassifiers() throws Exception {
        Map<String, Classifier> classifiers = new HashMap<>();
        classifierNames = new ArrayList<>();
        classifiers.put("svm-linear", getClassifiers(new LibSVM(), "-K", "0"));
        classifiers.put("svm-poly", getClassifiers(new LibSVM(), "-K", "1"));
        classifierNames.addAll(classifiers.keySet());
        return classifiers;
    }

    private Classifier getClassifiers(Classifier cls, String... args) throws Exception {
        if (!(cls instanceof OptionHandler)) {
            return cls;
        }
        OptionHandler clsWithOptions = (OptionHandler) cls;
        clsWithOptions.setOptions(args);
        return (Classifier) clsWithOptions;
    }

    @Override
    protected void getFeatures(List<Pair<TIntDoubleMap, String>> instances, FeatureAlphabet alphabet, ClassAlphabet
            classAlphabet) {
        try {
            RealisFeatureExtractor.getFeatures(reader, typeSystemDescription, instances, alphabet, classAlphabet);
        } catch (UIMAException | IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getClassifierNames() {
        return classifierNames;
    }
}
