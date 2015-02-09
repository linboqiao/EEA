package limo.modules;

import java.io.File;
import java.io.FileOutputStream;

import limo.exrel.utils.ProcessStreamHandler;

import limo.exrel.slots.ExternalCommandSlot;

import limo.exrel.modules.AbstractModule;
import limo.exrel.slots.InputDirSlot;
import limo.exrel.slots.OutputDirSlot;
import limo.exrel.slots.StringSlot;
import limo.io.Utils;
import limo.io.filter.PlainTextFilter;

/***
 * Module to invoke dependency parser (old name for this was StanfordParserModule)
 * @author Barbara Plank
 * 
 */
public class DependencyParserModule extends AbstractModule {

	public ExternalCommandSlot parserBin = new ExternalCommandSlot(true);
	public InputDirSlot inputDir = new InputDirSlot(true);
	public OutputDirSlot outputDir = new OutputDirSlot(true);
	
	public StringSlot fileEnding = new StringSlot(true);
	
	File binary;
	
	public DependencyParserModule(String instanceName, String configId) {
		super(instanceName,configId);
	}

	@Override
	protected void _run() {
		String inDir = inputDir.get().getAbsolutePath();
		File outDir = outputDir.get();

		if (outDir.exists()) {
			message("Removing content of output dir as it is non-empty.");
			Utils.deleteContentDir(outDir);
		}	
		
		message("InputDir: %s", inDir);
		message("OutputDir: %s", outDir.getAbsolutePath());

		binary = parserBin.get();
		
		File dir = new File(inDir);
		if (dir.isDirectory()) {

			File[] files = dir.listFiles(new PlainTextFilter());
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				message("Parsing file: " + file.getName());
				
				String fileParsedOut = outDir.getAbsolutePath() + File.separator + file.getName() + fileEnding.get();
				String fileParsedErr = outDir.getAbsolutePath() + File.separator + file.getName() + fileEnding.get() + ".err";
				File output = new File(fileParsedOut);
				File error = new File(fileParsedErr);
				try {
					ProcessStreamHandler.handle(
							Runtime.getRuntime().exec(getCommandString(file.getAbsolutePath())),
							new FileOutputStream(output), 
							new FileOutputStream(error)).waitFor();
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
				message("Finished parsing: " + fileParsedOut);
			}

		}
	}

	public String getCommandString(String file) {
		return String.format("%s %s", 
				binary.getAbsolutePath(),
				file);
	}
	
}
