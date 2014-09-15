
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
public class SemaforAnnotationSet_Type extends ComponentAnnotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (SemaforAnnotationSet_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = SemaforAnnotationSet_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new SemaforAnnotationSet(addr, SemaforAnnotationSet_Type.this);
  			   SemaforAnnotationSet_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new SemaforAnnotationSet(addr, SemaforAnnotationSet_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = SemaforAnnotationSet.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.SemaforAnnotationSet");
 
  /** @generated */
  final Feature casFeat_layers;
  /** @generated */
  final int     casFeatCode_layers;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getLayers(int addr) {
        if (featOkTst && casFeat_layers == null)
      jcas.throwFeatMissing("layers", "edu.cmu.cs.lti.script.type.SemaforAnnotationSet");
    return ll_cas.ll_getRefValue(addr, casFeatCode_layers);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setLayers(int addr, int v) {
        if (featOkTst && casFeat_layers == null)
      jcas.throwFeatMissing("layers", "edu.cmu.cs.lti.script.type.SemaforAnnotationSet");
    ll_cas.ll_setRefValue(addr, casFeatCode_layers, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getLayers(int addr, int i) {
        if (featOkTst && casFeat_layers == null)
      jcas.throwFeatMissing("layers", "edu.cmu.cs.lti.script.type.SemaforAnnotationSet");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_layers), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_layers), i);
  return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_layers), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setLayers(int addr, int i, int v) {
        if (featOkTst && casFeat_layers == null)
      jcas.throwFeatMissing("layers", "edu.cmu.cs.lti.script.type.SemaforAnnotationSet");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_layers), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_layers), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_layers), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_frameName;
  /** @generated */
  final int     casFeatCode_frameName;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getFrameName(int addr) {
        if (featOkTst && casFeat_frameName == null)
      jcas.throwFeatMissing("frameName", "edu.cmu.cs.lti.script.type.SemaforAnnotationSet");
    return ll_cas.ll_getStringValue(addr, casFeatCode_frameName);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setFrameName(int addr, String v) {
        if (featOkTst && casFeat_frameName == null)
      jcas.throwFeatMissing("frameName", "edu.cmu.cs.lti.script.type.SemaforAnnotationSet");
    ll_cas.ll_setStringValue(addr, casFeatCode_frameName, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public SemaforAnnotationSet_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_layers = jcas.getRequiredFeatureDE(casType, "layers", "uima.cas.FSArray", featOkTst);
    casFeatCode_layers  = (null == casFeat_layers) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_layers).getCode();

 
    casFeat_frameName = jcas.getRequiredFeatureDE(casType, "frameName", "uima.cas.String", featOkTst);
    casFeatCode_frameName  = (null == casFeat_frameName) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_frameName).getCode();

  }
}



    