package edu.cmu.cs.lti.emd.annotators;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.collection_reader.EventMentionDetectionDataReader;
import edu.cmu.cs.lti.ling.FrameDataReader;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/27/15
 * Time: 12:03 PM
 */
public class EventMentionCandidateIdentifier extends AbstractLoggingAnnotator {

    public static final String PARAM_SEM_LINK_DIR = "semLinkDir";

    @ConfigurationParameter(name = PARAM_SEM_LINK_DIR)
    String semLinkDirPath;

    Map<String, String> vn2Fn;
    Map<String, String> pb2Vn;

    TokenAlignmentHelper align = new TokenAlignmentHelper();
    TObjectIntMap<String> typeCount = new TObjectIntHashMap<>();

    double averageCoverage = 0;
    int docCount = 0;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        vn2Fn = FrameDataReader.getFN2VNFrameMap(semLinkDirPath + "/vn-fn/VN-FNRoleMapping.txt", true);
        pb2Vn = FrameDataReader.getFN2VNFrameMap(semLinkDirPath + "/vn-pb/vnpbMappings", true);
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas, logger);

        align.loadWord2Stanford(aJCas, EventMentionDetectionDataReader.componentId);
        align.loadFanse2Stanford(aJCas);

        Map<Word, String> semaforCandidates = semaforCandidateFinder(aJCas);
        Map<Word, String> fanseCandidates = fanseCandidateFinder(aJCas);

        Map<Word, String> allCandidates = mergeFrameNames(fanseCandidates, semaforCandidates);

        logger.info("Candidates " + allCandidates.size() + " " + fanseCandidates.size() + " " + semaforCandidates.size());

        int coverage = 0;

        Map<EventMention, Collection<Word>> goldAnnos = getGoldAnnotations(aJCas).asMap();

        for (Map.Entry<EventMention, Collection<Word>> gold : goldAnnos.entrySet()) {
            EventMention goldEventMention = gold.getKey();
            Collection<Word> goldWords = gold.getValue();

            boolean found = false;

            for (Map.Entry<Word, String> candidate : allCandidates.entrySet()) {
                Word candidateHeadWord = candidate.getKey();
                String candidateType = candidate.getValue();

                if (goldWords.contains(candidateHeadWord)) {
//                    logger.info("Found by " + candidateHeadWord.getCoveredText());
                    found = true;
                    coverage++;
                    typeCount.adjustOrPutValue(candidateType, 1, 1);
                    break;
                }
            }

            if (!found) {
                logger.info(String.format("Mention [%s]-%s is not covered, it has type [%s]",
                        goldEventMention.getCoveredText(), goldEventMention.getId(), goldEventMention.getEventType()));

                for (Word w : goldWords) {
                    System.err.println(w.getCoveredText() + " " + w.getBegin() + " " + w.getEnd() + " " + w.getId());
                }
            }
        }

        logger.info("Coverage : " + coverage * 1.0 / goldAnnos.size());
        averageCoverage += coverage * 1.0 / goldAnnos.size();
        docCount++;
    }


    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        for (TObjectIntIterator<String> iter = typeCount.iterator(); iter.hasNext(); ) {
            iter.advance();
            System.err.println(iter.key() + " " + iter.value());
        }

        System.err.println("Average coverage " + averageCoverage / docCount);
    }


    private ArrayListMultimap<EventMention, Word> getGoldAnnotations(JCas aJCas) {
        JCas goldStandard = UimaConvenience.getView(aJCas, "goldStandard");

        ArrayListMultimap<EventMention, Word> eid2Wid = ArrayListMultimap.create();

        for (EventMention mention : JCasUtil.select(goldStandard, EventMention.class)) {
            System.err.println(mention.getCoveredText() + " " + mention.getId());
            for (Word word : FSCollectionFactory.create(mention.getMentionTokens(), Word.class)) {
//                StanfordCorenlpToken goldWord = UimaNlpUtils.findHeadFromTreeAnnotation(aJCas, word);
                StanfordCorenlpToken goldWord = align.getStanfordToken(word);
                eid2Wid.put(mention, goldWord);
                System.err.println("Gold word : " + goldWord.getCoveredText());
            }
        }

        return eid2Wid;
    }

    private Map<Word, String> mergeFrameNames(Map<Word, String>... candidates) {
        Map<Word, String> merged = new HashMap<>();
        for (Map<Word, String> candidateMap : candidates) {
            merged.putAll(candidateMap);
        }
        return merged;
    }


    private Map<Word, String> semaforCandidateFinder(JCas aJCas) {
        Map<Word, String> word2Frame = new HashMap<>();

        for (SemaforAnnotationSet annoSet : JCasUtil.select(aJCas, SemaforAnnotationSet.class)) {
            String frameName = annoSet.getFrameName();
            SemaforLabel targetLabel = null;
            List<SemaforLabel> frameElements = new ArrayList<SemaforLabel>();

            for (SemaforLayer layer : FSCollectionFactory.create(annoSet.getLayers(), SemaforLayer.class)) {
                String layerName = layer.getName();
                if (layerName.equals("Target")) {// Target that invoke the frame
                    targetLabel = layer.getLabels(0);
                } else if (layerName.equals("FE")) {// Frame element
                    FSArray elements = layer.getLabels();
                    if (elements != null) {
                        for (SemaforLabel element : FSCollectionFactory.create(elements, SemaforLabel.class)) {
                            frameElements.add(element);
                        }
                    }
                }
            }

            if (targetLabel != null) {
                StanfordCorenlpToken labelHeadWord = UimaNlpUtils.findHeadFromTreeAnnotation(aJCas, targetLabel);
                word2Frame.put(labelHeadWord, frameName);
            }
        }
        return word2Frame;
    }

    private Map<Word, String> fanseCandidateFinder(JCas aJCas) {
        Map<Word, String> word2Frame = new HashMap<>();

        for (FanseToken token : JCasUtil.select(aJCas, FanseToken.class)) {
            String propbankSense = token.getLexicalSense();

            if (propbankSense != null) {
                String frameName = getFrameFromPropBankSense(propbankSense);
                Word candidateTrigger = align.getStanfordToken(token);
                word2Frame.put(candidateTrigger, frameName);
            }

            if (token.getChildSemanticRelations() != null) {
                for (FanseSemanticRelation childRelation : FSCollectionFactory.create(token.getChildSemanticRelations(), FanseSemanticRelation.class)) {

                }
            }
        }

        return word2Frame;
    }

    private String getFrameFromPropBankSense(String propBankSense) {
        if (propBankSense == null) {
            return null;
        }
        String vnFrame = pb2Vn.get(propBankSense);
        if (vnFrame != null) {
            return vn2Fn.get(vnFrame);
        }
        return null;
    }
}
