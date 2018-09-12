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
package limo.exrel.slots;

public abstract class LabelMapping {

	protected String targetLabel = null;

	/*!
	 * All the implementations of LabelMapping must define
	 * a constructor that takes as its first argument the target label
	 * for the mapping.
	 */
	public LabelMapping(String lab) {
		targetLabel = lab;
	}
	
	public String getTargetLabel() {
		return targetLabel;
	}
	
	/*!
	 * Return the sign describing how an example with the
	 * given label should be included in the set of classifier
	 * examples.
	 * 
	 * Return 0 if the example should not be included.
	 */
	public abstract int map(String lab);
	
}
