
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
public class Word_Type extends ComponentAnnotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Word_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Word_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Word(addr, Word_Type.this);
  			   Word_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Word(addr, Word_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Word.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.Word");
 
  /** @generated */
  final Feature casFeat_lemma;
  /** @generated */
  final int     casFeatCode_lemma;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getLemma(int addr) {
        if (featOkTst && casFeat_lemma == null)
      jcas.throwFeatMissing("lemma", "edu.cmu.cs.lti.script.type.Word");
    return ll_cas.ll_getStringValue(addr, casFeatCode_lemma);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setLemma(int addr, String v) {
        if (featOkTst && casFeat_lemma == null)
      jcas.throwFeatMissing("lemma", "edu.cmu.cs.lti.script.type.Word");
    ll_cas.ll_setStringValue(addr, casFeatCode_lemma, v);}
    
  
 
  /** @generated */
  final Feature casFeat_elliptical;
  /** @generated */
  final int     casFeatCode_elliptical;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getElliptical(int addr) {
        if (featOkTst && casFeat_elliptical == null)
      jcas.throwFeatMissing("elliptical", "edu.cmu.cs.lti.script.type.Word");
    return ll_cas.ll_getStringValue(addr, casFeatCode_elliptical);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setElliptical(int addr, String v) {
        if (featOkTst && casFeat_elliptical == null)
      jcas.throwFeatMissing("elliptical", "edu.cmu.cs.lti.script.type.Word");
    ll_cas.ll_setStringValue(addr, casFeatCode_elliptical, v);}
    
  
 
  /** @generated */
  final Feature casFeat_pos;
  /** @generated */
  final int     casFeatCode_pos;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getPos(int addr) {
        if (featOkTst && casFeat_pos == null)
      jcas.throwFeatMissing("pos", "edu.cmu.cs.lti.script.type.Word");
    return ll_cas.ll_getStringValue(addr, casFeatCode_pos);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setPos(int addr, String v) {
        if (featOkTst && casFeat_pos == null)
      jcas.throwFeatMissing("pos", "edu.cmu.cs.lti.script.type.Word");
    ll_cas.ll_setStringValue(addr, casFeatCode_pos, v);}
    
  
 
  /** @generated */
  final Feature casFeat_nerTag;
  /** @generated */
  final int     casFeatCode_nerTag;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getNerTag(int addr) {
        if (featOkTst && casFeat_nerTag == null)
      jcas.throwFeatMissing("nerTag", "edu.cmu.cs.lti.script.type.Word");
    return ll_cas.ll_getStringValue(addr, casFeatCode_nerTag);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setNerTag(int addr, String v) {
        if (featOkTst && casFeat_nerTag == null)
      jcas.throwFeatMissing("nerTag", "edu.cmu.cs.lti.script.type.Word");
    ll_cas.ll_setStringValue(addr, casFeatCode_nerTag, v);}
    
  
 
  /** @generated */
  final Feature casFeat_morpha;
  /** @generated */
  final int     casFeatCode_morpha;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getMorpha(int addr) {
        if (featOkTst && casFeat_morpha == null)
      jcas.throwFeatMissing("morpha", "edu.cmu.cs.lti.script.type.Word");
    return ll_cas.ll_getStringValue(addr, casFeatCode_morpha);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setMorpha(int addr, String v) {
        if (featOkTst && casFeat_morpha == null)
      jcas.throwFeatMissing("morpha", "edu.cmu.cs.lti.script.type.Word");
    ll_cas.ll_setStringValue(addr, casFeatCode_morpha, v);}
    
  
 
  /** @generated */
  final Feature casFeat_headDependencyRelations;
  /** @generated */
  final int     casFeatCode_headDependencyRelations;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getHeadDependencyRelations(int addr) {
        if (featOkTst && casFeat_headDependencyRelations == null)
      jcas.throwFeatMissing("headDependencyRelations", "edu.cmu.cs.lti.script.type.Word");
    return ll_cas.ll_getRefValue(addr, casFeatCode_headDependencyRelations);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setHeadDependencyRelations(int addr, int v) {
        if (featOkTst && casFeat_headDependencyRelations == null)
      jcas.throwFeatMissing("headDependencyRelations", "edu.cmu.cs.lti.script.type.Word");
    ll_cas.ll_setRefValue(addr, casFeatCode_headDependencyRelations, v);}
    
  
 
  /** @generated */
  final Feature casFeat_childDependencyRelations;
  /** @generated */
  final int     casFeatCode_childDependencyRelations;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getChildDependencyRelations(int addr) {
        if (featOkTst && casFeat_childDependencyRelations == null)
      jcas.throwFeatMissing("childDependencyRelations", "edu.cmu.cs.lti.script.type.Word");
    return ll_cas.ll_getRefValue(addr, casFeatCode_childDependencyRelations);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setChildDependencyRelations(int addr, int v) {
        if (featOkTst && casFeat_childDependencyRelations == null)
      jcas.throwFeatMissing("childDependencyRelations", "edu.cmu.cs.lti.script.type.Word");
    ll_cas.ll_setRefValue(addr, casFeatCode_childDependencyRelations, v);}
    
  
 
  /** @generated */
  final Feature casFeat_isDependencyRoot;
  /** @generated */
  final int     casFeatCode_isDependencyRoot;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public boolean getIsDependencyRoot(int addr) {
        if (featOkTst && casFeat_isDependencyRoot == null)
      jcas.throwFeatMissing("isDependencyRoot", "edu.cmu.cs.lti.script.type.Word");
    return ll_cas.ll_getBooleanValue(addr, casFeatCode_isDependencyRoot);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setIsDependencyRoot(int addr, boolean v) {
        if (featOkTst && casFeat_isDependencyRoot == null)
      jcas.throwFeatMissing("isDependencyRoot", "edu.cmu.cs.lti.script.type.Word");
    ll_cas.ll_setBooleanValue(addr, casFeatCode_isDependencyRoot, v);}
    
  
 
  /** @generated */
  final Feature casFeat_headSemanticRelations;
  /** @generated */
  final int     casFeatCode_headSemanticRelations;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getHeadSemanticRelations(int addr) {
        if (featOkTst && casFeat_headSemanticRelations == null)
      jcas.throwFeatMissing("headSemanticRelations", "edu.cmu.cs.lti.script.type.Word");
    return ll_cas.ll_getRefValue(addr, casFeatCode_headSemanticRelations);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setHeadSemanticRelations(int addr, int v) {
        if (featOkTst && casFeat_headSemanticRelations == null)
      jcas.throwFeatMissing("headSemanticRelations", "edu.cmu.cs.lti.script.type.Word");
    ll_cas.ll_setRefValue(addr, casFeatCode_headSemanticRelations, v);}
    
  
 
  /** @generated */
  final Feature casFeat_childSemanticRelations;
  /** @generated */
  final int     casFeatCode_childSemanticRelations;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getChildSemanticRelations(int addr) {
        if (featOkTst && casFeat_childSemanticRelations == null)
      jcas.throwFeatMissing("childSemanticRelations", "edu.cmu.cs.lti.script.type.Word");
    return ll_cas.ll_getRefValue(addr, casFeatCode_childSemanticRelations);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setChildSemanticRelations(int addr, int v) {
        if (featOkTst && casFeat_childSemanticRelations == null)
      jcas.throwFeatMissing("childSemanticRelations", "edu.cmu.cs.lti.script.type.Word");
    ll_cas.ll_setRefValue(addr, casFeatCode_childSemanticRelations, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Word_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_lemma = jcas.getRequiredFeatureDE(casType, "lemma", "uima.cas.String", featOkTst);
    casFeatCode_lemma  = (null == casFeat_lemma) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_lemma).getCode();

 
    casFeat_elliptical = jcas.getRequiredFeatureDE(casType, "elliptical", "uima.cas.String", featOkTst);
    casFeatCode_elliptical  = (null == casFeat_elliptical) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_elliptical).getCode();

 
    casFeat_pos = jcas.getRequiredFeatureDE(casType, "pos", "uima.cas.String", featOkTst);
    casFeatCode_pos  = (null == casFeat_pos) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_pos).getCode();

 
    casFeat_nerTag = jcas.getRequiredFeatureDE(casType, "nerTag", "uima.cas.String", featOkTst);
    casFeatCode_nerTag  = (null == casFeat_nerTag) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_nerTag).getCode();

 
    casFeat_morpha = jcas.getRequiredFeatureDE(casType, "morpha", "uima.cas.String", featOkTst);
    casFeatCode_morpha  = (null == casFeat_morpha) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_morpha).getCode();

 
    casFeat_headDependencyRelations = jcas.getRequiredFeatureDE(casType, "headDependencyRelations", "uima.cas.FSList", featOkTst);
    casFeatCode_headDependencyRelations  = (null == casFeat_headDependencyRelations) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_headDependencyRelations).getCode();

 
    casFeat_childDependencyRelations = jcas.getRequiredFeatureDE(casType, "childDependencyRelations", "uima.cas.FSList", featOkTst);
    casFeatCode_childDependencyRelations  = (null == casFeat_childDependencyRelations) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_childDependencyRelations).getCode();

 
    casFeat_isDependencyRoot = jcas.getRequiredFeatureDE(casType, "isDependencyRoot", "uima.cas.Boolean", featOkTst);
    casFeatCode_isDependencyRoot  = (null == casFeat_isDependencyRoot) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_isDependencyRoot).getCode();

 
    casFeat_headSemanticRelations = jcas.getRequiredFeatureDE(casType, "headSemanticRelations", "uima.cas.FSList", featOkTst);
    casFeatCode_headSemanticRelations  = (null == casFeat_headSemanticRelations) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_headSemanticRelations).getCode();

 
    casFeat_childSemanticRelations = jcas.getRequiredFeatureDE(casType, "childSemanticRelations", "uima.cas.FSList", featOkTst);
    casFeatCode_childSemanticRelations  = (null == casFeat_childSemanticRelations) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_childSemanticRelations).getCode();

  }
}



    