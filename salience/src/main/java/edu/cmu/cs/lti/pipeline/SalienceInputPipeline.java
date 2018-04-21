package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.salience.annotators.NaiveBodyAnnotator;
import edu.cmu.cs.lti.salience.annotators.SalienceInputCreator;
import edu.cmu.cs.lti.script.annotators.FrameBasedEventDetector;
import edu.cmu.cs.lti.uima.io.reader.PlainTextCollectionReader;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.File;
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
        String workingDir = argv[0];
        String inputDir = argv[1];
        String jsonOutput = argv[2];
        String xmiOutput = argv[3];
        String embeddingPath = argv[4];

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                PlainTextCollectionReader.class, typeSystemDescription,
                PlainTextCollectionReader.PARAM_INPUTDIR, inputDir,
                PlainTextCollectionReader.PARAM_DO_NOISE_FILTER, true
        );

        AnalysisEngineDescription detector = AnalysisEngineFactory.createEngineDescription(
                FrameBasedEventDetector.class, typeSystemDescription,
                FrameBasedEventDetector.PARAM_FRAME_RELATION, "../data/resources/fndata-1.7/frRelation.xml"
        );

        AnalysisEngineDescription bodyAnno = AnalysisEngineFactory.createEngineDescription(
                NaiveBodyAnnotator.class, typeSystemDescription
        );

        AnalysisEngineDescription jsonWriter = AnalysisEngineFactory.createEngineDescription(
                SalienceInputCreator.class, typeSystemDescription,
                SalienceInputCreator.PARAM_OUTPUT_DIR, new File(workingDir, jsonOutput),
                SalienceInputCreator.PARAM_JOINT_EMBEDDING, embeddingPath
        );


        new BasicPipeline(reader, true, true, 7, workingDir, xmiOutput, true, detector, bodyAnno, jsonWriter).run();
    }
}
