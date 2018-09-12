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

public class Classification implements Comparable<Classification> {

	private String _label = null;         //the label of the classifying  classifier
	private double _score = Double.NaN;   //the score of the classifier
	
	public Classification(String lab, double score) {
		_label = lab;
		_score = score;
	}
	
	public static Classification deserialize(String str) {
		String[] data = str.split(":");
		return new Classification(data[0], Double.parseDouble(data[1]));
	}
	
	public String serialize() {
		return toString();
	}
	
	public String toString() {
		return _label + ":" + _score;
	}
	
	public String getLabel() {
		return _label;
	}
	
	public double getScore() {
		return _score;
	}
	
	public void setScore(double score) {
		_score = score;
	}
	
	public void setLabel(String label) {
		_label = label;
	}
	
	
	public boolean equals(Object o) {
		return this.getLabel().equals(((Classification)o).getLabel());
	}
	
	public int compareTo(Classification o) {
		if (getLabel().equals(o.getLabel())) {
			return 0;
		}
		int diff = (int)Math.signum(this.getScore() - o.getScore());
		if (diff == 0) {
			return Math.random() >= .5 ? +1 : -1;
		}
		return diff;
	}
	
	@Override
	public Object clone() {
		return new Classification(_label, _score);
	}
	
}
