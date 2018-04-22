package edu.cmu.cs.lti.salience.annotators;

import edu.cmu.cs.lti.salience.utils.LookupTable;
import edu.cmu.cs.lti.salience.utils.SalienceFeatureExtractor;
import edu.cmu.cs.lti.salience.utils.SalienceUtils;
import edu.cmu.cs.lti.script.type.Body;
import edu.cmu.cs.lti.script.type.EventMention;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.utils.FeatureUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/20/18
 * Time: 6:14 PM
 *
 * @author Zhengzhong Liu
 */
public class SalienceInputCreator extends AbstractLoggingAnnotator {

    public static final String PARAM_JOINT_EMBEDDING = "jointEmbeddingFile";
    @ConfigurationParameter(name = PARAM_JOINT_EMBEDDING)
    private File jointEmbeddingFile;

    public static final String PARAM_OUTPUT_DIR = "outputDir";
    @ConfigurationParameter(name = PARAM_OUTPUT_DIR)
    private File outputDir;

    private LookupTable.SimCalculator simCalculator;

    BufferedWriter writer;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        try {
            LookupTable table = SalienceUtils.loadEmbedding(jointEmbeddingFile);
            simCalculator = new LookupTable.SimCalculator(table);
            writer = new BufferedWriter(new FileWriter(new File(outputDir, "data.json")));
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Body body = JCasUtil.selectSingle(aJCas, Body.class);

        // Dummy empty event and entity salience list.
        List<EventMention> bodyEventMentions = JCasUtil.selectCovered(EventMention.class, body);
        int[] eventSaliency = new int[bodyEventMentions.size()];
        Set<String> entitySaliency = new HashSet<>();

        List<FeatureUtils.SimpleInstance> entityInstances = SalienceFeatureExtractor.getKbInstances(aJCas,
                entitySaliency, simCalculator);
        List<FeatureUtils.SimpleInstance> eventInstances = SalienceFeatureExtractor.getEventInstances(body,
                entityInstances, eventSaliency, simCalculator);

        try {
            MultiFormatSalienceDataWriter.writeTagged(aJCas, writer, entityInstances, eventInstances, entitySaliency,
                    eventSaliency);
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        try {
            writer.close();
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }
}
