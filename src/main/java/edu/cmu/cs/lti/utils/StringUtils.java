/**
 * 
 */
package edu.cmu.cs.lti.utils;

/**
 * @author zhengzhongliu
 * 
 */
public class StringUtils {
  public static String text2CsvField(String text) {
    return text.replace(",", ".").replace(":", "_").replace("\n", " ");
  }

}
