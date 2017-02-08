package edu.cmu.cs.lti.learning.feature.mention_pair.functions.sequence;

import com.google.common.collect.ArrayListMultimap;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Date: 2/4/17
 * Time: 2:05 PM
 *
 * @author Zhengzhong Liu
 */
public class SchemaReader {

    private ArrayListMultimap<String, Integer> verb2Cluster;

    private ArrayListMultimap<String, Integer> noun2Cluster;

    public SchemaReader(File schemaFile) throws IOException {
        verb2Cluster = ArrayListMultimap.create();
        noun2Cluster = ArrayListMultimap.create();
        readSchema(schemaFile);
    }

    private void readSchema(File schemaFile) throws IOException {
        int clusterId = 0;

        for (String line : FileUtils.readLines(schemaFile)) {
            if (line.equals("*****")) {
                clusterId++;
            }
            if (line.startsWith("Events:")) {
                for (String verb : line.substring(9).split(" ")) {
                    verb2Cluster.put(verb, clusterId);
                }
            }

            if (line.startsWith("[")) {
                String nounPart = line.split("\\(")[1].trim();

                boolean take = true;
                for (String s : nounPart.split(" ")) {
                    if (take) {
                        noun2Cluster.put(s, clusterId);
                    }
                    take = !take;
                }
            }
        }
    }

    public boolean sameSchema(String verb1, String verb2) {
        if (verb2Cluster.containsKey(verb1) && verb2Cluster.containsKey(verb2)) {
            for (Integer cluster1 : verb2Cluster.get(verb1)) {
                for (Integer cluster2 : verb2Cluster.get(verb2)) {
                    if (cluster1.equals(cluster2)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean verbNounSameSchema(String verb, String noun) {
        if (verb2Cluster.containsKey(verb) && noun2Cluster.containsKey(noun)) {
            for (Integer verbCluster : verb2Cluster.get(verb)) {
                for (Integer nounCluster : noun2Cluster.get(noun)) {
                    if (verbCluster.equals(nounCluster)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean nounNounSameSchema(String noun1, String noun2) {
        if (verb2Cluster.containsKey(noun1) && noun2Cluster.containsKey(noun2)) {
            for (Integer cluster1 : verb2Cluster.get(noun1)) {
                for (Integer cluster2 : noun2Cluster.get(noun2)) {
                    if (cluster1.equals(cluster2)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
