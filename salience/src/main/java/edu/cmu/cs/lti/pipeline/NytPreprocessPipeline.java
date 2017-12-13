package edu.cmu.cs.lti.pipeline;

import edu.cmu.cs.lti.annotators.AnnotationRemover;
import edu.cmu.cs.lti.annotators.StanfordCoreNlpAnnotator;
import edu.cmu.cs.lti.annotators.TagmeEntityLinkerResultAnnotator;
import edu.cmu.cs.lti.collection_reader.AnnotatedNytReader;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.GzippedXmiCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.AbstractStepBasedDirWriter;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.io.writer.StepBasedDirGzippedXmiWriter;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Date: 7/18/17
 * Time: 7:07 PM
 *
 * @author Zhengzhong Liu
 */
public class NytPreprocessPipeline {

    private static void parse(TypeSystemDescription typeSystemDescription, String workingDir, String ignoreFile) throws
            UIMAException, SAXException, CpeDescriptorException, IOException {
        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, workingDir,
                GzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, "tokenized",
                GzippedXmiCollectionReader.PARAM_BASE_NAME_IGNORES, ignoreFile,
                GzippedXmiCollectionReader.PARAM_EXTENSION, ".xmi.gz"
        );

        AnalysisEngineDescription remover = AnalysisEngineFactory.createEngineDescription(
                AnnotationRemover.class, typeSystemDescription,
                AnnotationRemover.PARAM_TARGET_VIEWS, new String[]{CAS.NAME_DEFAULT_SOFA, AnnotatedNytReader
                        .ABSTRACT_VIEW_NAME},
                AnnotationRemover.PARAM_TARGET_ANNOTATIONS, new Class[]{
                        StanfordCorenlpSentence.class,
                        StanfordCorenlpToken.class
                },
                AnnotationRemover.MULTI_THREAD, true
        );

        AnalysisEngineDescription stanfordAnalyzer = AnalysisEngineFactory.createEngineDescription(
                StanfordCoreNlpAnnotator.class, typeSystemDescription,
                StanfordCoreNlpAnnotator.PARAM_LANGUAGE, "en",
                StanfordCoreNlpAnnotator.PARAM_KEEP_QUIET, true,
                StanfordCoreNlpAnnotator.PARAM_ADDITIONAL_VIEWS, AnnotatedNytReader.ABSTRACT_VIEW_NAME,
                StanfordCoreNlpAnnotator.MULTI_THREAD, true,
                StanfordCoreNlpAnnotator.PARAM_PARSER_MAXLEN, 40
        );

        AnalysisEngineDescription parsedWriter = CustomAnalysisEngineFactory.createGzippedXmiWriter(
                workingDir, "parsed");

        new BasicPipeline(reader, true, true, 7, remover, stanfordAnalyzer, parsedWriter).run();
    }

    private static void forceParse(TypeSystemDescription typeSystemDescription, String workingDir) throws UIMAException,
            SAXException, CpeDescriptorException, IOException {
        CollectionReaderDescription parsedReader = CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, workingDir,
                GzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, "temp",
                GzippedXmiCollectionReader.PARAM_EXTENSION, ".xmi.gz"
        );

        AnalysisEngineDescription remover = AnalysisEngineFactory.createEngineDescription(
                AnnotationRemover.class, typeSystemDescription,
                AnnotationRemover.PARAM_TARGET_VIEWS, new String[]{CAS.NAME_DEFAULT_SOFA, AnnotatedNytReader
                        .ABSTRACT_VIEW_NAME},
                AnnotationRemover.PARAM_TARGET_ANNOTATIONS, new Class[]{
                        StanfordCorenlpSentence.class,
                        StanfordCorenlpToken.class,
                        StanfordEntityMention.class,
                        Entity.class,
                        StanfordTreeAnnotation.class
                },
                AnnotationRemover.MULTI_THREAD, true
        );

        AnalysisEngineDescription parser = AnalysisEngineFactory.createEngineDescription(
                StanfordCoreNlpAnnotator.class, typeSystemDescription,
                StanfordCoreNlpAnnotator.PARAM_LANGUAGE, "en",
                StanfordCoreNlpAnnotator.PARAM_KEEP_QUIET, true,
                StanfordCoreNlpAnnotator.PARAM_ADDITIONAL_VIEWS, AnnotatedNytReader.ABSTRACT_VIEW_NAME,
                StanfordCoreNlpAnnotator.MULTI_THREAD, true,
                StanfordCoreNlpAnnotator.PARAM_PARSER_MAXLEN, 40,
                StanfordCoreNlpAnnotator.PARAM_SKIP_ANNOTATED, true,
                StanfordCoreNlpAnnotator.PARAM_WRITE_NEW_ANNOTATED, true
        );

        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
                StepBasedDirGzippedXmiWriter.class,
                StepBasedDirGzippedXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH, workingDir,
                StepBasedDirGzippedXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME, "temp_out",
                AbstractLoggingAnnotator.MULTI_THREAD, true,
                AbstractStepBasedDirWriter.PARAM_SKIP_INDICATED_DOCUMENTS, true
        );

        new BasicPipeline(parsedReader, true, true, 7, remover, parser, writer).run();
    }

    private static void reparse(TypeSystemDescription typeSystemDescription, String workingDir) throws UIMAException,
            SAXException, CpeDescriptorException, IOException {
        CollectionReaderDescription parsedReader = CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, workingDir,
                GzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, "parsed",
                GzippedXmiCollectionReader.PARAM_EXTENSION, ".xmi.gz"
        );

        AnalysisEngineDescription parser = AnalysisEngineFactory.createEngineDescription(
                StanfordCoreNlpAnnotator.class, typeSystemDescription,
                StanfordCoreNlpAnnotator.PARAM_LANGUAGE, "en",
                StanfordCoreNlpAnnotator.PARAM_KEEP_QUIET, true,
                StanfordCoreNlpAnnotator.PARAM_ADDITIONAL_VIEWS, AnnotatedNytReader.ABSTRACT_VIEW_NAME,
                StanfordCoreNlpAnnotator.MULTI_THREAD, true,
                StanfordCoreNlpAnnotator.PARAM_PARSER_MAXLEN, 40,
                StanfordCoreNlpAnnotator.PARAM_SKIP_ANNOTATED, true,
                StanfordCoreNlpAnnotator.PARAM_WRITE_NEW_ANNOTATED, true
        );

        AnalysisEngineDescription writer = AnalysisEngineFactory.createEngineDescription(
                StepBasedDirGzippedXmiWriter.class,
                StepBasedDirGzippedXmiWriter.PARAM_PARENT_OUTPUT_DIR_PATH, workingDir,
                StepBasedDirGzippedXmiWriter.PARAM_BASE_OUTPUT_DIR_NAME, "reparsed",
                AbstractLoggingAnnotator.MULTI_THREAD, true,
                AbstractStepBasedDirWriter.PARAM_SKIP_INDICATED_DOCUMENTS, true
        );

        new BasicPipeline(parsedReader, true, true, 7, parser, writer).run();
    }

    private static void tag(TypeSystemDescription typeSystemDescription, String workingDir, String input, String output,
                            String entityResultDir) throws UIMAException, SAXException, CpeDescriptorException,
            IOException {
        CollectionReaderDescription parsedReader = CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, workingDir,
                GzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, input,
                GzippedXmiCollectionReader.PARAM_EXTENSION, ".xmi.gz"
        );

        AnalysisEngineDescription linker = AnalysisEngineFactory.createEngineDescription(
                TagmeEntityLinkerResultAnnotator.class, typeSystemDescription,
                TagmeEntityLinkerResultAnnotator.PARAM_ENTITY_RESULT_FOLDER, entityResultDir,
                TagmeEntityLinkerResultAnnotator.PARAM_USE_TOKEN, true,
                TagmeEntityLinkerResultAnnotator.PARAM_ADDITIONAL_VIEW, AnnotatedNytReader.ABSTRACT_VIEW_NAME
        );

        AnalysisEngineDescription linkedWriter = CustomAnalysisEngineFactory.createGzippedXmiWriter(workingDir, output);

        new BasicPipeline(parsedReader, true, true, 7, linker, linkedWriter).run();
    }

    public static void main(String[] argv) throws UIMAException, IOException, CpeDescriptorException, SAXException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");

        String workingDir = argv[0];
        String entityResultDir = argv[1];
        String ignoreFile = null;
        if (argv.length > 2) {
            ignoreFile = argv[2];
        }

//        parse(typeSystemDescription, workingDir, ignoreFile);
        // Some files may failed during first parsing, we rerun to fix them.
//        reparse(typeSystemDescription, workingDir);

//        forceParse(typeSystemDescription, workingDir);

//        tag(typeSystemDescription, workingDir, "parsed", "tagged", entityResultDir);
    }
}
