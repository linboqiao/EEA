package edu.cmu.cs.lti.cds;

import edu.cmu.cs.lti.utils.StanfordCoreNlpUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 9/3/14
 * Time: 8:09 PM
 */
public class RelatedDocumentFinder {
    public  List<List<String>> findDocumentsByEntity(List<String> documents) {
        List<List<String>> relatedDocuments = new ArrayList<List<String>>();
        List<Set<String>> entityClusters = new ArrayList<Set<String>>();

        for (String document : documents) {
            Annotation parsedDoc = StanfordCoreNlpUtils.parseWithCorenlp(document);

            List<CoreMap> sentences = StanfordCoreNlpUtils.getSentences(parsedDoc);

            String entity = "";
            String lastLabel = "";

            List<String> entities = new ArrayList<String>();

            for (CoreMap sent : sentences) {
                for (CoreLabel word : StanfordCoreNlpUtils.getTokens(sent)) {
                    String thisLabel = word.get(CoreAnnotations.AnswerAnnotation.class);
                    if (!thisLabel.equals("O")) {
                        if (lastLabel.equals(thisLabel)) {
                            entity += " " + word.word();
                        } else {
                            if (lastLabel.equals("PERSON") || lastLabel.equals("ORGANIZATION")) {
                                System.out.println(entity);
                            }
                            entities.add(entity.toLowerCase());
                            entity = word.word();

                        }
                    }
                    lastLabel = thisLabel;
                }
            }

            if (relatedDocuments.size() == 0){
                relatedDocuments.add(new ArrayList<String>());
                relatedDocuments.get(0).add(document);
                entityClusters.add(new HashSet<String>());
                entityClusters.get(0).addAll(entities);
            }else {
                boolean inPreviousCluster = false;
                for (int clusterId = 0; clusterId < relatedDocuments.size(); clusterId++) {
                    Set<String> clusterEntities = entityClusters.get(clusterId);
                    for (String thisEntity : entities){
                        if (clusterEntities.contains(thisEntity)){
                            relatedDocuments.get(clusterId).add(document);
                            entityClusters.get(clusterId).addAll(entities);
                            inPreviousCluster = true;
                            break;
                        }
                    }
                }
                if (! inPreviousCluster){
                    relatedDocuments.add(new ArrayList<String>());
                    relatedDocuments.get(relatedDocuments.size() -1 ).add(document);
                    entityClusters.add(new HashSet<String>());
                    entityClusters.get(entityClusters.size() -1 ).addAll(entities);
                }
            }
        }

        return relatedDocuments;
    }
}
