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
package limo.exrel.data;


import java.util.Iterator;


public class ClassificationScores extends SortedStack<Classification> implements Cloneable {

	private static final long serialVersionUID = 1L;
	
	public ClassificationScores() {
		super();
	}
	
	public static ClassificationScores deserialize(String str) {
		String[] data = str.split(" ");
		ClassificationScores result = new ClassificationScores();
		for (String elem : data) {
		    //if (!elem.startsWith("NONE"))
			result.add(Classification.deserialize(elem));
		}
		return result;
	}
	
	public Classification get(String label) {
		for (Classification c : this) {
			if (c.getLabel().equals(label)) {
				return c;
			}
		}
		return null;
	}
	
	public void replace(Classification a, Classification b) {
		remove(a);
		add(b);
	}
	
	public void changeScore(String label, double value) {
		Classification c = null;
		for (Classification temp : this) {
			if (temp.getLabel().equals(label)) {
				c = temp;
				break;
			}
		}
		c.setScore(value);
		remove(c);
		add(c);
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		Iterator<Classification> i = iterator();
		Classification c;
		while (i.hasNext()) {
			c = i.next();
			if (buf.length() > 0) {
				buf.append(" ");
			}
			buf.append(c.serialize());
		}
		return buf.toString();
	}	
	
	@Override
	public Object clone() {
		ClassificationScores s = new ClassificationScores();
		Iterator<Classification> i = iterator();
		Classification c;
		while (i.hasNext()) {
			c = i.next();
			s.add((Classification)c.clone());
		}
		return s;
	}

}
