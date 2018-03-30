package edu.cmu.cs.lti.script.annotators.writer;

import edu.cmu.cs.lti.script.type.*;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.UimaConvenience;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Given dependency parses and coreference chains, create argument
 * cloze tasks.
 * Date: 3/24/18
 * Time: 4:32 PM
 *
 * @author Zhengzhong Liu
 */
public class ArgumentClozeTaskWriter extends AbstractLoggingAnnotator {
    public static final String PARAM_OUTPUT_FILE = "outputFile";
    @ConfigurationParameter(name = PARAM_OUTPUT_FILE)
    private File outputFile;
    private BufferedWriter writer;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        try {
            writer = new BufferedWriter(new FileWriter(outputFile));
        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        Map<Word, Integer> headToCoref = loadCoref(aJCas);

        Set<Word> usedHeads = new HashSet<>();

        try {
            writer.write("#" + UimaConvenience.getArticleName(aJCas) + "\n");
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }

        for (StanfordCorenlpToken token : JCasUtil.select(aJCas, StanfordCorenlpToken.class)) {
            if (!token.getPos().startsWith("V")) {
                continue;
            }

            if (usedHeads.contains(token)) {
                continue;
            }

            List<Word> complements = new ArrayList<>();
            String predicate_text = UimaNlpUtils.getPredicate(token, complements);
            usedHeads.addAll(complements);

            Map<String, String> args = getArgs(token, headToCoref);

            for (Word complement : complements) {
                Map<String, String> complement_args = getArgs(complement, headToCoref);

                for (Map.Entry<String, String> arg : complement_args.entrySet()) {
                    String role = arg.getKey();
                    String text = arg.getValue();
                    if (!args.containsKey(role)) {
                        args.put(role, text);
                    }
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append(predicate_text);

            for (Map.Entry<String, String> arg : args.entrySet()) {
                sb.append("\t");
                sb.append(arg.getKey()).append(":").append(arg.getValue());
            }

            sb.append("\n");

            try {
                writer.write(sb.toString());
            } catch (IOException e) {
                throw new AnalysisEngineProcessException(e);
            }
        }

        try {
            writer.write("\n");
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

    private Map<String, String> getArgs(Word predicate, Map<Word, Integer> headToCoref) {
        Map<String, String> args = new HashMap<>();

        if (predicate.getChildDependencyRelations() != null) {
            for (StanfordDependencyRelation dep : FSCollectionFactory.create(predicate
                    .getChildDependencyRelations(), StanfordDependencyRelation.class)) {
                Pair<String, Word> child = takeDep(dep);

                if (child == null) {
                    continue;
                }

                Word word = child.getRight();
                String role = child.getLeft();

                int entityId = -1;
                if (headToCoref.containsKey(word)) {
                    entityId = headToCoref.get(word);
                }

                args.put(role, entityId + "," + word.getLemma());
            }
        }

        return args;
    }

    private Pair<String, Word> takeDep(StanfordDependencyRelation dep) {
        String depType = dep.getDependencyType();
        Word depWord = dep.getChild();
        if (depType.equals("nsubj") || depType.equals("agent")) {
            return Pair.of("subj", depWord);
        } else if (depType.equals("dobj") || depType.equals("nsubjpass")) {
            return Pair.of("dobj", depWord);
        } else if (depType.equals("iobj")) {
            return Pair.of("iobj", depWord);
        } else if (depType.startsWith("prep_")) {
            return Pair.of(depType, depWord);
        }

        return null;
    }

    private Map<Word, Integer> loadCoref(JCas aJCas) {
        Map<Word, Integer> headToCoref = new HashMap<>();

        int corefId = 0;
        for (Entity entity : JCasUtil.select(aJCas, Entity.class)) {
            Collection<EntityMention> mentions = FSCollectionFactory.create(entity
                    .getEntityMentions(), EntityMention.class);

            if (mentions.size() > 1) {
                for (EntityMention mention : mentions) {
                    StanfordCorenlpToken head = UimaNlpUtils.findHeadFromStanfordAnnotation(mention);
                    headToCoref.put(head, corefId);
                }
                corefId += 1;
            }
        }
        return headToCoref;
    }

    public static void main(String[] args) throws UIMAException, IOException {
        String paramTypeSystemDescriptor = "TaskEventMentionDetectionTypeSystem";

        String inputDir = args[0];
        String outputFile = args[1];

        // Instantiate the analysis engine.
        TypeSystemDescription typeSystemDescription = TypeSystemDescriptionFactory
                .createTypeSystemDescription(paramTypeSystemDescriptor);

        CollectionReaderDescription reader =
                CustomCollectionReaderFactory.createXmiReader(typeSystemDescription, inputDir);

        AnalysisEngineDescription eventExtractor = AnalysisEngineFactory.createEngineDescription(
                ArgumentClozeTaskWriter.class, typeSystemDescription,
                ArgumentClozeTaskWriter.PARAM_OUTPUT_FILE, outputFile
        );

        // Run the pipeline.
        SimplePipeline.runPipeline(reader, eventExtractor);
    }
}
