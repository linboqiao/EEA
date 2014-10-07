package edu.cmu.cs.lti.cds.annotators;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.arizona.sista.discourse.rstparser.DiscourseTree;
import edu.arizona.sista.discourse.rstparser.RSTParser;
import edu.arizona.sista.discourse.rstparser.TokenOffset;
import edu.arizona.sista.processors.Document;
import edu.arizona.sista.processors.corenlp.CoreNLPProcessor;
import edu.arizona.sista.struct.DirectedGraph;
import edu.arizona.sista.struct.Tree;
import edu.cmu.cs.lti.cds.discourse.SistaDocumentMaker;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import scala.Tuple2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 9/30/14
 * Time: 2:17 PM
 */
public class DiscourseParserAnnotator extends AbstractLoggingAnnotator {
    Logger logger;
    RSTParser rstParser;
    PrintWriter writer;
    String parserPath;

    public static final String COMPONENT_ID = DiscourseParserAnnotator.class.getName();

    Table<Integer, Integer, StanfordCorenlpToken> tokenIndexedByPosition = HashBasedTable.create();

    @Override
    public void initialize(final UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        File out = new File("data/discourse_out_uima");
        try {
            writer = new PrintWriter(new FileOutputStream(out));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        super.initialize(context);
        logger = Logger.getLogger(this.getClass().getName());

         parserPath = RSTParser.DEFAULT_CONSTITUENTSYNTAX_MODEL_PATH();
//        parserPath = RSTParser.DEFAULT_DEPENDENCYSYNTAX_MODEL_PATH();

        logger.log(Level.INFO, "Loading RST parser from " + parserPath);
        rstParser = CoreNLPProcessor.fetchParser(parserPath);
        logger.log(Level.INFO, "Done.");
    }

    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        writer.close();
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        logger.info(progressInfo(aJCas));

        SistaDocumentMaker maker = new SistaDocumentMaker();

        for (Sentence sent : JCasUtil.select(aJCas, Sentence.class)) {
            List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sent);
            String[] words = new String[tokens.size()];
            String[] tags = new String[tokens.size()];
            String[] lemmas = new String[tokens.size()];
            int[] beginOffsets = new int[tokens.size()];
            int[] endOffsets = new int[tokens.size()];

            for (int i = 0; i < tokens.size(); i++) {
                StanfordCorenlpToken token = tokens.get(i);
                words[i] = token.getCoveredText();
                tags[i] = token.getPos();
                lemmas[i] = token.getLemma();
                beginOffsets[i] = token.getBegin();
                endOffsets[i] = token.getEnd();
                tokenIndexedByPosition.put(Integer.parseInt(sent.getId()), i, token);
            }


            //The latter one call with Constituent Tree given, which benefits head finding
            //Also, the sista depedency based head finding may ask for dependency from all nodes, which
            //is probably not available since the "CC-processed" and punctuation causing some nodes have
            //no dependency attached at all, you may face a "indexOutOfBoundary" error.
            //Having both parsing is like a safe-guard

//            maker.addSent(words, tags, lemmas, beginOffsets, endOffsets, getDependencies(tokens));
            maker.addSent(words, tags, lemmas, beginOffsets, endOffsets, getTree(sent), getDependencies(tokens));
        }

        Document document = maker.makeDocument(aJCas.getDocumentText());

        Tuple2<DiscourseTree, Tuple2<Object, Object>[][]> out = rstParser.parse(document, false);
        DiscourseTree dt = out._1();

//        writer.println(dt);

        annotateDiscourseTree(aJCas, dt);
    }


    private RstTree annotateDiscourseTree(JCas aJCas, DiscourseTree root) {
        TokenOffset firstTokenPosition = root.firstToken();
        TokenOffset lastTokenPosition = root.lastToken();

        StanfordCorenlpToken firstToken = tokenIndexedByPosition.get(firstTokenPosition.sentence(), firstTokenPosition.token());
        StanfordCorenlpToken lastToken = tokenIndexedByPosition.get(lastTokenPosition.sentence(), lastTokenPosition.token());

        int begin = firstToken.getBegin();
        int end = lastToken.getEnd();

        RstTree tree = new RstTree(aJCas);
        tree.setBegin(begin);
        tree.setEnd(end);

        DiscourseTree[] discourseChildren = root.children();

        if (discourseChildren != null) {
            FSArray childTrees = new FSArray(aJCas, discourseChildren.length);
            for (int i = 0; i < discourseChildren.length; i++) {
                childTrees.set(i, annotateDiscourseTree(aJCas, discourseChildren[i]));
            }
            tree.setChildren(childTrees);
        } else {
            tree.setIsTerminal(true);
        }

        tree.setRelationLabel(root.relationLabel());
        tree.setRelationDirection(root.relationDirection().toString());

//        System.out.println(tree.getRelationDirection() + " " + tree.getRelationLabel());

        UimaAnnotationUtils.finishAnnotation(tree, begin, end, COMPONENT_ID, 0, aJCas);

        return tree;
    }

    private Tree<String> getTree(Sentence sent) {
        StanfordTreeAnnotation root = null;
        StanfordTreeAnnotation maxTree = null;
        int maxSpan = -1;
        for (StanfordTreeAnnotation tree : JCasUtil.selectCovered(StanfordTreeAnnotation.class, sent)) {
            if (tree.getIsRoot()) {
                root = tree;
            }
            int treeSpan = tree.getBegin() - tree.getEnd();
            if (treeSpan > maxSpan) {
                maxSpan = treeSpan;
                maxTree = tree;
            }
        }

        if (root == null) {
            logger.warning("Using maximum tree as the root tree");
            root = maxTree;
        }

        return SistaDocumentMaker.toSistaTree(root, 0);
    }


    /**
     * @param tokens Tokens from one complete sentence
     * @return
     */
    private DirectedGraph<String> getDependencies(List<StanfordCorenlpToken> tokens) {
        Set<Integer> roots = new HashSet<>();

        Table<Integer, Integer, String> allDeps = HashBasedTable.create();
        int baseId = -1;
        for (StanfordCorenlpToken token : tokens) {
            int rawTokenId = Integer.parseInt(token.getId());
            if (baseId == -1) {
                baseId = rawTokenId;
            }
            int tokenId = rawTokenId - baseId;
            if (token.getIsDependencyRoot()) {
                roots.add(tokenId);
            }

            FSList headDependenciesFS = token.getHeadDependencyRelations();
            FSList childDependenciesFS = token.getChildDependencyRelations();

            if (headDependenciesFS != null) {
                for (StanfordDependencyRelation relation : FSCollectionFactory.create(headDependenciesFS, StanfordDependencyRelation.class)) {
                    String t = relation.getDependencyType();
                    int headTokenId = Integer.parseInt(relation.getHead().getId()) - baseId;
                    int childTokenId = Integer.parseInt(relation.getChild().getId()) - baseId;
                    allDeps.put(headTokenId, childTokenId, t);
                }
            }

            if (childDependenciesFS != null) {
                for (StanfordDependencyRelation relation : FSCollectionFactory.create(childDependenciesFS, StanfordDependencyRelation.class)) {
                    String t = relation.getDependencyType();
                    int headTokenId = Integer.parseInt(relation.getHead().getId()) - baseId;
                    int childTokenId = Integer.parseInt(relation.getChild().getId()) - baseId;
                    allDeps.put(headTokenId, childTokenId, t);
                }
            }
        }
        return SistaDocumentMaker.toSistaDependencies(allDeps, roots);
    }
}