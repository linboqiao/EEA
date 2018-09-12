package limo.exrel.modules.mallet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

import limo.exrel.modules.mallet.MalletMaxEnt.MMETrainer;
import limo.exrel.modules.mallet.MalletMaxEnt.MMaxEntModel;

import limo.exrel.modules.AbstractModule;
import limo.exrel.slots.DoubleSlot;
import limo.exrel.slots.FileSlot;
import limo.exrel.slots.OutputDirSlot;

public class FetMaxEntTrainer extends AbstractModule {
	
	public FileSlot examplesIdxFile = new FileSlot(true);
	public OutputDirSlot maxEntOutputDir = new OutputDirSlot(true);
	public DoubleSlot regularizationFactor = new DoubleSlot(1.0);
	
	public FetMaxEntTrainer(String moduleId, String configId) {
		super(moduleId, configId);
	}
	
	public File getDetectorDataFile() {
		return new File(maxEntOutputDir.get().getAbsolutePath() + File.separator +
				"detector.data");
	}
	
	public File getClassifierDataFile() {
		return new File(maxEntOutputDir.get().getAbsolutePath() + File.separator +
				"classifier.data");
	}
	
	public File getDetectorModelFile() {
		return new File(maxEntOutputDir.get().getAbsolutePath() + File.separator +
				"detector.model");
	}
	
	public File getClassifierModelFile() {
		return new File(maxEntOutputDir.get().getAbsolutePath() + File.separator +
				"classifier.model");
	}
	
	@Override
	protected void _run() {
		
		try {
			message("Regularization Factor = " + regularizationFactor.get());
			generateData();
			
			message("Training Detection Model with MaxEnt ...");
			MMaxEntModel md = new MMaxEntModel(getDetectorDataFile().getAbsolutePath(), getDetectorModelFile().getAbsolutePath());
			md.setIterations(600);
			md.buildModel(regularizationFactor.get().doubleValue(), MMETrainer.L2, false, 1.0);
			message("Saving Detection Model ...");
			md.saveModel();
			
			message("Training Classification Model with MaxEnt ...");
			MMaxEntModel mc = new MMaxEntModel(getClassifierDataFile().getAbsolutePath(), getClassifierModelFile().getAbsolutePath());
			mc.setIterations(600);
			mc.buildModel(regularizationFactor.get().doubleValue(), MMETrainer.L2, false, 1.0);
			message("Saving Classification Model ...");
			mc.saveModel();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
	private void generateData() {
		try {
			message("Generating training data for MaxEnt trainer ...");
			BufferedReader reader = new BufferedReader(new FileReader(examplesIdxFile.get()));
			PrintWriter detecterPrinter = new PrintWriter(new FileWriter(getDetectorDataFile()));
			PrintWriter classifierPrinter = new PrintWriter(new FileWriter(getClassifierDataFile()));
			
			String line = "", id = "", labelD = "", labelC = "", features = "", qid = "";
			String[] data = null;
			while ((line = reader.readLine()) != null) {
				data = line.split("\t", 4);
				qid = data[2].substring(0, data[2].indexOf(" "));
				features = data[2].substring(data[2].indexOf(" ")+1);
				id = data[0] + "-" + qid;
				
				labelC = data[1];
				labelD = data[1].equals("NONE") ? "0" : "1";
				
				detecterPrinter.println(id + "\t" + labelD + "\t" + features);
				
				if (!labelC.equals("NONE"))
					classifierPrinter.println(id + "\t" + labelC + "\t" + features);
			}
			
			reader.close();
			detecterPrinter.close();
			classifierPrinter.close();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}

}
