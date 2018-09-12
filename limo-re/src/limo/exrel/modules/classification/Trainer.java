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

import java.io.File;

import limo.exrel.modules.AbstractModule;
import limo.exrel.slots.ClassSlot;
import limo.exrel.slots.ExternalCommandSlot;
import limo.exrel.slots.FileSlot;
import limo.exrel.slots.LabelMapping;
import limo.exrel.slots.OutputDirSlot;
import limo.exrel.slots.StringSlot;


public class Trainer extends AbstractModule {
	
	public ExternalCommandSlot svmLearn = new ExternalCommandSlot(true);
	public FileSlot examplesIdxFile = new FileSlot(true);
	public ClassSlot<LabelMapping> labelMappingClass = new ClassSlot<LabelMapping>(true);
	public StringSlot targetLabel = new StringSlot(true);
	public StringSlot svmLearnParameters = new StringSlot(true);
	public OutputDirSlot svmOutputDir = new OutputDirSlot(true);
	
	
	public Trainer(String moduleId, String configId) {
		super(moduleId,configId);
	}
		
	public File getModelFile() {
		return new File(svmOutputDir.get().getAbsolutePath() + File.separator + 
				targetLabel.get() + ".model");
	}
	
	public File getExamplesFile() {
		return new File(svmOutputDir.get().getAbsolutePath() + File.separator + 
				targetLabel.get() + ".examples.txt");
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

		File model = getModelFile();
		File outDir = svmOutputDir.get();
		
		File examples = getExamplesFile();
		File output = getStdOutFile();
		File error = getStdErrFile();
					
		message("Output dir: %s", outDir.getAbsolutePath());
				
		SVMInputGenerator igen = new SVMInputGenerator(targetLabel.get(), getConfigId());
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
		
		SVMLearn learn = new SVMLearn(svmLearn.get());
		learn.setParameters(svmLearnParameters.get(), examples, model, output, error);
		message("Running svm_learn from: %s", svmLearn.get().getAbsolutePath());
		message("svm_learn parameters: %s", svmLearnParameters.get());
		message("Input examples file: %s", examples.getAbsolutePath());
		message("Output model file: %s", model.getAbsolutePath());
		message("Standard output file: %s", getStdOutFile().getAbsolutePath());
		message("Standard error file: %s", getStdErrFile().getAbsolutePath());
		learn.run();
	}

	public static void main(String[] args) {
		new Trainer("standalone","training")._main(args);
	}
	
}

