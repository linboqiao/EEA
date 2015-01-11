package edu.cmu.cs.lti.script.clustering;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.script.model.EntityCluster;
import edu.cmu.cs.lti.script.model.FeatureTable;
import edu.cmu.cs.lti.script.type.Entity;
import edu.cmu.cs.lti.script.type.EntityMention;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.utils.TimeUtils;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntProcedure;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;

public class EntityClusterManager {
    public enum entityType {
        ORGANIZATION, LOCATION, PERSON
    }

    private Table<String, String, Set<Integer>> typeSpecifiedRepresentativeEntity2Cluster = HashBasedTable
            .create();

    private Map<Integer, EntityCluster> clusters = new LinkedHashMap<Integer, EntityCluster>();

    private Map<Integer, FeatureTable> featureTables = new HashMap<Integer, FeatureTable>();

    private int numOfCluster;

    private File dumpingFolder;

    private File clusterIndexFolder;

    private Logger logger;


    public EntityClusterManager(File dumpingFolder, File clusterIndexFolder, Logger logger) {
        if (dumpingFolder.isDirectory()) {
            this.dumpingFolder = dumpingFolder;
        } else {
            throw new IllegalArgumentException("Please provide a directory to store cluster information");
        }

        if (clusterIndexFolder.isDirectory()) {
            this.clusterIndexFolder = clusterIndexFolder;
        } else {
            throw new IllegalArgumentException("Please provide a directory to store cluster index");
        }

        this.logger = logger;
    }

    public int numOfCluster() {
        return numOfCluster;
    }

    public static String getRepresentativeStr(Entity entity) {
        return entity.getRepresentativeMention().getCoveredText().replace("\n", "");
    }

    public String getUniqueEntityId(String articleName, Entity entity) {
        return articleName + "_" + entity.getId();
    }

    public TIntObjectHashMap<FeatureTable> getCandidateClusterFeatures(String entityType,
                                                                       Entity entity, int clusterIdLimit) {
        // String headMentionStr = getRepresentativeStr(entity);

        TIntObjectHashMap<FeatureTable> candidateClusters = new TIntObjectHashMap<FeatureTable>();

        for (String entityWord : getEntityMentionWords(entity)) {
            Set<Integer> candidateIds = typeSpecifiedRepresentativeEntity2Cluster.get(entityType,
                    entityWord);
            if (candidateIds != null) {
                for (int clusterId : candidateIds) {
                    if (clusterId < clusterIdLimit && !candidateClusters.contains(clusterId)) {
                        candidateClusters.put(clusterId, getClusterFeature(clusterId));
                        // System.out.println("Found candidate " + clusterId + " by using " + entityWord);
                    }
                }
            }
        }

        return candidateClusters;
    }

    public EntityCluster getCluster(int id) {
        return clusters.get(id);
    }

    public FeatureTable getClusterFeature(int id) {
        return featureTables.get(id);
    }

    public void createNewCluster(Date date, FeatureTable features, String clusterType, Entity entity, String articleName) {
        String entityId = getUniqueEntityId(articleName, entity);
        EntityCluster cluster = new EntityCluster(entityId, entity, date, clusterType);
        int clusterId = numOfCluster;
        featureTables.put(clusterId, features);
        for (String meaningfulWord : getEntityMentionWords(entity)) {
            hashClusterByTypeAndWord(clusterType, meaningfulWord, clusterId);
            cluster.addHashedWords(meaningfulWord);
        }
        clusters.put(clusterId, cluster);
//        cluster.addNewEvents(relatedEventMentions);
        numOfCluster++;
//        System.out.println("Creating new cluster [" + getRepresentativeStr(entity) + "], # cluster is : "
//                + clusters.size());
    }

    public void createNewCluster(Date date, FeatureTable features, String clusterType, Entity entity, ArrayListMultimap<String, EventMention> relatedEventMentions,
                                 String articleName) {
        String entityId = getUniqueEntityId(articleName, entity);
        EntityCluster cluster = new EntityCluster(entityId, entity, date, clusterType);
        int clusterId = numOfCluster;
        featureTables.put(clusterId, features);
        for (String meaningfulWord : getEntityMentionWords(entity)) {
            hashClusterByTypeAndWord(clusterType, meaningfulWord, clusterId);
            cluster.addHashedWords(meaningfulWord);
        }
        clusters.put(clusterId, cluster);
//        cluster.addNewEvents(relatedEventMentions);
        numOfCluster++;
//        System.out.println("Creating new cluster [" + getRepresentativeStr(entity) + "], # cluster is : "
//                + clusters.size());
    }

    private Set<String> getEntityMentionWords(Entity entity) {
        Set<String> meaningFulHeadWords = new HashSet<String>();
        for (int i = 0; i < entity.getEntityMentions().size(); i++) {
            EntityMention mention = entity.getEntityMentions(i);
            Word head = mention.getHead();
            if (head.getPos().startsWith("NN")) {
                meaningFulHeadWords.add(head.getCoveredText());
            }
        }
        return meaningFulHeadWords;
    }

    private void hashClusterByTypeAndWord(String clusterType, String clusterHead, int clusterId) {
//        System.out.println("Adding " + clusterHead + " " + clusterId);
        if (typeSpecifiedRepresentativeEntity2Cluster.contains(clusterType, clusterHead)) {
            typeSpecifiedRepresentativeEntity2Cluster.get(clusterType, clusterHead).add(clusterId);
        } else {
            Set<Integer> newClusterIds = new HashSet<Integer>();
            newClusterIds.add(clusterId);
            typeSpecifiedRepresentativeEntity2Cluster.put(clusterType, clusterHead, newClusterIds);
        }
    }

    public void addToExistingCluster(int clusterId, Date date, FeatureTable newEntityFeature,
                                     String clusterType, Entity entity, String articleName) {
        String entityId = getUniqueEntityId(articleName, entity);
        String mentionHead = getRepresentativeStr(entity);
        EntityCluster cluster = getCluster(clusterId);
        cluster.addNewEntity(entityId, date, mentionHead);
//        cluster.addNewEvents(relatedEventMentions);
        getClusterFeature(clusterId).mergeWith(newEntityFeature);
        // ensure new cluster is added at the end
        reinsertCluster(clusterId);
//        System.out.println("Adding to existing cluster: [" + mentionHead + "], " + clusterId);
    }

    public void addToExistingCluster(int clusterId, Date date, FeatureTable newEntityFeature,
                                     String clusterType, Entity entity, ArrayListMultimap<String, EventMention> relatedEventMentions, String articleName) {
        String entityId = getUniqueEntityId(articleName, entity);
        String mentionHead = getRepresentativeStr(entity);
        EntityCluster cluster = getCluster(clusterId);
        cluster.addNewEntity(entityId, date, mentionHead);
//        cluster.addNewEvents(relatedEventMentions);
        getClusterFeature(clusterId).mergeWith(newEntityFeature);
        // ensure new cluster is added at the end
        reinsertCluster(clusterId);
//        System.out.println("Adding to existing cluster: [" + mentionHead + "], " + clusterId);
    }

    public void reinsertCluster(int clusterId) {
        EntityCluster existingCluster = getCluster(clusterId);
        clusters.remove(clusterId);
        clusters.put(clusterId, existingCluster);
    }

    public void flushClusters(Date date) {
        //clusters are stored by order, so the first is the oldest
        TIntList cidsToRemove = new TIntArrayList();
        for (Entry<Integer, EntityCluster> nextEntry : clusters.entrySet()) {
            int cid = nextEntry.getKey();
            EntityCluster cluster = nextEntry.getValue();
            if (cluster.checkExpire(date)) {
                cidsToRemove.add(cid);
            } else {
                break;
            }
        }

        cidsToRemove.forEach(new TIntProcedure() {
            @Override
            public boolean execute(int cid) {
//                System.out.println("Removing " + cid);
                dropCluster(cid);
                return false;
            }
        });
    }

    public void dropCluster(int clusterId) {
        logger.info("Dropping cluster #" + clusterId);
        EntityCluster cluster = clusters.get(clusterId);
        try {
            dumpCluster(clusterId, cluster);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String ct = cluster.getClusterType();
        List<String> hws = cluster.getHashedWords();
        clusters.remove(clusterId);
        featureTables.remove(clusterId);

        for (String hw : hws) {
//            System.out.println("Removing " + ct + " " + hw);
            Set<Integer> clustersHashed = typeSpecifiedRepresentativeEntity2Cluster.get(ct, hw);
            clustersHashed.remove(clusterId);
        }
    }

    public void dumpCluster(int clusterId, EntityCluster cluster) throws IOException {
        String mentionRepresentative = cluster.getMentionHeads().get(0).replaceAll("\\W+", "_");
        int pathLength = mentionRepresentative.length() > 20 ? 20 : mentionRepresentative.length();
        String clusterInfoPath = clusterId + "_" + cluster.getMentionHeads().get(0).replaceAll("\\W+", "_").substring(0, pathLength) + ".cls";
        File outputFile = new File(dumpingFolder, clusterInfoPath);

        List<String> lines = new ArrayList<String>();

        String lifeSpan = TimeUtils.dateFormat.format(cluster.getFirstSeen()) + " "
                + TimeUtils.dateFormat.format(cluster.getLastSeen());

        lines.add(lifeSpan);

        int entityIndex = 0;
        for (String globalEntityId : cluster.getEntityIds()) {
            lines.add(globalEntityId + "\t" + cluster.getMentionHeads().get(entityIndex));
            String articleId = globalEntityId.substring(0, globalEntityId.lastIndexOf("_"));
            String localEntityId = globalEntityId.substring(globalEntityId.lastIndexOf("_") + 1);
            String clusterIndexingPath = clusterIndexFolder.getAbsolutePath() + System.getProperty("file.separator") + articleId;
            FileUtils.write(new File(clusterIndexingPath), localEntityId + "\t" + clusterInfoPath + "\n", true);
            entityIndex++;
        }

        try {
            FileUtils.writeLines(outputFile, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void finish() throws IOException {
        for (Entry<Integer, EntityCluster> clusterEntry : clusters.entrySet()) {
            dumpCluster(clusterEntry.getKey(), clusterEntry.getValue());
        }
    }
}