package edu.cmu.cs.lti.emd.annotators;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/27/15
 * Time: 12:03 PM
 */
public class EventMentionCandidateIdentifier extends AbstractLoggingAnnotator {



    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {

        Map<String, String> candidates = semaforCandidateFinder(aJCas);

        System.out.println("Candidates " + candidates.size());

        int coverage = 0;

        TObjectIntMap<String> typeCount = new TObjectIntHashMap<>();

        Map<String, Collection<String>> goldAnnos = getGoldAnnotations(aJCas).asMap();

        for (Map.Entry<String, Collection<String>> gold : goldAnnos.entrySet()) {
            String goldEid = gold.getKey();
            Collection<String> goldWids = gold.getValue();

            for (Map.Entry<String, String> candidate : candidates.entrySet()) {
                String candidateWid = candidate.getKey();
                String candidateType = candidate.getValue();

                if (goldWids.contains(candidateWid)) {
                    coverage++;
                    typeCount.adjustOrPutValue(candidateType, 1, 1);
                    break;
                }
            }
        }

        logger.info("Coverage : " + coverage * 1.0 / goldAnnos.size());
        for (TObjectIntIterator<String> iter = typeCount.iterator(); iter.hasNext(); ) {
            iter.advance();
            System.err.println(iter.key() + " " + iter.value());
        }

    }

    private ArrayListMultimap<String, String> getGoldAnnotations(JCas aJCas) {
        JCas goldStandard = UimaConvenience.getView(aJCas, "goldStandard");

        ArrayListMultimap<String, String> eid2Wid = ArrayListMultimap.create();

        for (EventMention mention : JCasUtil.select(goldStandard, EventMention.class)) {
            for (Word word : FSCollectionFactory.create(mention.getMentionTokens(), Word.class)) {
                eid2Wid.put(mention.getId(), word.getId());
            }
        }


        return eid2Wid;
    }


    private Map<String, String> semaforCandidateFinder(JCas aJCas) {
        Map<String, String> wid2Type = new HashMap<>();

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
                List<SingleEventFeature> allFeatures = new ArrayList<>();
                Word labelHeadWord = UimaNlpUtils.findHeadFromTreeAnnotation(aJCas, targetLabel);
                wid2Type.put("t" + labelHeadWord.getId(), frameName);
            }
        }
        return wid2Type;
    }

    private void fanseCandidateFinder(JCas aJCas) {

    }
}
