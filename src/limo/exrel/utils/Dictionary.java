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
package limo.exrel.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;

import limo.exrel.features.re.linear.RelationExtractionLinearFeature;

public class Dictionary implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private static final String realFeatureValue = "::REAL::";
	private HashMap<String, HashMap<String, Integer>> features;
	private int currentIndex = 0;
	private File dictionaryFile = null;
	private boolean loaded = false;
	
	private Dictionary(File file) {
		features = new HashMap<String, HashMap<String, Integer>>();
		dictionaryFile = file;
		Logging.message(this, "Initializing from: %s", dictionaryFile.getAbsolutePath());	
		System.err.println("Initializing from: %s"+ dictionaryFile.getAbsolutePath()  + " object: "+dictionaryFile);
	}
	
	private synchronized void load() {
		if (!dictionaryFile.exists()) {
			Logging.message(this, "A new dictionary file will be created: " + dictionaryFile.getAbsolutePath());
			return;
		}
		Logging.message(this, "Loading dictionary from file: %s", dictionaryFile.getAbsolutePath());	
		Dictionary dict = deserialize(dictionaryFile);
		this.features = dict.features;
		this.currentIndex = dict.currentIndex;
		Logging.message(this, "File size is: %d bytes", dictionaryFile.length());
		Logging.message(this, "Unique indices in file: %d", currentIndex);
	}
	
	private static Dictionary deserialize(File fname) {
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fname));
			Dictionary dict = (Dictionary)ois.readObject();
			ois.close();
			return dict;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
			
	public int getIndex(RelationExtractionLinearFeature feature, String value) {
		synchronized (features) {
			if (!loaded) {
				load();
				loaded = true;
			}
			if (value == null) {
				throw new RuntimeException("Requesting index for null valued feature " + feature.getClass().getName());
			}
			String featureName = feature.getName();
			if (feature.isBOWF())
				featureName = featureName+value;
			
			if (features.get(featureName) != null) {
				if (feature.isReal()) {
					return features.get(featureName).get(realFeatureValue);
				} else {
					Integer id = features.get(featureName).get(value);
					if (id != null) {
						return id;
					} else {
						features.get(featureName).put(value, ++currentIndex);
						return currentIndex;
					}
				}
			} else {
				features.put(featureName, new HashMap<String, Integer>());
				if (feature.isBoolean()) {
					features.get(featureName).put(value, ++currentIndex);
					return currentIndex;
				} else {
					features.get(featureName).put(realFeatureValue, ++currentIndex);
					return currentIndex;
				}		
			}
		}
	}
	
	public synchronized void save() {
		if (dictionaryFile.exists()) {
			Logging.message(this,"Dictionary already saved in: "+dictionaryFile.getAbsolutePath());
			return;
		}
		
		
		if (!dictionaryFile.getParentFile().exists()) {
			dictionaryFile.getParentFile().mkdirs();
		}
		try {
			Logging.message(this, "Saving dictionary to file: %s", dictionaryFile.getAbsolutePath());						
			new ObjectOutputStream(new FileOutputStream(dictionaryFile)).writeObject(this);
			Logging.message(this, "File size is: %d bytes", dictionaryFile.length());
			Logging.message(this, "Unique indices in file: %d", currentIndex);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
	public synchronized void unload() {
		Logging.message(this, "Unloading dictionary data");
		features.clear();
		currentIndex = 0;
		loaded = false;
	}
	
	public static void dump(File file) throws IOException {
		Dictionary dict = Dictionary.deserialize(file);
		dict.load();
		for (String feat : dict.features.keySet()) {
			for (String val : dict.features.get(feat).keySet()) {
				System.out.print(feat);
				System.out.print(" ");
				System.out.print(val);
				System.out.print(" ");
				System.out.print(dict.features.get(feat).get(val));
				System.out.print("\n");
			}
		}	
		System.out.flush();
	}
	
	public static void main(String[] args) {
		try {
			Dictionary.dump(new File(args[0]));
		} catch (Exception ex) {			
			System.err.println("Usage: Dictionary <dictionaryfile>");
			System.err.println("Reported exception:");
			ex.printStackTrace();
		}
	}
	
	
	
	public static Dictionary factory(File file) {
		URI normalized = Path.toNormalizedURI(file);
		Dictionary result = dictionaries.get(normalized);
		if (result != null) {
			Logging.message(Dictionary.class, "Dictionary from file %s already cached", file.getAbsolutePath());
			return result;
		} else {
			result = new Dictionary(file);
			dictionaries.put(normalized, result);
			return result;
		}
	}
	private static HashMap<URI, Dictionary> dictionaries = new HashMap<URI, Dictionary>();
	
	
}
