///*
// * Copyright (c) 2008 - 2009 , Daniele Pighin - All rights reserved.
// * 
// * This software is released under a double licensing scheme.
// * 
// * For personal or research uses, the software is available under the
// * GNU Lesser GPL (LGPL) v.3 license. 
// * 
// * See the file LICENSE in the source distribution for more details.
// * 
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
// * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
// * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
// * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
// * OTHER DEALINGS IN THE SOFTWARE.
// */
//package limo.core.trees.constituency;
//
//import java.util.Collection;
//import java.util.Vector;
//
//
//public abstract class TreeNode implements Cloneable {
//	
//	private ParseTree _tree = null;
//	private int _id = -1;
//	private String _label = null;
//	private String _mark = null;
//
//	public TreeNode(ParseTree tree, int id, String lab) {
//		_tree = tree;
//		_id = id;
//		_label = lab;
//	}
//	
//	public void setLabel(String lab) {
//		_label = lab;
//	}
//
//	public String getLabel() {
//		return _label;
//	}
//
//	public void setMark(String mark) {
//		_mark = mark;
//	}
//
//	public String getMark() {
//		return _mark;
//	}
//
//	public int getNodeId() {
//		return _id;
//	}
//
//	public int getDepth() {
//		return numAncestors();
//	}
//
//	public ParseTree tree() {
//		return _tree;
//	}
//	
//	public InternalNode getRoot() {
//		if (getParent() == null) {
//			return (InternalNode)this;
//		}
//		return getParent().getRoot();
//	}
//
//	public String dump() {
//		return String.format("[id:%3d] [label:%s]", getNodeId(), getLabel());
//	}
//
//	public boolean equals(Object obj) {
//		if (obj instanceof TreeNode) {
//			return _id == ((TreeNode)obj).getNodeId();
//		}
//		return false;
//	}
//
//	public InternalNode getParent() {
//		if (numAncestors() > 0) {
//			return getAncestor(0);
//		}
//		return null;
//	}
//	
//	public Vector<TreeNode> getSiblings() {
//		Vector<TreeNode> result = new Vector<TreeNode>();
//		if (getParent() != null) {
//			for (TreeNode temp : getParent().getChildren()) {
//				if (!temp.equals(this)) {
//					result.add(temp);
//				}
//			}
//		}
//		return result;
//	}
//
//	public InternalNode[] getAncestors() {
//		return tree().getAncestors(getNodeId());
//	}
//
//	
//	public InternalNode getAncestor(int distance) {
//		return tree().getAncestor(getNodeId(),distance);
//	}
//	
//
//	public int numAncestors() {
//		return tree().numAncestors(getNodeId());
//	}
//
//	public boolean isAncestorOf(TreeNode node) {
//		return tree().isAncestorOf(getNodeId(), node.getNodeId());
//	}
//	
//	public boolean isAncestorOfAll(Collection<TreeNode> nodes) {
//		for (TreeNode node : nodes) {
//			if (!this.isAncestorOf(node)) {
//				return false;
//			}
//		}
//		return true;
//	}
//
//	public Object clone() {
//		try {
//			return super.clone();
//		} catch (CloneNotSupportedException ex) {
//			ex.printStackTrace();
//			return null;
//		}
//	}
//
//	public boolean isHidden() {
//		return tree().isHidden(_id);
//	}
//
//	public void hide() {
//		tree().hide(_id);
//	}
//
//	public void unhide() {
//		tree().unhide(_id);
//	}
//
//	public abstract String getSurface();
//	
//	public abstract String toString();
//
//	public abstract int getOffsetBegin();
//
//	public abstract int getOffsetEnd();
//
//	public abstract int getHeight();
//
//	public String getNormalizedLabel() {
//		String tag = getLabel();
//		if (tag.startsWith("VB")) return "VB";
//		if (tag.startsWith("NN")) return "NN";
//		if (tag.startsWith("JJ")) return "JJ";
//		if (tag.startsWith("RB")) return "RB";
//		if (tag.startsWith("AUX")) return "AUX";
//		if (!tag.equals("$") && !tag.equals("#") && tag.matches("^\\W.*")) {
//			return "PUNCT";
//		}
//		int index;
//		if((index=tag.indexOf("-")) > 0)
//			if(!tag.startsWith("C-") && !tag.startsWith("R-")) {
//				return tag.substring(0,index);
//			} else {
//				if(tag.lastIndexOf('-')!=index) {
//					return tag.substring(0,tag.lastIndexOf('-'));
//				}
//			}
//		if((index=tag.indexOf("="))>0) {
//			return tag.substring(0,index);
//		}
//		return tag;
//	}
//	
//}
