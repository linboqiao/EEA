package edu.cmu.cs.lti.emd.annotators.twostep;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.emd.annotators.UsefulFramDetector;
import edu.cmu.cs.lti.ling.FrameDataReader;
import edu.cmu.cs.lti.ling.WordNetSearcher;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.mit.jwi.item.POS;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.javatuples.Pair;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

    public static final String PARAM_USEFUL_FRAME_DIR = "usefulFrameDir";

    public static final String PARAM_FOR_TRAINING = "forTraining";

    public static final String COMPONENT_ID = CandidateEventMentionDetector.class.getSimpleName();

    @ConfigurationParameter(name = PARAM_GOLD_STANDARD_VIEW_NAME)
    private String goldStandardViewName;

    @ConfigurationParameter(name = PARAM_FRAME_DATA_PATH)
    private String frameDirPath;

    @ConfigurationParameter(name = PARAM_WORDNET_PATH)
    private String wnDictPath;

    @ConfigurationParameter(name = PARAM_SEM_LINK_PATH)
    private String semLinkDirPath;

    @ConfigurationParameter(name = PARAM_USEFUL_FRAME_DIR)
    private String usefulFrameDir;

    @ConfigurationParameter(name = PARAM_FOR_TRAINING)
    private boolean forTraining;

    private ArrayListMultimap<String, String> lexicon2Frame;

    private WordNetSearcher wns;

    private POS[] targetPos = {POS.VERB, POS.NOUN};
    private Map<String, String> vn2Fn;
    private Map<String, String> pb2Vn;
    private Map<Pair<String, String>, Pair<String, String>> pb2VnRoles;
    private Map<Pair<String, String>, Pair<String, String>> vn2fnRoles;

    private String[] usefulPartOfSpeech = {"FW", "JJ", "NN", "V"};

    private Map<StanfordCorenlpToken, EventMention> goldWords;

    private Map<StanfordCorenlpToken, CandidateEventMention> token2Candidates;

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

        vn2Fn = FrameDataReader.getFN2VNFrameMap(semLinkDirPath + "/vn-fn/VN-FNRoleMapping.txt", false);
        pb2Vn = FrameDataReader.getFN2VNFrameMap(semLinkDirPath + "/vn-pb/vnpbMappings", true);

        pb2VnRoles = FrameDataReader.getVN2PBRoleMap(semLinkDirPath + "/vn-pb/vnpbMappings", true);
        vn2fnRoles = FrameDataReader.getFN2VNRoleMap(semLinkDirPath + "/vn-fn/VN-FNRoleMapping.txt", false);


        logger.info("Loading useful frames");

        try {
            readUsefulFrames();
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("Done loading");
    }

    private void readUsefulFrames() throws IOException {
        usefulFrames = new HashSet<>();
        usefulHeads = new HashSet<>();

        for (String line : FileUtils.readLines(new File(usefulFrameDir, UsefulFramDetector.frameFileName))) {
            usefulFrames.add(line.split(" ")[0]);
        }

        for (String line : FileUtils.readLines(new File(usefulFrameDir, UsefulFramDetector.headFileName))) {
            usefulHeads.add(line.split(" ")[0]);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        UimaConvenience.printProcessLog(aJCas, logger);
        token2Candidates = new HashMap<>();

        align = new TokenAlignmentHelper();
        align.loadWord2Stanford(aJCas, TbfEventDataReader.COMPONENT_ID);
        align.loadStanford2Fanse(aJCas);
        align.loadFanse2Stanford(aJCas);

        if (forTraining) {
            JCas goldView = UimaConvenience.getView(aJCas, goldStandardViewName);
            goldWords = new HashMap<>();
            for (EventMention mention : JCasUtil.select(goldView, EventMention.class)) {
                List<StanfordCorenlpToken> contentWords = getUsefulTokens(mention, aJCas, align);
                for (StanfordCorenlpToken contentWord : contentWords) {
                    goldWords.put(contentWord, mention);
                }
            }
        }

        semaforMentionFinder(aJCas);
        fanseMentionFinder(aJCas);
    }

    public void semaforMentionFinder(JCas aJCas) {
        for (SemaforAnnotationSet annoSet : JCasUtil.select(aJCas, SemaforAnnotationSet.class)) {
            String frameName = annoSet.getFrameName();

            SemaforLabel trigger = null;
            List<SemaforLabel> frameElements = new ArrayList<>();


            for (SemaforLayer layer : FSCollectionFactory.create(annoSet.getLayers(), SemaforLayer.class)) {
                String layerName = layer.getName();
                if (layerName.equals("Target")) {// Target that invoke the frame
                    trigger = layer.getLabels(0);
                } else if (layerName.equals("FE")) {// Frame element
                    FSArray elements = layer.getLabels();
                    if (elements != null) {
                        frameElements.addAll(FSCollectionFactory.create(elements, SemaforLabel.class).stream()
                                .collect(Collectors.toList()));
                    }
                }
            }

            StanfordCorenlpToken triggerHead = UimaNlpUtils.findHeadFromTreeAnnotation(trigger);

            if (!usefulFrames.contains(frameName) || !usefulHeads.contains(triggerHead.getLemma().toLowerCase())) {
                continue;
            }

            CandidateEventMention candidate;
            if (token2Candidates.containsKey(triggerHead)) {
                candidate = token2Candidates.get(triggerHead);
            } else {
                candidate = createCandidateMention(aJCas, trigger.getBegin(), trigger.getEnd(), triggerHead);
            }

            candidate.setPotentialFrames(UimaConvenience.appendStringList(aJCas, candidate.getPotentialFrames(),
                    frameName));

            for (SemaforLabel frameElement : frameElements) {
                String feName = frameElement.getName();
                CandidateEventMentionArgument argument = new CandidateEventMentionArgument(aJCas, frameElement
                        .getBegin(), frameElement.getEnd());
                UimaAnnotationUtils.finishAnnotation(argument, COMPONENT_ID, 0, aJCas);
                argument.setRoleName(feName);
                argument.setHeadWord(UimaNlpUtils.findHeadFromTreeAnnotation(argument));
                candidate.setArguments(UimaConvenience.appendFSList(aJCas, candidate.getArguments(), argument,
                        CandidateEventMentionArgument.class));
            }
        }
    }


    public void fanseMentionFinder(JCas aJCas) {
        for (FanseToken token : JCasUtil.select(aJCas, FanseToken.class)) {
            StanfordCorenlpToken triggerHead = align.getStanfordToken(token);

            String propbankSense = token.getLexicalSense();
            if (propbankSense == null) {
                propbankSense = triggerHead.getLemma().toLowerCase() + ".01";
            }

            String frameName = FrameDataReader.getFrameFromPropBankSense(propbankSense, pb2Vn, vn2Fn);
            FSList childSemantics = token.getChildSemanticRelations();

            if (!usefulFrames.contains(frameName) || !usefulHeads.contains(triggerHead.getLemma().toLowerCase())) {
                continue;
            }

            CandidateEventMention candidate;
            if (token2Candidates.containsKey(triggerHead)) {
                candidate = token2Candidates.get(triggerHead);
            } else {
                candidate = createCandidateMention(aJCas, token.getBegin(), token.getEnd(), triggerHead);
            }

            if (frameName != null) {
                candidate.setPotentialFrames(UimaConvenience.appendStringList(aJCas, candidate.getPotentialFrames(),
                        frameName));
            } else {
                for (String searchedFrame : searchForFrames(triggerHead.getLemma().toLowerCase(), triggerHead.getPos
                        ())) {
                    candidate.setPotentialFrames(UimaConvenience.appendStringList(aJCas, candidate.getPotentialFrames
                            (), searchedFrame));
                }
            }

            if (childSemantics != null) {
                for (FanseSemanticRelation relation : FSCollectionFactory.create(childSemantics,
                        FanseSemanticRelation.class)) {
                    String argX = relation.getSemanticAnnotation().replace("ARG", "");

                    Word role = relation.getChild();
                    CandidateEventMentionArgument argument = new CandidateEventMentionArgument(aJCas, role.getBegin()
                            , role.getEnd());
                    UimaAnnotationUtils.finishAnnotation(argument, COMPONENT_ID, 0, aJCas);

                    Pair<String, String> pbRolePair = new Pair<>(propbankSense, argX);

                    Pair<String, String> fnRolePair = null;

                    if (pb2VnRoles.containsKey(pbRolePair)) {
                        Pair<String, String> vnRolePair = pb2VnRoles.get(pbRolePair);
                        if (vnRolePair != null) {
                            fnRolePair = vn2fnRoles.get(vnRolePair);
                        }
                    }

                    if (fnRolePair != null) {
                        argument.setRoleName(fnRolePair.getValue1());
                    }
                    candidate.setArguments(UimaConvenience.appendFSList(aJCas, candidate.getArguments(), argument,
                            CandidateEventMentionArgument.class));
                }
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
                goldWords.add(align.getStanfordToken(JCasUtil.selectCovered(aJCas, Word.class, word.getBegin(), word
                        .getEnd()).get(0)));
            }
        } else {
            goldWords.addAll(JCasUtil.selectCovered(aJCas, StanfordCorenlpToken.class, mention.getBegin(), mention
                    .getEnd()));
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

    private CandidateEventMention createCandidateMention(JCas aJCas, int begin, int end, StanfordCorenlpToken
            triggerHead) {
        CandidateEventMention candidate = new CandidateEventMention(aJCas, begin, end);
        candidate.setHeadWord(triggerHead);
        if (forTraining && goldWords.containsKey(triggerHead)) {
            EventMention goldMention = goldWords.get(triggerHead);
            String goldType = goldMention.getEventType();
            if (goldType.equals("Movement_Transport")) {
                System.err.println("Correct transport type");
                goldType = "Movement_Transport-Person";
            } else if (goldType.equals("Contact_Phone-Write")) {
                System.err.println("Correct communicate type");
                goldType = "Contact_Communicate";
            }
            candidate.setGoldStandardMentionType(goldType);
            candidate.setGoldRealis(goldMention.getRealisType());
        }
        UimaAnnotationUtils.finishAnnotation(candidate, COMPONENT_ID, 0, aJCas);
        token2Candidates.put(triggerHead, candidate);
        return candidate;
    }
}