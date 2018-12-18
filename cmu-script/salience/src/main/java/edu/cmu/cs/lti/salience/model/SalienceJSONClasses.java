package edu.cmu.cs.lti.salience.model;

import com.google.gson.annotations.SerializedName;
import edu.cmu.cs.lti.utils.FeatureUtils;

import java.util.ArrayList;
import java.util.List;

import static edu.cmu.cs.lti.salience.utils.SalienceFeatureExtractor.lexicalPrefix;
import static edu.cmu.cs.lti.salience.utils.SalienceFeatureExtractor.sparsePrefix;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/20/17
 * Time: 9:53 PM
 *
 * @author Zhengzhong Liu
 */
public class SalienceJSONClasses {
    static public class DocStructure {
        //        @SerializedName("paperAbstract") // Semantic Scholar version
        public String bodyText;
        public String docno;
        public Spots spot;
        public Spots event;
        //        @SerializedName("title") // Semantic Scholar version
        @SerializedName("abstract") //NYT version
        public String abstractText;
    }

    static public class Spots {
        //        @SerializedName("paperAbstract") // Semantic Scholar version
        public List<Spot> bodyText;
        //        @SerializedName("title") // Semantic Scholar version
        @SerializedName("abstract") // NYT version
        public List<Spot> abstractSpots;
    }

    static public class Spot {
        public List<Integer> loc;
        public List<Integer> span;
        public List<Integer> head_span;
        public String surface;
        public Feature feature;
        public String id;
        public int salience;
    }

    static public class EventSpot extends Spot {
        public String frame_name;
        public List<Argument> arguments;
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
        public Feature(FeatureUtils.SimpleInstance instance) {
            featureArray = new ArrayList<>();
            featureNames = new ArrayList<>();
            sparseFeatureArray = new ArrayList<>();
            sparseFeatureName = new ArrayList<>();

            instance.getFeatureMap().keySet().stream().sorted().forEach(f -> {
                if (f.startsWith(sparsePrefix) || f.startsWith(lexicalPrefix)) {
                    // Sparse features go to a different field.
                    String featureName = f.split("_")[0];
                    sparseFeatureArray.add(f);
                    sparseFeatureName.add(featureName);
                } else {
                    featureArray.add(instance.getFeatureMap().get(f));
                    featureNames.add(f);
                }
            });
        }

        public List<Double> featureArray;
        public List<String> featureNames;

        public List<String> sparseFeatureArray;
        public List<String> sparseFeatureName;
    }
}
