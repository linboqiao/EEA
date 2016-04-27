package edu.cmu.cs.lti.learning.model;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/25/16
 * Time: 2:24 PM
 *
 * @author Zhengzhong Liu
 */
public class MultiNodeKey implements Iterable<NodeKey> {

    private SortedSet<NodeKey> keys;

    private boolean isRoot;

    public static final String REALIS_ROOT = "ROOT";

    public static final String TYPE_ROOT = "ROOT";

    static MultiNodeKey root;

    static {
        NodeKey singleRoot = new NodeKey(0, 0, TYPE_ROOT, REALIS_ROOT, -1);
        root = new MultiNodeKey();
        root.isRoot = true;
        root.keys.add(singleRoot);
    }

    public MultiNodeKey(NodeKey... ks) {
        keys = new TreeSet<>();
        for (NodeKey k : ks) {
            addKey(k);
        }
    }

    public static MultiNodeKey rootKey() {
        return root;
    }

    public void addKey(NodeKey k) {
        keys.add(k);
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
        return keys.first();
    }

    public int size() {
        return keys.size();
    }

    public SortedSet<NodeKey> getKeys() {
        return keys;
    }
}
