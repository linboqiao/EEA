
/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
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

/** Generic Semantic Relation
 * Updated by JCasGen Tue Sep 16 00:38:30 EDT 2014
 * @generated */
public class SemanticRelation_Type extends ComponentTOP_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (SemanticRelation_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = SemanticRelation_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new SemanticRelation(addr, SemanticRelation_Type.this);
  			   SemanticRelation_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new SemanticRelation(addr, SemanticRelation_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = SemanticRelation.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.SemanticRelation");
 
  /** @generated */
  final Feature casFeat_semanticAnnotation;
  /** @generated */
  final int     casFeatCode_semanticAnnotation;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getSemanticAnnotation(int addr) {
        if (featOkTst && casFeat_semanticAnnotation == null)
      jcas.throwFeatMissing("semanticAnnotation", "edu.cmu.cs.lti.script.type.SemanticRelation");
    return ll_cas.ll_getStringValue(addr, casFeatCode_semanticAnnotation);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setSemanticAnnotation(int addr, String v) {
        if (featOkTst && casFeat_semanticAnnotation == null)
      jcas.throwFeatMissing("semanticAnnotation", "edu.cmu.cs.lti.script.type.SemanticRelation");
    ll_cas.ll_setStringValue(addr, casFeatCode_semanticAnnotation, v);}
    
  
 
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
      jcas.throwFeatMissing("head", "edu.cmu.cs.lti.script.type.SemanticRelation");
    return ll_cas.ll_getRefValue(addr, casFeatCode_head);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setHead(int addr, int v) {
        if (featOkTst && casFeat_head == null)
      jcas.throwFeatMissing("head", "edu.cmu.cs.lti.script.type.SemanticRelation");
    ll_cas.ll_setRefValue(addr, casFeatCode_head, v);}
    
  
 
  /** @generated */
  final Feature casFeat_child;
  /** @generated */
  final int     casFeatCode_child;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getChild(int addr) {
        if (featOkTst && casFeat_child == null)
      jcas.throwFeatMissing("child", "edu.cmu.cs.lti.script.type.SemanticRelation");
    return ll_cas.ll_getRefValue(addr, casFeatCode_child);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setChild(int addr, int v) {
        if (featOkTst && casFeat_child == null)
      jcas.throwFeatMissing("child", "edu.cmu.cs.lti.script.type.SemanticRelation");
    ll_cas.ll_setRefValue(addr, casFeatCode_child, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public SemanticRelation_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_semanticAnnotation = jcas.getRequiredFeatureDE(casType, "semanticAnnotation", "uima.cas.String", featOkTst);
    casFeatCode_semanticAnnotation  = (null == casFeat_semanticAnnotation) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_semanticAnnotation).getCode();

 
    casFeat_head = jcas.getRequiredFeatureDE(casType, "head", "edu.cmu.cs.lti.script.type.Word", featOkTst);
    casFeatCode_head  = (null == casFeat_head) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_head).getCode();

 
    casFeat_child = jcas.getRequiredFeatureDE(casType, "child", "edu.cmu.cs.lti.script.type.Word", featOkTst);
    casFeatCode_child  = (null == casFeat_child) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_child).getCode();

  }
}



    