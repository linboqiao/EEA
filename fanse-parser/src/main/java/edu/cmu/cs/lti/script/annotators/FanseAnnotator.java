package edu.cmu.cs.lti.script.annotators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.util.FSCollectionFactory;
import org.uimafit.util.JCasUtil;

import com.google.common.collect.ArrayListMultimap;

import tratz.parse.FullSystemWrapper;
import tratz.parse.FullSystemWrapper.FullSystemResult;
import tratz.parse.types.Arc;
import tratz.parse.types.Parse;
import tratz.parse.types.Token;
import edu.cmu.cs.lti.script.type.FanseDependencyRelation;
import edu.cmu.cs.lti.script.type.FanseSemanticRelation;
import edu.cmu.cs.lti.script.type.FanseToken;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.uima.util.UimaConvenience;

/**
 * Runs FANSE parser, and annotate associated types.
 */
public class FanseAnnotator extends JCasAnnotator_ImplBase {

  public static final String PARAM_MODEL_BASE_DIR = "modelBaseDirectory";

  @ConfigurationParameter(name = PARAM_MODEL_BASE_DIR)
  private String modeBaseDir;

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

    modeBaseDir = (String) aContext.getConfigParameterValue(PARAM_MODEL_BASE_DIR);

    try {
      fullSystemWrapper = new FullSystemWrapper(modeBaseDir + PREPOSITION_MODELS, modeBaseDir
              + NOUN_COMPOUND_MODEL, modeBaseDir + POSSESSIVES_MODEL,
              modeBaseDir + SRL_ARGS_MODELS, modeBaseDir + SRL_PREDICATE_MODELS, modeBaseDir
                      + POS_MODEL, modeBaseDir + PARSE_MODEL, modeBaseDir + WORDNET);
    } catch (Exception e) {
      e.printStackTrace();
      throw new ResourceInitializationException();
    }
  }

  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    UimaConvenience.printProcessLog(aJCas);

    List<Sentence> sentList = UimaConvenience.getAnnotationList(aJCas, Sentence.class);

    for (Sentence sent : sentList) {
      List<Word> wordList = JCasUtil.selectCovered(Word.class, sent);

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
      int tokenId = 1;
      for (Token token : resultTokens) {
        Word goldStandardToken = wordList.get(token.getIndex() - 1);
        String fTokenStr = token.getText();
        String sTokenStr = goldStandardToken.getCoveredText();
        if (!fTokenStr.equals(sTokenStr)) {
          System.out.println("A Fanse token is different from a gold standard token. Fanse token: "
                  + fTokenStr + ", a gold standard token: " + sTokenStr);
        }

        int begin = goldStandardToken.getBegin();
        int end = goldStandardToken.getEnd();
        FanseToken fToken = new FanseToken(aJCas, begin, end);
        fToken.setId(Integer.toString(tokenId));
        fToken.setCoarsePos(token.getCoarsePos());
        fToken.setPos(token.getPos());
        fToken.setLexicalSense(token.getLexSense());
        fToken.addToIndexes();
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

          fArc.addToIndexes(aJCas);
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
          fArc.setChild(childToken);
          fArc.setSemanticAnnotation(arc.getSemanticAnnotation());

          semanticHeadRelationMap.put(childToken, fArc);
          semanticChildRelationMap.put(headToken, fArc);

          fArc.addToIndexes(aJCas);
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

        fToken.addToIndexes(aJCas);
      }
    }
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
