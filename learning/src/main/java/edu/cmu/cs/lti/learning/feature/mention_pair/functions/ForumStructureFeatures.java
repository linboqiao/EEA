package edu.cmu.cs.lti.learning.feature.mention_pair.functions;

import edu.cmu.cs.lti.learning.model.MentionCandidate;
import edu.cmu.cs.lti.learning.model.NodeKey;
import edu.cmu.cs.lti.script.type.StanfordCorenlpSentence;
import edu.cmu.cs.lti.script.type.TaggedArea;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.utils.Configuration;
import gnu.trove.map.TObjectDoubleMap;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/13/16
 * Time: 3:44 PM
 *
 * @author Zhengzhong Liu
 */
public class ForumStructureFeatures extends AbstractMentionPairFeatures {
    private Map<TaggedArea, String> contentAuthors;
    private Map<TaggedArea, String> originalAuthors;
    private Map<StanfordCorenlpSentence, TaggedArea> sentence2Tag;

    public ForumStructureFeatures(Configuration generalConfig, Configuration featureConfig) {
        super(generalConfig, featureConfig);
    }

    @Override
    public void initDocumentWorkspace(JCas context) {
        contentAuthors = new HashMap<>();
        originalAuthors = new HashMap<>();
        sentence2Tag = new HashMap<>();

        for (TaggedArea taggedArea : JCasUtil.select(context, TaggedArea.class)) {
            StringArray attributeNames = taggedArea.getTagAttributeNames();
            StringArray attributeValues = taggedArea.getTagAttributeValues();

            if (attributeNames == null || attributeValues == null) {
                continue;
            }

            for (int i = 0; i < attributeNames.size(); i++) {
                String attributeName = attributeNames.get(i);
                String attributeValue = attributeValues.get(i);

                if (attributeName.equals("author")) {
                    contentAuthors.put(taggedArea, attributeValue);
                }

                if (attributeName.equals("orig_author") && taggedArea.getTagName().equals("quote")) {
                    originalAuthors.put(taggedArea, attributeValue);
                }
            }

            for (StanfordCorenlpSentence sentence : JCasUtil.selectCovered(StanfordCorenlpSentence.class, taggedArea)) {
                sentence2Tag.put(sentence, taggedArea);
            }
        }
    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, List<MentionCandidate>
            candidates, NodeKey firstNode, NodeKey secondNode) {
        MentionCandidate firstCandidate = candidates.get(firstNode.getCandidateIndex());
        MentionCandidate secondCandidate = candidates.get(secondNode.getCandidateIndex());


        TaggedArea firstTag = sentence2Tag.get(firstCandidate.getContainedSentence());
        TaggedArea secondTag = sentence2Tag.get(secondCandidate.getContainedSentence());

        if (firstTag == null || secondTag == null) {
            return;
        }

        getTagPairFeatures(firstTag, secondTag, featuresNoLabel);
    }

    private void getTagPairFeatures(TaggedArea firstTag, TaggedArea secondTag, TObjectDoubleMap<String> features) {
        String firstAuthor = contentAuthors.get(firstTag);
        String secondAuthor = contentAuthors.get(secondTag);

        if (firstAuthor != null && secondAuthor != null && firstAuthor.equals(secondAuthor)) {
            addBoolean(features, "ForumSamePostAuthor");
        }

        List<String> secondQuotedAuthors = getQuotedAuthor(secondTag);

        for (String secondQuotedAuthor : secondQuotedAuthors) {
            if (secondQuotedAuthor != null && firstAuthor != null && secondQuotedAuthor.equals(firstAuthor)) {
                addBoolean(features, "ForumQuoteFirstPost");
            }
        }
    }

    private List<String> getQuotedAuthor(TaggedArea tag) {
        List<String> authors = new ArrayList<>();
        for (TaggedArea taggedArea : JCasUtil.selectCovered(TaggedArea.class, tag)) {
            if (taggedArea.getTagName().equals("quote")) {
                authors.add(originalAuthors.get(taggedArea));
            }
        }

        return authors;
    }

    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel,
                                        List<MentionCandidate> candidates, NodeKey firstNode, NodeKey secondNode) {

    }

    @Override
    public void extract(JCas documentContext, TObjectDoubleMap<String> featuresNoLabel, MentionCandidate
            secondCandidate, NodeKey secondNode) {

    }

    @Override
    public void extractCandidateRelated(JCas documentContext, TObjectDoubleMap<String> featuresNeedLabel,
                                        MentionCandidate secondCandidate, NodeKey secondNode) {

    }
}
