/*
 * Author: Barbara Plank
 * 
 */
package limo.exrel.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;

public class Index {
	
	public static enum OpenMode {
		READ("r"),
		WRITE("rw"),
		APPEND("rw");
		OpenMode(String mode) {
			_mode = mode;
		}
		String getModeString() {
			return _mode;
		}
		private String _mode;
	}
	
	private String dataFilePath = null;
	//private RandomAccessFile dataFile = null;
	private ArrayList<String> dataFile; //keep data as list of strings
	
	//private LinkedHashMap<IndexKey, String> index = null;
	private LinkedHashMap<Integer, String> index = null;
	private int numKeys = 0;
	private OpenMode mode = null;
	
	public Index(String fileName, OpenMode omode) {
		this(new File(fileName), omode);	
	}
	
	public Index(File file, OpenMode omode) {
		dataFilePath = file.getAbsolutePath();
		dataFile = new ArrayList<String>();
		mode = omode;
		//index = new LinkedHashMap<IndexKey, String>();
		index = new LinkedHashMap<Integer, String>();
		if (!file.exists()) {
			file.getParentFile().mkdirs();			
		}		
		try {
			//dataFile = new RandomAccessFile(dataFilePath, mode.getModeString());
			if (mode == OpenMode.READ || mode == OpenMode.APPEND) {
//				File f = new File(dataFilePath);
//				if (!f.exists()) //append used for quicker writing, but no file there yet 
//					return;
				
	            BufferedReader fin = new BufferedReader(new InputStreamReader(new FileInputStream(dataFilePath),"UTF-8"));
	            
	            String s;
	            while ((s=fin.readLine())!=null) {
	                dataFile.add(s);
	            }
	            fin.close();
	            
				if (dataFile.size() > 0) {
					loadIndex();
				}
			} else {
				
				//dataFile.setLength(0);
			}
		} catch (Exception e) {
			e.printStackTrace();		
		}
	}
		
	private int guessNumKeys() {
		try {
			String first =  dataFile.get(0);
			return first.split("\t").length-1;
		} catch (Exception ex) {
			throw new RuntimeException("Couldn't guess number of keys!");
		}
	}
	
	private Integer readKey(String keyValue) {
		return Integer.parseInt(keyValue.split("\t",2)[0]);
		
	}
	
	private String readValue(String keyValue) {
		return keyValue.split("\t",2)[1];
		
	}
	
/*	private Integer readKey() {
		try {
			StringBuffer buf = new StringBuffer();
			char read;
			while ((read = (char)dataFile.()) != ' ' && read != '\t') {
				buf.append(read);
			}
			return Integer.parseInt(buf.toString());
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}*/
	
/*	private String readValue(int idx) {
		try {
			StringBuffer buf = new StringBuffer();
			char read;
			while ((read = (char)dataFile.readByte()) != '\n') {
				buf.append(read);
			}
			return buf.toString();
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	*/
	private void loadIndex() throws Exception {
		if (mode == OpenMode.WRITE) {
			throw new RuntimeException("Cannot read from index open in WRITE mode");
		}
		//dataFile.seek(0);
		numKeys = guessNumKeys();
		int totalLines = dataFile.size();
		
		//while (dataFile.getFilePointer() != dataFile.length()) {
		for (int idx=0 ; idx < totalLines; idx++) {
			//IndexKey key = new IndexKey(numKeys);
			String keyValue = dataFile.get(idx);
//			for (int i=0; i<numKeys; i++) {
//				key.set(i, readKey(keyValue));
//			}
			int ikey = readKey(keyValue);
			//IndexKey key = new IndexKey(ikey);
			index.put(ikey, readValue(keyValue));
			//readValue(idx);
		}
	}
	
	public void dumpIndex() {
		//for (IndexKey key : index.keySet()) {
		for (Integer key : index.keySet()) {
			System.out.println(key + "==>" + index.get(key));  
		}
	}	
	
	public Set<Integer> allKeys() {
		return index.keySet();
    }
	
//	public KeySet allKeys() {
//		return searchKeys(new IndexKey());
//	}
//	
//	public KeySet searchKeys(Integer... queryKeys) {
//		return searchKeys(new IndexKey(queryKeys));
//	}
	
//	public KeySet searchKeys(IndexKey query) {
//		KeySet result = new KeySet();
//		//for (IndexKey k : index.keySet()) {
//		for (Integer k : index.keySet()) {
//			if (k.matches(query)) {
//				result.add(k);
//			}
//		}
//		return result;
//	}

	
//	public String get(Integer... keyValues) {
//		return get(new IndexKey().append(keyValues));
//	}
	
	public String get(Integer key) {
		String content = index.get(key);
		return content;
//		Long offset = index.get(key);
//		if (offset == null) {
//			return null;
//		}
//		try {
//			dataFile.seek(offset);
//			return readValue();
//		} catch (Exception ex) {
//			ex.printStackTrace();
//			return null;
//		}
		
	}
	
	public void put(String data, Integer... keyValues) {
		put(data, keyValues);
	}
	
	//public void put(String data, IndexKey k) {
	public void put(String data, Integer k) {
		if (mode == OpenMode.READ) {
			throw new RuntimeException("Cannot write from index open in READ mode");
		}
		//System.out.println("Putting: " + k);
		//System.out.println("Index size: " + index.size());
		//System.out.println();
		if (index.containsKey(k)) {			
			throw new RuntimeException(
					String.format(
							"Duplicate record for key: %s",
							k));
		}
		try {
//			if (numKeys < 1) {
//				numKeys = k.size();
//				/*
//				dataFile.seek(0);
//				dataFile.writeBytes(String.valueOf(numKeys));
//				dataFile.writeByte('\n');
//				*/
//			} else {
//				if (numKeys != k.size()) {
//					throw new RuntimeException(
//							String.format(
//									"Different key lengths: %d vs %d",
//									numKeys,
//									k.size()));
//				}
//			}
	/*		dataFile.seek(dataFile.length());
			for (int i=0; i<k.size(); i++) {
				dataFile.writeBytes(String.valueOf(k.get(i)));
				if (i < k.size() - 1) {
					dataFile.writeByte(' ');
				} else {
					dataFile.writeByte('\t');
				}
			}
			index.put(k, dataFile.getFilePointer());
			//dataFile.writeBytes(data);
			System.out.println("DATA: "+data);
			//byte[] utf8Bytes = data.getBytes("UTF8");
			//dataFile.write(utf8Bytes);
			dataFile.writeBytes(data);
			dataFile.writeByte('\n');*/
			
			dataFile.add(data);
			index.put(k, data);
			
			// do the writing only when closing file!!
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void close() {
		try {
			
			if (mode == OpenMode.WRITE || mode == OpenMode.APPEND) {
				OutputStreamWriter writer = new OutputStreamWriter(
		                  new FileOutputStream(dataFilePath, false), "UTF-8");
		            BufferedWriter fbw = new BufferedWriter(writer);
		        int sk=0;
		        for (String s : dataFile) {
		        	 fbw.write(sk +"\t" +s);
		            fbw.newLine();
		            sk++;
		        }
		        fbw.close();
			}
			
			
			//dataFile.close();
			numKeys = 0;
			index.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public int numRecords() {
		return index.size();
	}
	
	public int numKeys() {
		return numKeys;
	}
	
	public static void main(String[] args) {		
		
		Index test = new Index("/tmp/test.idx", OpenMode.APPEND);
		
		System.out.printf("File has %d keys.\n", test.numKeys());
		System.out.printf("File contains %d records.\n", test.numRecords());
		
//		if (test.numRecords() == 0) {
//			System.out.println("Populating...");
//			//create a file with data
//			for (int i=0; i<4; i++) {
//				for (int j=0; j<2; j++) {
//					for (int k=0; k<2; k++) {
//						test.put(
//							String.format("ciao %d:%d:%d", i, j, k), 
//							new IndexKey().append(i, j, k));
//					}
//				}
//			}
//		}
//		
//		System.out.printf("File has %d keys.\n", test.numKeys());
//		System.out.printf("File contains %d records.\n", test.numRecords());
//		
//		System.out.println("Checking retrieval...");
//		
//		System.out.printf("Getting elemnt at key 10:5:5 ==> %s\n", test.get(10, 5, 5));
//		
//		System.out.printf("Getting elemnt at key 20:8:9 ==> %s\n", test.get(20, 8, 9));
//		
//		System.out.println("Checking search...");		
//		System.out.println();
//		KeySet result;
//		System.out.println("Searching for: null, null, 5");
//		result = test.searchKeys(null, null, 5);
//		System.out.printf("Results %d\n", result.size());
//		for (IndexKey rec : result) {
//			System.out.println(test.get(rec));
//		}
//		System.out.println();
//		System.out.println("Searching for: null, 5, 5");
//		result = test.searchKeys(null, 5, 5);
//		System.out.printf("Results %d\n", result.size());
//		for (IndexKey rec : result) {
//			System.out.println(test.get(rec));
//		}
//		System.out.println();
//		System.out.println("Searching for: 5, null, 5");
//		result = test.searchKeys(5, null, 5);
//		System.out.printf("Results %d\n", result.size());
//		for (IndexKey rec : result) {
//			System.out.println(test.get(rec));
//		}
//		System.out.println();
//		System.out.println("Searching for: 5, 5, 5");
//		result = test.searchKeys(5, 5, 5);
//		System.out.printf("Results %d\n", result.size());
//		for (IndexKey rec : result) {
//			System.out.println(test.get(rec));
//		}
//		System.out.println();
//		System.out.println("Searching for: null, 1000, 1000");
//		result = test.searchKeys(null, 1000, 1000);
//		System.out.printf("Results %d\n", result.size());
//		for (IndexKey rec : result) {
//			System.out.println(test.get(rec));
//		}
//		
//		System.out.println();
//		System.out.println("Fetching all records");
//		result = test.allKeys();
//		System.out.printf("Results %d\n", result.size());
//		for (IndexKey rec : result) {
//			System.out.println(test.get(rec));
//		}
//		
//		System.out.println("Adding duplicate key (should trigger exception).");
//
////		try {
////			test.put("ciao", 4, 5, 5);
////		} catch (RuntimeException ex) {
////			System.out.println("Great! Extecption triggered!!");
////			ex.printStackTrace();
////		}
//		
//		test.put("ciao", 1,1,1001);
//		test.put("Frontières",1,1,1002);
//		
//		System.out.println(test.get(1,1,1002));
//		
//		System.out.println("Testing key operations");
//		IndexKey key = new IndexKey();
//		System.out.println(key);
//		key = key.append(10, 50, 67);
//		System.out.println(key);
//		key = key.append(20, 40, 898);
//		System.out.println(key);
//		key = key.subset(0, 5);
//		System.out.println(key);
//		key = key.subset(2,2).append(9, 8, 7);
//		System.out.println(key);
	}
	
}


///*
// * Copyright (c) 2008 - 2009 , Daniele Pighin - All rights reserved.
// * 
// * This software is released under a double licensing scheme.
// * 
// * For personal or research uses, the software is available under the
// * GNU Lesser GPL (LGPL) v.3 license. 
// * 
// * See the file LICENSE in the source distribution for more details.
// * 
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
// * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
// * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
// * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
// * OTHER DEALINGS IN THE SOFTWARE.
// */
//package limo.exrel.utils;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.RandomAccessFile;
//import java.util.LinkedHashMap;
//
//public class Index {
//	
//	public static enum OpenMode {
//		READ("r"),
//		WRITE("rw"),
//		APPEND("rw");
//		OpenMode(String mode) {
//			_mode = mode;
//		}
//		String getModeString() {
//			return _mode;
//		}
//		private String _mode;
//	}
//	
//	private String dataFilePath = null;
//	private RandomAccessFile dataFile = null;
//	
//	private LinkedHashMap<IndexKey, Long> index = null;
//	private int numKeys = 0;
//	private OpenMode mode = null;
//	
//	public Index(String fileName, OpenMode omode) {
//		this(new File(fileName), omode);	
//	}
//	
//	public Index(File file, OpenMode omode) {
//		dataFilePath = file.getAbsolutePath();
//		mode = omode;
//		index = new LinkedHashMap<IndexKey, Long>();
//		if (!file.exists()) {
//			file.getParentFile().mkdirs();			
//		}		
//		try {
//			dataFile = new RandomAccessFile(dataFilePath, mode.getModeString());
//			if (mode == OpenMode.READ || mode == OpenMode.APPEND) {
//				if (dataFile.length() > 0) {
//					loadIndex();
//				}
//			} else {
//				dataFile.setLength(0);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();		
//		}
//	}
//		
//	private int guessNumKeys() {
//		try {
//			int count = 0;
//			char read;
//				
//			while ((read = (char)dataFile.readByte()) != '\t') {
//				if (read == ' ') {
//					count++;
//				}
//			}
//			dataFile.seek(0);
//			return ++count;
//		} catch (Exception ex) {
//			throw new RuntimeException("Couldn't guess number of keys!");
//		}
//	}
//	
//	private Integer readKey() {
//		try {
//			StringBuffer buf = new StringBuffer();
//			char read;
//			while ((read = (char)dataFile.readByte()) != ' ' && read != '\t') {
//				buf.append(read);
//			}
//			return Integer.parseInt(buf.toString());
//		} catch (Exception ex) {
//			ex.printStackTrace();
//			return null;
//		}
//	}
//	
//	private String readValue() {
//		try {
//			StringBuffer buf = new StringBuffer();
//			char read;
//			while ((read = (char)dataFile.readByte()) != '\n') {
//				buf.append(read);
//			}
//			return buf.toString();
//		} catch (Exception ex) {
//			ex.printStackTrace();
//			return null;
//		}
//	}
//	
//	private void loadIndex() throws Exception {
//		if (mode == OpenMode.WRITE) {
//			throw new RuntimeException("Cannot read from index open in WRITE mode");
//		}
//		dataFile.seek(0);
//		numKeys = guessNumKeys();		
//		while (dataFile.getFilePointer() != dataFile.length()) {
//			IndexKey key = new IndexKey(numKeys);
//			for (int i=0; i<numKeys; i++) {
//				key.set(i, readKey());
//			}
//			index.put(key, dataFile.getFilePointer());
//			readValue();
//		}
//	}
//	
//	public void dumpIndex() {
//		for (IndexKey key : index.keySet()) {
//			System.out.println(key + "==>" + index.get(key));  
//		}
//	}	
//	
//	public KeySet allKeys() {
//		return searchKeys(new IndexKey());
//	}
//	
//	public KeySet searchKeys(Integer... queryKeys) {
//		return searchKeys(new IndexKey(queryKeys));
//	}
//	
//	public KeySet searchKeys(IndexKey query) {
//		KeySet result = new KeySet();
//		for (IndexKey k : index.keySet()) {
//			if (k.matches(query)) {
//				result.add(k);
//			}
//		}
//		return result;
//	}
//
//	
//	public String get(Integer... keyValues) {
//		return get(new IndexKey().append(keyValues));
//	}
//	
//	public String get(IndexKey key) {
//		Long offset = index.get(key);
//		if (offset == null) {
//			return null;
//		}
//		try {
//			dataFile.seek(offset);
//			return readValue();
//		} catch (Exception ex) {
//			ex.printStackTrace();
//			return null;
//		}
//		
//	}
//	
//	public void put(String data, Integer... keyValues) {
//		put(data, new IndexKey().append(keyValues));
//	}
//	
//	public void put(String data, IndexKey k) {
//		if (mode == OpenMode.READ) {
//			throw new RuntimeException("Cannot write from index open in READ mode");
//		}
//		//System.out.println("Putting: " + k);
//		//System.out.println("Index size: " + index.size());
//		//System.out.println();
//		if (index.containsKey(k)) {			
//			throw new RuntimeException(
//					String.format(
//							"Duplicate record for key: %s",
//							k));
//		}
//		try {
//			if (numKeys < 1) {
//				numKeys = k.size();
//				/*
//				dataFile.seek(0);
//				dataFile.writeBytes(String.valueOf(numKeys));
//				dataFile.writeByte('\n');
//				*/
//			} else {
//				if (numKeys != k.size()) {
//					throw new RuntimeException(
//							String.format(
//									"Different key lengths: %d vs %d",
//									numKeys,
//									k.size()));
//				}
//			}
//			dataFile.seek(dataFile.length());
//			for (int i=0; i<k.size(); i++) {
//				dataFile.writeBytes(String.valueOf(k.get(i)));
//				if (i < k.size() - 1) {
//					dataFile.writeByte(' ');
//				} else {
//					dataFile.writeByte('\t');
//				}
//			}
//			index.put(k, dataFile.getFilePointer());
//			//dataFile.writeBytes(data);
//			System.out.println("DATA: "+data);
//			//byte[] utf8Bytes = data.getBytes("UTF8");
//			//dataFile.write(utf8Bytes);
//			dataFile.writeBytes(data);
//			dataFile.writeByte('\n');
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//	
//	public void close() {
//		try {
//			dataFile.close();
//			numKeys = 0;
//			index.clear();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//	
//	public int numRecords() {
//		return index.size();
//	}
//	
//	public int numKeys() {
//		return numKeys;
//	}
//	
//	public static void main(String[] args) {		
//		
//		Index test = new Index("/tmp/test.idx", OpenMode.APPEND);
//		
//		System.out.printf("File has %d keys.\n", test.numKeys());
//		System.out.printf("File contains %d records.\n", test.numRecords());
//		
//		if (test.numRecords() == 0) {
//			System.out.println("Populating...");
//			//create a file with data
//			for (int i=0; i<4; i++) {
//				for (int j=0; j<2; j++) {
//					for (int k=0; k<2; k++) {
//						test.put(
//							String.format("ciao %d:%d:%d", i, j, k), 
//							new IndexKey().append(i, j, k));
//					}
//				}
//			}
//		}
//		
//		System.out.printf("File has %d keys.\n", test.numKeys());
//		System.out.printf("File contains %d records.\n", test.numRecords());
//		
//		System.out.println("Checking retrieval...");
//		
//		System.out.printf("Getting elemnt at key 10:5:5 ==> %s\n", test.get(10, 5, 5));
//		
//		System.out.printf("Getting elemnt at key 20:8:9 ==> %s\n", test.get(20, 8, 9));
//		
//		System.out.println("Checking search...");		
//		System.out.println();
//		KeySet result;
//		System.out.println("Searching for: null, null, 5");
//		result = test.searchKeys(null, null, 5);
//		System.out.printf("Results %d\n", result.size());
//		for (IndexKey rec : result) {
//			System.out.println(test.get(rec));
//		}
//		System.out.println();
//		System.out.println("Searching for: null, 5, 5");
//		result = test.searchKeys(null, 5, 5);
//		System.out.printf("Results %d\n", result.size());
//		for (IndexKey rec : result) {
//			System.out.println(test.get(rec));
//		}
//		System.out.println();
//		System.out.println("Searching for: 5, null, 5");
//		result = test.searchKeys(5, null, 5);
//		System.out.printf("Results %d\n", result.size());
//		for (IndexKey rec : result) {
//			System.out.println(test.get(rec));
//		}
//		System.out.println();
//		System.out.println("Searching for: 5, 5, 5");
//		result = test.searchKeys(5, 5, 5);
//		System.out.printf("Results %d\n", result.size());
//		for (IndexKey rec : result) {
//			System.out.println(test.get(rec));
//		}
//		System.out.println();
//		System.out.println("Searching for: null, 1000, 1000");
//		result = test.searchKeys(null, 1000, 1000);
//		System.out.printf("Results %d\n", result.size());
//		for (IndexKey rec : result) {
//			System.out.println(test.get(rec));
//		}
//		
//		System.out.println();
//		System.out.println("Fetching all records");
//		result = test.allKeys();
//		System.out.printf("Results %d\n", result.size());
//		for (IndexKey rec : result) {
//			System.out.println(test.get(rec));
//		}
//		
//		System.out.println("Adding duplicate key (should trigger exception).");
//
////		try {
////			test.put("ciao", 4, 5, 5);
////		} catch (RuntimeException ex) {
////			System.out.println("Great! Extecption triggered!!");
////			ex.printStackTrace();
////		}
//		
//		test.put("ciao", 1,1,1001);
//		test.put("Frontières",1,1,1002);
//		
//		System.out.println(test.get(1,1,1002));
//		
//		System.out.println("Testing key operations");
//		IndexKey key = new IndexKey();
//		System.out.println(key);
//		key = key.append(10, 50, 67);
//		System.out.println(key);
//		key = key.append(20, 40, 898);
//		System.out.println(key);
//		key = key.subset(0, 5);
//		System.out.println(key);
//		key = key.subset(2,2).append(9, 8, 7);
//		System.out.println(key);
//	}
//	
//}
