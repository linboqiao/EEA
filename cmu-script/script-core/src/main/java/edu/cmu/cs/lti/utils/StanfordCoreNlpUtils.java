package edu.cmu.cs.lti.utils;

import edu.cmu.cs.lti.script.type.*;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 9/3/14
 * Time: 8:19 PM
 */
public class StanfordCoreNlpUtils {

    public static Word fixByDependencyHead(ComponentAnnotation anno, Word currentHead) {
        return fixByDependencyHead(anno, currentHead, new HashSet<Word>());
    }


    public static Word fixByDependencyHead(ComponentAnnotation anno, Word currentHead, Set<Word> visitedHeadNodes) {
        if (!inRange(currentHead, anno.getBegin(), anno.getEnd())) {
            return null;
        } else {
            if (currentHead.getHeadDependencyRelations() != null) {
                // Actually we normally have one head, this for is not looping anyway.
                for (StanfordDependencyRelation headDep : FSCollectionFactory.create(currentHead
                        .getHeadDependencyRelations(), StanfordDependencyRelation.class)) {
                    Word headToken = headDep.getHead();
                    if (visitedHeadNodes.contains(headToken)) {
                        return currentHead;
                    } else {
                        visitedHeadNodes.add(headToken);
                    }

                    Word newHead = fixByDependencyHead(anno, headToken, visitedHeadNodes);
                    if (newHead == null) {
                        return currentHead;
                    } else {
                        return newHead;
                    }
                }
            }

            // No head dependency, either a root or a collapsed prep.
            return currentHead;
        }
    }


    public static boolean inRange(ComponentAnnotation anno, int begin, int end) {
        return begin <= anno.getBegin() && end >= anno.getEnd();
    }


    /**
     * @param aJCas
     * @param finder
     * @param tf
     * @param uimaTree
     * @return The head token if found, otherwise null.
     */
    public static StanfordCorenlpToken findHead(JCas aJCas, HeadFinder finder, TreeFactory tf, StanfordTreeAnnotation
            uimaTree) {
        int offset = uimaTree.getBegin();

        Tree tree = toStanfordTree(tf, uimaTree);

        Tree head = findHead(finder, tree);

        IntPair span = head.getSpan();

        if (span != null) {
            for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, uimaTree)) {
                if (token.getBegin() - offset == span.get(0) && token.getEnd() - offset == span.get(1)) {
                    return token;
                }
            }
        }

        return null;
    }

    public static Tree findHead(HeadFinder finder, Tree tree) {
        return finder.determineHead(tree);
    }

    public static HeadFinder getSemanticHeadFinder() {
        return new SemanticHeadFinder();
    }

    public static TreeFactory getLablledScoredTreeFactory() {
        return new LabeledScoredTreeFactory();
    }

    public static Tree toStanfordTree(TreeFactory tf, StanfordTreeAnnotation uimaTree) {
        Tree tree = uima2StanfordTree(tf, uimaTree);
        tree.setSpans();
        return tree;
    }

    public static Tree uima2StanfordTree(TreeFactory tf, StanfordTreeAnnotation uimaTree) {
        if (uimaTree.getIsLeaf()) {
            return tf.newLeaf(uimaTree.getPennTreeLabel());
        } else {
            FSArray uimaChildTree = uimaTree.getChildren();
            List<Tree> daugheterTreeList = new ArrayList<>(uimaChildTree.size());
            for (int i = 0; i < uimaChildTree.size(); i++) {
                StanfordTreeAnnotation childTree = (StanfordTreeAnnotation) uimaChildTree.get(i);
                Tree daugheterTree = uima2StanfordTree(tf, childTree);
                daugheterTreeList.add(uima2StanfordTree(tf, childTree));
            }
            return tf.newTreeNode(uimaTree.getPennTreeLabel(), daugheterTreeList);
        }
    }


    public static Annotation parseWithCorenlp(String text) {
        // creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        // create an empty Annotation just with the given text
        Annotation document = new Annotation(text);

        // run all Annotators on this text
        pipeline.annotate(document);

        return document;
    }

    public static List<CoreMap> getSentences(Annotation document) {
        return document.get(CoreAnnotations.SentencesAnnotation.class);
    }

    public static List<CoreLabel> getTokens(CoreMap sentence) {
        return sentence.get(CoreAnnotations.TokensAnnotation.class);
    }

    public static Tree getTree(CoreMap sentence) {
        // this is the parse tree of the current sentence
        return sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
    }

    public static SemanticGraph getDependencies(CoreMap sentence) {
        // this is the Stanford dependency graph of the current sentence
        return sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
    }

    public static String getText(CoreLabel token) {
        // this is the text of the token
        return token.get(CoreAnnotations.TextAnnotation.class);
    }

    public static String getPos(CoreLabel token) { // this is the POS tag of the token
        return token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
    }

    public static String getNer(CoreLabel token) {
        return token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
    }

    public static Map<Integer, CorefChain> getCorefGraph(Annotation document) {
        return document.get(CorefCoreAnnotations.CorefChainAnnotation.class);
    }


}
