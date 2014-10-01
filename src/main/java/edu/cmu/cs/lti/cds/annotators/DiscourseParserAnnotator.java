package edu.cmu.cs.lti.cds.annotators;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.arizona.sista.discourse.rstparser.DiscourseTree;
import edu.arizona.sista.discourse.rstparser.RSTParser;
import edu.arizona.sista.processors.Document;
import edu.arizona.sista.processors.fastnlp.FastNLPProcessor;
import edu.cmu.cs.lti.cds.discourse.SistaDocumentMaker;
import edu.cmu.cs.lti.script.type.Sentence;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.StanfordDependencyRelation;
import edu.cmu.cs.lti.script.type.StanfordTreeAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import scala.Tuple2;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 9/30/14
 * Time: 2:17 PM
 */
public class DiscourseParserAnnotator extends JCasAnnotator_ImplBase {

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

                for (StanfordDependencyRelation relation : FSCollectionFactory.create(token.getHeadDependencyRelations(), StanfordDependencyRelation.class)) {
                    String t = relation.getDependencyType();
                    int headTokenId = Integer.parseInt(relation.getHead().getId());
                    int childTokenId = Integer.parseInt(relation.getChild().getId());
                    allDeps.put(headTokenId, childTokenId, t);
                }
            }


            maker.addSent(words, tags, lemmas, beginOffsets, endOffsets, SistaDocumentMaker.toSistaDependencies(allDeps, roots));
//            maker.addSent(words, tags, lemmas, beginOffsets, endOffsets, SistaDocumentMaker.toSistaTree(getRootTree(sent), 0));
        }

        Document document = maker.makeDocument(aJCas.getDocumentText());
//        RSTParser rstParser = CoreNLPProcessor.fetchParser(RSTParser.DEFAULT_CONSTITUENTSYNTAX_MODEL_PATH());
        RSTParser rstParser = FastNLPProcessor.fetchParser(RSTParser.DEFAULT_DEPENDENCYSYNTAX_MODEL_PATH());

        Tuple2<DiscourseTree, Tuple2<Object, Object>[][]> out = rstParser.parse(document, true);
        DiscourseTree dt = out._1();

        System.out.println(dt);
    }

    private StanfordTreeAnnotation getRootTree(Sentence sent) {
        for (StanfordTreeAnnotation tree : JCasUtil.selectCovered(StanfordTreeAnnotation.class, sent)) {
            if (tree.getIsRoot()) {
                return tree;
            }
        }

        throw new IllegalAccessError("Error in parse tree, there is not root");
    }
}