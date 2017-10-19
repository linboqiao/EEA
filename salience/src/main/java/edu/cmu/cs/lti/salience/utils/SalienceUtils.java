package edu.cmu.cs.lti.salience.utils;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.collection_reader.AnnotatedNytReader;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.commons.io.FileUtils;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.uimafit.util.FSCollectionFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/18/17
 * Time: 11:59 PM
 *
 * @author Zhengzhong Liu
 */
public class SalienceUtils {
    public static LookupTable loadEmbedding(File entityEmbeddingFile) throws IOException {
        boolean firstLine = true;

        LookupTable table = null;

        for (String line : FileUtils.readLines(entityEmbeddingFile)) {
            String[] parts = line.split(" ");
            if (firstLine) {
                int vocSize = Integer.parseInt(parts[0]);
                int embeddingSize = Integer.parseInt(parts[1]);
                firstLine = false;
                table = new LookupTable(vocSize, embeddingSize);
            } else {
                String word = parts[0];

                double[] embedding = new double[parts.length - 1];
                for (int i = 1; i < parts.length; i++) {
                    embedding[i - 1] = Double.parseDouble(parts[i]);
                }

                table.setEmbedding(word, embedding);
            }
        }
        return table;
    }

    public static Set<String> getAbstractEntities(JCas mainView) {
        JCas abstractView = JCasUtil.getView(mainView, AnnotatedNytReader.ABSTRACT_VIEW_NAME, false);
        Set<String> abstractEntities = new HashSet<>();
        for (GroundedEntity groundedEntity : JCasUtil.select(abstractView, GroundedEntity.class)) {
            abstractEntities.add(groundedEntity.getKnowledgeBaseId());
        }
        return abstractEntities;
    }

    public void getEvents(){

    }

    public static Set<String> readSplit(File splitFile) throws IOException {
        Set<String> docs = new HashSet<>();
        for (String s : FileUtils.readLines(splitFile)) {
            docs.add(s.trim());
        }
        return docs;
    }

    static class MergedClusters {
        // Contains mapping to cluster elements.
        ArrayListMultimap<Integer, ComponentAnnotation> clusters;

        // Contains mapping from a kb id to the actual cluster.
        ArrayListMultimap<String, Integer> kbidEntities;

        MergedClusters() {
            clusters = ArrayListMultimap.create();
            kbidEntities = ArrayListMultimap.create();
        }

        ArrayListMultimap<Integer, ComponentAnnotation> getClusters() {
            return clusters;
        }

        ArrayListMultimap<String, Integer> getKbidEntities() {
            return kbidEntities;
        }

        void addKbEntity(String kbid, int index) {
            kbidEntities.put(kbid, index);
        }

        void addClusterElement(int index, ComponentAnnotation annotation) {
            clusters.put(index, annotation);
        }
    }

    public static MergedClusters getBodyCorefeEntities(JCas aJCas) {
        MergedClusters cluster = new MergedClusters();

        // Identify the entity id based on the head word.
        Map<Word, Integer> headwordEntities = new HashMap<>();

        // Using entity IDs for faster comparison.
        int index = 0;
        for (Entity entity : org.uimafit.util.JCasUtil.select(aJCas, Entity.class)) {
            entity.setIndex(index);
            index++;
        }

        Body body = org.uimafit.util.JCasUtil.selectSingle(aJCas, Body.class);
        Headline headline = org.uimafit.util.JCasUtil.selectSingle(aJCas, Headline.class);

        for (Entity entity : org.uimafit.util.JCasUtil.select(aJCas, Entity.class)) {
            for (EntityMention mention : FSCollectionFactory.create(entity.getEntityMentions(), EntityMention.class)) {
                if (mention.getEnd() <= headline.getEnd()) {
                    // Ignore headline mentions.
                    continue;
                }
                StanfordCorenlpToken headword = UimaNlpUtils.findHeadFromStanfordAnnotation(mention);
                headwordEntities.put(headword, entity.getIndex());
                cluster.addClusterElement(entity.getIndex(), mention);
            }
        }

        // Identify the entity id based on the KB id.
        for (GroundedEntity groundedEntity : org.uimafit.util.JCasUtil.selectCovered(GroundedEntity.class, body)) {
            String kbid = groundedEntity.getKnowledgeBaseId();
            Word headword = UimaNlpUtils.findHeadFromStanfordAnnotation(groundedEntity);

            if (headwordEntities.containsKey(headword)) {
                // If there is a stanford coreference cluster, we take that cluster.
                // We record that this kb id correspond to this entity, but we don't add additional mentions.
                int entityId = headwordEntities.get(headword);
                cluster.addKbEntity(kbid, entityId);
//                logger.info("Grounded entity " + kbid + " " + groundedEntity.getCoveredText() + " has a cluster " +
//                        entityId);
            } else {
                if (!cluster.getKbidEntities().containsKey(kbid)) {
                    // Create a new entity for this kb entry.
                    cluster.addKbEntity(kbid, index);
                    cluster.addClusterElement(index, groundedEntity);
                    index++;
                }
            }
        }
        return cluster;
    }

    public static String getCanonicalToken(Word word) {
        return word.getLemma().toLowerCase();
    }

}
