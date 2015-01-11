package edu.cmu.cs.lti.script.debug;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class CorrputedXmiChecker {
  public static void main(String args[]) {
    File folder = new File(
            "/Users/zhengzhongliu/Documents/projects/cross-document-script/data/00_xmi_bak");
    File[] listOfFiles = folder.listFiles();

    for (int i = 0; i < listOfFiles.length; i++) {
      try {
        File stocks = listOfFiles[i];
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(stocks);
        doc.getDocumentElement().normalize();
      } catch (Exception e) {
        System.out.println("Error processing " + listOfFiles[i].getName());
      }
    }
  }
}