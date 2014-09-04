package edu.cmu.cs.lti.utils;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 9/3/14
 * Time: 8:19 PM
 */
public class StanfordCoreNlpUtils {

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
