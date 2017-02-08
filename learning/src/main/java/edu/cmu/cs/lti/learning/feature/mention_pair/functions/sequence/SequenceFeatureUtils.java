package edu.cmu.cs.lti.learning.feature.mention_pair.functions.sequence;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.ling.WordNetSearcher;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Date: 2/4/17
 * Time: 12:59 PM
 *
 * @author Zhengzhong Liu
 */
public class SequenceFeatureUtils {
    private final Configuration modelConfig;

    private WordNetSearcher wns;

    private Map<String, SchemaReader> schemas;

    private Table<String, String, Integer> verbFreqTable;

    public SequenceFeatureUtils(Configuration modelConfig) throws IOException {
        this.modelConfig = modelConfig;

        String resourceDir = modelConfig.get("edu.cmu.cs.lti.resource.dir");

        String wnPath = new File(resourceDir, modelConfig.get("edu.cmu.cs.lti.wndict.path")).getPath();

        wns = new WordNetSearcher(wnPath, false);

        File schemaDir = new File(resourceDir, modelConfig.get("edu.cmu.cs.lti.schemas.path"));

        readSchemaData(schemaDir);
    }

    private void readSchemaData(File schemaDir) throws IOException {
        schemas = new HashMap<>();

        for (File file : schemaDir.listFiles()) {
            if (file.isFile()) {
                String filename = file.getName();
                if (filename.startsWith("schemas")) {
                    schemas.put(filename, new SchemaReader(file));
                } else if (filename.equals("verb-pair-orders")) {
                    readVerbPairs(file);
                }
            }
        }
    }

    private void readVerbPairs(File verbPairFile) throws IOException {
        verbFreqTable = HashBasedTable.create();
        for (String line : FileUtils.readLines(verbPairFile)) {
            String[] parts = line.split("\t");
            verbFreqTable.put(parts[0], parts[1], Integer.parseInt(parts[2]));
        }
    }

    public boolean equalRealisConstraint(NodeKey node1, NodeKey node2) {
        return node1.getRealis().equals(node2.getRealis());
    }

    public boolean strictEqualRealisConstraint(NodeKey node1, NodeKey node2) {
        return node1.getRealis().equals(node2.getRealis()) && !node1.getRealis().equals("Other");
    }

    public boolean sentenceWindowConstraint(MentionCandidate firstCandidate, MentionCandidate secondCandidate,
                                            int window) {
        return Math.abs(firstCandidate.getContainedSentence().getIndex()
                - secondCandidate.getContainedSentence().getIndex()) < window;
    }

    public Map<String, Double> generateScriptCompabilityFeatures(MentionCandidate firstCandidate,
                                                                 MentionCandidate secondCandidate) {
        return generateScriptCompabilityFeatures(firstCandidate, secondCandidate, false);
    }

    public Map<String, Double> generateScriptCompabilityFeatures(MentionCandidate firstCandidate,
                                                                 MentionCandidate secondCandidate,
                                                                 boolean addUnorderFeatures) {
        Map<String, Double> compatibilityFeatures = new HashMap<>();

        mentionTypeCompatibility(firstCandidate, secondCandidate, addUnorderFeatures, compatibilityFeatures);
        headwordCompatibility(firstCandidate, secondCandidate, addUnorderFeatures, compatibilityFeatures);
//        frameCompatibility(firstCandidate, secondCandidate, addUnorderFeatures, compatibilityFeatures);
        schemaVerbCompatibility(firstCandidate, secondCandidate, addUnorderFeatures, compatibilityFeatures);
//        schemaNounCompatibility(firstCandidate, secondCandidate, addUnorderFeatures, compatibilityFeatures);

        return compatibilityFeatures;
    }

    public void mentionTypeCompatibility(MentionCandidate firstCandidate, MentionCandidate secondCandidate,
                                         boolean addUnorderFeatures, Map<String, Double> features) {
        if (firstCandidate.getBegin() < secondCandidate.getBegin()) {
            features.put(String.format("ForwardMentionTypePair_%s:%s",
                    firstCandidate.getMentionType(), secondCandidate.getMentionType()), 1.0);
        } else {
            features.put(String.format("BackwardMentionTypePair_%s:%s",
                    firstCandidate.getMentionType(), secondCandidate.getMentionType()), 1.0);
        }

        if (addUnorderFeatures) {
            features.put(String.format("MentionTypePair_%s:%s",
                    firstCandidate.getMentionType(), secondCandidate.getMentionType()), 1.0);
        }
    }

    public void headwordCompatibility(MentionCandidate firstCandidate, MentionCandidate secondCandidate,
                                      boolean addUnorderFeatures, Map<String, Double> features) {
        String firstHead = firstCandidate.getHeadWord().getLemma().toLowerCase();
        String secondHead = secondCandidate.getHeadWord().getLemma().toLowerCase();
        if (firstCandidate.getBegin() < secondCandidate.getBegin()) {
            features.put(String.format("ForwardHeadwordPair_%s:%s", firstHead, secondHead), 1.0);
        } else {
            features.put(String.format("BackwardHeadwordPair_%s:%s", firstHead, secondHead), 1.0);
        }

        if (addUnorderFeatures) {
            features.put(String.format("HeadwordPair_%s:%s", firstHead, secondHead), 1.0);
        }
    }

    public void frameCompatibility(MentionCandidate firstCandidate, MentionCandidate secondCandidate,
                                   boolean addUnorderFeatures, Map<String, Double> features) {
        Word firstHead = firstCandidate.getHeadWord();
        Word secondHead = secondCandidate.getHeadWord();

        if (firstHead.getFrameName() != null && secondHead.getFrameName() != null) {
            if (firstCandidate.getBegin() < secondCandidate.getBegin()) {
                features.put(String.format("ForwardFramePair_%s:%s", firstHead.getFrameName(),
                        secondHead.getFrameName()), 1.0);
            } else {
                features.put(String.format("BackwardFramePair_%s:%s", firstHead.getFrameName(),
                        secondHead.getFrameName()), 1.0);
            }
        }

        if (addUnorderFeatures) {
            features.put(String.format("FramePair_%s:%s", firstHead.getFrameName(), secondHead.getFrameName()), 1.0);
        }
    }

    public void schemaVerbCompatibility(MentionCandidate firstCandidate, MentionCandidate secondCandidate,
                                        boolean addUnorderFeatures, Map<String, Double> features) {
        Word firstHead = firstCandidate.getHeadWord();
        Word secondHead = secondCandidate.getHeadWord();

        String firstLemma = firstHead.getLemma().toLowerCase();
        String secondLemma = secondHead.getLemma().toLowerCase();

        boolean inSchema = false;
        for (String firstVerb : getVerbForms(firstHead.getLemma().toLowerCase(), firstHead.getPos())) {
            for (String secondVerb : getVerbForms(secondHead.getLemma().toLowerCase(), secondHead.getPos())) {
                for (Map.Entry<String, SchemaReader> schema : schemas.entrySet()) {
                    SchemaReader schemaReader = schema.getValue();
                    if (schemaReader.sameSchema(firstVerb, secondVerb)) {
                        inSchema = true;
                        if (firstCandidate.getBegin() < secondCandidate.getBegin()) {
                            features.put("ForwardSameSchema_in_" + schema.getKey(), 1.0);
                        } else {
                            features.put("BackwardSameSchema_in_" + schema.getKey(), 1.0);
                        }

                        if (addUnorderFeatures) {
                            features.put("SameSchema_in_" + schema.getKey(), 1.0);
                        }
                    }
                }
            }
        }

        if (inSchema) {
            features.put("InSchema_order_score", getAddOneRatio(firstLemma, secondLemma));
        }
    }

    private double getAddOneRatio(String verb1, String verb2) {
        int forwardCount = getAddOneCount(verb1, verb2);
        int backwardCount = getAddOneCount(verb2, verb1);
        return 1.0 * forwardCount / backwardCount;
    }

    private int getAddOneCount(String verb1, String verb2) {
        if (verbFreqTable.contains(verb1, verb2)) {
            return verbFreqTable.get(verb1, verb2) + 1;
        } else {
            return 1;
        }
    }

    public void schemaNounCompatibility(MentionCandidate firstCandidate, MentionCandidate secondCandidate,
                                        boolean addUnorderFeatures, Map<String, Double> features) {
        Word firstHead = firstCandidate.getHeadWord();
        Word secondHead = secondCandidate.getHeadWord();

        String firstLemma = firstHead.getLemma().toLowerCase();
        String secondLemma = secondHead.getLemma().toLowerCase();

        Set<String> inSchema = new HashSet<>();

        if (firstHead.getPos().startsWith("N") || secondHead.getPos().startsWith("N")) {
            if (secondHead.getPos().startsWith("N")) {
                if (firstHead.getPos().startsWith("N")) {
                    for (Map.Entry<String, SchemaReader> schema : schemas.entrySet()) {
                        String name = schema.getKey();
                        SchemaReader reader = schema.getValue();
                        reader.nounNounSameSchema(firstLemma, secondLemma);
                        inSchema.add(name);
                    }
                } else if (firstHead.getPos().startsWith("V")) {
                    for (Map.Entry<String, SchemaReader> schema : schemas.entrySet()) {
                        String name = schema.getKey();
                        SchemaReader reader = schema.getValue();
                        reader.verbNounSameSchema(firstLemma, secondLemma);
                        inSchema.add(name);
                    }
                }
            } else if (secondHead.getPos().startsWith("V")) {
                for (Map.Entry<String, SchemaReader> schema : schemas.entrySet()) {
                    String name = schema.getKey();
                    SchemaReader reader = schema.getValue();
                    reader.verbNounSameSchema(secondLemma, firstLemma);
                    inSchema.add(name);
                }
            }
        }

        for (String s : inSchema) {
            if (firstCandidate.getBegin() < secondCandidate.getBegin()) {
                features.put("ForwardSameSchema_in_" + s, 1.0);
            } else {
                features.put("BackwardSameSchema_in_" + s, 1.0);
            }
            if (addUnorderFeatures) {
                features.put("SameSchemaByNoun_in_" + s, 1.0);
            }
        }
    }


    private Set<String> getVerbForms(String lemma, String pos) {
        return wns.getDerivations(lemma, pos).get("verb");
    }
}
