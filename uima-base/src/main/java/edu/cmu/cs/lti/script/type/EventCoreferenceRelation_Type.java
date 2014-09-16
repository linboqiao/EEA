
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
 * Updated by JCasGen Tue Sep 16 00:38:29 EDT 2014
 * @generated */
public class EventCoreferenceRelation_Type extends ComponentTOP_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (EventCoreferenceRelation_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = EventCoreferenceRelation_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new EventCoreferenceRelation(addr, EventCoreferenceRelation_Type.this);
  			   EventCoreferenceRelation_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new EventCoreferenceRelation(addr, EventCoreferenceRelation_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = EventCoreferenceRelation.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.EventCoreferenceRelation");
 
  /** @generated */
  final Feature casFeat_relationType;
  /** @generated */
  final int     casFeatCode_relationType;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getRelationType(int addr) {
        if (featOkTst && casFeat_relationType == null)
      jcas.throwFeatMissing("relationType", "edu.cmu.cs.lti.script.type.EventCoreferenceRelation");
    return ll_cas.ll_getStringValue(addr, casFeatCode_relationType);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setRelationType(int addr, String v) {
        if (featOkTst && casFeat_relationType == null)
      jcas.throwFeatMissing("relationType", "edu.cmu.cs.lti.script.type.EventCoreferenceRelation");
    ll_cas.ll_setStringValue(addr, casFeatCode_relationType, v);}
    
  
 
  /** @generated */
  final Feature casFeat_fromEventMention;
  /** @generated */
  final int     casFeatCode_fromEventMention;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getFromEventMention(int addr) {
        if (featOkTst && casFeat_fromEventMention == null)
      jcas.throwFeatMissing("fromEventMention", "edu.cmu.cs.lti.script.type.EventCoreferenceRelation");
    return ll_cas.ll_getRefValue(addr, casFeatCode_fromEventMention);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setFromEventMention(int addr, int v) {
        if (featOkTst && casFeat_fromEventMention == null)
      jcas.throwFeatMissing("fromEventMention", "edu.cmu.cs.lti.script.type.EventCoreferenceRelation");
    ll_cas.ll_setRefValue(addr, casFeatCode_fromEventMention, v);}
    
  
 
  /** @generated */
  final Feature casFeat_toEventMention;
  /** @generated */
  final int     casFeatCode_toEventMention;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getToEventMention(int addr) {
        if (featOkTst && casFeat_toEventMention == null)
      jcas.throwFeatMissing("toEventMention", "edu.cmu.cs.lti.script.type.EventCoreferenceRelation");
    return ll_cas.ll_getRefValue(addr, casFeatCode_toEventMention);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setToEventMention(int addr, int v) {
        if (featOkTst && casFeat_toEventMention == null)
      jcas.throwFeatMissing("toEventMention", "edu.cmu.cs.lti.script.type.EventCoreferenceRelation");
    ll_cas.ll_setRefValue(addr, casFeatCode_toEventMention, v);}
    
  
 
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
      jcas.throwFeatMissing("confidence", "edu.cmu.cs.lti.script.type.EventCoreferenceRelation");
    return ll_cas.ll_getDoubleValue(addr, casFeatCode_confidence);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setConfidence(int addr, double v) {
        if (featOkTst && casFeat_confidence == null)
      jcas.throwFeatMissing("confidence", "edu.cmu.cs.lti.script.type.EventCoreferenceRelation");
    ll_cas.ll_setDoubleValue(addr, casFeatCode_confidence, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public EventCoreferenceRelation_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_relationType = jcas.getRequiredFeatureDE(casType, "relationType", "uima.cas.String", featOkTst);
    casFeatCode_relationType  = (null == casFeat_relationType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_relationType).getCode();

 
    casFeat_fromEventMention = jcas.getRequiredFeatureDE(casType, "fromEventMention", "edu.cmu.cs.lti.script.type.EventMention", featOkTst);
    casFeatCode_fromEventMention  = (null == casFeat_fromEventMention) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_fromEventMention).getCode();

 
    casFeat_toEventMention = jcas.getRequiredFeatureDE(casType, "toEventMention", "edu.cmu.cs.lti.script.type.EventMention", featOkTst);
    casFeatCode_toEventMention  = (null == casFeat_toEventMention) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_toEventMention).getCode();

 
    casFeat_confidence = jcas.getRequiredFeatureDE(casType, "confidence", "uima.cas.Double", featOkTst);
    casFeatCode_confidence  = (null == casFeat_confidence) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_confidence).getCode();

  }
}



    