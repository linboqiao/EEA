package edu.cmu.cs.lti.emd.annotators.misc;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.ling.FrameDataReader;
import edu.cmu.cs.lti.ling.WordNetSearcher;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.TokenAlignmentHelper;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.mit.jwi.item.POS;
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
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/29/15
 * Time: 3:10 PM
 */
public class UsefulFramDetector extends AbstractLoggingAnnotator {
    public static final String PARAM_GOLD_STANDARD_VIEW_NAME = "goldStandard";

    public static final String PARAM_FRAME_DATA_PATH = "frameDataPath";

    public static final String PARAM_WORDNET_PATH = "wordNetPath";

    public static final String PARAM_SEM_LINK_PATH = "semLinkPath";

    public static final String PARAM_OUTPUT_DIR = "outputDirPath";


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
        UimaConvenience.printProcessLog(aJCas, logger);

        JCas goldView = UimaConvenience.getView(aJCas, goldStandardViewName);

        TokenAlignmentHelper align = new TokenAlignmentHelper();
        align.loadWord2Stanford(aJCas, TbfEventDataReader.COMPONENT_ID);
        align.loadStanford2Fanse(aJCas);

        Map<Word, String> target2Frame = getFrameAnnotations(aJCas);

        for (EventMention mention : JCasUtil.select(goldView, EventMention.class)) {
            List<StanfordCorenlpToken> contentWords = getUsefulTokens(mention, aJCas, align);

            boolean fbf = false;
            for (StanfordCorenlpToken contentWord : contentWords) {
                String mentionFrame = target2Frame.get(contentWord);

                if (mentionFrame == null) {
                    mentionFrame = getFrameFromPropBank(align.getFanseToken(contentWord));
                }

                if (mentionFrame == null) {
                    for (String searchedFrame : searchForFrames(contentWord.getLemma().toLowerCase(), contentWord.getPos())) {
                        frameCounter.adjustOrPutValue(searchedFrame, 1, 1);
                        fbf = true;
                    }
                } else {
                    frameCounter.adjustOrPutValue(mentionFrame, 1, 1);
                    fbf = true;
                }

                headCounter.adjustOrPutValue(contentWord.getLemma().toLowerCase(), 1, 1);
            }

            if (fbf) {
                totalFoundByFrame++;
            }

            totalMentions++;
        }
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

    private Map<Word, String> getFrameAnnotations(JCas aJCas) {
        Map<Word, String> invokeMapping = new HashMap<>();

        for (SemaforAnnotationSet annotationSet : JCasUtil.select(aJCas, SemaforAnnotationSet.class)) {
            FSArray layers = annotationSet.getLayers();
            if (layers != null) {
                for (SemaforLayer layer : FSCollectionFactory.create(layers, SemaforLayer.class)) {
                    if (layer.getName().equals("Target")) {
                        invokeMapping.put(UimaNlpUtils.findHeadFromStanfordAnnotation(layer.getLabels(0)), annotationSet.getFrameName());
                    }
                }
            }
        }
        return invokeMapping;
    }

    private String getFrameFromPropBank(FanseToken token) {
        String propbankSense = token.getLexicalSense();
        return FrameDataReader.getFrameFromPropBankSense(propbankSense, pb2Vn, vn2Fn);
    }

    private List<String> searchForFrames(String word, String wordPos) {
        String shortPos = wordPos.substring(0, 1);
        if (lexicon2Frame.containsKey(word + "." + shortPos)) {
            return lexicon2Frame.get(word + '.' + shortPos);
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

    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        System.err.println("Found by frame " + totalFoundByFrame + ", total mention " + totalMentions);

        try {
            Writer frameWriter = new BufferedWriter(new FileWriter(new File(outputDir, frameFileName)));
            Writer headWriter = new BufferedWriter(new FileWriter(new File(outputDir, headFileName)));

            for (TObjectIntIterator<String> iter = frameCounter.iterator(); iter.hasNext(); ) {
                iter.advance();
                frameWriter.write(iter.key() + " " + iter.value() + "\n");
            }

            for (TObjectIntIterator<String> iter = headCounter.iterator(); iter.hasNext(); ) {
                iter.advance();
                headWriter.write(iter.key() + " " + (iter.value()) + "\n");
            }

            frameWriter.close();
            headWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}