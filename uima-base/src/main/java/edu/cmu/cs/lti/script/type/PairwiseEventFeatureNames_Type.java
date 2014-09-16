
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

/** 
 * Updated by JCasGen Tue Sep 16 00:38:30 EDT 2014
 * @generated */
public class PairwiseEventFeatureNames_Type extends ComponentTOP_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (PairwiseEventFeatureNames_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = PairwiseEventFeatureNames_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new PairwiseEventFeatureNames(addr, PairwiseEventFeatureNames_Type.this);
  			   PairwiseEventFeatureNames_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new PairwiseEventFeatureNames(addr, PairwiseEventFeatureNames_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = PairwiseEventFeatureNames.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.PairwiseEventFeatureNames");
 
  /** @generated */
  final Feature casFeat_featureNames;
  /** @generated */
  final int     casFeatCode_featureNames;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getFeatureNames(int addr) {
        if (featOkTst && casFeat_featureNames == null)
      jcas.throwFeatMissing("featureNames", "edu.cmu.cs.lti.script.type.PairwiseEventFeatureNames");
    return ll_cas.ll_getRefValue(addr, casFeatCode_featureNames);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setFeatureNames(int addr, int v) {
        if (featOkTst && casFeat_featureNames == null)
      jcas.throwFeatMissing("featureNames", "edu.cmu.cs.lti.script.type.PairwiseEventFeatureNames");
    ll_cas.ll_setRefValue(addr, casFeatCode_featureNames, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public PairwiseEventFeatureNames_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_featureNames = jcas.getRequiredFeatureDE(casType, "featureNames", "uima.cas.StringList", featOkTst);
    casFeatCode_featureNames  = (null == casFeat_featureNames) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_featureNames).getCode();

  }
}



    