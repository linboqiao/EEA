package edu.cmu.cs.lti.ark.pipeline;

import edu.cmu.cs.lti.ark.fn.Semafor;
import edu.cmu.cs.lti.ark.fn.data.perp.formats.TokenBuilder;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Token;
import edu.cmu.cs.lti.ark.fn.parsing.SemaforParseResult;
import edu.cmu.cs.lti.ark.pipeline.parsing.MaltWrapper;
import edu.cmu.cs.lti.ark.pipeline.parsing.ParserImpl;
import edu.cmu.cs.lti.ark.pipeline.parsing.ParsingException;
import org.maltparser.core.exception.MaltChainedException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/23/15
 * Time: 11:09 PM
 */
public class SemaforFullPipeline {
    Semafor semafor;

    ParserImpl malt;

    public SemaforFullPipeline(File modelDir) throws IOException, URISyntaxException, ClassNotFoundException, MaltChainedException {
        semafor = Semafor.getSemaforInstance(modelDir.getCanonicalPath());
        malt = new MaltWrapper(modelDir);
    }

    //probably the easiest way to call without expose the implementations? a little bit ugly though
    public SemaforParseResult parse(List<String> wordSurfaces, List<String> lemmas, List<String> pos) throws ParsingException, IOException {
        List<Token> tokens = new ArrayList<Token>();
        for (int i = 0; i < wordSurfaces.size(); i++) {
//            Token parsedToken = TokenBuilder.aToken(tokens.get(i)).withId(i+1).withForm(wordSurfaces.get(i)).withLemma(lemmas.get(i))
//                    .withPostag(pos.get(i)).build();
            tokens.add(TokenBuilder.aToken(tokens.get(i)).withId(i + 1).withForm(wordSurfaces.get(i)).withLemma(lemmas.get(i))
                    .withPostag(pos.get(i)).build());
//            tokens.add(new Token(i + 1, wordSurfaces.get(i), lemmas.get(i), null, pos.get(i), null, null, null, null, null));
        }
        return parse(tokens);
    }

    public SemaforParseResult parse(Iterable<Token> sentence) throws ParsingException, IOException {
        return parse(new Sentence(sentence));
    }

    public SemaforParseResult parse(Sentence sentence) throws ParsingException, IOException {
        Sentence parsedSentence = malt.parse(sentence);
        return semafor.parseSentence(parsedSentence);
    }

    public static void main(String args[]) throws URISyntaxException, ClassNotFoundException, MaltChainedException, IOException, ParsingException {
        List<String> words = new ArrayList<String>() {
            {
                add("He");
                add("is");
                add("a");
                add("professor");
            }
        };

        List<String> lemmas = new ArrayList<String>() {
            {
                add("He");
                add("is");
                add("a");
                add("professor");
            }
        };


        List<String> pos = new ArrayList<String>() {
            {
                add("PRP");
                add("VBZ");
                add("DT");
                add("NN");
            }
        };

//        List<String> cpos = new ArrayList<String>() {
//            {
//                add("PRP");
//                add("VBZ");
//                add("DT");
//                add("NN");
//            }
//        };

        SemaforFullPipeline pipeline = new SemaforFullPipeline(new File("../models/semafor_malt_model_20121129"));

        SemaforParseResult result = pipeline.parse(words, lemmas, pos);

        System.out.println(result.toJson());
    }

}
