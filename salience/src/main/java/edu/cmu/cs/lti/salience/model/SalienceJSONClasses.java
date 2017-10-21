package edu.cmu.cs.lti.salience.model;

import com.google.gson.annotations.SerializedName;
import edu.cmu.cs.lti.salience.utils.FeatureUtils;
import edu.cmu.cs.lti.salience.utils.LookupTable;

import java.util.ArrayList;
import java.util.List;

import static edu.cmu.cs.lti.salience.utils.FeatureUtils.lexicalPrefix;

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
    }

    static public class EventSpot extends Spot {
    }

    static public class EntitySpot extends Spot {
        public String wiki_name;
        public List<Link> entities;
    }

    static public class Link {
        public Link(double score, String id) {
            this.score = score;
            this.id = id;
        }

        public double score;
        public String id;
        public Feature feature;
    }

    public static class Feature {
        public Feature(LookupTable table, FeatureUtils.SimpleInstance instance) {
            featureArray = new ArrayList<>();
            List<Double> lexicalFeatures = new ArrayList<>();
            instance.getFeatureMap().keySet().stream().sorted().forEach(f -> {
                if (!f.startsWith(lexicalPrefix)) {
                    featureArray.add(instance.getFeatureMap().get(f));
                } else {
                    String word = f.split("_")[1];
                    for (double v : table.getEmbedding(word)) {
                        lexicalFeatures.add(v);
                    }
                }
            });
            featureArray.addAll(lexicalFeatures);
            featureNames = instance.getFeatureNames();
        }

        public Feature() {
            featureArray = new ArrayList<>();
        }

        public List<Double> featureArray;
        public List<String> featureNames;
    }
}
