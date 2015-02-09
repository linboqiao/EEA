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
package limo.core.trees.constituency;


//public class LeafNode extends TreeNode {
//	
//	private int _offset = -1;
//	private String _surface;
//
//	public LeafNode(ParseTree tree, int id, String label, int off, String surf) {
//		super(tree, id, label);
//		_offset = off;
//		_surface = surf;
//	}
//
//	public String dump() {
//		return String.format("%s [off:%d] [surface:%s]", super.dump(), getOffset() , getSurface());
//	}
//
//	public String getSurface() {
//		return _surface;
//	}
//	
//	public String getNormalizedSurface() {
//		String surface = _surface;
//		if (surface.matches("^[0-9,.]+$")) {
//			return "::NUMBER::";
//		}
//		return surface.toLowerCase();
//	}
//	
//	public void setSurface(String surf) {
//		_surface = surf;
//	}
//
//	public int getOffset() {
//		return _offset;
//	}
//
//	public int getOffsetBegin() {
//		return _offset;
//	}
//
//	public int getOffsetEnd() {
//		return _offset;
//	}
//
//	public String toString() {
//		if (isHidden()) {
//			return "";
//		}
//		String mark = getMark();
//		if (mark != null) {
//			mark = "-" + mark;
//		} else {
//			mark = "";
//		}
//		
//		if (getLabel().equals(getSurface())) { //condensed nodes
//			return "("+getLabel()+mark+")";
//		}
//		
//		return "(" + getLabel() + mark + " " + getSurface() + ")";
//	}
//
//	public int getHeight() {
//		return 0;
//	}
//	
//	public LeafNode getHead() {
//		return this;
//	}
//	
//	public boolean isToBeVoice() {
//		return (
//				(getLabel().equals("AUX") && getSurface().toLowerCase().matches("^(be|been|am|'m|are|'re|is|'s|were|was)$"))
//				||
//				(getLabel().equals("AUXG") && getSurface().toLowerCase().equals("being|bein'"))
//				);	
//	}
//
//}
