package edu.cmu.cs.lti.ling;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.data.ILoadPolicy;
import edu.mit.jwi.item.*;
import edu.mit.jwi.morph.WordnetStemmer;
import org.javatuples.Pair;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JWI's cache lookup implementation doesn't seem to provide a concurrent access. Use the in memory version if you
 * need to have concurrent (I don't understand why there is an exception thrown though, but it doesn't seem to affect
 * the output).
 * User: zhengzhongliu
 * Date: 1/29/15
 * Time: 11:42 PM
 */
public class WordNetSearcher {
    IDictionary dict;
    WordnetStemmer stemmer;

    public WordNetSearcher(String wnDictPath) throws IOException {
        this(wnDictPath, false);
    }

    public WordNetSearcher(String wnDictPath, boolean inMemory) throws IOException {
        URL url = null;
        try {
            url = new URL("file", null, wnDictPath);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (url == null) return;

        if (inMemory) {
            dict = new RAMDictionary(url, ILoadPolicy.IMMEDIATE_LOAD);
        } else {
            dict = new Dictionary(url);
        }
        dict.open();

        stemmer = new WordnetStemmer(dict);
    }

    public List<String> stem(String word, POS pos) {
        return stemmer.findStems(word, pos);
    }


    public Set<Pair<String, String>> getDerivations(String word, String pos) {
        return getDerivations(word, pennTreeTag2POS(pos));
    }

    public Set<Pair<String, String>> getDerivations(String lemma, POS pos) {
        Set<Pair<String, String>> derivationWords = new HashSet<>();

        for (ISynset synset : getAllSynsets(lemma, pos)) {
            for (IWord iWord : synset.getWords()) {
                // Restrict the synset expansion here because we don't won't derivation of other words that have similar
                // meaning.
                if (iWord.getLemma().equals(lemma)) {
                    List<IWordID> derivationRelated = iWord.getRelatedWords(Pointer.DERIVATIONALLY_RELATED);
                    for (IWordID iWordID : derivationRelated) {
                        IWord derativeWord = dict.getWord(iWordID);
                        derivationWords.add(Pair.with(derativeWord.getLemma(), derativeWord.getPOS().toString()));
                    }
                }
            }
        }
        return derivationWords;
    }

    public List<String> getAllSynonyms(String wordType, String posTag) {
        return getAllSynonyms(wordType, pennTreeTag2POS(posTag));
    }

    public List<String> getAllSynonyms(String wordType, POS pos) {
        IIndexWord idxWord = dict.getIndexWord(wordType, pos);

        Set<String> synonyms = new HashSet<>();

        if (idxWord != null) {
            for (IWordID wordId : idxWord.getWordIDs()) {
                IWord word = dict.getWord(wordId);
                synonyms.addAll(word.getSynset().getWords().stream().map(IWord::getLemma).collect(Collectors.toList()));
            }
        }
        return new ArrayList<>(synonyms);
    }

    public List<ISynsetID> getAllHypernyms(ISynset synset) {
        return getAllHypernyms(synset, new HashSet<>());
    }

    public List<ISynsetID> getAllHypernyms(ISynset synset, Set<ISynset> alreadyVisited) {
        List<ISynsetID> allHyperNyms = new ArrayList<>();

        List<ISynsetID> thisHypers = synset.getRelatedSynsets(Pointer.HYPERNYM);
        alreadyVisited.add(synset);

        if (!thisHypers.isEmpty()) {
            for (ISynsetID thisHyper : thisHypers) {
                ISynset thisHyperSynset = dict.getSynset(thisHyper);
                if (!alreadyVisited.contains(thisHyperSynset)) {
                    allHyperNyms.add(thisHyper);
                    allHyperNyms.addAll(getAllHypernyms(thisHyperSynset, alreadyVisited));
                }
            }
        }

        return allHyperNyms;
    }

    public Set<String> getAllNounHypernymsForAllSense(String wordType) {
        return getAllHypernymsForAllSense(wordType, POS.NOUN);
    }

    public Set<String> getAllHypernymsForAllSense(String wordType, String posTag) {
        return getAllHypernymsForAllSense(wordType, pennTreeTag2POS(posTag));
    }

    public Set<String> getAllHypernymsForAllSense(String wordType, POS pos) {
        Set<String> allHyperNyms = new HashSet<>();

        for (ISynset synset : getAllSynsets(wordType, pos)) {
            for (ISynsetID hyperSynsetId : getAllHypernyms(synset)) {
                List<IWord> words = dict.getSynset(hyperSynsetId).getWords();
                for (IWord hyperWord : words) {
                    allHyperNyms.add(hyperWord.getLemma());
                }
            }
            for (IWord iWord : synset.getWords()) {
                allHyperNyms.add(iWord.getLemma());
//                System.out.println(iWord.getLemma());
            }
        }
        return allHyperNyms;
    }

    public Set<String> getFirstWordForAllHypernymsForAllSense(String wordType) {
        return getFirstWordForAllHypernymsForAllSense(wordType, POS.NOUN);
    }

    public Set<String> getFirstWordForAllHypernymsForAllSense(String wordType, POS pos) {
        Set<String> allHyperNyms = new HashSet<>();

        for (ISynset synset : getAllSynsets(wordType, pos)) {
            for (ISynsetID hyperSynsetId : getAllHypernyms(synset)) {
                List<IWord> words = dict.getSynset(hyperSynsetId).getWords();
                for (IWord hyperWord : words) {
                    allHyperNyms.add(hyperWord.getLemma());
                    break;
                }
            }
            for (IWord iWord : synset.getWords()) {
                allHyperNyms.add(iWord.getLemma());
                break;
//                System.out.println(iWord.getLemma());
            }
        }
        return allHyperNyms;
    }

    public List<ISynset> getAllSynsets(String wordType, POS pos) {
        List<ISynset> synsets = new ArrayList<>();
        IIndexWord idxWord = dict.getIndexWord(wordType, pos);
        if (idxWord != null) {
            for (IWordID wordId : idxWord.getWordIDs()) {
                IWord word = dict.getWord(wordId);
                ISynset synset = word.getSynset();
                synsets.add(synset);
            }
        }
        return synsets;
    }

    private POS pennTreeTag2POS(String posTag) {
        if (posTag.startsWith("N")) {
            return POS.NOUN;
        } else if (posTag.startsWith("J")) {
            return POS.ADJECTIVE;
        } else if (posTag.startsWith("V")) {
            return POS.VERB;
        } else if (posTag.startsWith("R")) {
            return POS.ADVERB;
        } else {
            return POS.NOUN;
        }
    }

    private void test() {
//        System.out.println(stem("advice", POS.VERB));
//        System.out.println(stem("debater", POS.VERB));
//        System.out.println(stem("injuries", POS.NOUN));
//        System.out.println(stem("taxes", POS.NOUN));
//        System.out.println(stem("sentencing", POS.NOUN));
//
//        System.out.println("Body part?");
//
//        System.out.println(getAllNounHypernymsForAllSense("jaw"));

        System.out.println("Money?");

        System.out.println(getFirstWordForAllHypernymsForAllSense("insurance"));
//        System.out.println(getAllNounHypernymsForAllSense("pension"));
//        System.out.println(getAllNounHypernymsForAllSense("revenue"));
//        System.out.println(getAllNounHypernymsForAllSense("bribe"));
//        System.out.println(getAllNounHypernymsForAllSense("bill"));
//        System.out.println(getAllNounHypernymsForAllSense("tax"));
//
//        System.out.println("Ownerships");
//
//        System.out.println(getAllNounHypernymsForAllSense("name"));
//        System.out.println(getAllNounHypernymsForAllSense("jewelry"));
//        System.out.println(getAllNounHypernymsForAllSense("item"));
//        System.out.println(getAllNounHypernymsForAllSense("bracelet"));
//        System.out.println(getAllNounHypernymsForAllSense("gun"));
//
//        System.out.println("Profession");
//
//        System.out.println(getAllNounHypernymsForAllSense("driver"));
//        System.out.println(getAllNounHypernymsForAllSense("senator"));
//        System.out.println(getAllNounHypernymsForAllSense("judge"));
//
//
//        System.out.println("Disease");
//
//        System.out.println(getAllNounHypernymsForAllSense("injury"));
//        System.out.println(getAllNounHypernymsForAllSense("have"));
//
//        System.out.println("Derivative words");
//
//        System.out.println(getDerivations("injure", "V"));
//        System.out.println(getDerivations("sentence", "V"));
//        System.out.println(getDerivations("have", "V"));
//        System.out.println(getDerivations("bruise", "N"));
//        System.out.println(getDerivations("growth", "N"));
    }


    public static void main(String[] argv) throws IOException {
        WordNetSearcher inmemoryWns = new WordNetSearcher("/Users/zhengzhongliu/Documents/projects/data/wnDict", true);
        WordNetSearcher cachingWns = new WordNetSearcher("/Users/zhengzhongliu/Documents/projects/data/wnDict", true);

        long start = System.nanoTime();

        inmemoryWns.test();

        long inmemoryTime = System.nanoTime() - start;

        start = System.nanoTime();

        cachingWns.test();

        long cacheTime = System.nanoTime() - start;

        System.out.println(String.format("In memory time is %d, caching time is %d", inmemoryTime, cacheTime));
    }
}