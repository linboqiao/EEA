package edu.cmu.cs.lti.frame;

import com.google.common.base.Function;
import com.google.common.collect.TreeTraverser;
import edu.cmu.cs.lti.model.FrameNode;
import org.apache.commons.io.FileUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/19/17
 * Time: 3:35 PM
 *
 * @author Zhengzhong Liu
 */
public class FrameRelationReader {
    private Map<String, FrameNode> frameByName;
    private Map<String, FrameNode.FrameElement> feByName;

    private String fnRelationPath;

    public FrameRelationReader(String fnRelationPath) throws JDOMException, IOException {
        frameByName = new HashMap<>();
        feByName = new HashMap<>();
        readNodes(fnRelationPath);
        this.fnRelationPath = fnRelationPath;
    }

    private void populateRelations(String targetRelationType) throws JDOMException, IOException {
        readFnRelations(fnRelationPath, targetRelationType);
    }

    public Map<String, FrameNode> getFrameByName() {
        return frameByName;
    }

    public Map<String, FrameNode.FrameElement> getFeByName() {
        return feByName;
    }

    private Set<String> getAllSubFrameNames(String superFrameName) {
        Set<String> childFrameNames = new HashSet<>();
        childFrameNames.add(superFrameName);
        for (FrameNode frameNode : getAllSubFrames(superFrameName)) {
            childFrameNames.add(frameNode.getFrameName());
        }
        return childFrameNames;
    }

    private Iterable<FrameNode> getAllSubFrames(String superFrameName) {
        FrameNode superFrame = frameByName.get(superFrameName);
        TreeTraverser<FrameNode> traverser = TreeTraverser.using(
                new Function<FrameNode, Iterable<FrameNode>>() {
                    @Nullable
                    @Override
                    public Iterable<FrameNode> apply(@Nullable FrameNode frameNode) {
                        return frameNode.getSubFrames();
                    }
                }
        );
        if (superFrame == null) {
            return new ArrayList<>();
        }

        return traverser.breadthFirstTraversal(superFrame);
    }

    private void readNodes(String fnRelatonPath) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        builder.setDTDHandler(null);
        Document doc = builder.build(fnRelatonPath);
        Element data = doc.getRootElement();
        Namespace ns = data.getNamespace();

        List<Element> relationTypeGroup = data.getChildren("frameRelationType", ns);

        for (Element relationsByType : relationTypeGroup) {
            List<Element> frameRelations = relationsByType.getChildren("frameRelation", ns);
            for (Element frameRelation : frameRelations) {
                String subFrameName = frameRelation.getAttributeValue("subFrameName");
                String superFrameName = frameRelation.getAttributeValue("superFrameName");
                getOrCreateNode(frameByName, subFrameName);
                getOrCreateNode(frameByName, superFrameName);
            }
        }
    }

    private Map<String, FrameNode> readFnRelations(String fnRelatonPath, String targetRelationType)
            throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        builder.setDTDHandler(null);
        Document doc = builder.build(fnRelatonPath);
        Element data = doc.getRootElement();
        Namespace ns = data.getNamespace();

        List<Element> relationTypeGroup = data.getChildren("frameRelationType", ns);

        for (Element relationsByType : relationTypeGroup) {
            String relationType = relationsByType.getAttributeValue("name");

            if (relationType.equals(targetRelationType)) {
                List<Element> frameRelations = relationsByType.getChildren("frameRelation", ns);
                for (Element frameRelation : frameRelations) {
                    String subFrameName = frameRelation.getAttributeValue("subFrameName");
                    String superFrameName = frameRelation.getAttributeValue("superFrameName");

                    List<Element> feRelations = frameRelation.getChildren("FERelation", ns);

                    FrameNode subNode = getOrCreateNode(frameByName, subFrameName);
                    FrameNode superNode = getOrCreateNode(frameByName, superFrameName);

                    for (Element feRelation : feRelations) {
                        String subFeName = feRelation.getAttributeValue("subFEName");
                        FrameNode.FrameElement subFe = getOrCreateFe(feByName, subNode, subFeName);
                        String superFeName = feRelation.getAttributeValue("superFEName");
                        FrameNode.FrameElement superFe = getOrCreateFe(feByName, superNode, superFeName);
                        superFe.addSubFrameElement(subFe);
                    }
                    superNode.addSubFrame(subNode);
                }
            }
        }
        return frameByName;
    }

    private FrameNode.FrameElement getOrCreateFe(Map<String, FrameNode.FrameElement> feByName, FrameNode frameNode,
                                                 String feName) {
        String fullFeName = getFullFeName(frameNode.getFrameName(), feName);
        if (feByName.containsKey(fullFeName)) {
            return feByName.get(fullFeName);
        } else {
            FrameNode.FrameElement fe = frameNode.addFrameElement(feName);
            feByName.put(fullFeName, fe);
            return fe;
        }
    }

    private FrameNode getOrCreateNode(Map<String, FrameNode> frameByName, String frameName) {
        if (frameByName.containsKey(frameName)) {
            return frameByName.get(frameName);
        } else {
            FrameNode node = new FrameNode(frameName);
            frameByName.put(frameName, node);
            return node;
        }
    }

    private String getFullFeName(String frameName, String feName) {
        return frameName + "." + feName;
    }

    public static void main(String[] argv) throws JDOMException, IOException {
        FrameRelationReader inheritanceExtractor =
                new FrameRelationReader("../data/resources/fndata-1.7/frRelation.xml");
        inheritanceExtractor.populateRelations("Inheritance");
        FrameRelationReader subframeExtractor =
                new FrameRelationReader("../data/resources/fndata-1.7/frRelation.xml");
        subframeExtractor.populateRelations("Subframe");

        String usefulDir = argv[0] + "/useful";
        String ignoreDir = argv[0] + "/ignore";

        for (String topLevel : new String[]{"Event", "Process", "Offenses", "Appeal", "Cogitation", "State",
                "Beat_opponent", "Being_pregnant", "Billing", "Disembarking", "Ride_vehicle", "Board_vehicle",
                "Fire_burning", "Exclude_member", "Expansion", "Exporting", "Giving_birth", "Giving_in", "Telling",
                "Body_movement", "Breaking_apart", "Breaking_off", "Bragging", "Bringing", "Violence", "Win_prize",
                "Preventing_or_letting", "Delivery", "Defending", "Deciding", "Detaining", "Discussion", "Emitting",
                "Food_gathering", "Growing_food", "Be_in_agreement_on_action", "Hindering", "Success_or_failure",
                "Import_export_scenario", "Impact", "Installing", "Judgment", "Judgment_communication", "Justifying",
                "Leadership", "Losing", "Make_agreement_on_action", "Meet_with", "Make_noise", "Occupy_rank", "Project",
                "Statement", "Delivery", "Rebellion", "Revolution", "Protest", "Protecting", "Provide_lodging",
                "Questioning", "Quarreling", "Reading_activity", "Repayment", "Reporting", "Request", "Research",
                "Resurrection", "Scheduling", "Sending", "Studying", "Meet_with_response", "Offering", "Prank",
                "Bond_maturation", "Breathing", "Catastrophe", "Catching_fire", "Cause_to_burn", "Cause_to_land",
                "Reciprocality", "Citing", "Claim_ownership", "Colonization", "Competition", "Convey_importance",
                "Excreting", "Forgiveness", "Grasp", "Importing", "Labeling", "Launch_process", "Locating", "Mention",
                "Offering", "Patrolling", "Performing_arts", "Planting", "Sacrificing_for", "Satisfying", "Sharing",
                "Speak_on_topic", "Storing", "Surrendering", "Take_place_of", "Terrorism", "Triggering", "Use_vehicle",
                "Wagering", "Waiting", "Want_suspect", "Adopt_selection", "Deny_or_grant_permission", "Dunking",
                "Estimating", "Forgoing", "Progression", "Prohibiting_or_licensing", "Reasoning", "Recovery",
                "Scouring", "Sent_items", "Tolerating", "Trap", "Try_defendant", "Undressing", "Verification",
                "Withdraw_from_participation", "Activity_prepare"
        }) {
            FileUtils.writeLines(new File(usefulDir, topLevel + "_inherit"),
                    inheritanceExtractor.getAllSubFrameNames(topLevel)
            );
        }

        writeSubFrames(subframeExtractor, inheritanceExtractor,
                new String[]{"Crime_scenario", "Process", "Employee_scenario", "Employer_scenario",
                        "Employment_scenario", "Hunting_scenario", "Fire_emergency_scenario", "Fire_end_scenario",
                        "Fire_stopping_scenario", "Sleep_wake_cycle", "Product_development_scenario",
                        "Invasion_scenario", "Similarity", "Arithmetic"},
                usefulDir
        );

        for (String topLevel : new String[]{"Attributes", "Relation", "Entity", "Locale", "Being_included",
                "Being_attached", "Being_in_operation", "Being_located", "Emotions", "Existence", "Employer_scenario",
                "Employment_scenario", "Aggregate", "Inclusion_scenario",
                "Locative_relation", "Posture", "State_of_entity", "Transportation_status"}) {
            FileUtils.writeLines(new File(ignoreDir, topLevel + "_inherit"),
                    inheritanceExtractor.getAllSubFrameNames(topLevel));
        }

        writeSubFrames(subframeExtractor, inheritanceExtractor, new String[]{"Artifact"}, ignoreDir);
    }

    private static void writeSubFrames(FrameRelationReader subframeExtractor, FrameRelationReader inheritanceExtractor,
                                       String[] targetFrames, String outDir) throws IOException {
        for (String topLevel : targetFrames) {
            Set<String> subframes = subframeExtractor.getAllSubFrameNames(topLevel);
            Set<String> frames = new HashSet<>();
            frames.addAll(subframes);
            for (String subframe : subframes) {
                frames.addAll(inheritanceExtractor.getAllSubFrameNames(subframe));
            }
            FileUtils.writeLines(new File(outDir, topLevel + "_subframe"), frames);
        }
    }
}
