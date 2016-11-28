package edu.cmu.cs.lti.script.annotators;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.io.writer.AbstractSimpleTextWriterAnalysisEngine;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Create basic tuple files around the verbs.
 *
 * @author Zhengzhong Liu
 */
public class SimpleTupleContextPrinter extends AbstractSimpleTextWriterAnalysisEngine {

    public static final String PARAM_DUPLICATE_FILE = "duplicateFile";

    @ConfigurationParameter(name = PARAM_DUPLICATE_FILE, mandatory = false)
    private File duplicateFileName;

    private Map<Word, Integer> head2EntityId;
    private int entityId;

    private Set<String> duplicates;

    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);

        duplicates = new HashSet<>();
        try {
            if (duplicateFileName != null) {
                duplicates.addAll(FileUtils.readLines(duplicateFileName));
            } else {
                logger.info("Not using duplicate files.");
            }
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

        logger.info("Start processing, number of files processed:");
    }

    @Override
    public String getTextToPrint(JCas aJCas) {
        String docId = JCasUtil.selectSingle(aJCas, Article.class).getArticleName();

        if (duplicates.contains(docId)) {
            // Ignore duplicated files.
            return "";
        }

        incrementCount();

        StringBuilder sb = new StringBuilder();
        sb.append("#").append(docId).append("\n");

        collectClusterIds(aJCas);

        for (StanfordCorenlpSentence sentence : JCasUtil.select(aJCas, StanfordCorenlpSentence.class)) {
            String sentenceStr = sentence.getCoveredText().replaceAll("\n", " ").replaceAll("\t", " ");
            for (StanfordCorenlpToken token : JCasUtil.selectCovered(StanfordCorenlpToken.class, sentence)) {
                if (token.getPos().startsWith("V")) {
                    if (isGoodHead(token)) {
                        sb.append(getTupleContent(token, sentenceStr)).append("\n");

                    }
                }
            }
        }

        sb.append("\n");

        if (getCount() % 10000 == 0) {
            System.out.print(getCount());
            System.out.print(" ");
        }

        return sb.toString();
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
        super.collectionProcessComplete();
        System.out.println();
    }

    private boolean isGoodHead(Word head) {
        String lemma = head.getLemma().toLowerCase();
        if (lemma.equals("be") || lemma.equals("have")) {
            return false;
        }
        return true;
    }

    private String getTupleContent(Word head, String sentence) {
        FSList childDeps = head.getChildDependencyRelations();

        // The fields are:
        // 0. Subject lemma
        // 1. Verb relation
        // 2. Direct Object lemma
        // 3. Indirect Object lemma
        // 4. Full sentence text
        // 5. Subject original
        // 6. Object original
        // 7. Indirect Object original
        // 8. Subject cluster Id
        // 9. Direct Object cluster Id
        // 10. Indirect object cluster Id
        String[] tupleFields = new String[11];

        tupleFields[1] = head.getLemma().toLowerCase();
        tupleFields[4] = sentence;

        if (childDeps != null) {
            for (Dependency dependency : FSCollectionFactory.create(childDeps, Dependency.class)) {
                String depType = dependency.getDependencyType();
                switch (depType) {
                    case "nsubj":
                    case "agent":
                        Word subjWord = dependency.getChild();
                        tupleFields[0] = subjWord.getLemma().toLowerCase();
                        tupleFields[5] = subjWord.getCoveredText();
                        tupleFields[8] = String.valueOf(getClusterId(subjWord));
                        break;
                    case "dobj":
                    case "nsubjpass":
                        Word objWord = dependency.getChild();
                        tupleFields[2] = objWord.getLemma().toLowerCase();
                        tupleFields[6] = objWord.getCoveredText();
                        tupleFields[9] = String.valueOf(getClusterId(objWord));
                        break;
                    case "iobj":
                        Word iobjWord = dependency.getChild();
                        tupleFields[3] = iobjWord.getLemma().toLowerCase();
                        tupleFields[7] = iobjWord.getCoveredText();
                        tupleFields[10] = String.valueOf(getClusterId(iobjWord));
                        break;
                }
            }
        }

        for (int i = 0; i < tupleFields.length; i++) {
            if (tupleFields[i] == null) {
                tupleFields[i] = "-";
            }
        }

        return Joiner.on("\t").join(tupleFields);
    }

    private int getClusterId(Word word) {
        int clusterId;
        if (head2EntityId.containsKey(word)) {
            clusterId = head2EntityId.get(word);
        } else {
            clusterId = entityId++;
            head2EntityId.put(word, clusterId);
        }
        return clusterId;
    }

    private Map<Word, Integer> collectClusterIds(JCas aJCas) {
        head2EntityId = new HashMap<>();
        entityId = 0;

        for (Entity entity : JCasUtil.select(aJCas, Entity.class)) {
            for (int i = 0; i < entity.getEntityMentions().size(); i++) {
                EntityMention mention = entity.getEntityMentions(i);
                Word head = mention.getHead();
                head2EntityId.put(head, entityId++);
            }
        }
        return head2EntityId;
    }

    public static void main(String[] args) throws UIMAException, IOException {
        String paramTypeSystemDescriptor = "TypeSystem";

        String inputDir = args[0];
        String outputDir = args[1];

        String duplicateFile = null;
        if (args.length >= 3) {
            duplicateFile = args[2];
        }

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createRecursiveGzippedXmiReader(typeSystemDescription, inputDir);

        AnalysisEngineDescription tupleExtractor = AnalysisEngineFactory.createEngineDescription(
                SimpleTupleContextPrinter.class, typeSystemDescription,
                SimpleTupleContextPrinter.PARAM_OUTPUT_PATH, outputDir,
                SimpleTupleContextPrinter.PARAM_DUPLICATE_FILE, duplicateFile,
                SimpleTupleContextPrinter.PARAM_NEW_FILE_AFTER_N, 10000
        );

        // Run the pipeline.
        SimplePipeline.runPipeline(reader, tupleExtractor);
    }
}
