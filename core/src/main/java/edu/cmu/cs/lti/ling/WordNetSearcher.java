package edu.cmu.cs.lti.ling;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.*;
import edu.mit.jwi.morph.WordnetStemmer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 1/29/15
 * Time: 11:42 PM
 */
public class WordNetSearcher {

    IDictionary dict;
    WordnetStemmer stemmer;


    public WordNetSearcher(String wnDictPath) throws IOException {
        URL url = null;
        try {
            url = new URL("file", null, wnDictPath);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (url == null) return;

        dict = new Dictionary(url);
        dict.open();

        stemmer = new WordnetStemmer(dict);
    }

    public List<String> stem(String word, POS pos) {
        return stemmer.findStems(word, pos);
    }


    public List<ISynsetID> getAllHypernyms(ISynset synset) {
        List<ISynsetID> allHyperNyms = new ArrayList<>();

        List<ISynsetID> thisHypers = synset.getRelatedSynsets(Pointer.HYPERNYM);

        if (!thisHypers.isEmpty()) {
            for (ISynsetID thisHyper : thisHypers) {
                allHyperNyms.add(thisHyper);
                allHyperNyms.addAll(getAllHypernyms(dict.getSynset(thisHyper)));
            }
        }

        return allHyperNyms;
    }

    public List<Set<String>> getAllHypernymsForAllSense(String wordType) {

        List<Set<String>> allHyperNyms = new ArrayList<>();

        IIndexWord idxWord = dict.getIndexWord(wordType, POS.NOUN);

        if (idxWord == null) {
            return allHyperNyms;
        }

        for (IWordID wordId : idxWord.getWordIDs()) {
            IWord word = dict.getWord(wordId);
            ISynset synset = word.getSynset();

            Set<String> thisHyperNyms = new HashSet<>();
            List<ISynsetID> hyperNyms = getAllHypernyms(synset);

            for (ISynsetID sid : hyperNyms) {
                List<IWord> words = dict.getSynset(sid).getWords();
                for (IWord hyperWord : words) {
                    thisHyperNyms.add(hyperWord.getLemma());
                }
            }

            allHyperNyms.add(thisHyperNyms);
        }

        return allHyperNyms;
    }

    public static void main(String[] argv) throws IOException {
        WordNetSearcher wns = new WordNetSearcher("/Users/zhengzhongliu/Documents/projects/cmu-script/data/resources/wnDict");

        System.out.println(wns.stem("advice", POS.VERB));
        System.out.println(wns.stem("debater", POS.VERB));
        System.out.println(wns.stem("injuries", POS.NOUN));
        System.out.println(wns.stem("taxes", POS.NOUN));


        System.out.println(wns.getAllHypernymsForAllSense("jaw"));

        System.out.println("Money?");

        System.out.println(wns.getAllHypernymsForAllSense("insurance"));
        System.out.println(wns.getAllHypernymsForAllSense("pension"));
        System.out.println(wns.getAllHypernymsForAllSense("revenue"));
        System.out.println(wns.getAllHypernymsForAllSense("bribe"));
        System.out.println(wns.getAllHypernymsForAllSense("bill"));
        System.out.println(wns.getAllHypernymsForAllSense("tax"));

        System.out.println("Ownerships");

        System.out.println(wns.getAllHypernymsForAllSense("name"));
        System.out.println(wns.getAllHypernymsForAllSense("jewelry"));
        System.out.println(wns.getAllHypernymsForAllSense("item"));
        System.out.println(wns.getAllHypernymsForAllSense("bracelet"));
        System.out.println(wns.getAllHypernymsForAllSense("gun"));


    }
}