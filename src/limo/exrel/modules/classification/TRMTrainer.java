/* Updated by Barbara Plank
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
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import limo.exrel.modules.AbstractModule;
import limo.exrel.slots.BooleanSlot;
import limo.exrel.slots.ExternalCommandSlot;
import limo.exrel.slots.ExternalFileSlot;
import limo.exrel.slots.FileSlot;
import limo.exrel.slots.IntegerSlot;
import limo.exrel.slots.OutputDirSlot;
import limo.exrel.slots.StringSlot;

public class TRMTrainer extends AbstractModule {
	
	public ExternalCommandSlot svmLearn = new ExternalCommandSlot(true);
	public StringSlot svmLearnParameters = new StringSlot(true);
	public FileSlot examplesIdxFile = new FileSlot(true);
	public OutputDirSlot svmOutputDir = new OutputDirSlot(true);
	//public ExternalFileSlot labelsFile = new ExternalFileSlot(true);
	public IntegerSlot numThreads = new IntegerSlot(1);
	public ExternalFileSlot paramsFile = new ExternalFileSlot(false); //optional
	
	public BooleanSlot trainNone = new BooleanSlot("false"); //default is not to include a NONE classifier
	public BooleanSlot calculateJ = new BooleanSlot("true");
	
	public TRMTrainer(String moduleId, String configId) {
		super(moduleId, configId);
	}
	
	
	class RelationTrainer extends Trainer {
		RelationTrainer(String label, String configId) {
			super("Rel:" + label,configId);
		}
	}
	
	@Override
	protected void _run() {
		
		HashMap<String, String> labels = new HashMap<String,String>();
		
		int totalInstances = 0;
		HashMap<String, Integer> countPositivePerLabel = new HashMap<String,Integer>();
		
		try {
			//reading labels from idx file
			BufferedReader labelsReader = new BufferedReader(new FileReader(examplesIdxFile.get()));
			String line;
			message("Reading output labels from file: %s", examplesIdxFile.get());
			while ((line = labelsReader.readLine()) != null) {
				String data[] = line.split("\t",3);
				String label = data[1];
				
				if (trainNone.get() == false) { 
					if (!label.startsWith("NONE")) {
						String svmparameters = svmLearnParameters.get();
						labels.put(label, svmparameters);
					}
				}
				else {
					String svmparameters = svmLearnParameters.get();
					labels.put(label, svmparameters);
				}
				//count positive
				if (countPositivePerLabel.containsKey(label)) {
					int newvalue = countPositivePerLabel.get(label)+1;
					countPositivePerLabel.put(label, newvalue);
				} else {
					countPositivePerLabel.put(label, 1);
				}
				totalInstances++;
			}
			labelsReader.close();
			
			for (String key : countPositivePerLabel.keySet()) {
				int countPos = countPositivePerLabel.get(key);
				int negatives = totalInstances-countPos;
				message("Label: %s Positive: %s Negative %s", key,countPos,negatives);
							
				if (calculateJ.get() == true) {
					double j_parameter = (double) negatives / countPos; //calculate j as inverse of the imbalance ratio
					//int j_parameter = (int) negatives / countPos; //calculate j as inverse of the imbalance ratio (take integer)
					if (j_parameter > 1.0) 
						j_parameter = (int)j_parameter; //use integer only for those above 1
					
					String svmparameters = " -j "+j_parameter;
					if (j_parameter > 0.0) {
						String params = labels.get(key);
						if (params != null) {
							params = params + svmparameters; // concatenate
							labels.remove(key);
							labels.put(key, params);
						} else {
							System.err.println("An error occured. Please check:");
							System.err.println(key);
						}
					}
				}  
				
			}
			
			
			// if parameter file given, override settings
			if (paramsFile.get() != null) { 
				message("Reading parameters per classifier");
				BufferedReader paramsReader = new BufferedReader(new FileReader(paramsFile.get()));
				line = "";
				message("Reading parameters from file: %s", paramsFile.get());
				while ((line = paramsReader.readLine()) != null && line != "") {
					if (line.contains(":")) {
						String data[] = line.split(":", 2);
						String label = data[0];
						String svmparameters = data[1];
						message("Using label: %s with svmlearn parameters: %s", label, svmparameters);
	
						String params = labels.get(label);
						if (params != null) {
							params = updateParams(params,svmparameters);
							//params = params + svmparameters; // concatenate
							labels.remove(label);
							labels.put(label, params);
						} else {
							
							System.err.println("An error occured. Please check the parameters file ("+paramsFile.get()+");");
							System.err.println(label);
							//System.exit(-1);
						}
					}
					
					
				}
				paramsReader.close();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		
		ExecutorService service = Executors.newFixedThreadPool(numThreads.get());
		for (String label : labels.keySet()) {
			// don't train NONE 
		    //if (!label.startsWith("NONE")) {
				RelationTrainer trainer = new RelationTrainer(label,getConfigId());
				trainer.svmLearn.set(svmLearn);
				trainer.svmLearnParameters.set(labels.get(label));
				//trainer.svmLearnParameters.set(svmLearnParameters.get());
				trainer.examplesIdxFile.set(examplesIdxFile);
				trainer.labelMappingClass.setString(TRMTrainerMapping.class
						.getName());
				trainer.targetLabel.set(label);
				trainer.svmOutputDir.set(svmOutputDir);
				service.submit(trainer);
			//}
		}
		service.shutdown();
		while (!service.isTerminated()) {
			try {
				message("Waiting for all child processes to shutdown");
				service.awaitTermination(4, TimeUnit.SECONDS);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
		}
		
	}

	// check parameters, if svmparameters overrides use those
	private static String updateParams(String params, String svmparameters) {
		String[] newParams = svmparameters.trim().split("\\s+");
		boolean override = false;
		for (int i=0; i < newParams.length; i+=2) { // just look at every second
			if (params.contains(newParams[i])) 
				override = true;
		}
		if (override == false)
			return params + " " +svmparameters;
		else {
			String[] p = params.trim().split("\\s+");
			StringBuilder out = new StringBuilder();
			for (int i=0; i < p.length; i+=2) {
				if (svmparameters.contains(p[i])) {
					for (int j=0; j<newParams.length;j+=2) {
						if (p[i].equals(newParams[j]) && !out.toString().contains(newParams[j])) {
							//update parameter
							String newValue = newParams[j+1];
							out.append(p[i] + " " +newValue + " ");
						}
						if (!params.contains(newParams[j]) && !out.toString().contains(newParams[j])) {
							//append new parameter
							out.append(newParams[j] + " " + newParams[j+1]+ " ");
						}
					}
					
				} else {
				    out.append(p[i] + " "+ p[i+1] + " ");
				}
			}
			return out.toString();
		}
	}

	public static void main(String[] args) {
		System.out.println(TRMTrainer.updateParams("-t 5 -F 1 -L 0.4 -c 2.4 -m 2000", "-j 20"));
		System.out.println(TRMTrainer.updateParams("-t 5 -F 1 -L 0.4 -c 2.4 -m 2000 -j 0.111", "-j 20"));
		System.out.println(TRMTrainer.updateParams("-t 5 -F 1    -j 0.99999 -L 0.4 -c 2.4 -m 2000 -j 0.111", "-L 0.1 -j 22.2222"));
		System.out.println(TRMTrainer.updateParams("-t 5 -F 1    -j 0.99999 -L 0.4 -c 2.4 -m 2000 -j 0.111", "  -t 4 -U 1 -L 0.999 "));
		
	}
}

