/*
 * Copyright (c) 2008 - 2009 , Daniele Pighin - All rights reserved.
 *
 * From ExRel0.9 software
 * Imported 2011, by Barbara Plank
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
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


public class XMLConfig {
	
	private InputSource _inputSource = null;
	private Document _document = null;
	private HashMap<String, String> _variables; 
	private static final XPath _xpath = XPathFactory.newInstance().newXPath();
	private static final Pattern _xpathExpression = Pattern.compile("^(.*?)\\$\\{(.+?)\\}(.*)$");
	private static final Pattern _varExpression = Pattern.compile("^(.*?)@\\{(.+?)\\}(.*)$");
	
	// if we use temp output dir
	private File tmpDir = null;
	private boolean useTempOutputDir = false; //default case
	private boolean isMainExrelConfig = false; 
	
	public XMLConfig(String configFileName) {
		try {
			if (configFileName.equals("-")) {
				Logging.message(this, "Reading XML configuration from standard input");
				_inputSource = new InputSource(System.in);	
			} else {
				Logging.message(this, "Reading XML configuration from file: %s", configFileName);
				_inputSource = new InputSource(configFileName);
			}
			_document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(_inputSource);
			_variables = getVariables();
			
			if (isMainExrelConfig  && !this.containsTestDirectory()) {
				useTempOutputDir=true;
				tmpDir = createTempDir();
				Logging.message(this, "Using tmpDir: "+tmpDir.getAbsolutePath());
			}
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public XMLConfig(String configFileName, boolean isExrelConfigFile) {
		this(configFileName);
		
	}

	
	
	public String parse(String variable, Node node) {
		Matcher matcher;
		String var, evaluated;
		
		// match variables
		matcher = _varExpression.matcher(variable);
		while (matcher.matches()) {
			var = matcher.group(2);
			evaluated = getPropertyById(var, node);	
			if (evaluated == null || evaluated.length() == 0) {
				//exception for TestDir: it might still be defined as @TestDir in the xmlConfig, although no <variables><TestDir>... is given. This means we use a temp directory and give that back here
				if (var.equals("TestDir")) {
					if (useTempOutputDir) {
						//return tmpDir.getAbsolutePath();
						evaluated = tmpDir.getAbsolutePath();
					}
				} else
					throw new RuntimeException(String.format("No such variable defined: \"%s\"", var));
			}
			variable = matcher.group(1) + evaluated + matcher.group(3);
			matcher = _varExpression.matcher(variable);
		}
		
		// match nested queries
		matcher = _xpathExpression.matcher(variable);
		while (matcher.matches()) {
			var = matcher.group(2);			
			evaluated = evaluate(var, node);
			variable = matcher.group(1) + evaluated + matcher.group(3);
			matcher = _xpathExpression.matcher(variable);
		}
		
		return variable;
	}
	
	public String evaluate(String xpathExpression, Node node) {
		try {
			//System.out.println("xpath:" + xpathExpression);
			String value = (String) _xpath.evaluate(xpathExpression, node, XPathConstants.STRING);
			return parse(value, node);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	public String getPropertyById(String var, Node node) {
		return evaluate(String.format("/exrel/variables/%s/text()", var), node);
	}
	
	public Element getDocumentElement() {
		return _document.getDocumentElement();
	}
	
	public Element getModuleElement(String className, String moduleId) {
		NodeList elements = _document.getElementsByTagName("module");
		for (int i=0; i<elements.getLength(); i++) {
			Element elem = (Element)elements.item(i);
			if (elem.getAttribute("id").equals(moduleId)) {
				return elem;
			}
		}
		throw new RuntimeException(String.format("No such module [%s] having id [%s]", className, moduleId));
	}
	
	public Vector<Element> getConfigurations() {
		Vector<Element> result = new Vector<Element>();
		NodeList elements = _document.getElementsByTagName("configuration");
		for (int i=0; i<elements.getLength(); i++) {
			Element elem = (Element)elements.item(i);
			String id = elem.getAttribute("id");
			if (id == null || id.length() == 0) {
				throw new RuntimeException("The id field is required for configuration elements.");
			}
			result.add(elem);
		}
		return result;
	}
	
	public Element getExtractorLayoutElement() {
		NodeList elements = _document.getElementsByTagName("extractorLayout");
		if (elements.getLength() > 1) {
			throw new RuntimeException("More than one extractor elements defined in file.");
		}
		return (Element)elements.item(0);
	}
	
	public boolean applyCheck(String checkName, String valueToCheck) {
		try {
			Method m = XMLConfig.class.getMethod(checkName, String.class);
			return (Boolean)m.invoke(this, valueToCheck);
		} catch (Exception ex) {
			throw new RuntimeException("No such check function defined: " + checkName);
		}
	}
	
	public boolean checkFileExists(String fname) {
		return new File(fname).exists();
	}
	
	public boolean checkFileCanExecute(String fname) {
		return new File(fname).canExecute();
	}
	
	public boolean checkFileCanRead(String fname) {
		return new File(fname).canRead();
	}

	public HashMap<String,String> getVariables() {
		HashMap<String,String> result = new HashMap<String,String>();
	
		// normalize text representation
		//_document.getDocumentElement ().normalize ();
  
        NodeList listOfVariables = _document.getElementsByTagName("variables");		    
        for(int s=0; s<listOfVariables.getLength() ; s++){
        	Node variable_node = listOfVariables.item(s);
        	NodeList variables = variable_node.getChildNodes();
        	
        	for (int i=0; i < variables.getLength(); i++) {
        		Node  var = variables.item(i);
        		if(var.getNodeType() == Node.ELEMENT_NODE){ 
        			Element current_var = (Element)var;
        			//System.out.println(current_var.getNodeName() + " " + current_var.getTextContent());
        			result.put(current_var.getNodeName(), current_var.getTextContent());
        		}
        	}


        }
		return result;
	}

	// check whether /exrel/variables contains TestDir
	public boolean containsTestDirectory() {
		return (_variables.keySet().contains("TestDir"));
	}


	/***
	 * if <TestDir> variable is not given, we're using a temporary output directory
	 * @return
	 */
	public boolean useTempOutputDirectory() {
		return this.useTempOutputDir;
	}


	/***
	 * Return the absolute path of the temporary output directory
	 * @return
	 */
	public String getTemporaryOuputDirPath() {
		return this.tmpDir.getAbsolutePath() + "/";
	}

	/***
	 * Delete temporary output directory on exit
	 * @throws FileNotFoundException 
	 */
	public void deleteTempOutputDirectory() throws FileNotFoundException {		
		deleteRecursive(this.tmpDir);
	}
	
	public static boolean deleteRecursive(File path) throws FileNotFoundException{
        if (!path.exists()) throw new FileNotFoundException(path.getAbsolutePath());
        boolean ret = true;
        if (path.isDirectory()){
            for (File f : path.listFiles()){
                ret = ret && deleteRecursive(f);
            }
        }
        return ret && path.delete();
    }

	/***
	 * specifies whether xml file is main exrel config (for temporary file)
	 * @param isMainConfig
	 */
	public void isMainConfig(boolean isMainConfig) {
		this.isMainExrelConfig = isMainConfig;
		//init dir if not already done
		if (this.isMainExrelConfig == true && this.tmpDir == null && !this.containsTestDirectory()) {
				useTempOutputDir=true;
				tmpDir = createTempDir();
				Logging.message(this, "Using tmpDir: "+tmpDir.getAbsolutePath());
		}
	}
	
	private static final int TEMP_DIR_ATTEMPTS = 10000;
	/*** Creates a temporary directory in /tmp 
	 * Inspired from Google guave code -> com.google.common.io.Files;
	 * @return TempFile
	 */
	public static File createTempDir() {
		File baseDir = new File(System.getProperty("java.io.tmpdir"));
		String baseName = System.currentTimeMillis() + "-";

		for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {

			File tempDir = new File(baseDir, baseName + counter);
			if (tempDir.mkdir()) {
				return tempDir;
			}

		}

		throw new IllegalStateException("Failed to create directory within "
				+ TEMP_DIR_ATTEMPTS + " attempts (tried " + baseName + "0 to "
				+ baseName + (TEMP_DIR_ATTEMPTS - 1) + ')');

	}
}
