
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
public class EntityBasedComponentLink_Type extends ComponentTOP_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (EntityBasedComponentLink_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = EntityBasedComponentLink_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new EntityBasedComponentLink(addr, EntityBasedComponentLink_Type.this);
  			   EntityBasedComponentLink_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new EntityBasedComponentLink(addr, EntityBasedComponentLink_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = EntityBasedComponentLink.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.EntityBasedComponentLink");
 
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
      jcas.throwFeatMissing("eventMention", "edu.cmu.cs.lti.script.type.EntityBasedComponentLink");
    return ll_cas.ll_getRefValue(addr, casFeatCode_eventMention);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEventMention(int addr, int v) {
        if (featOkTst && casFeat_eventMention == null)
      jcas.throwFeatMissing("eventMention", "edu.cmu.cs.lti.script.type.EntityBasedComponentLink");
    ll_cas.ll_setRefValue(addr, casFeatCode_eventMention, v);}
    
  
 
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
      jcas.throwFeatMissing("argument", "edu.cmu.cs.lti.script.type.EntityBasedComponentLink");
    return ll_cas.ll_getRefValue(addr, casFeatCode_argument);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArgument(int addr, int v) {
        if (featOkTst && casFeat_argument == null)
      jcas.throwFeatMissing("argument", "edu.cmu.cs.lti.script.type.EntityBasedComponentLink");
    ll_cas.ll_setRefValue(addr, casFeatCode_argument, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public EntityBasedComponentLink_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_eventMention = jcas.getRequiredFeatureDE(casType, "eventMention", "edu.cmu.cs.lti.script.type.EventMention", featOkTst);
    casFeatCode_eventMention  = (null == casFeat_eventMention) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_eventMention).getCode();

 
    casFeat_argument = jcas.getRequiredFeatureDE(casType, "argument", "edu.cmu.cs.lti.script.type.EntityBasedComponent", featOkTst);
    casFeatCode_argument  = (null == casFeat_argument) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_argument).getCode();

  }
}



    