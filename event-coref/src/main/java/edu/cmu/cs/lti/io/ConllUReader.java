package edu.cmu.cs.lti.io;

import edu.cmu.cs.lti.model.UimaConst;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.uima.annotator.AbstractCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.CustomAnalysisEngineFactory;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.Progress;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/15/18
 * Time: 3:00 PM
 *
 * @author Zhengzhong Liu
 */
public class ConllUReader extends AbstractCollectionReader {
    public static final String PARAM_USE_LEMMA_TEXT = "useLemmaText";

    @ConfigurationParameter(name = PARAM_USE_LEMMA_TEXT)
    private boolean useLemmaText;

    private List<Iterator<String>> lineIters;

    private int currentFile = 0;

    private Iterator<String> currentIter;

    private String docid = "";

    private boolean firstDoc = true;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        lineIters = new ArrayList<>();
        for (File file : files) {
            try {
                Stream<String> lines = Files.lines(file.toPath());
                lineIters.add(lines.iterator());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        currentIter = lineIters.get(currentFile);
    }

    @Override
    public void getNext(JCas jCas) throws IOException, CollectionException {
        StringBuilder sb = new StringBuilder();

        boolean firstLine = true;

        int lastEnd = 0;

        String nextDocId = "";
        int wordId = 0;

        while (currentIter.hasNext()) {
            String line = currentIter.next().trim();

            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("#")) {
                if (line.startsWith("# newdoc")) {
                    if (line.contains("id = ")) {
                        nextDocId = line.split("id = ")[1];
                    }
                    if (firstDoc) {
                        docid = nextDocId;
                    } else {
                        break;
                    }
                } else if (line.startsWith("# sent_id")) {
                    String sentId = line.split("sent_id = ")[1];
                    if (!firstLine) {
                        sb.append("\n");
                        lastEnd += 1;
                    }
                    firstLine = false;
                }
                continue;
            }

            String[] fields = line.split("\t");
            String text = fields[1];
            String lemma = fields[2];
            String pos = fields[3];

            int begin, end;

            if (useLemmaText) {
                begin = lastEnd + 1;
                end = begin + lemma.length();

                if (begin > sb.length()) {
                    sb.append(StringUtils.repeat(' ', begin - sb.length()));
                }

                sb.append(lemma).append(' ');
            } else {
                String[] offset_str = fields[fields.length - 1].split(",");
                begin = Integer.parseInt(offset_str[0]);
                end = Integer.parseInt(offset_str[1]);

                if (begin > lastEnd) {
                    sb.append(StringUtils.repeat(' ', begin - lastEnd));
                }

                sb.append(text);
            }

            Word word = new Word(jCas, begin, end);
            word.setPos(pos);
            UimaAnnotationUtils.finishAnnotation(word, UimaConst.goldComponentName, wordId, jCas);
            word.setIndex(wordId);
            lastEnd = end;

            wordId++;
        }

        String docText = sb.toString();
        jCas.setDocumentText(docText);

        UimaConvenience.setDocInfo(jCas, language, COMPONENT_ID, docText.length(), docid, dataPath, false);

        docid = nextDocId;
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
        while (currentFile < lineIters.size()) {
            if (currentIter.hasNext()) {
                logger.info("Has next in iter");
                return true;
            } else {
                currentFile += 1;
                if (currentFile < lineIters.size()) {
                    logger.info("Trying to use next file iter");
                    currentIter = lineIters.get(currentFile);
                }
            }
        }


        logger.info("Nothing remains");

        return false;
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[0];
    }

    public static void main(String[] args) throws UIMAException, IOException {
        String inputPath = args[0];
        String outputPath = args[1];
        String language = args[2];

        boolean useLemma = false;
        if (args.length > 3 && args[3].equals("useLemma")) {
            useLemma = true;
        }

        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TaskEventMentionDetectionTypeSystem");

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                ConllUReader.class, typeSystemDescription,
                ConllUReader.PARAM_USE_LEMMA_TEXT, useLemma,
                ConllUReader.PARAM_LANGUAGE, language,
                ConllUReader.PARAM_DATA_PATH, inputPath,
                ConllUReader.PARAM_EXTENSION, "conllu"
        );

        AnalysisEngineDescription writer = CustomAnalysisEngineFactory.createXmiWriter(outputPath, "xmi");

        SimplePipeline.runPipeline(reader, writer);
    }
}
