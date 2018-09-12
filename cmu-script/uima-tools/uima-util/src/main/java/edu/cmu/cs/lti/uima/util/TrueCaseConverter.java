package edu.cmu.cs.lti.uima.util;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/21/15
 * Time: 12:47 AM
 *
 * @author Zhengzhong Liu
 */
public class TrueCaseConverter {

    private StanfordCoreNLP pipeline;

    public TrueCaseConverter() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma , truecase");
        pipeline = new StanfordCoreNLP(props);
    }


    public void convert(String rawText) {
        Annotation document = new Annotation(rawText);
        pipeline.annotate(document);
        for (CoreLabel token : document.get(CoreAnnotations.TokensAnnotation.class)) {
            int beginIndex = token.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
            int endIndex = token.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
            String truecaseToken = token.get(CoreAnnotations.TrueCaseTextAnnotation.class);

            System.out.println(token.get(CoreAnnotations.TrueCaseAnnotation.class));
            System.out.println(truecaseToken);
        }
    }

    public static void main(String[] args) {
        TrueCaseConverter converter = new TrueCaseConverter();
        // This sentence does not work well.
        converter.convert("POLICE SHOOT TO DEATH SUSPECT IN S.C. KILLING SPREE.");
    }

}
