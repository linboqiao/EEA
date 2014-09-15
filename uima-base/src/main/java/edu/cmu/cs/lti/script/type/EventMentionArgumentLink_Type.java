
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

/** Link between an event mention to its argument (which is an entity mention)
 * Updated by JCasGen Sun Sep 14 23:46:31 EDT 2014
 * @generated */
public class EventMentionArgumentLink_Type extends ComponentTOP_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (EventMentionArgumentLink_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = EventMentionArgumentLink_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new EventMentionArgumentLink(addr, EventMentionArgumentLink_Type.this);
  			   EventMentionArgumentLink_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new EventMentionArgumentLink(addr, EventMentionArgumentLink_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = EventMentionArgumentLink.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
 
  /** @generated */
  final Feature casFeat_eventMention;
  /** @generated */
  final int     casFeatCode_eventMention;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getEventMention(int addr) {
        if (featOkTst && casFeat_eventMention == null)
      jcas.throwFeatMissing("eventMention", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    return ll_cas.ll_getRefValue(addr, casFeatCode_eventMention);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEventMention(int addr, int v) {
        if (featOkTst && casFeat_eventMention == null)
      jcas.throwFeatMissing("eventMention", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    ll_cas.ll_setRefValue(addr, casFeatCode_eventMention, v);}
    
  
 
  /** @generated */
  final Feature casFeat_verbNetRoleName;
  /** @generated */
  final int     casFeatCode_verbNetRoleName;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getVerbNetRoleName(int addr) {
        if (featOkTst && casFeat_verbNetRoleName == null)
      jcas.throwFeatMissing("verbNetRoleName", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    return ll_cas.ll_getStringValue(addr, casFeatCode_verbNetRoleName);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setVerbNetRoleName(int addr, String v) {
        if (featOkTst && casFeat_verbNetRoleName == null)
      jcas.throwFeatMissing("verbNetRoleName", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    ll_cas.ll_setStringValue(addr, casFeatCode_verbNetRoleName, v);}
    
  
 
  /** @generated */
  final Feature casFeat_frameElementName;
  /** @generated */
  final int     casFeatCode_frameElementName;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getFrameElementName(int addr) {
        if (featOkTst && casFeat_frameElementName == null)
      jcas.throwFeatMissing("frameElementName", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    return ll_cas.ll_getStringValue(addr, casFeatCode_frameElementName);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setFrameElementName(int addr, String v) {
        if (featOkTst && casFeat_frameElementName == null)
      jcas.throwFeatMissing("frameElementName", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    ll_cas.ll_setStringValue(addr, casFeatCode_frameElementName, v);}
    
  
 
  /** @generated */
  final Feature casFeat_propbankRoleName;
  /** @generated */
  final int     casFeatCode_propbankRoleName;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getPropbankRoleName(int addr) {
        if (featOkTst && casFeat_propbankRoleName == null)
      jcas.throwFeatMissing("propbankRoleName", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    return ll_cas.ll_getStringValue(addr, casFeatCode_propbankRoleName);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setPropbankRoleName(int addr, String v) {
        if (featOkTst && casFeat_propbankRoleName == null)
      jcas.throwFeatMissing("propbankRoleName", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    ll_cas.ll_setStringValue(addr, casFeatCode_propbankRoleName, v);}
    
  
 
  /** @generated */
  final Feature casFeat_argumentRole;
  /** @generated */
  final int     casFeatCode_argumentRole;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getArgumentRole(int addr) {
        if (featOkTst && casFeat_argumentRole == null)
      jcas.throwFeatMissing("argumentRole", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    return ll_cas.ll_getStringValue(addr, casFeatCode_argumentRole);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArgumentRole(int addr, String v) {
        if (featOkTst && casFeat_argumentRole == null)
      jcas.throwFeatMissing("argumentRole", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    ll_cas.ll_setStringValue(addr, casFeatCode_argumentRole, v);}
    
  
 
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
      jcas.throwFeatMissing("confidence", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    return ll_cas.ll_getDoubleValue(addr, casFeatCode_confidence);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setConfidence(int addr, double v) {
        if (featOkTst && casFeat_confidence == null)
      jcas.throwFeatMissing("confidence", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    ll_cas.ll_setDoubleValue(addr, casFeatCode_confidence, v);}
    
  
 
  /** @generated */
  final Feature casFeat_superFrameElementRoleNames;
  /** @generated */
  final int     casFeatCode_superFrameElementRoleNames;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getSuperFrameElementRoleNames(int addr) {
        if (featOkTst && casFeat_superFrameElementRoleNames == null)
      jcas.throwFeatMissing("superFrameElementRoleNames", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    return ll_cas.ll_getRefValue(addr, casFeatCode_superFrameElementRoleNames);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setSuperFrameElementRoleNames(int addr, int v) {
        if (featOkTst && casFeat_superFrameElementRoleNames == null)
      jcas.throwFeatMissing("superFrameElementRoleNames", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    ll_cas.ll_setRefValue(addr, casFeatCode_superFrameElementRoleNames, v);}
    
  
 
  /** @generated */
  final Feature casFeat_argument;
  /** @generated */
  final int     casFeatCode_argument;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getArgument(int addr) {
        if (featOkTst && casFeat_argument == null)
      jcas.throwFeatMissing("argument", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    return ll_cas.ll_getRefValue(addr, casFeatCode_argument);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArgument(int addr, int v) {
        if (featOkTst && casFeat_argument == null)
      jcas.throwFeatMissing("argument", "edu.cmu.cs.lti.script.type.EventMentionArgumentLink");
    ll_cas.ll_setRefValue(addr, casFeatCode_argument, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public EventMentionArgumentLink_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_eventMention = jcas.getRequiredFeatureDE(casType, "eventMention", "edu.cmu.cs.lti.script.type.EventMention", featOkTst);
    casFeatCode_eventMention  = (null == casFeat_eventMention) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_eventMention).getCode();

 
    casFeat_verbNetRoleName = jcas.getRequiredFeatureDE(casType, "verbNetRoleName", "uima.cas.String", featOkTst);
    casFeatCode_verbNetRoleName  = (null == casFeat_verbNetRoleName) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_verbNetRoleName).getCode();

 
    casFeat_frameElementName = jcas.getRequiredFeatureDE(casType, "frameElementName", "uima.cas.String", featOkTst);
    casFeatCode_frameElementName  = (null == casFeat_frameElementName) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_frameElementName).getCode();

 
    casFeat_propbankRoleName = jcas.getRequiredFeatureDE(casType, "propbankRoleName", "uima.cas.String", featOkTst);
    casFeatCode_propbankRoleName  = (null == casFeat_propbankRoleName) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_propbankRoleName).getCode();

 
    casFeat_argumentRole = jcas.getRequiredFeatureDE(casType, "argumentRole", "uima.cas.String", featOkTst);
    casFeatCode_argumentRole  = (null == casFeat_argumentRole) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_argumentRole).getCode();

 
    casFeat_confidence = jcas.getRequiredFeatureDE(casType, "confidence", "uima.cas.Double", featOkTst);
    casFeatCode_confidence  = (null == casFeat_confidence) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_confidence).getCode();

 
    casFeat_superFrameElementRoleNames = jcas.getRequiredFeatureDE(casType, "superFrameElementRoleNames", "uima.cas.StringList", featOkTst);
    casFeatCode_superFrameElementRoleNames  = (null == casFeat_superFrameElementRoleNames) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_superFrameElementRoleNames).getCode();

 
    casFeat_argument = jcas.getRequiredFeatureDE(casType, "argument", "edu.cmu.cs.lti.script.type.EntityBasedComponent", featOkTst);
    casFeatCode_argument  = (null == casFeat_argument) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_argument).getCode();

  }
}



    