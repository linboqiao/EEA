package limo.exrel.modules.mallet.MalletMaxEnt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import cc.mallet.types.Alphabet;
import cc.mallet.types.AlphabetCarrying;
import cc.mallet.types.Sequence;

public class FeatureSequenceValues implements Sequence, Serializable, AlphabetCarrying {
	
	Alphabet dictionary;
	int[] features;
	double[] values;
	int length;

	/**
	 *  Creates a FeatureSequence given all of the objects in the
	 *  sequence.
	 *
	 *  @param dict A dictionary that maps objects in the sequence
	 *     to numeric indices.
	 *  @param features An array where features[i] gives the index
	 *     in dict of the ith element of the sequence.
	 */
	public FeatureSequenceValues (Alphabet dict, int[] features, double[] values)
	{
		//assert(features.length == values.length): "features and values must have the same lenght";
		this(dict, features.length);
		for (int i = 0; i < features.length; i++)
			add(features[i], values[i]);
	}

	public FeatureSequenceValues (Alphabet dict, int[] features, double[] values, int len)
	{
		this(dict, len);
		for (int i = 0; i < len; i++)
			add(features[i], values[i]);
	}

	public FeatureSequenceValues (Alphabet dict, int capacity)
	{
		dictionary = dict;
		features = new int[capacity > 2 ? capacity : 2];
		values = new double[capacity > 2 ? capacity : 2];
		length = 0;
	}

	public FeatureSequenceValues (Alphabet dict)
	{
		this (dict, 2);
	}
	
	public int[] getFeatures() { return features ;}
	
	public double[] getValues() { return values; }
	
	public Alphabet getAlphabet ()	{	return dictionary; }
	
	public Alphabet[] getAlphabets() {
		return new Alphabet[] {getAlphabet()};
	}
	
	public boolean alphabetsMatch (AlphabetCarrying object)	{
		return getAlphabet().equals (object.getAlphabet());
	}

	public final int getLength () { return length; }

	public final int size () { return length; }

	public final int getIndexAtPosition (int pos)
	{
		return features[pos];
	}
	
	public final double getValueAtPosition (int pos)
	{
		return values[pos];
	}

	public Object getObjectAtPosition (int pos)
	{
		return dictionary.lookupObject (features[pos]);
	}

	// xxx This method name seems a bit ambiguous?
	public Object get (int pos)
	{
		return dictionary.lookupObject (features[pos]);
	}

	public String toString ()
	{
		StringBuffer sb = new StringBuffer ();
		for (int fsi = 0; fsi < length; fsi++) {
			Object o = dictionary.lookupObject(features[fsi]);
      sb.append (fsi);
      sb.append (": ");
			sb.append (o.toString());
			sb.append (" (");
			sb.append (features[fsi]);
			sb.append(" , ");
			sb.append(values[fsi]);
			sb.append (")\n");
		}
		return sb.toString();
	}

	protected void growIfNecessary ()
	{
		if (length == features.length) {
			int[] newFeatures = new int[features.length * 2];
			System.arraycopy (features, 0, newFeatures, 0, length);
			features = newFeatures;
			
			double[] newValues = new double[features.length * 2];
			System.arraycopy(values, 0, newValues, 0, length);
			values = newValues;
		}
	}

	public void add (int featureIndex, double value)
	{
		growIfNecessary ();
		assert (featureIndex < dictionary.size());
		//features[length++] = featureIndex;
		features[length] = featureIndex;
		values[length] = value;
		length++;
	}

	public void add (Object key, double value)
	{
		int fi = dictionary.lookupIndex (key);
		if (fi >= 0)
			add (fi, value);
		
		// gdruck@cs.umass.edu
		// With the exception below, it is not possible to pipe data
		// when growth of the alphabet is stopped.  We want to be 
		// able to do this, for example to process new data using 
		// an old Pipe (for example from a fixed, cached classifier
		// that we want to apply to new data.).
		//else
			// xxx Should we raise an exception if the appending doesn't happen?  "yes" -akm, added 1/2008
		//	throw new IllegalStateException ("Object cannot be added to FeatureSequence because its Alphabet is frozen.");
	}

//	public void addFeatureWeightsTo (double[] weights)
//	{
//		for (int i = 0; i < length; i++)
//			weights[features[i]]++;
//	}
//
//	public void addFeatureWeightsTo (double[] weights, double scale)
//	{
//		for (int i = 0; i < length; i++)
//			weights[features[i]] += scale;
//	}

	public int[] toFeatureIndexSequence ()
	{
		int[] feats = new int[length];
		System.arraycopy (features, 0, feats, 0, length);
		return feats;
	}
	
//	public double[] toFeatureValueSequence() {
//		double[] vals = new double[length];
//		System.arraycopy(values, 0, vals, 0, length);
//		return vals;
//	}

	public int[] toSortedFeatureIndexSequence ()
	{
		int[] feats = this.toFeatureIndexSequence ();
		java.util.Arrays.sort (feats);
		return feats;
	}
	
	public Object[] toSortedFeatureIndexValueSequence() {
		int[] feats = new int[length];
		double[] vals = new double[length];
		
		if (length > 0) {
			int idx = 0, minIdx = 0;
			double val = 0;
			
			System.arraycopy(features, 0, feats, 0, length);
			System.arraycopy(values, 0, vals, 0, length);
			
			for (int i = 0; i < length; i++) {
				minIdx = feats[i];
				idx = i;
				for (int j = i+1; j < length; j++) {
					if (feats[j] < minIdx) {
						minIdx = feats[j];
						idx = j;
					}
				}
				
				if (i != idx) {
					val = vals[i];
					vals[i] = vals[idx];
					vals[idx] = val;
					
					feats[idx] = feats[i];
					feats[i] = minIdx;
				}
			}
		}
		
		Object[] ret = {feats, vals};
		return ret;
	}
	
	
	/** 
	 *  Remove features from the sequence that occur fewer than 
	 *  <code>cutoff</code> times in the corpus, as indicated by 
	 *  the provided counts. Also swap in the new, reduced alphabet.
	 *  This method alters the instance in place; it is not appropriate
	 *  if the original instance will be needed.
	 */
//    public void prune (double[] counts, Alphabet newAlphabet,
//                       int cutoff) {
//        // The goal is to replace the sequence of features in place, by
//        //  creating a new array and then swapping it in.
//
//        // First: figure out how long the new array will have to be
//
//        int newLength = 0;
//        for (int i = 0; i < length; i++) {
//            if (counts[features[i]] >= cutoff) {
//                newLength++;
//            }
//        }
//
//        // Second: allocate a new features array
//        
//        int[] newFeatures = new int[newLength];
//
//        // Third: fill the new array
//
//        int newIndex = 0;
//        for (int i = 0; i < length; i++) {
//            if (counts[features[i]] >= cutoff) {
//
//                Object feature = dictionary.lookupObject(features[i]);
//                newFeatures[newIndex] = newAlphabet.lookupIndex(feature);
//
//                newIndex++;
//            }
//        }
//
//        // Fourth: swap out the arrays
//
//        features = newFeatures;
//        length = newLength;
//        dictionary = newAlphabet;
//
//    }
    
   	// Serialization
		
	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 0;
	private static final int NULL_INTEGER = -1;
	
	private void writeObject (ObjectOutputStream out) throws IOException {
		out.writeInt (CURRENT_SERIAL_VERSION);
		out.writeObject (dictionary);
		out.writeInt (features.length);
		for (int i = 0; i < features.length; i++)
			out.writeInt (features[i]);
		out.writeInt(values.length);
		for (int i = 0; i < values.length; i++)
			out.writeDouble(values[i]);
		out.writeInt (length);
	}
	
	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		int featuresLength, valuesLength;
		int version = in.readInt ();
		dictionary = (Alphabet) in.readObject ();
		featuresLength = in.readInt();
		features = new int[featuresLength];
		for (int i = 0; i < featuresLength; i++)
			features[i] = in.readInt ();
		valuesLength = in.readInt();
		values = new double[valuesLength];
		for (int i = 0; i < valuesLength; i++)
			values[i] = in.readDouble();
		length = in.readInt ();
	}
	

}
