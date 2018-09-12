package limo.exrel.modules.mallet.MalletMaxEnt;

import java.util.Iterator;


import limo.exrel.modules.mallet.MalletMaxEnt.MaxEnt;
import limo.exrel.modules.mallet.MalletMaxEnt.MaxEntOptimizableByLabelLikelihood;
import limo.exrel.modules.mallet.MalletMaxEnt.Regularizer.ConstantRegularizer;
import limo.exrel.modules.mallet.MalletMaxEnt.Regularizer.RegularizationWeightProducer;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Labeling;
import cc.mallet.types.MatrixOps;
import cc.mallet.util.Maths;

public class MMEOptimizableByLabelLikelihoodL2 extends MaxEntOptimizableByLabelLikelihood {
	
	RegularizationWeightProducer regularizer = null;
	
	public MMEOptimizableByLabelLikelihoodL2() {
		super();
		super.usingGaussianPrior = false;
		super.usingHyperbolicPrior = false;
	}
	
	public MMEOptimizableByLabelLikelihoodL2(InstanceList trainingSet, MaxEnt initialClassifier, RegularizationWeightProducer regularizer) {
		super(trainingSet, initialClassifier);
		this.regularizer = regularizer;
		super.usingHyperbolicPrior = false;
		super.usingGaussianPrior = regularizer != null;
		if (super.usingGaussianPrior && regularizer instanceof ConstantRegularizer && regularizer.getWeights(0, 0) == 0)
			super.usingGaussianPrior = false;
	}
	
	public MMEOptimizableByLabelLikelihoodL2(InstanceList trainingSet, MaxEnt initialClassifier) {
		super(trainingSet, initialClassifier);
		//this.regularizer = new ConstantRegularizer(1.0);
		super.usingHyperbolicPrior = false;
		super.usingGaussianPrior = false;
	}
	
	public RegularizationWeightProducer getRegularizer() {
		return regularizer;
	}
	
	public void setRegularizer(RegularizationWeightProducer regularizer) {
		this.regularizer = regularizer;
		super.usingGaussianPrior = regularizer != null;
		if (super.usingGaussianPrior && regularizer instanceof ConstantRegularizer && regularizer.getWeights(0, 0) == 0)
			super.usingGaussianPrior = false;
	}
	
	// log probability of the training labels
	@Override
	public double getValue ()
		{
			if (cachedValueStale) {
				numGetValueCalls++;
				cachedValue = 0;
				// We'll store the expectation values in "cachedGradient" for now
				cachedGradientStale = true;
				MatrixOps.setAll (cachedGradient, 0.0);
				// Incorporate likelihood of data
				double[] scores = new double[trainingList.getTargetAlphabet().size()];
				double value = 0.0;
				Iterator<Instance> iter = trainingList.iterator();
				int ii=0;
				while (iter.hasNext()) {
					ii++;
					Instance instance = iter.next();
					double instanceWeight = trainingList.getInstanceWeight(instance);
					Labeling labeling = instance.getLabeling ();
					if (labeling == null)
						continue;
					//System.out.println("L Now "+inputAlphabet.size()+" regular features.");

					this.theClassifier.getClassificationScores (instance, scores);
					FeatureVector fv = (FeatureVector) instance.getData ();
					int li = labeling.getBestIndex();
					value = - (instanceWeight * Math.log (scores[li]));
					if(Double.isNaN(value)) {
						logger.fine ("MaxEntTrainer: Instance " + instance.getName() +
								"has NaN value. log(scores)= " + Math.log(scores[li]) +
								" scores = " + scores[li] + 
								" has instance weight = " + instanceWeight);

					}
					if (Double.isInfinite(value)) {
						logger.warning ("Instance "+instance.getSource() + " has infinite value; skipping value and gradient");
						cachedValue -= value;
						cachedValueStale = false;
						return -value;
//						continue;
					}
					cachedValue += value;
					for (int si = 0; si < scores.length; si++) {
						if (scores[si] == 0) continue;
						assert (!Double.isInfinite(scores[si]));
						MatrixOps.rowPlusEquals (cachedGradient, numFeatures,
								si, fv, -instanceWeight * scores[si]);
						cachedGradient[numFeatures*si + defaultFeatureIndex] += (-instanceWeight * scores[si]);
					}
				}
				//logger.info ("-Expectations:"); cachedGradient.print();

				// Incorporate prior on parameters
				double prior = 0;
				if (usingHyperbolicPrior) {
					for (int li = 0; li < numLabels; li++)
						for (int fi = 0; fi < numFeatures; fi++)
							prior += (hyperbolicPriorSlope / hyperbolicPriorSharpness
									* Math.log (Maths.cosh (hyperbolicPriorSharpness * parameters[li *numFeatures + fi])));
				}
				else if (usingGaussianPrior) {
					for (int li = 0; li < numLabels; li++)
						for (int fi = 0; fi < numFeatures; fi++) {
							double param = parameters[li*numFeatures + fi];
							prior += param * param / (2 * regularizer.getWeights(fi, li)); //gaussianPriorVariance
						}
				}

				double oValue = cachedValue;
				cachedValue += prior;
				cachedValue *= -1.0; // MAXIMIZE, NOT MINIMIZE
				cachedValueStale = false;
				progressLogger.info ("Value (labelProb="+oValue+" prior="+prior+") loglikelihood = "+cachedValue);
			}
			return cachedValue;
		}
	
	@Override
	public void getValueGradient (double [] buffer) {

		// Gradient is (constraint - expectation - parameters/gaussianPriorVariance)
		if (cachedGradientStale) {
			numGetValueGradientCalls++;
			if (cachedValueStale)
				// This will fill in the cachedGradient with the "-expectation"
				getValue ();
			MatrixOps.plusEquals (cachedGradient, constraints);
			// Incorporate prior on parameters
			if (usingHyperbolicPrior) {
				throw new UnsupportedOperationException ("Hyperbolic prior not yet implemented.");
			}
			else if (usingGaussianPrior) {
//				MatrixOps.plusEquals (cachedGradient, parameters,
//									  -1.0 / gaussianPriorVariance);
				
				for (int li = 0; li < numLabels; li++)
					for (int fi = 0; fi < numFeatures; fi++) {
						int index = li*numFeatures + fi;
						//double param = parameters[li*numFeatures + fi];
						cachedGradient[index] += parameters[index] / (-1.0 * regularizer.getWeights(fi, li)); //gaussianPriorVariance
					}
			}

			// A parameter may be set to -infinity by an external user.
			// We set gradient to 0 because the parameter's value can
			// never change anyway and it will mess up future calculations
			// on the matrix, such as norm().
			MatrixOps.substitute (cachedGradient, Double.NEGATIVE_INFINITY, 0.0);
			// Set to zero all the gradient dimensions that are not among the selected features
			if (perLabelFeatureSelection == null) {
				for (int labelIndex = 0; labelIndex < numLabels; labelIndex++)
					MatrixOps.rowSetAll (cachedGradient, numFeatures,
							labelIndex, 0.0, featureSelection, false);
			} else {
				for (int labelIndex = 0; labelIndex < numLabels; labelIndex++)
					MatrixOps.rowSetAll (cachedGradient, numFeatures,
							labelIndex, 0.0,
							perLabelFeatureSelection[labelIndex], false);
			}
			cachedGradientStale = false;
		}
		assert (buffer != null && buffer.length == parameters.length);
		System.arraycopy (cachedGradient, 0, buffer, 0, cachedGradient.length);
		//System.out.println ("MaxEntTrainer gradient infinity norm = "+MatrixOps.infinityNorm(cachedGradient));
	}

}
