/* Updated by Barbara Plank
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
package limo.exrel.features.re.linear;

import org.w3c.dom.Element;

import limo.core.Mention;
import limo.core.Relation;
import limo.core.Sentence;
import limo.core.trees.constituency.ParseTree;
import limo.exrel.features.AbstractFeature;

public abstract class RelationExtractionLinearFeature extends AbstractFeature {

	public static final String BOWseparator = "@BOW";
	public static final String CONFIDENCEseparator = "@CONF";
	
	boolean realValued = false;
	boolean isBOWF = false;
	
	boolean containsConfidenceValue = false; // since real is not really for multiple values!
	
	@Override
	public void init(Element e) {
		String type = e.getAttribute("type");
		if (type != null && type.matches("^[rR][eE][aA][lL]$")) {
			realValued = true;
		}
		if (type != null && type.matches("^[cC][oO][nN][fF]$")) {
			containsConfidenceValue = true;
		}
		String bow = e.getAttribute("bow");
		if (bow != null && bow.length()>0 && bow.matches("[tT]rue")) 
			isBOWF = true;
		
		_init(e);
	}
	
	/*
	 * Override to handle per-feature specific configuration options.
	 */
	protected void _init(Element e) {};
	
	public boolean isBoolean() {
		return !realValued;
	}
	
	public boolean isReal() {
		return realValued;
	}
	
	public boolean isBOWF() {
		return this.isBOWF;
	}
	
	public String toString() {
		return getName() + " (" + (isBoolean() ? "bool" : "real") + ") ";
	}
	
	
	@Override
	public String extract(Object... args) {
		return _extract((ParseTree)args[0], (Mention)args[1], (Mention)args[2], (Relation)args[3], (Sentence)args[4], (String)args[5]);
	}
	
	protected abstract String _extract(ParseTree parseTree, Mention mention1, Mention mention2, Relation relation, Sentence sentence, String groupId);

	public boolean containsConfidence() {
		return containsConfidenceValue;
	}
	
}
