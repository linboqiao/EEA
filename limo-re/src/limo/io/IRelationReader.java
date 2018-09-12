package limo.io;

import java.io.FilenameFilter;
import java.util.ArrayList;

import limo.core.interfaces.IRelationDocument;

public interface IRelationReader {

	// list of documents
	public ArrayList<IRelationDocument> readDocuments() throws Exception;
	
	// read documents but skip sentences without annotated relation
	public ArrayList<IRelationDocument> readDocuments(boolean skipSentences) throws Exception;
	
	// path to directory (or file)
	public String getPath();
	
	// filter for filenames
	public FilenameFilter getFilenameFilter();

	
	
}


