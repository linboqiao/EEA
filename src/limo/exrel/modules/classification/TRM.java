/* Updated by Barbara Plank
 *  Copyright (c) 2008 - 2009 , Daniele Pighin - All rights reserved.
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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import limo.exrel.modules.AbstractModule;
import limo.exrel.slots.ExternalCommandSlot;
import limo.exrel.slots.FileSlot;
import limo.exrel.slots.InputDirSlot;
import limo.exrel.slots.IntegerSlot;
import limo.exrel.slots.OutputDirSlot;
import limo.exrel.slots.OutputFileSlot;
import limo.exrel.utils.Index;
import limo.exrel.utils.Index.OpenMode;

import limo.exrel.data.Classification;
import limo.exrel.data.ClassificationScores;

public class TRM extends AbstractModule {
	
	public ExternalCommandSlot svmClassify = new ExternalCommandSlot(true);
	public InputDirSlot modelsDir = new InputDirSlot(true);
	public FileSlot examplesIdxFile = new FileSlot(true);
	public OutputFileSlot outScoresIdxFile = new OutputFileSlot(true);
	public OutputDirSlot svmOutputDir = new OutputDirSlot(false);
	public IntegerSlot numThreads = new IntegerSlot(1);
	public FileSlot mentionTypes = new FileSlot(false); //optional
	
	public TRM(String moduleId, String configId) {
		super(moduleId,configId);
	}
	
	class RelationClassifier extends BinaryClassifier {
		RelationClassifier(String label, String configId) {
			super("RelTRM:" + label, configId);
		}
	}
	
	@Override
	protected void _run() throws Exception {

		HashMap<String, String> predictionsFiles = new HashMap<String, String>();		
		
		File outputDirFile = svmOutputDir.get();
		
		File modelsDirFile = modelsDir.get();
		File[] models = modelsDirFile.listFiles(
				new FilenameFilter() {
					public boolean accept(File f, String name) {
						return name.matches(".*\\.model$");
					}
				}
			);
		
		
		HashMap<String, ArrayList<String>> mentionTypesMap = null;
		
		if (mentionTypes.get() != null) {
			message("**** using mentionTypes from: " + mentionTypes.get());
			mentionTypesMap = readMentionTypesFromFile(mentionTypes.get().getAbsolutePath());
		}
	
		if (models.length == 0) {
			throw new Exception("Models directory is empty: " + modelsDir.get());
		}
		
		String modelName;
		ExecutorService service = Executors.newFixedThreadPool(numThreads.get());
		for (File model : models) {
			modelName = model.getName().replaceFirst("\\.model", "");
			RelationClassifier svmclass = new RelationClassifier(modelName, getConfigId());
			svmclass.svmClassify.set(svmClassify.get());
			svmclass.modelDir.set(modelsDir.get());
			svmclass.examplesIdxFile.set(examplesIdxFile.get());
			svmclass.labelMappingClass.setString(TRMClassifierMapping.class.getName());
			svmclass.targetLabel.set(modelName);
			message("Set label of SVMclass: %s", modelName);
			svmclass.svmOutputDir.set(outputDirFile);
			service.submit(svmclass);		
			try {
				predictionsFiles.put(modelName, svmclass.getPredictionsFile().getAbsolutePath());				
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
		}
		service.shutdown();
		while (!service.isTerminated()) {
			try {
				message("Waiting for all child processes to shutdown");
				service.awaitTermination(5, TimeUnit.SECONDS);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
		}
		
		HashMap<String, BufferedReader> predictionsReaders = new HashMap<String, BufferedReader>();
		for (String name : predictionsFiles.keySet()) {
			try {
				//System.out.println(name);
				predictionsReaders.put(name, new BufferedReader(new FileReader(predictionsFiles.get(name))));
			} catch (FileNotFoundException ex) {
				throw new RuntimeException(ex);
			}
		}
		message("Building output scores index file: %s", outScoresIdxFile.get().getAbsolutePath());
		Index examples = new Index(examplesIdxFile.get(), OpenMode.READ);
		Index scoresIdx = new Index(outScoresIdxFile.get(), OpenMode.WRITE);
		ClassificationScores scores;
		//for (IndexKey exKeys : examples.allKeys()) {
		for (Integer exKeys : examples.allKeys()) {
			scores = new ClassificationScores();
			String exampleLine = examples.get(exKeys);
			String[] temp = exampleLine.split("\t");
			String entityInfo = temp[2];
			//String[] entities = entityInfo.split("@");
			//String e1 = entities[0];
			//String e2 = entities[1];

			for (String label : predictionsReaders.keySet()) {
				String scoreString = "";
				try {
					scoreString = predictionsReaders.get(label).readLine();
					Double scoreValue = Double.parseDouble(scoreString);
					
					// check type for non-null labels
					if (mentionTypesMap != null && !label.equals("NONE")) {
						ArrayList<String> possibleClasses = mentionTypesMap.get(entityInfo);
						if (possibleClasses != null) {
							// check if instance is possible for a given class
							boolean isAllowed = false;
							for (String possLab : possibleClasses) {
								if (label.startsWith(possLab)) {
									//System.out.println("ok!");
									isAllowed=true;
									continue;
								}
							}
							if (!isAllowed) {
								scoreValue=-9999.99;
							}
						}
							
							

							
					}		
					
					
					scores.add(new Classification(label, scoreValue));
				} catch (Exception ex) {
					System.err.println("Problem with: "+scoreString + " (label: "+label+")");
					ex.printStackTrace();
					throw new RuntimeException(ex);
				}
			}
			scoresIdx.put(scores.toString(), exKeys);
		}
		examples.close();
		message("%d lines written to file: %s", scoresIdx.numRecords(), outScoresIdxFile.get().getAbsolutePath());
		scoresIdx.close();		
		
		
		for (String label : predictionsReaders.keySet()) {
			try {
				predictionsReaders.get(label).close();
			} catch (IOException ex) {
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
		}
	}
	
	private HashMap<String, ArrayList<String>> readMentionTypesFromFile(String absolutePath) {
		HashMap<String, ArrayList<String>> map = new HashMap<String,ArrayList<String>>();
		
		try {
			BufferedReader inputReader = new BufferedReader(new FileReader(absolutePath));
			String line = inputReader.readLine();
			while (line != null) {
				if (!line.startsWith("#")) {
					String[] fields = line.split(" ",3);
					String label = fields[0];
					String mention1type = fields[1];
					String mention2type = fields[2];
					//System.out.println(label + " "+ mention1type + " "+mention2type);
					
					String key = mention1type + "@"+mention2type; 
					ArrayList<String> values = map.get(key);
					if (values==null) {
						ArrayList<String> newvalues = new ArrayList<String>();
						newvalues.add(label);
						map.put(key,newvalues);
					} else {
						values.add(label);
					}
				}
				line = inputReader.readLine();
			}
			inputReader.close();
			return map;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}

