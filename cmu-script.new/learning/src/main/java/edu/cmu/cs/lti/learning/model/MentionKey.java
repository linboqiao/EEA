package edu.cmu.cs.lti.learning.model;

import edu.cmu.cs.lti.learning.model.graph.EdgeType;
import edu.cmu.cs.lti.script.type.Word;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/25/16
 * Time: 2:24 PM
 *
 * @author Zhengzhong Liu
 */
public class MentionKey implements Iterable<NodeKey> {
    private List<NodeKey> keys;

    private boolean isRoot;

    private String combinedType;

    private Word headWord;

    private static MentionKey root;

    static {
        root = new MentionKey(null, "ROOT");
        root.isRoot = true;

        for (EdgeType edgeType : EdgeType.getNormalTypes()) {
            root.keys.add(NodeKey.getRootKey(edgeType));
        }
    }

    public MentionKey(Word headWord, String combinedType, NodeKey... ks) {
        keys = new ArrayList<>();
        for (NodeKey k : ks) {
            keys.add(k);
        }
        this.combinedType = combinedType;
        this.headWord = headWord;
    }

    public static MentionKey rootKey() {
        return root;
    }

    public Stream<NodeKey> stream() {
        return keys.stream();
    }

    @Override
    public Iterator<NodeKey> iterator() {
        return keys.iterator();
    }

    public boolean isRoot() {
        return isRoot;
    }

    public NodeKey takeFirst() {
        return keys.get(0);
    }

    public int size() {
        return keys.size();
    }

    public List<NodeKey> getKeys() {
        return keys;
    }

    public String getCombinedType() {
        return combinedType;
    }

    public Word getHeadWord() {
        return headWord;
    }
}
