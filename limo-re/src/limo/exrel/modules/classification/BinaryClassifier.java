/*
 * Copyright (c) 2008 - 2009 , Daniele Pighin - All rights reserved.
 * 
 * This software is released under a double licensing scheme.
 * 
 * For personal or research uses, the software is available under the
 * GNU Lesser GPL (LGPL) v.3 license. 
 * 
 * See the file LICENSE in the source distribution for more details.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package limo.exrel.modules.classification;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;

import limo.exrel.modules.AbstractModule;
import limo.exrel.slots.ClassSlot;
import limo.exrel.slots.ExternalCommandSlot;
import limo.exrel.slots.FileSlot;
import limo.exrel.slots.InputDirSlot;
import limo.exrel.slots.LabelMapping;
import limo.exrel.slots.OutputDirSlot;
import limo.exrel.slots.OutputFileSlot;
import limo.exrel.slots.StringSlot;
import limo.exrel.utils.Index;
import limo.exrel.utils.Index.OpenMode;

import limo.exrel.data.Classification;


public class BinaryClassifier extends AbstractModule {
	
	public ExternalCommandSlot svmClassify = new ExternalCommandSlot(true);
	public InputDirSlot modelDir = new InputDirSlot(true);
	public ClassSlot<LabelMapping> labelMappingClass = new ClassSlot<LabelMapping>(true);
	public StringSlot targetLabel = new StringSlot(true);
	public OutputDirSlot svmOutputDir = new OutputDirSlot(true);
	public FileSlot examplesIdxFile = new FileSlot(true);
	public OutputFileSlot outScoresIdxFile = new OutputFileSlot(false);	
	
	public BinaryClassifier(String moduleId, String configId) {
		super(moduleId,configId);
	}
	
	public File getModelFile() {
		return new File(modelDir.get().getAbsolutePath() + File.separator + 
				targetLabel.get() + ".model");
	}
		
	public File getExamplesFile() {
		return new File(svmOutputDir.get().getAbsolutePath() + File.separator + 
				targetLabel.get() + ".examples.txt");
	}
	
	public File getPredictionsFile() {
		return new File(svmOutputDir.get().getAbsolutePath() + File.separator + 
				targetLabel.get() + ".predictions.txt");
	}
	
	public File getStdOutFile() {
		return new File(svmOutputDir.get().getAbsolutePath() + File.separator + 
				targetLabel.get() + ".stdout.txt");
	}
	
	public File getStdErrFile() {
		return new File(svmOutputDir.get().getAbsolutePath() + File.separator + 
				targetLabel.get() + ".stderr.txt");
	}
	
	@Override
	protected void _run() {
		
		File svmClassifyFile = svmClassify.get();
		File model = getModelFile();
		File outDir = svmOutputDir.get();
		
		File examples = getExamplesFile();
		File predictions = getPredictionsFile();
		File output = getStdOutFile();
		File error = getStdErrFile();
					
		message("Output dir: %s", outDir.getAbsolutePath());
		
		message("Reading examples from: %s", examplesIdxFile);				
		Index examplesIdx = new Index(examplesIdxFile.get(), OpenMode.READ);
		message("%d lines found in file: %s", examplesIdx.numRecords(), examplesIdxFile);
				
		SVMInputGenerator igen = new SVMInputGenerator(targetLabel.get(),getConfigId());
		igen.examplesIdxFile.set(examplesIdxFile);
		igen.labelMappingClass.set(labelMappingClass);
		igen.targetLabel.set(targetLabel);
		igen.outExamplesFile.set(examples);
		igen.run();
		try {
			igen.join();
		} catch (InterruptedException ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		
		SVMClassify classify = new SVMClassify(svmClassifyFile);
		classify.setParameters(examples, model, predictions, output, error);
		message("Running SVM from: %s", svmClassifyFile.getAbsolutePath());
		message("Using model from: %s", model.getAbsolutePath());
		message("Output predictions file: %s", getPredictionsFile().getAbsolutePath());
		message("Standard output file: %s", getStdOutFile().getAbsolutePath());
		message("Standard error file: %s", getStdErrFile().getAbsolutePath());
		classify.run();		
		
		//create classification scores index file
		if (outScoresIdxFile.get() != null) {
			message("Building output scores file: %s", outScoresIdxFile.get().getAbsolutePath());		
			Index scoresIdx = new Index(outScoresIdxFile.get(), OpenMode.WRITE);
			try {
				BufferedReader predictionsReader = new BufferedReader(new FileReader(predictions));
				double score;
				for (Integer exKeys : examplesIdx.allKeys()) {
					String line = predictionsReader.readLine();
					score = Double.parseDouble(line);
					scoresIdx.put(new Classification(targetLabel.get(), score).toString(), exKeys);
				}
				predictionsReader.close();
				message("%d lines written to file: %s", scoresIdx.numRecords(), outScoresIdxFile.get().getAbsolutePath());
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
			scoresIdx.close();
		}
		
	}

	public static void main(String[] args) {
		new BinaryClassifier("standalone","testing")._main(args);
	}
	
}

