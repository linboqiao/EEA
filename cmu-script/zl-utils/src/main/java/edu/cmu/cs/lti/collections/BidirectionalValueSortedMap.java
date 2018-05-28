package edu.cmu.cs.lti.collections;

import com.google.common.collect.TreeMultimap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: zhengzhongliu
 * Date: 11/21/14
 * Time: 11:19 AM
 */
public class BidirectionalValueSortedMap<K extends Comparable<K>, V extends Comparable<V>> implements Map<K, V> {
    TreeMultimap<V, K> valueLookUp;
    Map<K, V> primaryMap;

    public BidirectionalValueSortedMap() {
        valueLookUp = TreeMultimap.create();
        valueLookUp.size();
        primaryMap = new HashMap<>();
    }

    public java.util.NavigableSet<K> getKey(V val) {
        return valueLookUp.get(val);
    }

    public Entry<V, Collection<K>> pollFirstEntry() {
        Entry<V, Collection<K>> firstEntry = valueLookUp.asMap().pollFirstEntry();
        if (firstEntry != null) {
            for (K key : firstEntry.getValue()) {
                primaryMap.remove(key);
            }
        }
        return firstEntry;
    }


    public Entry<V, Collection<K>> pollLastEntry() {
        Entry<V, Collection<K>> lastEntry = valueLookUp.asMap().pollLastEntry();

        if (lastEntry != null) {
            for (K key : lastEntry.getValue()) {
                primaryMap.remove(key);
            }
        }
        return lastEntry;
    }

    @Override
    public int size() {
        return primaryMap.size();
    }

    @Override
    public boolean isEmpty() {
        return primaryMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return primaryMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return valueLookUp.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return primaryMap.get(key);
    }

    @Override
    public V put(K key, V value) {
        V oldKValue = primaryMap.put(key, value);
        if (oldKValue != null && valueLookUp.containsEntry(oldKValue, key)) {
            valueLookUp.remove(oldKValue, key);
        }
        valueLookUp.put(value, key);

        return oldKValue;
    }

    @Override
    public V remove(Object key) {
        V v = primaryMap.remove(key);
        valueLookUp.remove(v, key);
        return v;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        //fairly inefficient, avoid using this too much
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        valueLookUp.clear();
        primaryMap.clear();
    }

    @Override
    public Set<K> keySet() {
        return primaryMap.keySet();
    }

    @Override
    public Collection<V> values() {
        return primaryMap.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return primaryMap.entrySet();
    }

    public static void main(String[] args) {
        BidirectionalValueSortedMap<String, Double> map = new BidirectionalValueSortedMap<>();

        map.put("a", 5.0);
        map.put("b", 1.0);
        map.put("c", 3.0);
        map.put("d", 0.0);
        //ensure it's still a map (by overwriting a key, but with a new value)
        map.put("d", 2.0);
        //Ensure multiple values do not clobber keys
        map.put("e", 2.0);

        //check whether put will return the previous value
        double v = map.put("a", 1.0);

        System.out.println(v);

        while (!map.isEmpty()) {
            Entry<Double, Collection<String>> largestEntry = map.pollLastEntry();
            System.out.println("Entry:");
            System.out.println("  " + largestEntry.getKey() + " " + largestEntry.getValue());
        }
    }
}
