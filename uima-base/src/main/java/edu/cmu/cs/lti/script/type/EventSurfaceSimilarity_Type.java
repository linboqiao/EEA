
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
public class EventSurfaceSimilarity_Type extends ComponentTOP_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (EventSurfaceSimilarity_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = EventSurfaceSimilarity_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new EventSurfaceSimilarity(addr, EventSurfaceSimilarity_Type.this);
  			   EventSurfaceSimilarity_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new EventSurfaceSimilarity(addr, EventSurfaceSimilarity_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = EventSurfaceSimilarity.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
 
  /** @generated */
  final Feature casFeat_wordNetWuPalmer;
  /** @generated */
  final int     casFeatCode_wordNetWuPalmer;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public double getWordNetWuPalmer(int addr) {
        if (featOkTst && casFeat_wordNetWuPalmer == null)
      jcas.throwFeatMissing("wordNetWuPalmer", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    return ll_cas.ll_getDoubleValue(addr, casFeatCode_wordNetWuPalmer);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setWordNetWuPalmer(int addr, double v) {
        if (featOkTst && casFeat_wordNetWuPalmer == null)
      jcas.throwFeatMissing("wordNetWuPalmer", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    ll_cas.ll_setDoubleValue(addr, casFeatCode_wordNetWuPalmer, v);}
    
  
 
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
      jcas.throwFeatMissing("eventMentionI", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    return ll_cas.ll_getRefValue(addr, casFeatCode_eventMentionI);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEventMentionI(int addr, int v) {
        if (featOkTst && casFeat_eventMentionI == null)
      jcas.throwFeatMissing("eventMentionI", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
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
      jcas.throwFeatMissing("eventMentionJ", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    return ll_cas.ll_getRefValue(addr, casFeatCode_eventMentionJ);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEventMentionJ(int addr, int v) {
        if (featOkTst && casFeat_eventMentionJ == null)
      jcas.throwFeatMissing("eventMentionJ", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    ll_cas.ll_setRefValue(addr, casFeatCode_eventMentionJ, v);}
    
  
 
  /** @generated */
  final Feature casFeat_sennaSimilarity;
  /** @generated */
  final int     casFeatCode_sennaSimilarity;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public double getSennaSimilarity(int addr) {
        if (featOkTst && casFeat_sennaSimilarity == null)
      jcas.throwFeatMissing("sennaSimilarity", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    return ll_cas.ll_getDoubleValue(addr, casFeatCode_sennaSimilarity);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setSennaSimilarity(int addr, double v) {
        if (featOkTst && casFeat_sennaSimilarity == null)
      jcas.throwFeatMissing("sennaSimilarity", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    ll_cas.ll_setDoubleValue(addr, casFeatCode_sennaSimilarity, v);}
    
  
 
  /** @generated */
  final Feature casFeat_diceCoefficient;
  /** @generated */
  final int     casFeatCode_diceCoefficient;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public double getDiceCoefficient(int addr) {
        if (featOkTst && casFeat_diceCoefficient == null)
      jcas.throwFeatMissing("diceCoefficient", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    return ll_cas.ll_getDoubleValue(addr, casFeatCode_diceCoefficient);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setDiceCoefficient(int addr, double v) {
        if (featOkTst && casFeat_diceCoefficient == null)
      jcas.throwFeatMissing("diceCoefficient", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    ll_cas.ll_setDoubleValue(addr, casFeatCode_diceCoefficient, v);}
    
  
 
  /** @generated */
  final Feature casFeat_morphalizedWuPalmer;
  /** @generated */
  final int     casFeatCode_morphalizedWuPalmer;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public double getMorphalizedWuPalmer(int addr) {
        if (featOkTst && casFeat_morphalizedWuPalmer == null)
      jcas.throwFeatMissing("morphalizedWuPalmer", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    return ll_cas.ll_getDoubleValue(addr, casFeatCode_morphalizedWuPalmer);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setMorphalizedWuPalmer(int addr, double v) {
        if (featOkTst && casFeat_morphalizedWuPalmer == null)
      jcas.throwFeatMissing("morphalizedWuPalmer", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    ll_cas.ll_setDoubleValue(addr, casFeatCode_morphalizedWuPalmer, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public EventSurfaceSimilarity_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_wordNetWuPalmer = jcas.getRequiredFeatureDE(casType, "wordNetWuPalmer", "uima.cas.Double", featOkTst);
    casFeatCode_wordNetWuPalmer  = (null == casFeat_wordNetWuPalmer) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_wordNetWuPalmer).getCode();

 
    casFeat_eventMentionI = jcas.getRequiredFeatureDE(casType, "eventMentionI", "edu.cmu.cs.lti.script.type.EventMention", featOkTst);
    casFeatCode_eventMentionI  = (null == casFeat_eventMentionI) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_eventMentionI).getCode();

 
    casFeat_eventMentionJ = jcas.getRequiredFeatureDE(casType, "eventMentionJ", "edu.cmu.cs.lti.script.type.EventMention", featOkTst);
    casFeatCode_eventMentionJ  = (null == casFeat_eventMentionJ) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_eventMentionJ).getCode();

 
    casFeat_sennaSimilarity = jcas.getRequiredFeatureDE(casType, "sennaSimilarity", "uima.cas.Double", featOkTst);
    casFeatCode_sennaSimilarity  = (null == casFeat_sennaSimilarity) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_sennaSimilarity).getCode();

 
    casFeat_diceCoefficient = jcas.getRequiredFeatureDE(casType, "diceCoefficient", "uima.cas.Double", featOkTst);
    casFeatCode_diceCoefficient  = (null == casFeat_diceCoefficient) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_diceCoefficient).getCode();

 
    casFeat_morphalizedWuPalmer = jcas.getRequiredFeatureDE(casType, "morphalizedWuPalmer", "uima.cas.Double", featOkTst);
    casFeatCode_morphalizedWuPalmer  = (null == casFeat_morphalizedWuPalmer) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_morphalizedWuPalmer).getCode();

  }
}



    