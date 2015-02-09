package limo.modules;

import java.io.File;

import limo.core.Corpus;
import limo.exrel.modules.AbstractModule;
import limo.exrel.slots.InputDirSlot;
import limo.exrel.slots.OutputDirSlot;
import limo.io.Utils;
import limo.io.ry.RothYihConll2004Document;
import limo.io.ry.RothYihConll2004Reader;

/**
 * Module to read ACE 2004 files
 * @author Barbara Plank
 *
 */
public class RYRelationsReaderModule extends AbstractModule {

	public InputDirSlot ryDataDir = new InputDirSlot(true);
	public OutputDirSlot plainTextDataDir = new OutputDirSlot(true);
	
	public RYRelationsReaderModule(String instanceName, String configId) {
		super(instanceName,configId);
	}
	
	
	@Override
	protected void _run() {
		String path = ryDataDir.get().getAbsolutePath();
		File outDir = plainTextDataDir.get();
		
		if (outDir.exists()) {
			message("Removing content of output dir as it is non-empty.");
			Utils.deleteContentDir(outDir);
		}
		
		message("Directory: %s", path);
		message("Output directory: %s", outDir.getAbsolutePath());
		
		Corpus corpus = null;
		
		corpus = new Corpus(new RothYihConll2004Reader(path));
		
		message("Initializing Corpus.");
		corpus.init();
		message("Corpus initalized.");
		
		
		RothYihConll2004Document doc = (RothYihConll2004Document)corpus.getNextDocument();
		while (doc != null) {
			message("Save tokenized file: %s", doc.getURI());
			doc.saveTokenizedTextAsFile(outDir);
			doc = (RothYihConll2004Document)corpus.getNextDocument();
		}
		
	}

}
