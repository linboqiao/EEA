package edu.cmu.cs.lti.cds.annotators;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.arizona.sista.discourse.rstparser.DiscourseTree;
import edu.arizona.sista.discourse.rstparser.RSTParser;
import edu.arizona.sista.processors.Document;
import edu.arizona.sista.processors.fastnlp.FastNLPProcessor;
import edu.arizona.sista.struct.DirectedGraph;
import edu.arizona.sista.struct.Tree;
import edu.cmu.cs.lti.cds.discourse.SistaDocumentMaker;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.StanfordDependencyRelation;
import edu.cmu.cs.lti.script.type.StanfordTreeAnnotation;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
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
public class DiscourseParserAnnotator extends JCasAnnotator_ImplBase {
    Logger logger;
    RSTParser rstParser;
    PrintWriter writer;
    @Override
    public void initialize(final UimaContext context) throws ResourceInitializationException {
        File out = new File("data/discourse_out_uima");
        try {
             writer = new PrintWriter(new FileOutputStream(out));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        super.initialize(context);
        logger = Logger.getLogger(this.getClass().getName());
        logger.log(Level.INFO,"Loading RST parser");
        rstParser = FastNLPProcessor.fetchParser(RSTParser.DEFAULT_DEPENDENCYSYNTAX_MODEL_PATH());
        logger.log(Level.INFO,"Done.");
    }

    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        writer.close();
    }

        @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        SistaDocumentMaker maker = new SistaDocumentMaker();
        for (Sentence sent : JCasUtil.select(aJCas, Sentence.class)) {
            List<StanfordCorenlpToken> tokens = JCasUtil.selectCovered(StanfordCorenlpToken.class, sent);
            String[] words = new String[tokens.size()];
            String[] tags = new String[tokens.size()];
            String[] lemmas = new String[tokens.size()];
            int[] beginOffsets = new int[tokens.size()];
            int[] endOffsets = new int[tokens.size()];

            maker.addSent(words, tags, lemmas, beginOffsets, endOffsets, getTree(sent), getDependencies(tokens));
        }

        Document document = maker.makeDocument(aJCas.getDocumentText());

        Tuple2<DiscourseTree, Tuple2<Object, Object>[][]> out = rstParser.parse(document, false);
        DiscourseTree dt = out._1();

        writer.println(dt);
    }

    private Tree<String> getTree(Sentence sent){
        StanfordTreeAnnotation root = null;
        StanfordTreeAnnotation maxTree = null;
        int maxSpan = -1;
        for (StanfordTreeAnnotation tree : JCasUtil.selectCovered( StanfordTreeAnnotation.class, sent)){
            if (tree .getIsRoot()){
                root = tree;
            }
            int treeSpan = tree.getBegin() - tree.getEnd();
            if (treeSpan > maxSpan ){
                maxSpan = treeSpan;
                maxTree = tree;
            }
        }

        if (root == null){
            logger.warning("Using maximum tree as the root tree");
            root = maxTree;
        }

        return SistaDocumentMaker.toSistaTree(root, 0);
    }


    /**
     *
     * @param tokens Tokens from one complete sentence
     * @return
     */
    private DirectedGraph<String> getDependencies(List<StanfordCorenlpToken> tokens){
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

            FSList headDependenciesFS =  token.getHeadDependencyRelations();
            FSList childDependenciesFS = token.getChildDependencyRelations();

            if (headDependenciesFS != null) {
                for (StanfordDependencyRelation relation : FSCollectionFactory.create(headDependenciesFS, StanfordDependencyRelation.class)) {
                    String t = relation.getDependencyType();
                    int headTokenId = Integer.parseInt(relation.getHead().getId()) - baseId;
                    int childTokenId = Integer.parseInt(relation.getChild().getId()) - baseId;
                    allDeps.put(headTokenId, childTokenId, t);
                }
            }

            if (childDependenciesFS != null){
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