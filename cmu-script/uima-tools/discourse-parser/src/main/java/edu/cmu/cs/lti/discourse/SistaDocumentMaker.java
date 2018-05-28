package edu.cmu.cs.lti.discourse;

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
import scala.Option;
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

    public SistaDocumentMaker() {
        sents = new ArrayList<>();
    }

    /**
     * Add sentence with syntatic and dependency tree
     *
     * @param tokens
     * @param tags
     * @param lemmas
     * @param beginOffsets
     * @param endOffsets
     * @param dependencies
     */
    public void addSent(String[] tokens, String[] tags, String[] lemmas, int[] beginOffsets, int[] endOffsets, Tree<String> syntacticTree, DirectedGraph<String> dependencies) {
        Sentence sent = new Sentence(tokens, beginOffsets, endOffsets, Some.apply(tags), Some.apply(lemmas), null, null, null, Some.apply(syntacticTree), Some.apply(dependencies));
        sents.add(sent);
    }

    public void addSent(String[] tokens, String[] tags, String[] lemmas, int[] beginOffsets, int[] endOffsets, DirectedGraph<String> dependencies) {
        Option<Tree<String>> noneTree = Option.apply(null);
        Sentence sent = new Sentence(tokens, beginOffsets, endOffsets, Some.apply(tags), Some.apply(lemmas), null, null, null, noneTree, Some.apply(dependencies));
        sents.add(sent);
    }


    public Document makeDocument(String fullText) {
        Annotation anno = new Annotation(fullText);
        Document doc = new CoreNLPDocument(sents.toArray(new Sentence[sents.size()]), Some.apply(anno));
        return doc;
    }

    public static DirectedGraph<String> toSistaDependencies(Table<Integer, Integer, String> allDeps, Set<Integer> roots) {
        List<Tuple3<Object, Object, String>> edges = new ArrayList<>();

        for (Table.Cell<Integer, Integer, String> cell : allDeps.cellSet()) {
            int gov = cell.getRowKey();
            int dep = cell.getColumnKey();
            String label = cell.getValue();
            edges.add(new Tuple3<Object, Object, String>(gov, dep, label));
        }

        return new DirectedGraph<String>(JavaConversions.asScalaBuffer(edges).toList(), JavaConversions.asScalaSet(roots).toSet());
    }

    public static Tree<String> toSistaTree(StanfordTreeAnnotation stanfordTree, int position) {
        if (stanfordTree.getIsLeaf()) {
            Option<Tree<String>[]> noneChildren = Option.apply(null);
            Tree<String> tree = new Tree<>(stanfordTree.getPennTreeLabel(), noneChildren, 0, position, position + 1);
            return tree;
        }

        @SuppressWarnings("unchecked")
        Tree<String>[] children = new Tree[stanfordTree.getChildren().size()];

        int firstTokenIdx = firstTokenIndex(stanfordTree);

        for (int i = 0; i < stanfordTree.getChildren().size(); i++) {
            StanfordTreeAnnotation childTree = stanfordTree.getChildren(i);
            int childTokenIdx = firstTokenIndex(childTree);
            children[i] = toSistaTree(stanfordTree.getChildren(i), position + childTokenIdx - firstTokenIdx);
        }

        int headTokenId = Integer.parseInt(stanfordTree.getHead().getId()); //document wide id
        int headTokenIndex = headTokenId - firstTokenIdx; //system wide position
        int headTokenPosition = headTokenIndex + position; //tree wide position

//        System.out.println(stanfordTree.getCoveredText());
//        System.out.println(stanfordTree.getHead().getCoveredText());
//        System.out.println("Head index "+headTokenIndex);
//        System.out.println("# Children "+children.length);
//        System.out.println("Head token position "+headTokenPosition);

        int headTreeIndex = -1;
        int i = 0;
        for (Tree<String> child: children){
//            System.out.println(child.toString());
//            System.out.println(child.startOffset());
//            System.out.println(child.endOffset());
            if (child.startOffset() <= headTokenPosition && child.endOffset() > headTokenPosition){
                headTreeIndex = i;
                break;
            }

            i++;
        }

//        System.out.println("Head tree position "+headTreeIndex);

        int start = children[0].startOffset();
        int end = children[children.length - 1].endOffset();

        return new Tree<>(stanfordTree.getPennTreeLabel(), Some.apply(children), headTreeIndex, start, end);
    }

    private static int firstTokenIndex(StanfordTreeAnnotation stanfordTree){
        return Integer.parseInt(Iterables.get(JCasUtil.selectCovered(StanfordCorenlpToken.class, stanfordTree), 0).getId());
    }
}