
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

/** The type storing the information on event coreference.
 * Updated by JCasGen Sun Sep 14 23:46:31 EDT 2014
 * @generated */
public class PairwiseEventCoreferenceEvaluation_Type extends ComponentTOP_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (PairwiseEventCoreferenceEvaluation_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = PairwiseEventCoreferenceEvaluation_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new PairwiseEventCoreferenceEvaluation(addr, PairwiseEventCoreferenceEvaluation_Type.this);
  			   PairwiseEventCoreferenceEvaluation_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new PairwiseEventCoreferenceEvaluation(addr, PairwiseEventCoreferenceEvaluation_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = PairwiseEventCoreferenceEvaluation.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
 
  /** @generated */
  final Feature casFeat_eventMentionI;
  /** @generated */
  final int     casFeatCode_eventMentionI;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getEventMentionI(int addr) {
        if (featOkTst && casFeat_eventMentionI == null)
      jcas.throwFeatMissing("eventMentionI", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    return ll_cas.ll_getRefValue(addr, casFeatCode_eventMentionI);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEventMentionI(int addr, int v) {
        if (featOkTst && casFeat_eventMentionI == null)
      jcas.throwFeatMissing("eventMentionI", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    ll_cas.ll_setRefValue(addr, casFeatCode_eventMentionI, v);}
    
  
 
  /** @generated */
  final Feature casFeat_eventMentionJ;
  /** @generated */
  final int     casFeatCode_eventMentionJ;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getEventMentionJ(int addr) {
        if (featOkTst && casFeat_eventMentionJ == null)
      jcas.throwFeatMissing("eventMentionJ", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    return ll_cas.ll_getRefValue(addr, casFeatCode_eventMentionJ);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEventMentionJ(int addr, int v) {
        if (featOkTst && casFeat_eventMentionJ == null)
      jcas.throwFeatMissing("eventMentionJ", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    ll_cas.ll_setRefValue(addr, casFeatCode_eventMentionJ, v);}
    
  
 
  /** @generated */
  final Feature casFeat_eventCoreferenceRelationGoldStandard;
  /** @generated */
  final int     casFeatCode_eventCoreferenceRelationGoldStandard;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getEventCoreferenceRelationGoldStandard(int addr) {
        if (featOkTst && casFeat_eventCoreferenceRelationGoldStandard == null)
      jcas.throwFeatMissing("eventCoreferenceRelationGoldStandard", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    return ll_cas.ll_getStringValue(addr, casFeatCode_eventCoreferenceRelationGoldStandard);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEventCoreferenceRelationGoldStandard(int addr, String v) {
        if (featOkTst && casFeat_eventCoreferenceRelationGoldStandard == null)
      jcas.throwFeatMissing("eventCoreferenceRelationGoldStandard", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    ll_cas.ll_setStringValue(addr, casFeatCode_eventCoreferenceRelationGoldStandard, v);}
    
  
 
  /** @generated */
  final Feature casFeat_eventCoreferenceRelationSystem;
  /** @generated */
  final int     casFeatCode_eventCoreferenceRelationSystem;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getEventCoreferenceRelationSystem(int addr) {
        if (featOkTst && casFeat_eventCoreferenceRelationSystem == null)
      jcas.throwFeatMissing("eventCoreferenceRelationSystem", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    return ll_cas.ll_getStringValue(addr, casFeatCode_eventCoreferenceRelationSystem);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEventCoreferenceRelationSystem(int addr, String v) {
        if (featOkTst && casFeat_eventCoreferenceRelationSystem == null)
      jcas.throwFeatMissing("eventCoreferenceRelationSystem", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    ll_cas.ll_setStringValue(addr, casFeatCode_eventCoreferenceRelationSystem, v);}
    
  
 
  /** @generated */
  final Feature casFeat_pairwiseEventFeatures;
  /** @generated */
  final int     casFeatCode_pairwiseEventFeatures;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getPairwiseEventFeatures(int addr) {
        if (featOkTst && casFeat_pairwiseEventFeatures == null)
      jcas.throwFeatMissing("pairwiseEventFeatures", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    return ll_cas.ll_getRefValue(addr, casFeatCode_pairwiseEventFeatures);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setPairwiseEventFeatures(int addr, int v) {
        if (featOkTst && casFeat_pairwiseEventFeatures == null)
      jcas.throwFeatMissing("pairwiseEventFeatures", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    ll_cas.ll_setRefValue(addr, casFeatCode_pairwiseEventFeatures, v);}
    
  
 
  /** @generated */
  final Feature casFeat_isUnified;
  /** @generated */
  final int     casFeatCode_isUnified;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public boolean getIsUnified(int addr) {
        if (featOkTst && casFeat_isUnified == null)
      jcas.throwFeatMissing("isUnified", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    return ll_cas.ll_getBooleanValue(addr, casFeatCode_isUnified);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setIsUnified(int addr, boolean v) {
        if (featOkTst && casFeat_isUnified == null)
      jcas.throwFeatMissing("isUnified", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    ll_cas.ll_setBooleanValue(addr, casFeatCode_isUnified, v);}
    
  
 
  /** @generated */
  final Feature casFeat_confidence;
  /** @generated */
  final int     casFeatCode_confidence;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public double getConfidence(int addr) {
        if (featOkTst && casFeat_confidence == null)
      jcas.throwFeatMissing("confidence", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    return ll_cas.ll_getDoubleValue(addr, casFeatCode_confidence);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setConfidence(int addr, double v) {
        if (featOkTst && casFeat_confidence == null)
      jcas.throwFeatMissing("confidence", "edu.cmu.cs.lti.script.type.PairwiseEventCoreferenceEvaluation");
    ll_cas.ll_setDoubleValue(addr, casFeatCode_confidence, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public PairwiseEventCoreferenceEvaluation_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_eventMentionI = jcas.getRequiredFeatureDE(casType, "eventMentionI", "edu.cmu.cs.lti.script.type.EventMention", featOkTst);
    casFeatCode_eventMentionI  = (null == casFeat_eventMentionI) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_eventMentionI).getCode();

 
    casFeat_eventMentionJ = jcas.getRequiredFeatureDE(casType, "eventMentionJ", "edu.cmu.cs.lti.script.type.EventMention", featOkTst);
    casFeatCode_eventMentionJ  = (null == casFeat_eventMentionJ) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_eventMentionJ).getCode();

 
    casFeat_eventCoreferenceRelationGoldStandard = jcas.getRequiredFeatureDE(casType, "eventCoreferenceRelationGoldStandard", "uima.cas.String", featOkTst);
    casFeatCode_eventCoreferenceRelationGoldStandard  = (null == casFeat_eventCoreferenceRelationGoldStandard) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_eventCoreferenceRelationGoldStandard).getCode();

 
    casFeat_eventCoreferenceRelationSystem = jcas.getRequiredFeatureDE(casType, "eventCoreferenceRelationSystem", "uima.cas.String", featOkTst);
    casFeatCode_eventCoreferenceRelationSystem  = (null == casFeat_eventCoreferenceRelationSystem) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_eventCoreferenceRelationSystem).getCode();

 
    casFeat_pairwiseEventFeatures = jcas.getRequiredFeatureDE(casType, "pairwiseEventFeatures", "uima.cas.FSList", featOkTst);
    casFeatCode_pairwiseEventFeatures  = (null == casFeat_pairwiseEventFeatures) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_pairwiseEventFeatures).getCode();

 
    casFeat_isUnified = jcas.getRequiredFeatureDE(casType, "isUnified", "uima.cas.Boolean", featOkTst);
    casFeatCode_isUnified  = (null == casFeat_isUnified) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_isUnified).getCode();

 
    casFeat_confidence = jcas.getRequiredFeatureDE(casType, "confidence", "uima.cas.Double", featOkTst);
    casFeatCode_confidence  = (null == casFeat_confidence) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_confidence).getCode();

  }
}



    