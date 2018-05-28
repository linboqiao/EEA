package edu.cmu.cs.lti.learning.model;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.Serializable;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * Date: 9/16/15
 * Time: 8:36 PM
 *
 * @author Zhengzhong Liu
 */
public class GraphFeatureVector implements Serializable {
    private static final long serialVersionUID = -8364410793971904933L;

    private TIntObjectMap<FeatureVector> nodeFv;

    private Table<Integer, Integer, FeatureVector> edgeFv;

    private FeatureAlphabet featureAlphabet;

    private ClassAlphabet classAlphabet;

    public GraphFeatureVector(ClassAlphabet classAlphabet, FeatureAlphabet featureAlphabet) {
        nodeFv = new TIntObjectHashMap<>();
        edgeFv = HashBasedTable.create();
        this.featureAlphabet = featureAlphabet;
        this.classAlphabet = classAlphabet;
    }

    public GraphFeatureVector newGraphFeatureVector() {
        return new GraphFeatureVector(classAlphabet, featureAlphabet);
    }

    public TIntObjectIterator<FeatureVector> nodeFvIter() {
        return nodeFv.iterator();
    }

    public Iterator<Table.Cell<Integer, Integer, FeatureVector>> edgeFvIter() {
        return edgeFv.cellSet().iterator();
    }

    private FeatureVector newFeatureVector() {
        return new RealValueHashFeatureVector(featureAlphabet);
    }

    public void extend(FeatureVector fv, String endClassName, String fromClassName) {
        extend(fv, classAlphabet.getClassIndex(endClassName), classAlphabet.getClassIndex(fromClassName));
    }

    public void extend(FeatureVector fv, int edgeEnd, int edgeFrom) {
        extend(fv, edgeEnd, edgeFrom, 1.0);
    }

    public void extend(FeatureVector fv, int edgeEnd, int edgeFrom, double multiplier) {
        FeatureVector thisFv;
        if (edgeFv.contains(edgeEnd, edgeFrom)) {
            thisFv = edgeFv.get(edgeEnd, edgeFrom);
        } else {
            thisFv = newFeatureVector();
            edgeFv.put(edgeEnd, edgeFrom, thisFv);
        }
        thisFv.extend(fv, multiplier);
    }

    public void extend(FeatureVector fv, String className) {
        extend(fv, classAlphabet.getClassIndex(className));
    }

    /**
     * Extend with default multiplier.
     *
     * @param fv      The FeatureVector to be extend.
     * @param nodeKey The node key of the FeatureVector.
     */
    public void extend(FeatureVector fv, int nodeKey) {
        extend(fv, nodeKey, 1.0);
    }

    /**
     * Extend by a FeatureVector with multiplier.
     *
     * @param fv         The FeatureVector to be extend.
     * @param nodeKey    The node key of the FeatureVector.
     * @param multiplier The multiplier.
     */
    public void extend(FeatureVector fv, int nodeKey, double multiplier) {
        FeatureVector thisFv;
        if (nodeFv.containsKey(nodeKey)) {
            thisFv = nodeFv.get(nodeKey);
        } else {
            thisFv = newFeatureVector();
            nodeFv.put(nodeKey, thisFv);
        }
        thisFv.extend(fv, multiplier);
    }

    public void extend(GraphFeatureVector vectorToAdd) {
        extend(vectorToAdd, 1.0);
    }

    public void extend(GraphFeatureVector vectorToAdd, double multiplier) {
        for (TIntObjectIterator<FeatureVector> newNodeFvIter = vectorToAdd.nodeFvIter(); newNodeFvIter.hasNext(); ) {
            newNodeFvIter.advance();
            int addKey = newNodeFvIter.key();
            FeatureVector newNodeVector = newNodeFvIter.value();
            extend(newNodeVector, addKey, multiplier);
        }

        for (Iterator<Table.Cell<Integer, Integer, FeatureVector>> newNodeEdgeFvIter = vectorToAdd.edgeFvIter();
             newNodeEdgeFvIter.hasNext(); ) {
            Table.Cell<Integer, Integer, FeatureVector> newEdgeFeature = newNodeEdgeFvIter.next();
            int row = newEdgeFeature.getRowKey();
            int col = newEdgeFeature.getColumnKey();
            FeatureVector newEdgeVector = newEdgeFeature.getValue();
            extend(newEdgeVector, row, col, multiplier);
        }
    }

    public void diff(GraphFeatureVector vectorToDeduct, GraphFeatureVector resultVector) {
        resultVector.extend(this);

        for (TIntObjectIterator<FeatureVector> newNodeFvIter = vectorToDeduct.nodeFvIter(); newNodeFvIter.hasNext(); ) {
            newNodeFvIter.advance();
            int addKey = newNodeFvIter.key();
            FeatureVector newNodeVector = newNodeFvIter.value();
            resultVector.extend(newNodeVector.negation(), addKey);
        }

        for (Iterator<Table.Cell<Integer, Integer, FeatureVector>> newNodeEdgeFvIter = vectorToDeduct.edgeFvIter();
             newNodeEdgeFvIter.hasNext(); ) {
            Table.Cell<Integer, Integer, FeatureVector> newEdgeFeature = newNodeEdgeFvIter.next();
            int row = newEdgeFeature.getRowKey();
            int col = newEdgeFeature.getColumnKey();
            FeatureVector newEdgeVector = newEdgeFeature.getValue();
            resultVector.extend(newEdgeVector.negation(), row, col);
        }
    }

    public FeatureVector getFeatureVectorAtClass(int classIndex) {
        return nodeFv.get(classIndex);
    }

    public FeatureVector getFeatureVectorAtEdge(int edgeEnd, int edgeFrom) {
        return edgeFv.get(edgeEnd, edgeFrom);
    }

    // Currently only implemented differences on node key.
    public GraphFeatureVector nodeOnlyDiff(GraphFeatureVector minusVector) {
        GraphFeatureVector resultVector = new GraphFeatureVector(classAlphabet, featureAlphabet);

        TIntSet classesUsed = new TIntHashSet();
        FeatureVector zeroVector = newFeatureVector();

        for (TIntObjectIterator<FeatureVector> minusIter = minusVector.nodeFvIter(); minusIter.hasNext(); ) {
            minusIter.advance();
            int classIndex = minusIter.key();
            FeatureVector thisVectorAtClass = this.getFeatureVectorAtClass(classIndex);
            FeatureVector resultVectorAtClass = newFeatureVector();
            if (thisVectorAtClass != null) {
                thisVectorAtClass.diff(minusIter.value(), resultVectorAtClass);
            } else {
                zeroVector.diff(minusIter.value(), resultVectorAtClass);
            }
            resultVector.extend(resultVectorAtClass, classIndex);
            classesUsed.add(classIndex);
        }

        for (TIntObjectIterator<FeatureVector> iter = nodeFvIter(); iter.hasNext(); ) {
            iter.advance();
            int classIndex = iter.key();
            if (!classesUsed.contains(classIndex)) {
                FeatureVector resultVectorAtClass = newFeatureVector();
                resultVectorAtClass.extend(iter.value());
                resultVector.extend(resultVectorAtClass, classIndex);
            }
        }

        return resultVector;
    }

    public ClassAlphabet getClassAlphabet() {
        return classAlphabet;
    }

    public FeatureAlphabet getFeatureAlphabet() {
        return featureAlphabet;
    }

    /**
     * Compute the L2 norm of the feature vector.
     *
     * @return The L2 norm of the feature vector.
     */
    public double getFeatureL2() {
        double l2Sq = 0;

        for (TIntObjectIterator<FeatureVector> iter = nodeFvIter(); iter.hasNext(); ) {
            iter.advance();
            FeatureVector fv = iter.value();
            l2Sq += fv.dotProd(fv);
        }

        for (Iterator<Table.Cell<Integer, Integer, FeatureVector>> iter = edgeFvIter(); iter.hasNext(); ) {
            Table.Cell<Integer, Integer, FeatureVector> cell = iter.next();
            FeatureVector fv = cell.getValue();
            l2Sq += fv.dotProd(fv);
        }

        return Math.sqrt(l2Sq);
    }

    public String readableNodeVector() {
        StringBuilder sb = new StringBuilder();
        sb.append("Node Vector:");
        for (TIntObjectIterator<FeatureVector> iter = nodeFvIter(); iter.hasNext(); ) {
            iter.advance();
            sb.append("\n###### Feature at class ").append(classAlphabet.getClassName(iter.key())).append("\n");
            sb.append(iter.value().readableString());
            sb.append("\n");
        }
        return sb.toString();
    }

    public String readableEdgeVector() {
        StringBuilder sb = new StringBuilder();
        sb.append("Edge Vector:");
        for (Iterator<Table.Cell<Integer, Integer, FeatureVector>> iter = edgeFvIter(); iter.hasNext(); ) {
            Table.Cell<Integer, Integer, FeatureVector> cell = iter.next();
            sb.append("\n###### Feature at class ").append(classAlphabet.getClassName(cell.getRowKey())).append(" <- " +
                    "").append(classAlphabet.getClassName(cell.getColumnKey())).append("\n");
            sb.append(cell.getValue().readableString());
            sb.append("\n");
        }
        return sb.toString();
    }

}
