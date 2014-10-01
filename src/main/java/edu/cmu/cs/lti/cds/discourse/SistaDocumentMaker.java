package edu.cmu.cs.lti.cds.discourse;

import com.google.common.collect.Iterables;
import com.google.common.collect.Table;
import edu.arizona.sista.processors.Document;
import edu.arizona.sista.processors.Sentence;
import edu.arizona.sista.processors.corenlp.CoreNLPDocument;
import edu.arizona.sista.struct.DirectedGraph;
import edu.arizona.sista.struct.Tree;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.StanfordTreeAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import org.apache.uima.fit.util.JCasUtil;
import scala.Some;
import scala.Tuple3;
import scala.collection.JavaConversions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 9/30/14
 * Time: 2:50 PM
 */
public class SistaDocumentMaker {
    List<Sentence> sents;

    /**
     * Add constituent, will use constituent discourse parsing
     *
     * @param tokens
     * @param tags
     * @param lemmas
     * @param beginOffsets
     * @param endOffsets
     * @param syntacticTree
     */
    public void addSent(String[] tokens, String[] tags, String[] lemmas, int[] beginOffsets, int[] endOffsets, Tree<String> syntacticTree) {
        Sentence sent = new Sentence(tokens, beginOffsets, endOffsets, Some.apply(tags), Some.apply(lemmas), null, null, null, Some.apply(syntacticTree), null);
        sents.add(sent);
    }

    /**
     * Add syntax, will use syntax parsing
     *
     * @param tokens
     * @param tags
     * @param lemmas
     * @param beginOffsets
     * @param endOffsets
     * @param dependencies
     */
    public void addSent(String[] tokens, String[] tags, String[] lemmas, int[] beginOffsets, int[] endOffsets, DirectedGraph<String> dependencies) {
        Sentence sent = new Sentence(tokens, beginOffsets, endOffsets, Some.apply(tags), Some.apply(lemmas), null, null, null, null, Some.apply(dependencies));
        sents.add(sent);
    }

    public Document makeDocument(String fullText) {
        Annotation anno = new Annotation(fullText);
        Document doc = new CoreNLPDocument(sents.toArray(new Sentence[sents.size()]), Some.apply(anno));
        return doc;
    }

    public static DirectedGraph<String> toSistaDependencies(Table<Integer,Integer, String> allDeps, Set<Integer> roots){
        List<Tuple3<Object, Object, String>> edges = new ArrayList<>();

        for (Table.Cell<Integer,Integer,String> cell : allDeps.cellSet()){
            int gov = cell.getRowKey();
            int dep = cell.getColumnKey();
            String label = cell.getValue();
            edges.add(new Tuple3<Object, Object, String>(gov,dep,label));
        }


        return new DirectedGraph<String>(JavaConversions.asScalaBuffer(edges).toList(), JavaConversions.asScalaSet(roots).toSet());
    }

    public static Tree<String> toSistaTree(StanfordTreeAnnotation stanfordTree, int position) {
        if (stanfordTree.getIsLeaf()) {
            Tree<String> tree = new Tree<>(
                    stanfordTree.getPennTreeLabel(), null, 0, position, position + 1);
            position += 1;
            return tree;
        }

        @SuppressWarnings("unchecked")
        Tree<String>[] children = new Tree[stanfordTree.getChildren().size()];

        for (int i = 0; i < stanfordTree.getChildren().size(); i++) {
            children[i] = toSistaTree(stanfordTree.getChildren(i), position);
        }

        int headId = Integer.parseInt(stanfordTree.getHead().getId());
        int firstTokenId = Integer.parseInt(Iterables.get(JCasUtil.selectCovered(StanfordCorenlpToken.class, stanfordTree), 0).getId());
        int headIndex = headId - firstTokenId;

        int start = children[0].startOffset();
        int end = children[children.length - 1].startOffset();

        return new Tree<>(stanfordTree.getPennTreeLabel(), Some.apply(children), headIndex, start, end);
    }
}