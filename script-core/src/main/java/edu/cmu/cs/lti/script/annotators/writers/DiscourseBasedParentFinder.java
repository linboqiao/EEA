package edu.cmu.cs.lti.script.annotators.writers;

import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.RstTree;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.FileNotFoundException;
import java.io.IOException;
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

    Set<EventMention> printedEvent;

    public void initialize(final UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        printedEvent = new HashSet<>();
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        try {
            writer = new PrintWriter("data/event_hierarchy/" + UimaConvenience.getShortDocumentNameWithOffset(aJCas));
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

        writer.println("[" + tree.getRelationLabel() + " " + tree.getRelationDirection() + "]");

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

    /**
     * @param args
     * @throws java.io.IOException
     * @throws org.apache.uima.UIMAException
     */
    public static void main(String[] args) throws UIMAException, IOException {
        String className = DiscourseBasedParentFinder.class.getSimpleName();

        System.out.println(className + " started...");

        // ///////////////////////// Parameter Setting ////////////////////////////
        // Note that you should change the parameters below for your configuration.
        // //////////////////////////////////////////////////////////////////////////
        // Parameters for the reader
        String paramInputDir = "data/02_discourse_parsed";

        // Parameters for the writer
        String paramParentOutputDir = "data";
        String paramBaseOutputDirName = "discourse_parsed";
        String paramOutputFileSuffix = null;
        // ////////////////////////////////////////////////////////////////

        String paramTypeSystemDescriptor = "TypeSystem";

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        // Instantiate a collection reader to get XMI as input.
        // Note that you should change the following parameters for your setting.
        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createTimeSortedGzipXmiReader(typeSystemDescription, paramInputDir, false);

        AnalysisEngineDescription discourseParser = AnalysisEngineFactory.createEngineDescription(
                DiscourseBasedParentFinder.class, typeSystemDescription);

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createGzipWriter(
                paramParentOutputDir, paramBaseOutputDirName, 2, paramOutputFileSuffix, null);

        SimplePipeline.runPipeline(reader, discourseParser, writer);
    }
}
