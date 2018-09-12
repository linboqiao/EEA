package edu.cmu.cs.lti.annotators;

import edu.cmu.cs.lti.ling.WordNetSearcher;
import edu.cmu.cs.lti.script.type.StanfordCorenlpToken;
import edu.cmu.cs.lti.script.type.WordNetBasedEntity;
import edu.cmu.cs.lti.uima.annotator.AbstractLoggingAnnotator;
import edu.cmu.cs.lti.uima.util.UimaAnnotationUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.javatuples.Pair;

import java.io.IOException;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/13/15
 * Time: 11:36 PM
 *
 * @author Zhengzhong Liu
 */
public class WordNetBasedEntityAnnotator extends AbstractLoggingAnnotator {

    public static final String PARAM_WN_PATH = "WordNetPath";

    @ConfigurationParameter(name = PARAM_WN_PATH)
    private String wnDictPath;

    private WordNetSearcher wns;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
        try {
            wns = new WordNetSearcher(wnDictPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        JCasUtil.select(aJCas, StanfordCorenlpToken.class).stream().forEach(token -> {
            // Nouns.
            if (token.getPos().startsWith("N")) {
                if (isOfNounType(token, "worker", "leader")) {
                    WordNetBasedEntity jobTitle = new WordNetBasedEntity(aJCas);
                    UimaAnnotationUtils.finishAnnotation(jobTitle, token.getBegin(), token.getEnd(), COMPONENT_ID, 0,
                            aJCas);
                    jobTitle.setSense("JobTitle");
                } else if (isOfNounType(token, "body_part")) {
                    WordNetBasedEntity bodyPart = new WordNetBasedEntity(aJCas);
                    UimaAnnotationUtils.finishAnnotation(bodyPart, token.getBegin(), token.getEnd(), COMPONENT_ID, 0,
                            aJCas);
                    bodyPart.setSense("BodyPart");
                } else if (isOfNounType(token, "monetary_system")) {
                    WordNetBasedEntity monetary = new WordNetBasedEntity(aJCas);
                    UimaAnnotationUtils.finishAnnotation(monetary, token.getBegin(), token.getEnd(), COMPONENT_ID, 0,
                            aJCas);
                    monetary.setSense("Monetary");
                } else if (isOfNounType(token, "possession")) {
                    WordNetBasedEntity possession = new WordNetBasedEntity(aJCas);
                    UimaAnnotationUtils.finishAnnotation(possession, token.getBegin(), token.getEnd(), COMPONENT_ID, 0,
                            aJCas);
                    possession.setSense("Possession");
                } else if (isOfNounType(token, "government")) {
                    WordNetBasedEntity government = new WordNetBasedEntity(aJCas);
                    UimaAnnotationUtils.finishAnnotation(government, token.getBegin(), token.getEnd(), COMPONENT_ID, 0,
                            aJCas);
                    government.setSense("Government");
                } else if (isOfNounType(token, "crime")) {
                    WordNetBasedEntity crime = new WordNetBasedEntity(aJCas);
                    UimaAnnotationUtils.finishAnnotation(crime, token.getBegin(), token.getEnd(), COMPONENT_ID, 0,
                            aJCas);
                    crime.setSense("Crime");
                } else if (isOfNounType(token, "pathological_state")) {
                    WordNetBasedEntity pathology = new WordNetBasedEntity(aJCas);
                    UimaAnnotationUtils.finishAnnotation(pathology, token.getBegin(), token.getEnd(), COMPONENT_ID,
                            0, aJCas);
                    pathology.setSense("Pathology");
                }
            }

            // In cases where the original token is not a noun.
            for (Pair<String, String> der : wns.getDerivations(token.getLemma().toLowerCase(), token.getPos())) {
                if (der.getValue1().equals("noun") && isOfNounType(der.getValue0(), "pathological_state")) {
                    WordNetBasedEntity pathology = new WordNetBasedEntity(aJCas);
                    UimaAnnotationUtils.finishAnnotation(pathology, token.getBegin(), token.getEnd(), COMPONENT_ID,
                            0, aJCas);
                    pathology.setSense("Pathology");
                }
            }
        });
    }

    private boolean isOfType(StanfordCorenlpToken token, String... targetTypes) {
        String lemma = token.getLemma().toLowerCase();
        String posTag = token.getPos();
        return isOfType(lemma, posTag, targetTypes);
    }

    private boolean isOfType(String lemma, String posTag, String... targetTypes) {
        Set<String> hypernyms = wns.getAllHypernymsForAllSense(lemma.toLowerCase(), posTag);

        for (String type : targetTypes) {
            if (hypernyms.contains(type)) {
                return true;
            }
        }
        return false;
    }


    private boolean isOfNounType(StanfordCorenlpToken token, String... targetTypes) {
        return isOfNounType(token.getLemma().toLowerCase(), targetTypes);
    }

    private boolean isOfNounType(String lemma, String... targetTypes) {
        Set<String> hypernyms = wns.getAllNounHypernymsForAllSense(lemma);

        for (String type : targetTypes) {
            if (hypernyms.contains(type)) {
                return true;
            }
        }
        return false;
    }
}