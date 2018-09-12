package edu.cmu.cs.lti.annotators;

import com.google.common.collect.ArrayListMultimap;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.descriptor.ConfigurationParameter;
import tratz.parse.FullSystemWrapper;
import tratz.parse.FullSystemWrapper.FullSystemResult;
import tratz.parse.types.Arc;
import tratz.parse.types.Parse;
import tratz.parse.types.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import org.uimafit.util.FSCollectionFactory;
//import org.uimafit.util.JCasUtil;

/**
 * Runs FANSE parser, and annotate associated types.
 * Required Stanford Parse
 */
public class FanseAnnotator extends AbstractLoggingAnnotator {

    public static final String PARAM_MODEL_BASE_DIR = "modelBaseDirectory";

    public static final String COMPONENT_ID = FanseAnnotator.class.getSimpleName();

    @ConfigurationParameter(name = PARAM_MODEL_BASE_DIR, mandatory = true)
    private String modelBaseDir;

    // these file are assume existence in the base directory
    private static final String POS_MODEL = "posTaggingModel.gz", PARSE_MODEL = "parseModel.gz",
            POSSESSIVES_MODEL = "possessivesModel.gz", NOUN_COMPOUND_MODEL = "nnModel.gz",
            SRL_ARGS_MODELS = "srlArgsWrapper.gz", SRL_PREDICATE_MODELS = "srlPredWrapper.gz",
            PREPOSITION_MODELS = "psdModels.gz", WORDNET = "data/wordnet3";

    public final static Boolean DEFAULT_VCH_CONVERT = Boolean.FALSE;

    public final static String DEFAULT_SENTENCE_READER_CLASS = tratz.parse.io.ConllxSentenceReader.class
            .getName();

    FullSystemWrapper fullSystemWrapper = null;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        modelBaseDir = (String) aContext.getConfigParameterValue(PARAM_MODEL_BASE_DIR);
        if (!modelBaseDir.endsWith("/")) {
            modelBaseDir += "/";
        }

        try {
            fullSystemWrapper = new FullSystemWrapper(modelBaseDir + PREPOSITION_MODELS, modelBaseDir
                    + NOUN_COMPOUND_MODEL, modelBaseDir + POSSESSIVES_MODEL,
                    modelBaseDir + SRL_ARGS_MODELS, modelBaseDir + SRL_PREDICATE_MODELS, modelBaseDir
                    + POS_MODEL, modelBaseDir + PARSE_MODEL, modelBaseDir + WORDNET);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResourceInitializationException();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        logger.info("Annotated with Fanse.");
        annotateFanse(aJCas);
        for (JCas view : getAdditionalViews(aJCas)) {
            annotateFanse(view);
        }
    }

    private void annotateFanse(JCas aJCas) {
        logger.info(progressInfo(aJCas));

        List<Sentence> sentList = UimaConvenience.getAnnotationList(aJCas, Sentence.class);

        for (Sentence sent : sentList) {
            List<Word> wordList = getUniqueWordList(sent);

            Parse par = wordListToParse(wordList);
            tratz.parse.types.Sentence fSent = par.getSentence();
            List<Token> tokens = fSent.getTokens();

            FullSystemResult result = fullSystemWrapper.process(fSent, tokens.size() > 0
                    && tokens.get(0).getPos() == null, true, true, true, true, true);

            Parse dependencyParse = result.getParse();
            Parse semanticParse = result.getSrlParse();

            tratz.parse.types.Sentence resultSent = dependencyParse.getSentence();
            List<Token> resultTokens = resultSent.getTokens();

            // get Token annotation and convert them to UIMA
            Map<Token, FanseToken> Fanse2UimaMap = new HashMap<Token, FanseToken>();
            // Id starts from 1 is confusing, but I started it, better not change it.
            int tokenId = 0;
            for (Token token : resultTokens) {
                Word goldStandardToken = wordList.get(token.getIndex() - 1);
                String fTokenStr = token.getText();
                String sTokenStr = goldStandardToken.getCoveredText();
                if (!fTokenStr.equals(sTokenStr)) {
                    logger.warn("A Fanse token is different from a gold standard token. Fanse token: "
                            + fTokenStr + ", a gold standard token: " + sTokenStr);
                }

                int begin = goldStandardToken.getBegin();
                int end = goldStandardToken.getEnd();
                FanseToken fToken = new FanseToken(aJCas, begin, end);
                fToken.setCoarsePos(token.getCoarsePos());
                fToken.setPos(token.getPos());
                fToken.setLexicalSense(token.getLexSense());
                UimaAnnotationUtils.finishAnnotation(fToken, COMPONENT_ID, tokenId++, aJCas);
                tokenId++;
                Fanse2UimaMap.put(token, fToken);
            }

            // now create depedency edges of these nodes
            ArrayListMultimap<FanseToken, FanseDependencyRelation> dependencyHeadRelationMap = ArrayListMultimap
                    .create();
            ArrayListMultimap<FanseToken, FanseDependencyRelation> dependencyChildRelationMap = ArrayListMultimap
                    .create();

            for (Arc arc : dependencyParse.getArcs()) {
                if (arc == null) {
                    continue;
                }

                FanseToken childToken = Fanse2UimaMap.get(arc.getChild());
                FanseToken headToken = Fanse2UimaMap.get(arc.getHead());

                if (childToken != null || headToken != null) {
                    FanseDependencyRelation fArc = new FanseDependencyRelation(aJCas);
                    fArc.setHead(headToken);
                    fArc.setChild(childToken);
                    fArc.setDependencyType(arc.getDependency());
                    dependencyHeadRelationMap.put(childToken, fArc);
                    dependencyChildRelationMap.put(headToken, fArc);
                    UimaAnnotationUtils.finishTop(fArc, COMPONENT_ID, 0, aJCas);
                }
            }

            // now creat semantic edges of these nodes
            ArrayListMultimap<FanseToken, FanseSemanticRelation> semanticHeadRelationMap = ArrayListMultimap
                    .create();
            ArrayListMultimap<FanseToken, FanseSemanticRelation> semanticChildRelationMap = ArrayListMultimap
                    .create();

            for (Arc arc : semanticParse.getArcs()) {
                if (arc == null || arc.getSemanticAnnotation() == null) {
                    continue;
                }

                FanseToken childToken = Fanse2UimaMap.get(arc.getChild());
                FanseToken headToken = Fanse2UimaMap.get(arc.getHead());

                if (childToken != null || headToken != null) {
                    FanseSemanticRelation fArc = new FanseSemanticRelation(aJCas);
                    fArc.setHead(headToken);
                    fArc.setChildHead(childToken);
                    fArc.setSemanticAnnotation(arc.getSemanticAnnotation());

                    semanticHeadRelationMap.put(childToken, fArc);
                    semanticChildRelationMap.put(headToken, fArc);
                    UimaAnnotationUtils.finishTop(fArc, COMPONENT_ID, 0, aJCas);
                }
            }

            // associate token annotation with arc
            for (FanseToken fToken : Fanse2UimaMap.values()) {
                if (dependencyHeadRelationMap.containsKey(fToken)) {
                    fToken.setHeadDependencyRelations(FSCollectionFactory.createFSList(aJCas,
                            dependencyHeadRelationMap.get(fToken)));
                }
                if (dependencyChildRelationMap.containsKey(fToken)) {
                    fToken.setChildDependencyRelations(FSCollectionFactory.createFSList(aJCas,
                            dependencyChildRelationMap.get(fToken)));
                }
                if (semanticHeadRelationMap.containsKey(fToken)) {
                    fToken.setHeadSemanticRelations(FSCollectionFactory.createFSList(aJCas,
                            semanticHeadRelationMap.get(fToken)));
                }
                if (semanticChildRelationMap.containsKey(fToken)) {
                    fToken.setChildSemanticRelations(FSCollectionFactory.createFSList(aJCas,
                            semanticChildRelationMap.get(fToken)));
                }
            }
        }

        logger.info(String.format("Annotated %d fanse tokens.", JCasUtil.select(aJCas, FanseToken.class).size()));
    }


    private List<Word> getUniqueWordList(Sentence sent) {
        List<Word> wordList = new ArrayList<>();
        for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, sent)) {
            wordList.add(token);
        }
        return wordList;
    }

    private Parse wordListToParse(List<Word> words) {
        Token root = new Token("[ROOT]", 0);
        List<Token> tokens = new ArrayList<Token>();
        List<Arc> arcs = new ArrayList<Arc>();

        int tokenNum = 0;
        for (Word word : words) {
            tokenNum++;
            String wordString = word.getCoveredText();
            Token token = new Token(wordString, tokenNum);
            tokens.add(token);
        }
        // Currently does not implement the Quote converstion by Tratz in TokenizingSentenceReader
        // line = mDoubleQuoteMatcher.reset(line).replaceAll("\"");
        // line = mSingleQuoteMatcher.reset(line).replaceAll("'");

        Parse result = new Parse(new tratz.parse.types.Sentence(tokens), root, arcs);
        return result;
    }
}
