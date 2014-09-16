
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
public class EventCoreferenceCluster_Type extends ComponentTOP_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (EventCoreferenceCluster_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = EventCoreferenceCluster_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new EventCoreferenceCluster(addr, EventCoreferenceCluster_Type.this);
  			   EventCoreferenceCluster_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new EventCoreferenceCluster(addr, EventCoreferenceCluster_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = EventCoreferenceCluster.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.EventCoreferenceCluster");
 
  /** @generated */
  final Feature casFeat_clusterType;
  /** @generated */
  final int     casFeatCode_clusterType;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getClusterType(int addr) {
        if (featOkTst && casFeat_clusterType == null)
      jcas.throwFeatMissing("clusterType", "edu.cmu.cs.lti.script.type.EventCoreferenceCluster");
    return ll_cas.ll_getStringValue(addr, casFeatCode_clusterType);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setClusterType(int addr, String v) {
        if (featOkTst && casFeat_clusterType == null)
      jcas.throwFeatMissing("clusterType", "edu.cmu.cs.lti.script.type.EventCoreferenceCluster");
    ll_cas.ll_setStringValue(addr, casFeatCode_clusterType, v);}
    
  
 
  /** @generated */
  final Feature casFeat_parentEventMention;
  /** @generated */
  final int     casFeatCode_parentEventMention;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getParentEventMention(int addr) {
        if (featOkTst && casFeat_parentEventMention == null)
      jcas.throwFeatMissing("parentEventMention", "edu.cmu.cs.lti.script.type.EventCoreferenceCluster");
    return ll_cas.ll_getRefValue(addr, casFeatCode_parentEventMention);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setParentEventMention(int addr, int v) {
        if (featOkTst && casFeat_parentEventMention == null)
      jcas.throwFeatMissing("parentEventMention", "edu.cmu.cs.lti.script.type.EventCoreferenceCluster");
    ll_cas.ll_setRefValue(addr, casFeatCode_parentEventMention, v);}
    
  
 
  /** @generated */
  final Feature casFeat_childEventMentions;
  /** @generated */
  final int     casFeatCode_childEventMentions;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getChildEventMentions(int addr) {
        if (featOkTst && casFeat_childEventMentions == null)
      jcas.throwFeatMissing("childEventMentions", "edu.cmu.cs.lti.script.type.EventCoreferenceCluster");
    return ll_cas.ll_getRefValue(addr, casFeatCode_childEventMentions);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setChildEventMentions(int addr, int v) {
        if (featOkTst && casFeat_childEventMentions == null)
      jcas.throwFeatMissing("childEventMentions", "edu.cmu.cs.lti.script.type.EventCoreferenceCluster");
    ll_cas.ll_setRefValue(addr, casFeatCode_childEventMentions, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public EventCoreferenceCluster_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_clusterType = jcas.getRequiredFeatureDE(casType, "clusterType", "uima.cas.String", featOkTst);
    casFeatCode_clusterType  = (null == casFeat_clusterType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_clusterType).getCode();

 
    casFeat_parentEventMention = jcas.getRequiredFeatureDE(casType, "parentEventMention", "edu.cmu.cs.lti.script.type.EventMention", featOkTst);
    casFeatCode_parentEventMention  = (null == casFeat_parentEventMention) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_parentEventMention).getCode();

 
    casFeat_childEventMentions = jcas.getRequiredFeatureDE(casType, "childEventMentions", "uima.cas.FSList", featOkTst);
    casFeatCode_childEventMentions  = (null == casFeat_childEventMentions) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_childEventMentions).getCode();

  }
}



    