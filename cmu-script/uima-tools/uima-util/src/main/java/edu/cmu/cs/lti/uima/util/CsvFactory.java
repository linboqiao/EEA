package edu.cmu.cs.lti.uima.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * A CSVUtil based on open CSV
 * 
 * @author Zhengzhong Liu, Hector
 * 
 */
public class CsvFactory {
  public static CSVWriter getCSVWriter(String outputPath) {
    File file = new File(outputPath);
    return getCSVWriter(file);
  }

  public static CSVWriter getCSVWriter(String outputPath, char sep) {
    File file = new File(outputPath);
    return getCSVWriter(file);
  }

  public static CSVWriter getCSVWriter(String outputPath, char sep, char escape) {
    File file = new File(outputPath);
    return getCSVWriter(file, sep, escape);
  }

  /**
   * Will overwrite file
   * 
   * @param file
   * @return
   */
  private static FileWriter getFileToWrite(File file) {
    if (file.exists()) {
      file.delete();
    }

    FileWriter fileWriter = null;
    try {
      file.createNewFile();
      fileWriter = new FileWriter(file.getAbsoluteFile(), true);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return fileWriter;
  }

  public static CSVWriter getCSVWriter(File file) {
    return new CSVWriter(getFileToWrite(file));
  }

  public static CSVWriter getCSVWriter(File file, char sep, char escape) {
    return new CSVWriter(getFileToWrite(file), sep, escape);
  }

  public static CSVWriter getCSVWriter(File file, char sep) {
    return new CSVWriter(getFileToWrite(file), sep);
  }

  public static CSVReader getCSVReader(String inputPath) {
    File file = new File(inputPath);
    return getCSVReader(file);
  }

  public static CSVReader getCSVReader(String inputPath, char sep, char escape) {
    File file = new File(inputPath);
    return getCSVReader(file, sep, escape);
  }

  public static CSVReader getCSVReader(File file) {
    CSVReader reader = null;
    try {
      reader = new CSVReader(new FileReader(file));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return reader;
  }

  public static CSVReader getCSVReader(File file, char sep, char escape) {
    CSVReader reader = null;
    try {
      reader = new CSVReader(new FileReader(file), sep, escape);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return reader;
  }

}
