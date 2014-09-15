
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
public class EventMention_Type extends ComponentAnnotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (EventMention_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = EventMention_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new EventMention(addr, EventMention_Type.this);
  			   EventMention_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new EventMention(addr, EventMention_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = EventMention.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.EventMention");
 
  /** @generated */
  final Feature casFeat_goldStandardEventMentionId;
  /** @generated */
  final int     casFeatCode_goldStandardEventMentionId;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getGoldStandardEventMentionId(int addr) {
        if (featOkTst && casFeat_goldStandardEventMentionId == null)
      jcas.throwFeatMissing("goldStandardEventMentionId", "edu.cmu.cs.lti.script.type.EventMention");
    return ll_cas.ll_getStringValue(addr, casFeatCode_goldStandardEventMentionId);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setGoldStandardEventMentionId(int addr, String v) {
        if (featOkTst && casFeat_goldStandardEventMentionId == null)
      jcas.throwFeatMissing("goldStandardEventMentionId", "edu.cmu.cs.lti.script.type.EventMention");
    ll_cas.ll_setStringValue(addr, casFeatCode_goldStandardEventMentionId, v);}
    
  
 
  /** @generated */
  final Feature casFeat_eventMentionIndex;
  /** @generated */
  final int     casFeatCode_eventMentionIndex;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getEventMentionIndex(int addr) {
        if (featOkTst && casFeat_eventMentionIndex == null)
      jcas.throwFeatMissing("eventMentionIndex", "edu.cmu.cs.lti.script.type.EventMention");
    return ll_cas.ll_getIntValue(addr, casFeatCode_eventMentionIndex);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEventMentionIndex(int addr, int v) {
        if (featOkTst && casFeat_eventMentionIndex == null)
      jcas.throwFeatMissing("eventMentionIndex", "edu.cmu.cs.lti.script.type.EventMention");
    ll_cas.ll_setIntValue(addr, casFeatCode_eventMentionIndex, v);}
    
  
 
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
      jcas.throwFeatMissing("eventType", "edu.cmu.cs.lti.script.type.EventMention");
    return ll_cas.ll_getStringValue(addr, casFeatCode_eventType);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEventType(int addr, String v) {
        if (featOkTst && casFeat_eventType == null)
      jcas.throwFeatMissing("eventType", "edu.cmu.cs.lti.script.type.EventMention");
    ll_cas.ll_setStringValue(addr, casFeatCode_eventType, v);}
    
  
 
  /** @generated */
  final Feature casFeat_epistemicStatus;
  /** @generated */
  final int     casFeatCode_epistemicStatus;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getEpistemicStatus(int addr) {
        if (featOkTst && casFeat_epistemicStatus == null)
      jcas.throwFeatMissing("epistemicStatus", "edu.cmu.cs.lti.script.type.EventMention");
    return ll_cas.ll_getStringValue(addr, casFeatCode_epistemicStatus);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEpistemicStatus(int addr, String v) {
        if (featOkTst && casFeat_epistemicStatus == null)
      jcas.throwFeatMissing("epistemicStatus", "edu.cmu.cs.lti.script.type.EventMention");
    ll_cas.ll_setStringValue(addr, casFeatCode_epistemicStatus, v);}
    
  
 
  /** @generated */
  final Feature casFeat_eventCoreferenceClusters;
  /** @generated */
  final int     casFeatCode_eventCoreferenceClusters;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getEventCoreferenceClusters(int addr) {
        if (featOkTst && casFeat_eventCoreferenceClusters == null)
      jcas.throwFeatMissing("eventCoreferenceClusters", "edu.cmu.cs.lti.script.type.EventMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_eventCoreferenceClusters);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEventCoreferenceClusters(int addr, int v) {
        if (featOkTst && casFeat_eventCoreferenceClusters == null)
      jcas.throwFeatMissing("eventCoreferenceClusters", "edu.cmu.cs.lti.script.type.EventMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_eventCoreferenceClusters, v);}
    
  
 
  /** @generated */
  final Feature casFeat_agentLinks;
  /** @generated */
  final int     casFeatCode_agentLinks;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getAgentLinks(int addr) {
        if (featOkTst && casFeat_agentLinks == null)
      jcas.throwFeatMissing("agentLinks", "edu.cmu.cs.lti.script.type.EventMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_agentLinks);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAgentLinks(int addr, int v) {
        if (featOkTst && casFeat_agentLinks == null)
      jcas.throwFeatMissing("agentLinks", "edu.cmu.cs.lti.script.type.EventMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_agentLinks, v);}
    
  
 
  /** @generated */
  final Feature casFeat_patientLinks;
  /** @generated */
  final int     casFeatCode_patientLinks;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getPatientLinks(int addr) {
        if (featOkTst && casFeat_patientLinks == null)
      jcas.throwFeatMissing("patientLinks", "edu.cmu.cs.lti.script.type.EventMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_patientLinks);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setPatientLinks(int addr, int v) {
        if (featOkTst && casFeat_patientLinks == null)
      jcas.throwFeatMissing("patientLinks", "edu.cmu.cs.lti.script.type.EventMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_patientLinks, v);}
    
  
 
  /** @generated */
  final Feature casFeat_locationLinks;
  /** @generated */
  final int     casFeatCode_locationLinks;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getLocationLinks(int addr) {
        if (featOkTst && casFeat_locationLinks == null)
      jcas.throwFeatMissing("locationLinks", "edu.cmu.cs.lti.script.type.EventMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_locationLinks);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setLocationLinks(int addr, int v) {
        if (featOkTst && casFeat_locationLinks == null)
      jcas.throwFeatMissing("locationLinks", "edu.cmu.cs.lti.script.type.EventMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_locationLinks, v);}
    
  
 
  /** @generated */
  final Feature casFeat_timeLinks;
  /** @generated */
  final int     casFeatCode_timeLinks;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getTimeLinks(int addr) {
        if (featOkTst && casFeat_timeLinks == null)
      jcas.throwFeatMissing("timeLinks", "edu.cmu.cs.lti.script.type.EventMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_timeLinks);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTimeLinks(int addr, int v) {
        if (featOkTst && casFeat_timeLinks == null)
      jcas.throwFeatMissing("timeLinks", "edu.cmu.cs.lti.script.type.EventMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_timeLinks, v);}
    
  
 
  /** @generated */
  final Feature casFeat_childEventRelations;
  /** @generated */
  final int     casFeatCode_childEventRelations;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getChildEventRelations(int addr) {
        if (featOkTst && casFeat_childEventRelations == null)
      jcas.throwFeatMissing("childEventRelations", "edu.cmu.cs.lti.script.type.EventMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_childEventRelations);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setChildEventRelations(int addr, int v) {
        if (featOkTst && casFeat_childEventRelations == null)
      jcas.throwFeatMissing("childEventRelations", "edu.cmu.cs.lti.script.type.EventMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_childEventRelations, v);}
    
  
 
  /** @generated */
  final Feature casFeat_headEventRelations;
  /** @generated */
  final int     casFeatCode_headEventRelations;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getHeadEventRelations(int addr) {
        if (featOkTst && casFeat_headEventRelations == null)
      jcas.throwFeatMissing("headEventRelations", "edu.cmu.cs.lti.script.type.EventMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_headEventRelations);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setHeadEventRelations(int addr, int v) {
        if (featOkTst && casFeat_headEventRelations == null)
      jcas.throwFeatMissing("headEventRelations", "edu.cmu.cs.lti.script.type.EventMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_headEventRelations, v);}
    
  
 
  /** @generated */
  final Feature casFeat_headWord;
  /** @generated */
  final int     casFeatCode_headWord;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getHeadWord(int addr) {
        if (featOkTst && casFeat_headWord == null)
      jcas.throwFeatMissing("headWord", "edu.cmu.cs.lti.script.type.EventMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_headWord);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setHeadWord(int addr, int v) {
        if (featOkTst && casFeat_headWord == null)
      jcas.throwFeatMissing("headWord", "edu.cmu.cs.lti.script.type.EventMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_headWord, v);}
    
  
 
  /** @generated */
  final Feature casFeat_singleEventFeatures;
  /** @generated */
  final int     casFeatCode_singleEventFeatures;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getSingleEventFeatures(int addr) {
        if (featOkTst && casFeat_singleEventFeatures == null)
      jcas.throwFeatMissing("singleEventFeatures", "edu.cmu.cs.lti.script.type.EventMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_singleEventFeatures);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setSingleEventFeatures(int addr, int v) {
        if (featOkTst && casFeat_singleEventFeatures == null)
      jcas.throwFeatMissing("singleEventFeatures", "edu.cmu.cs.lti.script.type.EventMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_singleEventFeatures, v);}
    
  
 
  /** @generated */
  final Feature casFeat_quantity;
  /** @generated */
  final int     casFeatCode_quantity;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getQuantity(int addr) {
        if (featOkTst && casFeat_quantity == null)
      jcas.throwFeatMissing("quantity", "edu.cmu.cs.lti.script.type.EventMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_quantity);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setQuantity(int addr, int v) {
        if (featOkTst && casFeat_quantity == null)
      jcas.throwFeatMissing("quantity", "edu.cmu.cs.lti.script.type.EventMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_quantity, v);}
    
  
 
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
      jcas.throwFeatMissing("arguments", "edu.cmu.cs.lti.script.type.EventMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_arguments);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArguments(int addr, int v) {
        if (featOkTst && casFeat_arguments == null)
      jcas.throwFeatMissing("arguments", "edu.cmu.cs.lti.script.type.EventMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_arguments, v);}
    
  
 
  /** @generated */
  final Feature casFeat_mentionContext;
  /** @generated */
  final int     casFeatCode_mentionContext;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getMentionContext(int addr) {
        if (featOkTst && casFeat_mentionContext == null)
      jcas.throwFeatMissing("mentionContext", "edu.cmu.cs.lti.script.type.EventMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_mentionContext);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setMentionContext(int addr, int v) {
        if (featOkTst && casFeat_mentionContext == null)
      jcas.throwFeatMissing("mentionContext", "edu.cmu.cs.lti.script.type.EventMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_mentionContext, v);}
    
  
 
  /** @generated */
  final Feature casFeat_referringEvent;
  /** @generated */
  final int     casFeatCode_referringEvent;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getReferringEvent(int addr) {
        if (featOkTst && casFeat_referringEvent == null)
      jcas.throwFeatMissing("referringEvent", "edu.cmu.cs.lti.script.type.EventMention");
    return ll_cas.ll_getRefValue(addr, casFeatCode_referringEvent);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setReferringEvent(int addr, int v) {
        if (featOkTst && casFeat_referringEvent == null)
      jcas.throwFeatMissing("referringEvent", "edu.cmu.cs.lti.script.type.EventMention");
    ll_cas.ll_setRefValue(addr, casFeatCode_referringEvent, v);}
    
  
 
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
      jcas.throwFeatMissing("frameName", "edu.cmu.cs.lti.script.type.EventMention");
    return ll_cas.ll_getStringValue(addr, casFeatCode_frameName);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setFrameName(int addr, String v) {
        if (featOkTst && casFeat_frameName == null)
      jcas.throwFeatMissing("frameName", "edu.cmu.cs.lti.script.type.EventMention");
    ll_cas.ll_setStringValue(addr, casFeatCode_frameName, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public EventMention_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_goldStandardEventMentionId = jcas.getRequiredFeatureDE(casType, "goldStandardEventMentionId", "uima.cas.String", featOkTst);
    casFeatCode_goldStandardEventMentionId  = (null == casFeat_goldStandardEventMentionId) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_goldStandardEventMentionId).getCode();

 
    casFeat_eventMentionIndex = jcas.getRequiredFeatureDE(casType, "eventMentionIndex", "uima.cas.Integer", featOkTst);
    casFeatCode_eventMentionIndex  = (null == casFeat_eventMentionIndex) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_eventMentionIndex).getCode();

 
    casFeat_eventType = jcas.getRequiredFeatureDE(casType, "eventType", "uima.cas.String", featOkTst);
    casFeatCode_eventType  = (null == casFeat_eventType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_eventType).getCode();

 
    casFeat_epistemicStatus = jcas.getRequiredFeatureDE(casType, "epistemicStatus", "uima.cas.String", featOkTst);
    casFeatCode_epistemicStatus  = (null == casFeat_epistemicStatus) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_epistemicStatus).getCode();

 
    casFeat_eventCoreferenceClusters = jcas.getRequiredFeatureDE(casType, "eventCoreferenceClusters", "uima.cas.FSList", featOkTst);
    casFeatCode_eventCoreferenceClusters  = (null == casFeat_eventCoreferenceClusters) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_eventCoreferenceClusters).getCode();

 
    casFeat_agentLinks = jcas.getRequiredFeatureDE(casType, "agentLinks", "uima.cas.FSList", featOkTst);
    casFeatCode_agentLinks  = (null == casFeat_agentLinks) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_agentLinks).getCode();

 
    casFeat_patientLinks = jcas.getRequiredFeatureDE(casType, "patientLinks", "uima.cas.FSList", featOkTst);
    casFeatCode_patientLinks  = (null == casFeat_patientLinks) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_patientLinks).getCode();

 
    casFeat_locationLinks = jcas.getRequiredFeatureDE(casType, "locationLinks", "uima.cas.FSList", featOkTst);
    casFeatCode_locationLinks  = (null == casFeat_locationLinks) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_locationLinks).getCode();

 
    casFeat_timeLinks = jcas.getRequiredFeatureDE(casType, "timeLinks", "uima.cas.FSList", featOkTst);
    casFeatCode_timeLinks  = (null == casFeat_timeLinks) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_timeLinks).getCode();

 
    casFeat_childEventRelations = jcas.getRequiredFeatureDE(casType, "childEventRelations", "uima.cas.FSList", featOkTst);
    casFeatCode_childEventRelations  = (null == casFeat_childEventRelations) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_childEventRelations).getCode();

 
    casFeat_headEventRelations = jcas.getRequiredFeatureDE(casType, "headEventRelations", "uima.cas.FSList", featOkTst);
    casFeatCode_headEventRelations  = (null == casFeat_headEventRelations) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_headEventRelations).getCode();

 
    casFeat_headWord = jcas.getRequiredFeatureDE(casType, "headWord", "edu.cmu.cs.lti.script.type.Word", featOkTst);
    casFeatCode_headWord  = (null == casFeat_headWord) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_headWord).getCode();

 
    casFeat_singleEventFeatures = jcas.getRequiredFeatureDE(casType, "singleEventFeatures", "uima.cas.FSList", featOkTst);
    casFeatCode_singleEventFeatures  = (null == casFeat_singleEventFeatures) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_singleEventFeatures).getCode();

 
    casFeat_quantity = jcas.getRequiredFeatureDE(casType, "quantity", "edu.cmu.cs.lti.script.type.NumberAnnotation", featOkTst);
    casFeatCode_quantity  = (null == casFeat_quantity) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_quantity).getCode();

 
    casFeat_arguments = jcas.getRequiredFeatureDE(casType, "arguments", "uima.cas.FSList", featOkTst);
    casFeatCode_arguments  = (null == casFeat_arguments) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_arguments).getCode();

 
    casFeat_mentionContext = jcas.getRequiredFeatureDE(casType, "mentionContext", "edu.cmu.cs.lti.script.type.EventMentionContext", featOkTst);
    casFeatCode_mentionContext  = (null == casFeat_mentionContext) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_mentionContext).getCode();

 
    casFeat_referringEvent = jcas.getRequiredFeatureDE(casType, "referringEvent", "edu.cmu.cs.lti.script.type.Event", featOkTst);
    casFeatCode_referringEvent  = (null == casFeat_referringEvent) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_referringEvent).getCode();

 
    casFeat_frameName = jcas.getRequiredFeatureDE(casType, "frameName", "uima.cas.String", featOkTst);
    casFeatCode_frameName  = (null == casFeat_frameName) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_frameName).getCode();

  }
}



    