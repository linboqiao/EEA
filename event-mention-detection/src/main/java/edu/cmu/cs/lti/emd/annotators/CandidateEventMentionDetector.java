package edu.cmu.cs.lti.emd.annotators;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.collection_reader.EventMentionDetectionDataReader;
import edu.cmu.cs.lti.ling.FrameDataReader;
import edu.cmu.cs.lti.ling.WordNetSearcher;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.TokenAlignmentHelper;
import edu.mit.jwi.item.POS;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/30/15
 * Time: 12:03 PM
 */
public class CandidateEventMentionDetector extends AbstractLoggingAnnotator {
    public static final String PARAM_GOLD_STANDARD_VIEW_NAME = "goldStandard";

    public static final String PARAM_FRAME_DATA_PATH = "frameDataPath";

    public static final String PARAM_WORDNET_PATH = "wordNetPath";

    public static final String PARAM_SEM_LINK_PATH = "semLinkPath";

    public static final String PARAM_OUTPUT_DIR = "outputDirPath";

    public static final String COMPONENT_ID = CandidateEventMentionDetector.class.getSimpleName();

    @ConfigurationParameter(name = PARAM_GOLD_STANDARD_VIEW_NAME)
    private String goldStandardViewName;

    @ConfigurationParameter(name = PARAM_FRAME_DATA_PATH)
    private String frameDirPath;

    @ConfigurationParameter(name = PARAM_WORDNET_PATH)
    private String wnDictPath;

    @ConfigurationParameter(name = PARAM_SEM_LINK_PATH)
    private String semLinkDirPath;

    @ConfigurationParameter(name = PARAM_OUTPUT_DIR)
    private String outputDirPath;

    TObjectIntMap<String> frameCounter = new TObjectIntHashMap<>();
    TObjectIntMap<String> headCounter = new TObjectIntHashMap<>();

    private ArrayListMultimap<String, String> lexicon2Frame;

    private WordNetSearcher wns;

    private POS[] targetPos = {POS.VERB, POS.NOUN};
    private Map<String, String> vn2Fn;
    private Map<String, String> pb2Vn;

    private int totalFoundByFrame = 0;
    private int totalMentions = 0;

    public static String frameFileName = "usefulFrames";

    public static String headFileName = "usefulHeads";

    private File outputDir;

    private String[] usefulPartOfSpeech = {"FW", "JJ", "NN", "V"};

    private Set<StanfordCorenlpToken> goldWords;

    private Map<StanfordCorenlpToken, CandidateEventMention> candidates;

    Set<String> usefulFrames;
    Set<String> usefulHeads;

    private TokenAlignmentHelper align;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        try {
            logger.info("Loading frame lexicon");
            lexicon2Frame = FrameDataReader.getLexicon2Frame(frameDirPath);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }

        try {
            logger.info("Loading wordnet dictionary");
            wns = new WordNetSearcher(wnDictPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Loading SemLink");

        vn2Fn = FrameDataReader.getFN2VNFrameMap(semLinkDirPath + "/vn-fn/VN-FNRoleMapping.txt", true);
        pb2Vn = FrameDataReader.getFN2VNFrameMap(semLinkDirPath + "/vn-pb/vnpbMappings", true);

        logger.info("Done loading");

        outputDir = new File(outputDirPath);

        if (!outputDir.isDirectory()) {
            outputDir.mkdirs();
        }

    }


    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCas goldView = UimaConvenience.getView(aJCas, goldStandardViewName);

        TokenAlignmentHelper align = new TokenAlignmentHelper();
        align.loadWord2Stanford(aJCas, EventMentionDetectionDataReader.componentId);
        align.loadStanford2Fanse(aJCas);
        align.loadFanse2Stanford(aJCas);

        goldWords = new HashSet<>();

        for (EventMention mention : JCasUtil.select(goldView, EventMention.class)) {
            List<StanfordCorenlpToken> contentWords = getUsefulTokens(mention, aJCas, align);
            goldWords.addAll(contentWords);
        }

        semaforMentionFinder(aJCas);

    }

    public void semaforMentionFinder(JCas aJCas) {
        for (SemaforAnnotationSet annoSet : JCasUtil.select(aJCas, SemaforAnnotationSet.class)) {
            SemaforLabel trigger = null;
            List<SemaforLabel> frameElements = new ArrayList<>();


            for (SemaforLayer layer : FSCollectionFactory.create(annoSet.getLayers(), SemaforLayer.class)) {
                String layerName = layer.getName();
                if (layerName.equals("Target")) {// Target that invoke the frame
                    trigger = layer.getLabels(0);
                } else if (layerName.equals("FE")) {// Frame element
                    FSArray elements = layer.getLabels();
                    if (elements != null) {
                        for (SemaforLabel element : FSCollectionFactory.create(elements, SemaforLabel.class)) {
                            frameElements.add(element);
                        }
                    }
                }
            }

            StanfordCorenlpToken triggerHead = UimaNlpUtils.findHeadFromTreeAnnotation(aJCas, trigger);

            CandidateEventMention candidate;
            if (candidates.containsKey(triggerHead)) {
                candidate = candidates.get(triggerHead);
            } else {
                candidate = new CandidateEventMention(aJCas, trigger.getBegin(), trigger.getEnd());
                UimaAnnotationUtils.finishAnnotation(candidate, COMPONENT_ID, 0, aJCas);
                candidates.put(triggerHead, candidate);
            }

            for (SemaforLabel frameElement : frameElements) {
                String feName = frameElement.getName();
                CandidateEventMentionArgument argument = new CandidateEventMentionArgument(aJCas, frameElement.getBegin(), frameElement.getEnd());
                UimaAnnotationUtils.finishAnnotation(argument, COMPONENT_ID, 0, aJCas);
                argument.setRoleName(feName);
                argument.setHeadWord(UimaNlpUtils.findHeadFromTreeAnnotation(aJCas, argument));
                UimaConvenience.appendFSList(aJCas, candidate.getArgument(), argument, CandidateEventMentionArgument.class);
            }
        }
    }


    public void fanseMentionFinder(JCas aJCas, Set<String> targetFrames) {
        for (FanseToken token : JCasUtil.select(aJCas, FanseToken.class)) {
            String propbankSense = token.getLexicalSense();
            String frameName = FrameDataReader.getFrameFromPropBankSense(propbankSense, pb2Vn, vn2Fn);
            FSList childSemantics = token.getChildSemanticRelations();

            StanfordCorenlpToken sToken = align.getStanfordToken(token);


            if (frameName == null) {
                searchForFrames(sToken.getLemma().toLowerCase(), sToken.getPos());
            }


            if (childSemantics != null) {

            }
        }
    }

    public void frameLookupMentionFinder(JCas aJCas, Set<String> targetFrames) {

    }


    public void brownClusteringMentionFinder(JCas aJCas, Set<String> targetFrames) {

    }

    private List<StanfordCorenlpToken> getUsefulTokens(EventMention mention, JCas aJCas, TokenAlignmentHelper align) {
        List<StanfordCorenlpToken> contentTokens = new ArrayList<>();

        List<StanfordCorenlpToken> goldWords = new ArrayList<>();
        if (mention.getMentionTokens() != null) {
            for (Word word : FSCollectionFactory.create(mention.getMentionTokens(), Word.class)) {
                goldWords.add(align.getStanfordToken(JCasUtil.selectCovered(aJCas, Word.class, word.getBegin(), word.getEnd()).get(0)));
            }
        } else {
            goldWords.addAll(JCasUtil.selectCovered(aJCas, StanfordCorenlpToken.class, mention.getBegin(), mention.getEnd()));
        }

        for (StanfordCorenlpToken goldWord : goldWords) {
            for (String usefulPos : usefulPartOfSpeech) {
                if (goldWord.getPos().startsWith(usefulPos)) {
                    contentTokens.add(goldWord);
                }
            }
        }
        return contentTokens;
    }

    private List<String> searchForFrames(String word, String wordPos) {
        if (lexicon2Frame.containsKey(word + "." + wordPos)) {
            return lexicon2Frame.get(word + '.' + wordPos);
        } else {
            Set<String> potentialFrames = new HashSet<>();
            for (POS pos : targetPos) {
                for (String stem : wns.stem(word, pos)) {
                    String frameLexeme = stem + "." + Character.toUpperCase(pos.getTag());
                    if (lexicon2Frame.containsKey(frameLexeme)) {
                        potentialFrames.addAll(lexicon2Frame.get(frameLexeme));
                    }
                }
            }
            return new ArrayList<>(potentialFrames);
        }
    }
}