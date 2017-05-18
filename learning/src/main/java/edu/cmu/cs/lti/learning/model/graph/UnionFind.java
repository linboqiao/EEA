package edu.cmu.cs.lti.learning.model.graph;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * Date: 4/14/17
 * Time: 9:10 PM
 *
 * @author Zhengzhong Liu
 */
public class UnionFind {

    private int[] _parent;
    private int[] _rank;


    public int find(int i) {

        int p = _parent[i];
        if (i == p) {
            return i;
        }
        return _parent[i] = find(p);
    }


    public void union(int i, int j) {
        int root1 = find(i);
        int root2 = find(j);

        if (root2 == root1) return;

        if (_rank[root1] > _rank[root2]) {
            _parent[root2] = root1;
        } else if (_rank[root2] > _rank[root1]) {
            _parent[root1] = root2;
        } else {
            _parent[root2] = root1;
            _rank[root1]++;
        }
    }

    public UnionFind(int max) {
        _parent = new int[max];
        _rank = new int[max];

        for (int i = 0; i < max; i++) {
            _parent[i] = i;
        }
    }


    public String toString() {
        return "<UnionFind\np " + Arrays.toString(_parent) + "\nr " + Arrays.toString(_rank) + "\n>";
    }

    public static void main(String[] args) {
        UnionFind uf = new UnionFind(5);
        System.out.println(uf);

        uf.union(1, 2);
        System.out.println("union 1 2");
        System.out.println(uf);

        uf.union(1, 2);
        System.out.println("union 1 2");
        System.out.println(uf);

        uf.union(3, 4);
        System.out.println("union 3 4");
        System.out.println(uf);

        uf.union(1, 0);
        System.out.println("union 1 0");
        System.out.println(uf);

        uf.union(1, 3);
        System.out.println("union 1 3");
        System.out.println(uf);

        System.out.println(uf.find(4));
        System.out.println("find 4");
        System.out.println(uf);

    }
}