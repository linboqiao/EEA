package edu.cmu.cs.lti.salience.model;

import com.google.gson.annotations.SerializedName;
import edu.cmu.cs.lti.salience.utils.FeatureUtils;
import edu.cmu.cs.lti.salience.utils.LookupTable;

import java.util.ArrayList;
import java.util.List;

import static edu.cmu.cs.lti.salience.utils.FeatureUtils.lexicalPrefix;
import static edu.cmu.cs.lti.salience.utils.FeatureUtils.sparsePrefix;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/20/17
 * Time: 9:53 PM
 *
 * @author Zhengzhong Liu
 */
public class SalienceJSONClasses {
    static public class DocStructure {
        public String bodyText;
        public String docno;
        public Spots spot;
        public Spots event;
        public String title;
        @SerializedName("abstract")
        public String abstractText;
    }

    static public class Spots {
        public List<Spot> bodyText;
        @SerializedName("abstract")
        public List<Spot> abstractSpots;
        public List<Spot> title;
    }

    static public class Spot {
        public List<Integer> loc;
        public String surface;
        public Feature feature;
        public String id;
        public int salience;
    }

    static public class EventSpot extends Spot {
        public String frame_name;
        public List<String> arguments;
    }

    static public class EntitySpot extends Spot {
        public String wiki_name;
        public double score;
    }

    static public class Argument {
        public String type;
        public String surface;
        public String headEntityId;
    }

    public static class Feature {
        public Feature(LookupTable table, FeatureUtils.SimpleInstance instance) {
            featureArray = new ArrayList<>();
            featureNames = new ArrayList<>();
            sparseFeatureArray = new ArrayList<>();
            sparseFeatureName = new ArrayList<>();

            List<Double> lexicalFeatures = new ArrayList<>();

            instance.getFeatureMap().keySet().stream().sorted().forEach(f -> {
                if (f.startsWith(lexicalPrefix)) {
                    String word = f.split("_")[1];
                    for (double v : table.getEmbedding(word)) {
                        lexicalFeatures.add(v);
                    }
                    featureNames.add(f);
                } else if (f.startsWith(sparsePrefix)) {
                    // Sparse features go to a different field.
                    String featureName = f.split("_")[0];
                    sparseFeatureArray.add(f);
                    sparseFeatureName.add(featureName);
                } else {
                    featureArray.add(instance.getFeatureMap().get(f));
                    featureNames.add(f);
                }
            });
            featureArray.addAll(lexicalFeatures);
        }

        public Feature() {
            featureArray = new ArrayList<>();
        }

        public List<Double> featureArray;
        public List<String> featureNames;

        public List<String> sparseFeatureArray;
        public List<String> sparseFeatureName;
    }
}
