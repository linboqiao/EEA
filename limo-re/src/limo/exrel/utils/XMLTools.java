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

import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XMLTools {
	
	/*!
	 * Load an XML document from file "filename" and return its root element.
	 */
	public static Element load(String filename) {
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(filename)).getDocumentElement();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	

	/*!
	 * Get all the children of an XML element with a given tagName.
	 * 
	 * If tagName is null, then all the children are returned.
	 */
	public static Vector<Element> getChildren(Element e, String tagName) {
		Vector<Element> result = new Vector<Element>();
		NodeList children = e.getChildNodes();
		for (int i=0; i<children.getLength(); i++) {
			if (children.item(i) instanceof Element) {
				Element temp = (Element)children.item(i);
				if (tagName == null || temp.getTagName().equals(tagName)) {
					result.add((Element)children.item(i));
				}
			}
		}
		return result;
	}
	
	/*!
	 * An alis for getChildren(e, null).
	 */
	public static Vector<Element> getChildren(Element e) {
		return XMLTools.getChildren(e, null);
	}

}
