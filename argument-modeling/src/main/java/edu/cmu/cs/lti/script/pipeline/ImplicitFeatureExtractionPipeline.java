package edu.cmu.cs.lti.script.pipeline;

import edu.cmu.cs.lti.annotators.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.collection_reader.JsonEventDataReader;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.annotators.ArgumentMerger;
import edu.cmu.cs.lti.script.annotators.SemaforAnnotator;
import edu.cmu.cs.lti.script.annotators.argument.ExistingEventStructureAnnotator;
import edu.cmu.cs.lti.script.annotators.writer.ArgumentClozeTaskWriter;
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
 * Date: 10/22/18
 * Time: 12:06 PM
 *
 * @author Zhengzhong Liu
 */
public class ImplicitFeatureExtractionPipeline {
    public static void main(String[] args) throws UIMAException, SAXException, CpeDescriptorException, IOException {
        String sourceTextDir = args[0];
        String annotateDir = args[1];
        String workingDir = args[2];

        String semaforModelDirectory = "../models/semafor_malt_model_20121129";


        TypeSystemDescription des = TypeSystemDescriptionFactory.createTypeSystemDescription("TypeSystem");

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                PlainTextCollectionReader.class,
                PlainTextCollectionReader.PARAM_INPUTDIR, sourceTextDir,
                PlainTextCollectionReader.PARAM_TEXT_SUFFIX, ".txt");

        AnalysisEngineDescription goldAnnotator = AnalysisEngineFactory.createEngineDescription(
                JsonEventDataReader.class, des,
                JsonEventDataReader.PARAM_JSON_ANNO_DIR, annotateDir
        );

        AnalysisEngineDescription parser = AnalysisEngineFactory.createEngineDescription(
                StanfordCoreNlpAnnotator.class, des,
                StanfordCoreNlpAnnotator.PARAM_LANGUAGE, "en"
        );

        AnalysisEngineDescription semafor = AnalysisEngineFactory.createEngineDescription(
                SemaforAnnotator.class, des,
                SemaforAnnotator.SEMAFOR_MODEL_PATH, semaforModelDirectory);

        AnalysisEngineDescription merger = AnalysisEngineFactory.createEngineDescription(
                ArgumentMerger.class, des);

        AnalysisEngineDescription eventAnnotator = AnalysisEngineFactory.createEngineDescription(
                ExistingEventStructureAnnotator.class, des
        );

        AnalysisEngineDescription featureExtractor = AnalysisEngineFactory.createEngineDescription(
                ArgumentClozeTaskWriter.class, des,
                ArgumentClozeTaskWriter.PARAM_OUTPUT_FILE, new File(workingDir, "cloze.json")
        );

        new BasicPipeline(reader, workingDir, "gold", goldAnnotator, parser, semafor, merger, eventAnnotator,
                featureExtractor).run();
    }
}
