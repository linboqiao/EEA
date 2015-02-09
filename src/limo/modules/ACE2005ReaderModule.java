package limo.modules;


import java.io.File;

import limo.exrel.slots.OutputDirSlot;
import limo.core.Corpus;
import limo.exrel.slots.InputDirSlot;
import limo.exrel.slots.FileSlot;
import limo.exrel.modules.AbstractModule;
import limo.io.Utils;
import limo.io.ace2005.ACE2005Document;
import limo.io.ace2005.ACE2005Reader;

/***
 * Read ACE 2005 data dir
 * @author Barbara Plank
 *
 */
public class ACE2005ReaderModule extends AbstractModule {

	public InputDirSlot aceDataDir = new InputDirSlot(true);
	public OutputDirSlot plainTextDataDir = new OutputDirSlot(true);
	
	public FileSlot ignoreRelationsFile = new FileSlot(false); //optional
	public FileSlot mappingRelationsFile = new FileSlot(false); //optional
	
	public ACE2005ReaderModule(String instanceName, String configId) {
		super(instanceName,configId);
	}
	
	
	@Override
	protected void _run() {
		String path = aceDataDir.get().getAbsolutePath();
		File outDir = plainTextDataDir.get();
		
		if (outDir.exists()) {
			message("Removing content of output dir as it is non-empty.");
			Utils.deleteContentDir(outDir);
		}
		
		File ignoreRel = ignoreRelationsFile.get(); //optional
		
		message("Directory: %s", path);
		message("Output directory: %s", outDir.getAbsolutePath());
		
		Corpus corpus;
		
		if (ignoreRel != null) {
			message("Ignoring relations from file: %s", ignoreRel.getAbsolutePath());
			corpus = new Corpus(new ACE2005Reader(path, ignoreRel.getAbsolutePath()));
		}
		else {
			corpus = new Corpus(new ACE2005Reader(path));
		}
		
		message("Initializing Corpus.");
		corpus.init();
		message("Corpus initalized.");
		
		
		ACE2005Document doc = (ACE2005Document)corpus.getNextDocument();
		while (doc != null) {
			message("Save tokenized file: %s", doc.getURI());
			doc.saveTokenizedTextAsFile(outDir);
			doc = (ACE2005Document)corpus.getNextDocument();
		}
		
	}

}
