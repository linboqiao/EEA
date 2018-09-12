package limo.modules;

import java.io.File;

import limo.exrel.slots.OutputDirSlot;
import limo.core.Corpus;
import limo.exrel.slots.BooleanSlot;
import limo.exrel.slots.InputDirSlot;
import limo.exrel.slots.FileSlot;
import limo.exrel.modules.AbstractModule;
import limo.io.Utils;
import limo.io.ace2004.ACE2004Document;
import limo.io.ace2004.ACE2004Reader;

/**
 * Module to read ACE 2004 files
 * @author Barbara Plank
 *
 */
public class ACE2004ReaderModule extends AbstractModule {

	public InputDirSlot aceDataDir = new InputDirSlot(true);
	public OutputDirSlot plainTextDataDir = new OutputDirSlot(true);
	
	public FileSlot ignoreRelationsFile = new FileSlot(false); //optional
	
	public FileSlot mappingEntitiesFile = new FileSlot(false); //optional
	public FileSlot trainRelationsFile = new FileSlot(false); //optional
	public BooleanSlot mapMentionPREtype = new BooleanSlot("false"); //optional: maps PRE to NAM (if starts with uppercase letter, otherwise NOM)
	
	public ACE2004ReaderModule(String instanceName, String configId) {
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
		
		Corpus corpus = null;
		
		if (ignoreRel != null) {
			message("Ignoring relations from file: %s", ignoreRel.getAbsolutePath());
			corpus = new Corpus(new ACE2004Reader(path, ignoreRel.getAbsolutePath()));
		}
		else if (mappingEntitiesFile.get() != null && trainRelationsFile.get() != null) {
			message("Using entity mapping from file: %s", mappingEntitiesFile.get().getAbsolutePath());
			message("Using train relations from file: %s", trainRelationsFile.get().getAbsolutePath());
			try {
				corpus = new Corpus(new ACE2004Reader(path, mappingEntitiesFile.get().getAbsolutePath(), trainRelationsFile.get().getAbsolutePath()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			corpus = new Corpus(new ACE2004Reader(path));
		}
		
		message("Initializing Corpus.");
		corpus.init();
		message("Corpus initalized.");
		
		
		ACE2004Document doc = (ACE2004Document)corpus.getNextDocument();
		while (doc != null) {
			message("Save tokenized file: %s", doc.getURI());
			doc.saveTokenizedTextAsFile(outDir);
			doc = (ACE2004Document)corpus.getNextDocument();
		}
		
	}

}
