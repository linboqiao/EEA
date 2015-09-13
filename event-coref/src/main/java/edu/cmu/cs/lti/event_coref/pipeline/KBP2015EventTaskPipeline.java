package edu.cmu.cs.lti.event_coref.pipeline;

import edu.cmu.cs.lti.annotator.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.annotators.FanseAnnotator;
import edu.cmu.cs.lti.annotators.OpenNlpChunker;
import edu.cmu.cs.lti.collection_reader.TbfEventDataReader;
import edu.cmu.cs.lti.emd.annotators.TbfStyleEventWriter;
import edu.cmu.cs.lti.emd.annotators.acceptors.AllCandidateAcceptor;
import edu.cmu.cs.lti.emd.annotators.classification.RealisClassifierTrainer;
import edu.cmu.cs.lti.emd.annotators.crf.CrfMentionTypeAnnotator;
import edu.cmu.cs.lti.emd.pipeline.CrfMentionTrainingLooper;
import edu.cmu.cs.lti.event_coref.annotators.GoldStandardEventMentionAnnotator;
import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.pipeline.AbstractProcessorBuilder;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.annotators.SemaforAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.utils.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * A pipeline structure for KBP 2015 event task of both Mention Detection and Coref.
 * This pipeline should be easily adapted to a general pipeline by replacing the preprocessors.
 * <p>
 * Date: 8/16/15
 * Time: 4:21 PM
 *
 * @author Zhengzhong Liu
 */
public class KBP2015EventTaskPipeline {
    final TypeSystemDescription typeSystemDescription;

    // Input data.
    final String goldStandardFilePath;
    final String plainTextDataDir;
    final String tokenMapDir;

    // Output directory.
    final String workingDir;

    // Models.
    final String modelDir;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public KBP2015EventTaskPipeline(String typeSystemName, String goldStandardFilePath, String plainTextDataDir,
                                    String tokenMapDir, String modelDir, String workingDir) {
        this.typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescription(typeSystemName);
        this.goldStandardFilePath = goldStandardFilePath;
        this.plainTextDataDir = plainTextDataDir;
        this.tokenMapDir = tokenMapDir;
        this.modelDir = modelDir;
        this.workingDir = workingDir;

        logger.info(String.format("Reading gold tbf from %s , token from %s, source from %s", goldStandardFilePath,
                tokenMapDir, plainTextDataDir));
        logger.info(String.format("Main output can be found at %s.", workingDir));
    }

    public void prepare(String preprocessOutputBase) throws UIMAException, IOException {
        final String semaforModelDirectory = modelDir + "/semafor_malt_model_20121129";
        final String fanseModelDirectory = modelDir + "/fanse_models";
        final String opennlpDirectory = modelDir + "/opennlp/en-chunker.bin";

        BasicPipeline pipeline = new BasicPipeline(new AbstractProcessorBuilder() {
            @Override
            public CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException {
                return CollectionReaderFactory.createReaderDescription(
                        TbfEventDataReader.class, typeSystemDescription,
                        TbfEventDataReader.PARAM_GOLD_STANDARD_FILE, goldStandardFilePath,
                        TbfEventDataReader.PARAM_SOURCE_EXT, ".txt",
                        TbfEventDataReader.PARAM_SOURCE_TEXT_DIRECTORY, plainTextDataDir,
                        TbfEventDataReader.PARAM_TOKEN_DIRECTORY, tokenMapDir,
                        TbfEventDataReader.PARAM_TOKEN_EXT, ".tab",
                        TbfEventDataReader.PARAM_INPUT_VIEW_NAME, UimaConst.inputViewName
                );
            }

            @Override
            public AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription stanfordAnalyzer = AnalysisEngineFactory.createEngineDescription(
                        StanfordCoreNlpAnnotator.class, typeSystemDescription,
                        StanfordCoreNlpAnnotator.PARAM_USE_SUTIME, true);

                AnalysisEngineDescription semaforAnalyzer = AnalysisEngineFactory.createEngineDescription(
                        SemaforAnnotator.class, typeSystemDescription,
                        SemaforAnnotator.SEMAFOR_MODEL_PATH, semaforModelDirectory);

                AnalysisEngineDescription fanseParser = AnalysisEngineFactory.createEngineDescription(
                        FanseAnnotator.class, typeSystemDescription, FanseAnnotator.PARAM_MODEL_BASE_DIR,
                        fanseModelDirectory);

                AnalysisEngineDescription opennlp = AnalysisEngineFactory.createEngineDescription(
                        OpenNlpChunker.class, typeSystemDescription,
                        OpenNlpChunker.PARAM_MODEL_PATH, opennlpDirectory);

                AnalysisEngineDescription xmiWriter = CustomAnalysisEngineFactory.createXmiWriter(workingDir,
                        preprocessOutputBase);

                return new AnalysisEngineDescription[]{
                        stanfordAnalyzer, semaforAnalyzer, fanseParser, opennlp, xmiWriter
                };
            }
        }, typeSystemDescription);

        pipeline.run();
    }

    public String trainMentionTypeLv1(Configuration kbpConfig, CollectionReaderDescription trainingReader,
                                      String suffix, boolean skipTrain) throws UIMAException, IOException,
            ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        logger.info("Starting Training ...");

        String cvModelDir = kbpConfig.get("edu.cmu.cs.lti.model.crf.mention.lv1.dir") + suffix;
        if (!skipTrain) {
            File classFile = kbpConfig.getFile("edu.cmu.cs.lti.mention.classes.path");
            File cacheDir = new File(kbpConfig.get("edu.cmu.cs.lti.mention.cache.dir") + suffix);
            String[] classes = FileUtils.readLines(classFile).stream().map(l -> l.split("\t"))
                    .filter(p -> p.length >= 1).map(p -> p[0]).toArray(String[]::new);

            for (String c : classes) {
                logger.info("Register class " + c);
            }

            logger.info("Saving model directory at " + cvModelDir);

            CrfMentionTrainingLooper mentionTypeTrainer = new CrfMentionTrainingLooper(classes, kbpConfig, cvModelDir,
                    cacheDir, typeSystemDescription, trainingReader);
            mentionTypeTrainer.runLoopPipeline();
        } else {
            logger.info("Skipping training");
        }

        return cvModelDir;
    }

    public void trainRealisTypes(Configuration kbpConfig, CollectionReaderDescription trainingReader, String
            suffix, boolean skipTrain) throws Exception {
        RealisClassifierTrainer trainer = new RealisClassifierTrainer(typeSystemDescription, trainingReader, kbpConfig);
        String realisCvModelDir = kbpConfig.get("edu.cmu.cs.lti.model.realis.dir") + suffix;
        trainer.buildModels(realisCvModelDir);
    }

    public void mentionDetection(CollectionReaderDescription reader, String modelDir, String tbfOutput,
                                 String goldTbfOutput, Configuration config) throws UIMAException, IOException {
        // Static variable is not so nice here.
        CrfMentionTypeAnnotator.kbpConfig = config;

        AnalysisEngineDescription everythingAcceptor = AnalysisEngineFactory.createEngineDescription(
                AllCandidateAcceptor.class, typeSystemDescription
        );

        BasicPipeline systemPipeline = new BasicPipeline(new AbstractProcessorBuilder() {
            @Override
            public CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription crfLevel1Annotator = AnalysisEngineFactory.createEngineDescription(
                        CrfMentionTypeAnnotator.class, typeSystemDescription,
                        CrfMentionTypeAnnotator.PARAM_MODEL_DIRECTORY, modelDir
                );

                AnalysisEngineDescription resultWriter = AnalysisEngineFactory.createEngineDescription(
                        TbfStyleEventWriter.class, typeSystemDescription,
                        TbfStyleEventWriter.PARAM_OUTPUT_PATH, tbfOutput,
                        TbfStyleEventWriter.PARAM_SYSTEM_ID, "crf-lv1"
                );

                return new AnalysisEngineDescription[]{crfLevel1Annotator, everythingAcceptor, resultWriter};
            }
        }, typeSystemDescription);


        BasicPipeline goldPipeline = new BasicPipeline(new AbstractProcessorBuilder() {
            @Override
            public CollectionReaderDescription buildCollectionReader() throws ResourceInitializationException {
                return reader;
            }

            @Override
            public AnalysisEngineDescription[] buildProcessors() throws ResourceInitializationException {
                AnalysisEngineDescription goldCopier = AnalysisEngineFactory.createEngineDescription(
                        GoldStandardEventMentionAnnotator.class, typeSystemDescription,
                        GoldStandardEventMentionAnnotator.PARAM_TARGET_VIEWS, new String[]{CAS.NAME_DEFAULT_SOFA}
                );

                AnalysisEngineDescription resultWriter = AnalysisEngineFactory.createEngineDescription(
                        TbfStyleEventWriter.class, typeSystemDescription,
                        TbfStyleEventWriter.PARAM_OUTPUT_PATH, goldTbfOutput,
                        TbfStyleEventWriter.PARAM_SYSTEM_ID, "gold"
                );

                return new AnalysisEngineDescription[]{goldCopier, resultWriter};
            }
        }, typeSystemDescription);

        systemPipeline.run();
        goldPipeline.run();
    }

    // TODO calling coreference only.
    public void coreference() {

    }

    // TODO joint inference of mention and detection.
    public void joinMentionDetectionAndCoreference() {

    }

    public void crossValidation(Configuration kbpConfig, String inputBaseDir) throws UIMAException, IOException,
            ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException,
            IllegalAccessException {
        int numSplit = kbpConfig.getInt("edu.cmu.cs.lti.cv.split", 5);
        int seed = kbpConfig.getInt("edu.cmu.cs.lti.cv.seed", 17);
        boolean skipTrain = kbpConfig.getBoolean("edu.cmu.cs.lti.cv.skiptrain", true);
        String evalPath = kbpConfig.get("edu.cmu.cs.lti.eval.base");

        File typeLv1Eval = new File(new File(workingDir, evalPath), "lv1_types");
        edu.cmu.cs.lti.utils.FileUtils.ensureDirectory(typeLv1Eval);

        for (int slice = 0; slice < numSplit; slice++) {
            String sliceSuffix = "split_" + slice;
            CollectionReaderDescription trainingReader = CustomCollectionReaderFactory.createCrossValidationReader(
                    typeSystemDescription, workingDir, inputBaseDir, false, seed, slice);
            String crfTypeModelDir = trainMentionTypeLv1(kbpConfig, trainingReader, sliceSuffix, skipTrain);

            CollectionReaderDescription evalReader = CustomCollectionReaderFactory.createCrossValidationReader(
                    typeSystemDescription, workingDir, inputBaseDir, true, seed, slice);
            String predictedTbf = new File(typeLv1Eval, "predicted" + sliceSuffix + ".tbf").getAbsolutePath();
            String goldTbf = new File(typeLv1Eval, "gold" + sliceSuffix + ".tbf").getAbsolutePath();
            logger.info("Finding models in " + crfTypeModelDir);
            mentionDetection(evalReader, crfTypeModelDir, predictedTbf, goldTbf, kbpConfig);
        }
    }

    public static void main(String argv[]) throws UIMAException, IOException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Configuration kbpConfig = new Configuration("settings/kbp.properties");
        Configuration commonConfig = new Configuration("settings/common.properties");

        String modelPath = commonConfig.get("edu.cmu.cs.lti.model.dir");
        String typeSystemName = commonConfig.get("edu.cmu.cs.lti.event.typesystem");

        String workingDir = kbpConfig.get("edu.cmu.cs.lti.working.dir");
        String goldTbf = kbpConfig.get("edu.cmu.cs.lti.gold.tbf");
        String sourceDir = kbpConfig.get("edu.cmu.cs.lti.source_text.dir");
        String tokenDir = kbpConfig.get("edu.cmu.cs.lti.token_map.dir");
        String preprocessBase = kbpConfig.get("edu.cmu.cs.lti.output.preprocess.dir");

        KBP2015EventTaskPipeline pipeline = new KBP2015EventTaskPipeline(typeSystemName, goldTbf, sourceDir,
                tokenDir, modelPath, workingDir);

//        pipeline.prepare(preprocessBase);
//        pipeline.trainMentionTypeLv1(kbpConfig, preprocessBase);
        pipeline.crossValidation(kbpConfig, preprocessBase);
    }
}
