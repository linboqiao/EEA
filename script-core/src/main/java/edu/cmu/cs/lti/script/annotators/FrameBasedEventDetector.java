package edu.cmu.cs.lti.script.annotators;

import edu.cmu.cs.lti.frame.FrameRelationReader;
import edu.cmu.cs.lti.frame.FrameStructure;
import edu.cmu.cs.lti.frame.UimaFrameExtractor;
import edu.cmu.cs.lti.pipeline.BasicPipeline;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.IOUtils;
import edu.cmu.cs.lti.uima.io.reader.GzippedXmiCollectionReader;
import edu.cmu.cs.lti.uima.io.writer.StepBasedDirGzippedXmiWriter;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import edu.cmu.cs.lti.utils.DebugUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescriptorException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 10/25/17
 * Time: 3:58 PM
 *
 * @author Zhengzhong Liu
 */
public class FrameBasedEventDetector extends AbstractLoggingAnnotator {
    public static final String PARAM_FRAME_RELATION = "frameRelationFile";
    @ConfigurationParameter(name = PARAM_FRAME_RELATION)
    private File frameRelationFile;

    private Set<String> ignoredHeadWords;
    private UimaFrameExtractor extractor;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        String[] ignoredVerbs = new String[]{"appear", "be", "become", "do", "have", "seem", "do", "get", "give",
                "go", "have", "keep", "make", "put", "set", "take", "argue", "claim", "say", "suggest", "tell"};
        ignoredHeadWords = new HashSet<>();
        Collections.addAll(ignoredHeadWords, ignoredVerbs);

        try {
            FrameRelationReader frameReader = new FrameRelationReader(frameRelationFile.getPath());
            logger.info(String.valueOf(getClass().getResource("/event_evoking_frames.txt").getPath()));

            HashSet<String> targetFrames = new HashSet<>();
            InputStream frameInputStream = getClass().getResourceAsStream("/event_evoking_frames.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(frameInputStream));
            String line;
            while ((line = br.readLine()) != null) {
                targetFrames.add(line.trim());
            }

            logger.info(String.format("Loaded %d event target frames.", targetFrames.size()));

            extractor = new UimaFrameExtractor(frameReader.getFeByName(), targetFrames);
        } catch (IOException | JDOMException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        annotateEvents(aJCas);
    }

    private void annotateEvents(JCas aJCas) {
        ArticleComponent article = JCasUtil.selectSingle(aJCas, Article.class);

        UimaConvenience.printProcessLog(aJCas);

        for (FrameStructure frameStructure : extractor.getTargetFrames(article)) {
            SemaforLabel predicate = frameStructure.getTarget();
            String frameName = frameStructure.getFrameName();

            StanfordCorenlpToken headword = UimaNlpUtils.findHeadFromStanfordAnnotation(predicate);

            logger.info(headword.getCoveredText());

            if (headword == null) {
                continue;
            } else if (ignoredHeadWords.contains(headword.getLemma().toLowerCase())) {
                logger.info("Filtered because light.");
                continue;
            }

            EventMention eventMention = new EventMention(aJCas, predicate.getBegin(), predicate.getEnd());

            eventMention.setEventType(frameName);
            eventMention.setFrameName(frameName);

            List<EventMentionArgumentLink> argumentLinks = new ArrayList<>();

            List<String> superFeNames = frameStructure.getSuperFeNames();

            int i = 0;
            for (SemaforLabel frameElement : frameStructure.getFrameElements()) {
                String feName = frameElement.getName();
                EventMentionArgumentLink argumentLink = new EventMentionArgumentLink(aJCas);
                EntityMention argumentMention = UimaNlpUtils.createEntityMention(aJCas,
                        frameElement.getBegin(), frameElement.getEnd(), COMPONENT_ID);
                argumentLink.setArgumentRole(feName);
                argumentLink.setArgument(argumentMention);
                argumentLink.setEventMention(eventMention);
                argumentLink.setFrameElementName(feName);
                UimaAnnotationUtils.finishTop(argumentLink, COMPONENT_ID, 0, aJCas);
                argumentLinks.add(argumentLink);

                String superFeName = superFeNames.get(i);
                argumentLink.setSuperFrameElementRoleName(superFeName);
                i++;
            }

            eventMention.setArguments(FSCollectionFactory.createFSList(aJCas, argumentLinks));
            UimaAnnotationUtils.finishAnnotation(eventMention, COMPONENT_ID, 0, aJCas);
        }

        DebugUtils.pause();
    }

    public static void main(String[] argv) throws UIMAException, SAXException, CpeDescriptorException, IOException {
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription("TypeSystem");
        String workingDir = argv[0];
        String inputDir = argv[1];
        String outputDir = argv[2];

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                GzippedXmiCollectionReader.class, typeSystemDescription,
                GzippedXmiCollectionReader.PARAM_PARENT_INPUT_DIR_PATH, workingDir,
                GzippedXmiCollectionReader.PARAM_BASE_INPUT_DIR_NAME, inputDir,
                GzippedXmiCollectionReader.PARAM_EXTENSION, ".xmi.gz",
                GzippedXmiCollectionReader.PARAM_RECURSIVE, true
        );

        AnalysisEngineDescription detector = AnalysisEngineFactory.createEngineDescription(
                FrameBasedEventDetector.class, typeSystemDescription,
                FrameBasedEventDetector.PARAM_FRAME_RELATION, "../data/resources/fndata-1.7/frRelation.xml"
        );

        StepBasedDirGzippedXmiWriter.dirSegFunction = IOUtils::indexBasedSegFunc;

        new BasicPipeline(reader, true, true, 7, workingDir, outputDir, true, detector).run();
    }
}
