
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

/** An abstract annotation for event, which is a generalized concept that contains event mentions. It could be seen as we assign some attributes to the event mention clusters.
 * Updated by JCasGen Sun Sep 14 23:46:31 EDT 2014
 * @generated */
public class Event_Type extends ComponentTOP_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Event_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Event_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Event(addr, Event_Type.this);
  			   Event_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Event(addr, Event_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Event.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.Event");
 
  /** @generated */
  final Feature casFeat_eventType;
  /** @generated */
  final int     casFeatCode_eventType;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getEventType(int addr) {
        if (featOkTst && casFeat_eventType == null)
      jcas.throwFeatMissing("eventType", "edu.cmu.cs.lti.script.type.Event");
    return ll_cas.ll_getStringValue(addr, casFeatCode_eventType);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEventType(int addr, String v) {
        if (featOkTst && casFeat_eventType == null)
      jcas.throwFeatMissing("eventType", "edu.cmu.cs.lti.script.type.Event");
    ll_cas.ll_setStringValue(addr, casFeatCode_eventType, v);}
    
  
 
  /** @generated */
  final Feature casFeat_eventSubtype;
  /** @generated */
  final int     casFeatCode_eventSubtype;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getEventSubtype(int addr) {
        if (featOkTst && casFeat_eventSubtype == null)
      jcas.throwFeatMissing("eventSubtype", "edu.cmu.cs.lti.script.type.Event");
    return ll_cas.ll_getStringValue(addr, casFeatCode_eventSubtype);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEventSubtype(int addr, String v) {
        if (featOkTst && casFeat_eventSubtype == null)
      jcas.throwFeatMissing("eventSubtype", "edu.cmu.cs.lti.script.type.Event");
    ll_cas.ll_setStringValue(addr, casFeatCode_eventSubtype, v);}
    
  
 
  /** @generated */
  final Feature casFeat_modality;
  /** @generated */
  final int     casFeatCode_modality;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getModality(int addr) {
        if (featOkTst && casFeat_modality == null)
      jcas.throwFeatMissing("modality", "edu.cmu.cs.lti.script.type.Event");
    return ll_cas.ll_getStringValue(addr, casFeatCode_modality);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setModality(int addr, String v) {
        if (featOkTst && casFeat_modality == null)
      jcas.throwFeatMissing("modality", "edu.cmu.cs.lti.script.type.Event");
    ll_cas.ll_setStringValue(addr, casFeatCode_modality, v);}
    
  
 
  /** @generated */
  final Feature casFeat_polarity;
  /** @generated */
  final int     casFeatCode_polarity;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getPolarity(int addr) {
        if (featOkTst && casFeat_polarity == null)
      jcas.throwFeatMissing("polarity", "edu.cmu.cs.lti.script.type.Event");
    return ll_cas.ll_getStringValue(addr, casFeatCode_polarity);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setPolarity(int addr, String v) {
        if (featOkTst && casFeat_polarity == null)
      jcas.throwFeatMissing("polarity", "edu.cmu.cs.lti.script.type.Event");
    ll_cas.ll_setStringValue(addr, casFeatCode_polarity, v);}
    
  
 
  /** @generated */
  final Feature casFeat_genericity;
  /** @generated */
  final int     casFeatCode_genericity;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getGenericity(int addr) {
        if (featOkTst && casFeat_genericity == null)
      jcas.throwFeatMissing("genericity", "edu.cmu.cs.lti.script.type.Event");
    return ll_cas.ll_getStringValue(addr, casFeatCode_genericity);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setGenericity(int addr, String v) {
        if (featOkTst && casFeat_genericity == null)
      jcas.throwFeatMissing("genericity", "edu.cmu.cs.lti.script.type.Event");
    ll_cas.ll_setStringValue(addr, casFeatCode_genericity, v);}
    
  
 
  /** @generated */
  final Feature casFeat_tense;
  /** @generated */
  final int     casFeatCode_tense;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getTense(int addr) {
        if (featOkTst && casFeat_tense == null)
      jcas.throwFeatMissing("tense", "edu.cmu.cs.lti.script.type.Event");
    return ll_cas.ll_getStringValue(addr, casFeatCode_tense);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTense(int addr, String v) {
        if (featOkTst && casFeat_tense == null)
      jcas.throwFeatMissing("tense", "edu.cmu.cs.lti.script.type.Event");
    ll_cas.ll_setStringValue(addr, casFeatCode_tense, v);}
    
  
 
  /** @generated */
  final Feature casFeat_arguments;
  /** @generated */
  final int     casFeatCode_arguments;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getArguments(int addr) {
        if (featOkTst && casFeat_arguments == null)
      jcas.throwFeatMissing("arguments", "edu.cmu.cs.lti.script.type.Event");
    return ll_cas.ll_getRefValue(addr, casFeatCode_arguments);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArguments(int addr, int v) {
        if (featOkTst && casFeat_arguments == null)
      jcas.throwFeatMissing("arguments", "edu.cmu.cs.lti.script.type.Event");
    ll_cas.ll_setRefValue(addr, casFeatCode_arguments, v);}
    
  
 
  /** @generated */
  final Feature casFeat_eventIndex;
  /** @generated */
  final int     casFeatCode_eventIndex;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getEventIndex(int addr) {
        if (featOkTst && casFeat_eventIndex == null)
      jcas.throwFeatMissing("eventIndex", "edu.cmu.cs.lti.script.type.Event");
    return ll_cas.ll_getIntValue(addr, casFeatCode_eventIndex);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEventIndex(int addr, int v) {
        if (featOkTst && casFeat_eventIndex == null)
      jcas.throwFeatMissing("eventIndex", "edu.cmu.cs.lti.script.type.Event");
    ll_cas.ll_setIntValue(addr, casFeatCode_eventIndex, v);}
    
  
 
  /** @generated */
  final Feature casFeat_eventMentions;
  /** @generated */
  final int     casFeatCode_eventMentions;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getEventMentions(int addr) {
        if (featOkTst && casFeat_eventMentions == null)
      jcas.throwFeatMissing("eventMentions", "edu.cmu.cs.lti.script.type.Event");
    return ll_cas.ll_getRefValue(addr, casFeatCode_eventMentions);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEventMentions(int addr, int v) {
        if (featOkTst && casFeat_eventMentions == null)
      jcas.throwFeatMissing("eventMentions", "edu.cmu.cs.lti.script.type.Event");
    ll_cas.ll_setRefValue(addr, casFeatCode_eventMentions, v);}
    
  
 
  /** @generated */
  final Feature casFeat_isEmpty;
  /** @generated */
  final int     casFeatCode_isEmpty;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public boolean getIsEmpty(int addr) {
        if (featOkTst && casFeat_isEmpty == null)
      jcas.throwFeatMissing("isEmpty", "edu.cmu.cs.lti.script.type.Event");
    return ll_cas.ll_getBooleanValue(addr, casFeatCode_isEmpty);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setIsEmpty(int addr, boolean v) {
        if (featOkTst && casFeat_isEmpty == null)
      jcas.throwFeatMissing("isEmpty", "edu.cmu.cs.lti.script.type.Event");
    ll_cas.ll_setBooleanValue(addr, casFeatCode_isEmpty, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Event_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_eventType = jcas.getRequiredFeatureDE(casType, "eventType", "uima.cas.String", featOkTst);
    casFeatCode_eventType  = (null == casFeat_eventType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_eventType).getCode();

 
    casFeat_eventSubtype = jcas.getRequiredFeatureDE(casType, "eventSubtype", "uima.cas.String", featOkTst);
    casFeatCode_eventSubtype  = (null == casFeat_eventSubtype) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_eventSubtype).getCode();

 
    casFeat_modality = jcas.getRequiredFeatureDE(casType, "modality", "uima.cas.String", featOkTst);
    casFeatCode_modality  = (null == casFeat_modality) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_modality).getCode();

 
    casFeat_polarity = jcas.getRequiredFeatureDE(casType, "polarity", "uima.cas.String", featOkTst);
    casFeatCode_polarity  = (null == casFeat_polarity) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_polarity).getCode();

 
    casFeat_genericity = jcas.getRequiredFeatureDE(casType, "genericity", "uima.cas.String", featOkTst);
    casFeatCode_genericity  = (null == casFeat_genericity) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_genericity).getCode();

 
    casFeat_tense = jcas.getRequiredFeatureDE(casType, "tense", "uima.cas.String", featOkTst);
    casFeatCode_tense  = (null == casFeat_tense) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_tense).getCode();

 
    casFeat_arguments = jcas.getRequiredFeatureDE(casType, "arguments", "uima.cas.FSList", featOkTst);
    casFeatCode_arguments  = (null == casFeat_arguments) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_arguments).getCode();

 
    casFeat_eventIndex = jcas.getRequiredFeatureDE(casType, "eventIndex", "uima.cas.Integer", featOkTst);
    casFeatCode_eventIndex  = (null == casFeat_eventIndex) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_eventIndex).getCode();

 
    casFeat_eventMentions = jcas.getRequiredFeatureDE(casType, "eventMentions", "uima.cas.FSList", featOkTst);
    casFeatCode_eventMentions  = (null == casFeat_eventMentions) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_eventMentions).getCode();

 
    casFeat_isEmpty = jcas.getRequiredFeatureDE(casType, "isEmpty", "uima.cas.Boolean", featOkTst);
    casFeatCode_isEmpty  = (null == casFeat_isEmpty) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_isEmpty).getCode();

  }
}



    