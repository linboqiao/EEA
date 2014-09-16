
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
public class EntityBasedComponent_Type extends ComponentAnnotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (EntityBasedComponent_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = EntityBasedComponent_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new EntityBasedComponent(addr, EntityBasedComponent_Type.this);
  			   EntityBasedComponent_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new EntityBasedComponent(addr, EntityBasedComponent_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = EntityBasedComponent.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.EntityBasedComponent");
 
  /** @generated */
  final Feature casFeat_containingEntityMentions;
  /** @generated */
  final int     casFeatCode_containingEntityMentions;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getContainingEntityMentions(int addr) {
        if (featOkTst && casFeat_containingEntityMentions == null)
      jcas.throwFeatMissing("containingEntityMentions", "edu.cmu.cs.lti.script.type.EntityBasedComponent");
    return ll_cas.ll_getRefValue(addr, casFeatCode_containingEntityMentions);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setContainingEntityMentions(int addr, int v) {
        if (featOkTst && casFeat_containingEntityMentions == null)
      jcas.throwFeatMissing("containingEntityMentions", "edu.cmu.cs.lti.script.type.EntityBasedComponent");
    ll_cas.ll_setRefValue(addr, casFeatCode_containingEntityMentions, v);}
    
  
 
  /** @generated */
  final Feature casFeat_componentLinks;
  /** @generated */
  final int     casFeatCode_componentLinks;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getComponentLinks(int addr) {
        if (featOkTst && casFeat_componentLinks == null)
      jcas.throwFeatMissing("componentLinks", "edu.cmu.cs.lti.script.type.EntityBasedComponent");
    return ll_cas.ll_getRefValue(addr, casFeatCode_componentLinks);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setComponentLinks(int addr, int v) {
        if (featOkTst && casFeat_componentLinks == null)
      jcas.throwFeatMissing("componentLinks", "edu.cmu.cs.lti.script.type.EntityBasedComponent");
    ll_cas.ll_setRefValue(addr, casFeatCode_componentLinks, v);}
    
  
 
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
      jcas.throwFeatMissing("headWord", "edu.cmu.cs.lti.script.type.EntityBasedComponent");
    return ll_cas.ll_getRefValue(addr, casFeatCode_headWord);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setHeadWord(int addr, int v) {
        if (featOkTst && casFeat_headWord == null)
      jcas.throwFeatMissing("headWord", "edu.cmu.cs.lti.script.type.EntityBasedComponent");
    ll_cas.ll_setRefValue(addr, casFeatCode_headWord, v);}
    
  
 
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
      jcas.throwFeatMissing("quantity", "edu.cmu.cs.lti.script.type.EntityBasedComponent");
    return ll_cas.ll_getRefValue(addr, casFeatCode_quantity);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setQuantity(int addr, int v) {
        if (featOkTst && casFeat_quantity == null)
      jcas.throwFeatMissing("quantity", "edu.cmu.cs.lti.script.type.EntityBasedComponent");
    ll_cas.ll_setRefValue(addr, casFeatCode_quantity, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public EntityBasedComponent_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_containingEntityMentions = jcas.getRequiredFeatureDE(casType, "containingEntityMentions", "uima.cas.FSList", featOkTst);
    casFeatCode_containingEntityMentions  = (null == casFeat_containingEntityMentions) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_containingEntityMentions).getCode();

 
    casFeat_componentLinks = jcas.getRequiredFeatureDE(casType, "componentLinks", "uima.cas.FSList", featOkTst);
    casFeatCode_componentLinks  = (null == casFeat_componentLinks) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_componentLinks).getCode();

 
    casFeat_headWord = jcas.getRequiredFeatureDE(casType, "headWord", "edu.cmu.cs.lti.script.type.Word", featOkTst);
    casFeatCode_headWord  = (null == casFeat_headWord) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_headWord).getCode();

 
    casFeat_quantity = jcas.getRequiredFeatureDE(casType, "quantity", "edu.cmu.cs.lti.script.type.NumberAnnotation", featOkTst);
    casFeatCode_quantity  = (null == casFeat_quantity) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_quantity).getCode();

  }
}



    