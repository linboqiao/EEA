package edu.cmu.cs.lti.cds.annotators;

import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.RstTree;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 10/2/14
 * Time: 10:40 AM
 */
public class DiscourseBasedParentFinder extends JCasAnnotator_ImplBase {

    PrintWriter writer;

    Set<EventMention> printedEvent ;

    public void initialize(final UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        printedEvent = new HashSet<>();
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        try {
            writer = new PrintWriter("data/event_hierarchy/"+UimaConvenience.getShortDocumentNameWithOffset(aJCas));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        UimaConvenience.printProcessLog(aJCas);
        int maxSpan = 0;
        RstTree rootTree = null;
        for (RstTree tree : JCasUtil.select(aJCas, RstTree.class)) {
            int treeSpan = tree.getEnd() - tree.getBegin();
            if (treeSpan > maxSpan) {
                rootTree = tree;
                maxSpan = treeSpan;
            }
        }

        System.out.println("Recursing");
        recurse(rootTree, 0, writer);
        System.out.println("Recurse done");

        writer.close();
    }

    private void recurse(RstTree tree, int depth, PrintWriter writer) {


        if (tree.getIsTerminal()) {
            return;
        } else {
            for (int i = 0; i < tree.getChildren().size(); i++) {
                recurse(tree.getChildren(i), depth + 1, writer);
            }
        }

        int d = 0;
        List<EventMention> mentions = JCasUtil.selectCovered(EventMention.class, tree);

        writer.println("["+tree.getRelationLabel()+" "+tree.getRelationDirection()+"]");

        if (mentions.size() > 0) {
            while (d < depth) {
                writer.print(". ");
                d++;
            }
            for (EventMention mention : JCasUtil.selectCovered(EventMention.class, tree)) {
                if (!printedEvent.contains(mention)) {
                    printedEvent.add(mention);
                    writer.print(mention.getCoveredText());
                    writer.print(" ");
                }
            }
            writer.println();
        }
    }
}
