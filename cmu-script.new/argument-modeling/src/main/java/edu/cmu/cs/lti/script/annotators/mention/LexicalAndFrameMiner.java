package edu.cmu.cs.lti.script.annotators.mention;

import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.script.type.SemaforAnnotationSet;
import edu.cmu.cs.lti.script.type.SemaforLabel;
import edu.cmu.cs.lti.script.type.SemaforLayer;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.stanford.nlp.util.Triple;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 11/2/17
 * Time: 3:09 PM
 *
 * @author Zhengzhong Liu
 */
public class LexicalAndFrameMiner extends AbstractLoggingAnnotator {
    private TObjectIntMap<Triple<String, String, String>> lexicalTypes;

    public static final String PARAM_OUTPUT_FILE = "outputFile";
    @ConfigurationParameter(name = PARAM_OUTPUT_FILE)
    private File outputFile;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        lexicalTypes = new TObjectIntHashMap<>();
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Map<SemaforLabel, String> invokedFrames = getAllLabelFrameTypes(aJCas);
        for (EventMention eventMention : JCasUtil.select(aJCas, EventMention.class)) {
            for (SemaforLabel semaforLabel : JCasUtil.selectCovered(SemaforLabel.class, eventMention)) {
                if (invokedFrames.containsKey(semaforLabel)) {
                    for (String eventType : eventMention.getEventType().split(";")) {
                        lexicalTypes.adjustOrPutValue(Triple.makeTriple(
                                UimaNlpUtils.getLemmatizedHead(eventMention),
                                eventType, invokedFrames.get(semaforLabel)), 1, 1);
                    }
                }
            }
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
            lexicalTypes.forEachEntry((triple, count) -> {
                try {
                    writer.write(String.format("%s\t%s\t%s\t%d\n", triple.first, triple.second, triple.third, count));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            });
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<SemaforLabel, String> getAllLabelFrameTypes(JCas aJCas) {
        Map<SemaforLabel, String> invokedFrames = new HashMap<>();
        for (SemaforAnnotationSet annoSet : JCasUtil.select(aJCas, SemaforAnnotationSet.class)) {
            Collection<SemaforLayer> layers = FSCollectionFactory.create(
                    annoSet.getLayers(), SemaforLayer.class);
            for (SemaforLayer layer : layers) {
                if (layer.getName().equals("Target")) {
                    SemaforLabel targetLabel = layer.getLabels(0);
                    invokedFrames.put(targetLabel, annoSet.getFrameName());
                }
            }
        }
        return invokedFrames;
    }

    public static void main(String[] argv) throws UIMAException, SAXException, CpeDescriptorException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");
        String workingDir = argv[0];
        String inputDir = argv[1];
        String outputFile = argv[2];

        CollectionReaderDescription reader = CustomCollectionReaderFactory
                .createXmiReader(typeSystemDescription, workingDir, inputDir);

        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
                LexicalAndFrameMiner.class, typeSystemDescription,
                LexicalAndFrameMiner.PARAM_OUTPUT_FILE, outputFile
        );

        new BasicPipeline(reader, true, true, 2, writer).run();
    }
}
