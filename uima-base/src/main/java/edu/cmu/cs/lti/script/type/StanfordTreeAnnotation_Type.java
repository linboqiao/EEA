
/* First created by JCasGen Sat Sep 13 18:23:45 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** 
 * Updated by JCasGen Sun Sep 14 23:46:31 EDT 2014
 * @generated */
public class StanfordTreeAnnotation_Type extends ComponentAnnotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (StanfordTreeAnnotation_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = StanfordTreeAnnotation_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new StanfordTreeAnnotation(addr, StanfordTreeAnnotation_Type.this);
  			   StanfordTreeAnnotation_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new StanfordTreeAnnotation(addr, StanfordTreeAnnotation_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = StanfordTreeAnnotation.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
 
  /** @generated */
  final Feature casFeat_pennTreeLabel;
  /** @generated */
  final int     casFeatCode_pennTreeLabel;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getPennTreeLabel(int addr) {
        if (featOkTst && casFeat_pennTreeLabel == null)
      jcas.throwFeatMissing("pennTreeLabel", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    return ll_cas.ll_getStringValue(addr, casFeatCode_pennTreeLabel);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setPennTreeLabel(int addr, String v) {
        if (featOkTst && casFeat_pennTreeLabel == null)
      jcas.throwFeatMissing("pennTreeLabel", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    ll_cas.ll_setStringValue(addr, casFeatCode_pennTreeLabel, v);}
    
  
 
  /** @generated */
  final Feature casFeat_children;
  /** @generated */
  final int     casFeatCode_children;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getChildren(int addr) {
        if (featOkTst && casFeat_children == null)
      jcas.throwFeatMissing("children", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    return ll_cas.ll_getRefValue(addr, casFeatCode_children);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setChildren(int addr, int v) {
        if (featOkTst && casFeat_children == null)
      jcas.throwFeatMissing("children", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    ll_cas.ll_setRefValue(addr, casFeatCode_children, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getChildren(int addr, int i) {
        if (featOkTst && casFeat_children == null)
      jcas.throwFeatMissing("children", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_children), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_children), i);
  return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_children), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setChildren(int addr, int i, int v) {
        if (featOkTst && casFeat_children == null)
      jcas.throwFeatMissing("children", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_children), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_children), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_children), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_isLeaf;
  /** @generated */
  final int     casFeatCode_isLeaf;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public boolean getIsLeaf(int addr) {
        if (featOkTst && casFeat_isLeaf == null)
      jcas.throwFeatMissing("isLeaf", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    return ll_cas.ll_getBooleanValue(addr, casFeatCode_isLeaf);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setIsLeaf(int addr, boolean v) {
        if (featOkTst && casFeat_isLeaf == null)
      jcas.throwFeatMissing("isLeaf", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    ll_cas.ll_setBooleanValue(addr, casFeatCode_isLeaf, v);}
    
  
 
  /** @generated */
  final Feature casFeat_isRoot;
  /** @generated */
  final int     casFeatCode_isRoot;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public boolean getIsRoot(int addr) {
        if (featOkTst && casFeat_isRoot == null)
      jcas.throwFeatMissing("isRoot", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    return ll_cas.ll_getBooleanValue(addr, casFeatCode_isRoot);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setIsRoot(int addr, boolean v) {
        if (featOkTst && casFeat_isRoot == null)
      jcas.throwFeatMissing("isRoot", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    ll_cas.ll_setBooleanValue(addr, casFeatCode_isRoot, v);}
    
  
 
  /** @generated */
  final Feature casFeat_parent;
  /** @generated */
  final int     casFeatCode_parent;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getParent(int addr) {
        if (featOkTst && casFeat_parent == null)
      jcas.throwFeatMissing("parent", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    return ll_cas.ll_getRefValue(addr, casFeatCode_parent);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setParent(int addr, int v) {
        if (featOkTst && casFeat_parent == null)
      jcas.throwFeatMissing("parent", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    ll_cas.ll_setRefValue(addr, casFeatCode_parent, v);}
    
  
 
  /** @generated */
  final Feature casFeat_head;
  /** @generated */
  final int     casFeatCode_head;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getHead(int addr) {
        if (featOkTst && casFeat_head == null)
      jcas.throwFeatMissing("head", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    return ll_cas.ll_getRefValue(addr, casFeatCode_head);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setHead(int addr, int v) {
        if (featOkTst && casFeat_head == null)
      jcas.throwFeatMissing("head", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation");
    ll_cas.ll_setRefValue(addr, casFeatCode_head, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public StanfordTreeAnnotation_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_pennTreeLabel = jcas.getRequiredFeatureDE(casType, "pennTreeLabel", "uima.cas.String", featOkTst);
    casFeatCode_pennTreeLabel  = (null == casFeat_pennTreeLabel) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_pennTreeLabel).getCode();

 
    casFeat_children = jcas.getRequiredFeatureDE(casType, "children", "uima.cas.FSArray", featOkTst);
    casFeatCode_children  = (null == casFeat_children) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_children).getCode();

 
    casFeat_isLeaf = jcas.getRequiredFeatureDE(casType, "isLeaf", "uima.cas.Boolean", featOkTst);
    casFeatCode_isLeaf  = (null == casFeat_isLeaf) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_isLeaf).getCode();

 
    casFeat_isRoot = jcas.getRequiredFeatureDE(casType, "isRoot", "uima.cas.Boolean", featOkTst);
    casFeatCode_isRoot  = (null == casFeat_isRoot) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_isRoot).getCode();

 
    casFeat_parent = jcas.getRequiredFeatureDE(casType, "parent", "edu.cmu.cs.lti.script.type.StanfordTreeAnnotation", featOkTst);
    casFeatCode_parent  = (null == casFeat_parent) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_parent).getCode();

 
    casFeat_head = jcas.getRequiredFeatureDE(casType, "head", "edu.cmu.cs.lti.script.type.StanfordCorenlpToken", featOkTst);
    casFeatCode_head  = (null == casFeat_head) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_head).getCode();

  }
}



    