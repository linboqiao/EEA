package edu.cmu.cs.lti.ling;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.morph.WordnetStemmer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

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


    public static void main(String[] argv) throws IOException {
        WordNetSearcher wns = new WordNetSearcher("/Users/zhengzhongliu/Documents/projects/cmu-script/data/resources/wnDict");

        System.out.println(wns.stem("advice", POS.VERB));
        System.out.println(wns.stem("debaterr", POS.VERB));
        System.out.println(wns.stem("injuries", POS.NOUN));
        System.out.println(wns.stem("taxes", POS.NOUN));


    }

}
