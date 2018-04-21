package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.salience.annotators.NaiveBodyAnnotator;
import edu.cmu.cs.lti.salience.annotators.SalienceInputCreator;
import edu.cmu.cs.lti.utils.Configuration;
import edu.cmu.cs.lti.utils.DispatchReader;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/20/18
 * Time: 6:07 PM
 *
 * @author Zhengzhong Liu
 */
public class SalienceInputPipeline {
    public static void main(String[] argv) throws UIMAException, SAXException, CpeDescriptorException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");
        String inputPath = argv[0];
        String jsonOutput = argv[1];
        String inputType = argv[2];
        String embeddingPath = argv[3];

        Configuration config = new Configuration();
        config.add("language", "en");

        CollectionReaderDescription reader = DispatchReader.getReader(typeSystemDescription, inputPath, inputType,
                config);

        AnalysisEngineDescription bodyAnno = AnalysisEngineFactory.createEngineDescription(
                NaiveBodyAnnotator.class, typeSystemDescription
        );

        AnalysisEngineDescription jsonWriter = AnalysisEngineFactory.createEngineDescription(
                SalienceInputCreator.class, typeSystemDescription,
                SalienceInputCreator.PARAM_OUTPUT_DIR, jsonOutput,
                SalienceInputCreator.PARAM_JOINT_EMBEDDING, embeddingPath
        );

        new BasicPipeline(reader, true, true, 7, bodyAnno, jsonWriter).run();
    }
}
