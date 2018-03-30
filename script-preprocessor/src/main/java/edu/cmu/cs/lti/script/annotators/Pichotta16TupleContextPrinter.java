package edu.cmu.cs.lti.script.annotators;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.script.type.Dependency;
import edu.cmu.cs.lti.script.type.StanfordDependencyRelation;
import edu.cmu.cs.lti.script.type.Word;
import edu.cmu.cs.lti.uima.io.reader.CustomCollectionReaderFactory;
import edu.cmu.cs.lti.uima.util.UimaNlpUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.uimafit.factory.TypeSystemDescriptionFactory;

import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * Date: 12/5/16
 * Time: 7:37 PM
 *
 * @author Zhengzhong Liu
 */
public class Pichotta16TupleContextPrinter extends AbstractTupleContextPrinter {
    //    private Set<String> shareSubjDeps = new HashSet<>(Arrays.asList("xcomp", "advcl", "purpcl"));
    private Set<String> combineVerbRelations = new HashSet<>(Arrays.asList("xcomp"));

    private Set<String> sharedSubjDeps = new HashSet<>(Arrays.asList("purpcl"));

    private Set<Word> usedHeads;

    @Override
    public String getTextToPrint(JCas aJCas) {
        usedHeads = new HashSet<>();
        return super.getTextToPrint(aJCas);
    }

    @Override
    protected boolean useThisHead(Word head) {
        if (usedHeads.contains(head)) {
            return false;
        }

        String lemma = head.getLemma().toLowerCase();
        if (lemma.equals("be")) {
            return false;
        }
        return true;
    }

    @Override
    protected String getTupleContent(Word head, String sentence) {
        String[] tupleFields = new String[9];

        List<Word> complements = new ArrayList<>();
        String eventHead = UimaNlpUtils.getPredicate(head, complements);

        tupleFields[1] = eventHead;
        tupleFields[7] = sentence;

        List<String> prepNouns = new ArrayList<>();

        for (Word extendedHead : complements) {
            usedHeads.add(extendedHead);
            getSyntacticArguments(extendedHead, tupleFields, prepNouns);
        }

        for (int i = 0; i < tupleFields.length; i++) {
            String v = tupleFields[i];
            if (v == null) {
                tupleFields[i] = "-";
            } else {
                tupleFields[i] = v.replaceAll("\t", " ").replaceAll("\n", " ");
            }
        }

        return Joiner.on("\t").join(tupleFields);
    }

    private void getSyntacticArguments(Word head, String[] tupleFields, List<String> prepNouns) {
        FSList childDeps = head.getChildDependencyRelations();

        if (childDeps != null) {
            for (Dependency dependency : FSCollectionFactory.create(childDeps, Dependency.class)) {
                String depType = dependency.getDependencyType();
                switch (depType) {
                    case "nsubj":
                    case "agent":
                        Word subjWord = dependency.getChild();
                        tupleFields[0] = subjWord.getLemma().toLowerCase();
                        tupleFields[4] = String.valueOf(getClusterId(subjWord));
                        break;
                    case "dobj":
                    case "nsubjpass":
                        Word objWord = dependency.getChild();
                        tupleFields[2] = objWord.getLemma().toLowerCase();
                        tupleFields[5] = String.valueOf(getClusterId(objWord));
                        break;
                    case "iobj":
                        Word iobjWord = dependency.getChild();
                        tupleFields[3] = iobjWord.getLemma().toLowerCase();
                        tupleFields[6] = String.valueOf(getClusterId(iobjWord));
                        break;
                }

                if (depType.startsWith("prep_")) {
                    Word prepWord = dependency.getChild();
                    if (prepWord.getPos().startsWith("N")) {
                        String prep = depType.substring(5);
                        prepNouns.add(prep);
                        prepNouns.add(prepWord.getLemma().toLowerCase());
                        prepNouns.add(String.valueOf(getClusterId(prepWord)));
                    }
                }

                if (prepNouns.isEmpty()) {
                    tupleFields[8] = "-";
                } else {
                    tupleFields[8] = Joiner.on("\t").join(prepNouns);
                }
            }
        }

        if (tupleFields[0] == null) {
            Word sharedSubjFromParent = findSubjectFromParent(head);
            if (sharedSubjFromParent != null) {
                tupleFields[0] = sharedSubjFromParent.getLemma().toLowerCase();
                tupleFields[4] = String.valueOf(getClusterId(sharedSubjFromParent));
            }
        }
    }

    private Word findSubjectFromParent(Word target) {
        FSList parents = target.getHeadDependencyRelations();
        if (parents != null) {
            for (StanfordDependencyRelation dep : FSCollectionFactory.create(parents,
                    StanfordDependencyRelation.class)) {
                if (sharedSubjDeps.contains(dep.getDependencyType())) {
                    Word sharedSubjParent = dep.getHead();
                    FSList siblings = sharedSubjParent.getChildDependencyRelations();
                    if (siblings != null) {
                        for (StanfordDependencyRelation siblingDep : FSCollectionFactory.create(siblings,
                                StanfordDependencyRelation.class)) {
                            String siblingDepType = siblingDep.getDependencyType();
                            if (siblingDepType.equals("nsubj") || siblingDepType.equals("agent")) {
                                Word sibling = siblingDep.getChild();
                                if (sibling != target) {
                                    return sibling;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
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
                Pichotta16TupleContextPrinter.class, typeSystemDescription,
                Pichotta16TupleContextPrinter.PARAM_OUTPUT_PATH, outputDir,
                Pichotta16TupleContextPrinter.PARAM_DUPLICATE_FILE, duplicateFile,
                Pichotta16TupleContextPrinter.PARAM_NEW_FILE_AFTER_N, 10000
        );

        // Run the pipeline.
        SimplePipeline.runPipeline(reader, tupleExtractor);
    }
}
