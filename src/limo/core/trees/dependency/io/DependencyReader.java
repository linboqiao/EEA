package limo.core.trees.dependency.io;

///////////////////////////////////////////////////////////////////////////////
// Copyright (C) 2007 University of Texas at Austin and (C) 2005
// University of Pennsylvania and Copyright (C) 2002, 2003 University
// of Massachusetts Amherst, Department of Computer Science.
//
// This software is licensed under the terms of the Common Public
// License, Version 1.0 or (at your option) any subsequent version.
// 
// The license is approved by the Open Source Initiative, and is
// available from their website at http://www.opensource.org.
///////////////////////////////////////////////////////////////////////////////


import java.io.*;
import java.util.ArrayList;

import limo.core.trees.dependency.DependencyInstance;

/**
 * A class that defines common behavior and abstract methods for readers for
 * different formats.
 * 
 * <p>
 * Created: Sat Nov 10 15:25:10 2001
 * </p>
 * 
 * @author Jason Baldridge
 * @version $Id: DependencyReader.java 112 2007-03-23 19:19:28Z jasonbaldridge $
 */
public abstract class DependencyReader {

	protected BufferedReader inputReader;
	protected boolean labeled = true;

	public static DependencyReader createDependencyReader(String format,
			boolean nBestMode) throws IOException {

		if (format.equals("CONLL07")) {
			return new CONLL07Reader(nBestMode);
		} else {
			System.out.println("!!!!!!!  Not a supported format: " + format);
			System.out.println("********* Assuming CONLL format. **********");
			return new CONLL07Reader(nBestMode);
		}
	}

	public static DependencyReader createDependencyReader(String format)
			throws IOException {

		return createDependencyReader(format, false);
	}

	public boolean startReading(String file) throws IOException {
		labeled = fileContainsLabels(file);
		inputReader = new BufferedReader(new InputStreamReader(
				new FileInputStream(file), "UTF8"));
		return labeled;
	}

	public boolean isLabeled() {
		return labeled;
	}

	public abstract ArrayList<DependencyInstance> getNext() throws IOException;

	protected abstract boolean fileContainsLabels(String filename)
			throws IOException;

	protected String normalize(String s) {
		//B: replaces numbers in FORM or LEMMA with <num>
		//if (s.matches("[0-9]+|[0-9]+\\.[0-9]+|[0-9]+[0-9,]+"))
		//	return "<num>";
		if (s.contains("("))
			return s.replaceAll("\\(", "-LRB-");
		if (s.contains(")"))
			return s.replaceAll("\\)", "-RRB-");

		return s;
	}
	

	public static DependencyInstance findBest(
			ArrayList<DependencyInstance> parsesForSentence,
			DependencyInstance goldInstance) {
		//find best parse
		DependencyInstance best = parsesForSentence.get(0);
		float maxLas = 0;
		for (DependencyInstance instance : parsesForSentence) {
			float las = evalLAS(instance,goldInstance);
			//System.out.println(las);
			if (las > maxLas) {
				maxLas = las;
				best = instance;
			}
		}
		return best;
	}

		
	public static DependencyInstance findExactMatch(
			ArrayList<DependencyInstance> parsesForSentence,
			DependencyInstance goldInstance) {
		//find best parse
		//DependencyInstance best = parsesForSentence.get(0);
	
		for (DependencyInstance instance : parsesForSentence) {
			if (exactMatch(instance, goldInstance))
				return instance;
			else 
				return null;
		}
		return null;
	}

	private static float evalLAS(DependencyInstance instance,
			DependencyInstance goldInstance) {
		int matches=0;
		for (int i=1; i < instance.forms.length; i++) { //start with 1: don't count <root>
			if (instance.deprels[i].equals(goldInstance.deprels[i]) && 
					instance.heads[i] == goldInstance.heads[i])
				matches++;
				
				
		}
		return (float)matches/instance.forms.length;
	}
	
	private static boolean exactMatch(DependencyInstance instance, DependencyInstance goldInstance) {
		for (int i=1; i < instance.forms.length; i++) { //start with 1: don't count <root>
			if (!instance.deprels[i].equals(goldInstance.deprels[i]) || ! (instance.heads[i] == goldInstance.heads[i]))
				return false;				
		}
		return true;
	}


}
