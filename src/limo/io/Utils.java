package limo.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Utils {

	public static void deleteContentDir(File dir) {
		String[] children = dir.list();
		for (int i = 0; i < children.length; i++) {
			File f = new File(dir, children[i]);
			f.delete();
		}
	}
	
	public static ArrayList<String> readIgnoreFile(String ignoreRelations) {
		BufferedReader inputReader;
		ArrayList<String> relationsToIgnoreIds = null;
		try {
			inputReader = new BufferedReader(new FileReader(ignoreRelations));
			relationsToIgnoreIds = new ArrayList<String>();
			String line = inputReader.readLine();
			while (line != null && !line.equals("")) {
				relationsToIgnoreIds.add(line);
				line = inputReader.readLine();
			}
			inputReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return relationsToIgnoreIds;
	}

	/***
	 * Read entity mapping file
	 * returns HashMap where key is old entity type (like ACE) and value is the new entity type (like Stanford NER)
	 * @param entityMappingFile
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("resource")
	public static HashMap<String,String> readEntityMappingFile(String entityMappingFile) throws Exception {
		BufferedReader inputReader;
		HashMap<String,String> mapping = new HashMap<String,String>();
		try {
			inputReader = new BufferedReader(new FileReader(entityMappingFile));
			String line = inputReader.readLine();
			while (line != null && !line.equals("")) {
				String[] fields  = line.trim().split("\t");
				if (fields.length != 2)
					throw new Exception("entityMappingFile is not in correct format! ");
				String origin = fields[0]; // ACE entity type
				String target = fields[1]; // Stanford NER types
				mapping.put(origin, target);
				line = inputReader.readLine();
			}
			inputReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return mapping;
	}
	
	public static HashMap<String,String> readRelationMapping(String pathToRelationMappingFile) {
		BufferedReader inputReader;
		HashMap<String,String> relationMapping = new HashMap<String,String>();
		try {
			inputReader = new BufferedReader(new FileReader(pathToRelationMappingFile));
			String line = inputReader.readLine();
			while (line != null) {
				if (!line.startsWith("#") && !line.equals("")) {
					String[] fields = line.split(" ");
					relationMapping.put(fields[0], fields[1]);
					line = inputReader.readLine();
				}
				else 
					line = inputReader.readLine();
			}
			inputReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return relationMapping;
	}

}
