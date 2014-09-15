
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
public class EntityMentionAlignment_Type extends ComponentTOP_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (EntityMentionAlignment_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = EntityMentionAlignment_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new EntityMentionAlignment(addr, EntityMentionAlignment_Type.this);
  			   EntityMentionAlignment_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new EntityMentionAlignment(addr, EntityMentionAlignment_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = EntityMentionAlignment.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.EntityMentionAlignment");
 
  /** @generated */
  final Feature casFeat_sourceViewName;
  /** @generated */
  final int     casFeatCode_sourceViewName;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getSourceViewName(int addr) {
        if (featOkTst && casFeat_sourceViewName == null)
      jcas.throwFeatMissing("sourceViewName", "edu.cmu.cs.lti.script.type.EntityMentionAlignment");
    return ll_cas.ll_getStringValue(addr, casFeatCode_sourceViewName);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setSourceViewName(int addr, String v) {
        if (featOkTst && casFeat_sourceViewName == null)
      jcas.throwFeatMissing("sourceViewName", "edu.cmu.cs.lti.script.type.EntityMentionAlignment");
    ll_cas.ll_setStringValue(addr, casFeatCode_sourceViewName, v);}
    
  
 
  /** @generated */
  final Feature casFeat_targetViewName;
  /** @generated */
  final int     casFeatCode_targetViewName;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getTargetViewName(int addr) {
        if (featOkTst && casFeat_targetViewName == null)
      jcas.throwFeatMissing("targetViewName", "edu.cmu.cs.lti.script.type.EntityMentionAlignment");
    return ll_cas.ll_getStringValue(addr, casFeatCode_targetViewName);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTargetViewName(int addr, String v) {
        if (featOkTst && casFeat_targetViewName == null)
      jcas.throwFeatMissing("targetViewName", "edu.cmu.cs.lti.script.type.EntityMentionAlignment");
    ll_cas.ll_setStringValue(addr, casFeatCode_targetViewName, v);}
    
  
 
  /** @generated */
  final Feature casFeat_sourceEntityMention;
  /** @generated */
  final int     casFeatCode_sourceEntityMention;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getSourceEntityMention(int addr) {
        if (featOkTst && casFeat_sourceEntityMention == null)
      jcas.throwFeatMissing("sourceEntityMention", "edu.cmu.cs.lti.script.type.EntityMentionAlignment");
    return ll_cas.ll_getRefValue(addr, casFeatCode_sourceEntityMention);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setSourceEntityMention(int addr, int v) {
        if (featOkTst && casFeat_sourceEntityMention == null)
      jcas.throwFeatMissing("sourceEntityMention", "edu.cmu.cs.lti.script.type.EntityMentionAlignment");
    ll_cas.ll_setRefValue(addr, casFeatCode_sourceEntityMention, v);}
    
  
 
  /** @generated */
  final Feature casFeat_targetEntityMention;
  /** @generated */
  final int     casFeatCode_targetEntityMention;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getTargetEntityMention(int addr) {
        if (featOkTst && casFeat_targetEntityMention == null)
      jcas.throwFeatMissing("targetEntityMention", "edu.cmu.cs.lti.script.type.EntityMentionAlignment");
    return ll_cas.ll_getRefValue(addr, casFeatCode_targetEntityMention);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTargetEntityMention(int addr, int v) {
        if (featOkTst && casFeat_targetEntityMention == null)
      jcas.throwFeatMissing("targetEntityMention", "edu.cmu.cs.lti.script.type.EntityMentionAlignment");
    ll_cas.ll_setRefValue(addr, casFeatCode_targetEntityMention, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public EntityMentionAlignment_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_sourceViewName = jcas.getRequiredFeatureDE(casType, "sourceViewName", "uima.cas.String", featOkTst);
    casFeatCode_sourceViewName  = (null == casFeat_sourceViewName) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_sourceViewName).getCode();

 
    casFeat_targetViewName = jcas.getRequiredFeatureDE(casType, "targetViewName", "uima.cas.String", featOkTst);
    casFeatCode_targetViewName  = (null == casFeat_targetViewName) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_targetViewName).getCode();

 
    casFeat_sourceEntityMention = jcas.getRequiredFeatureDE(casType, "sourceEntityMention", "edu.cmu.cs.lti.script.type.StanfordEntityMention", featOkTst);
    casFeatCode_sourceEntityMention  = (null == casFeat_sourceEntityMention) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_sourceEntityMention).getCode();

 
    casFeat_targetEntityMention = jcas.getRequiredFeatureDE(casType, "targetEntityMention", "edu.cmu.cs.lti.script.type.StanfordEntityMention", featOkTst);
    casFeatCode_targetEntityMention  = (null == casFeat_targetEntityMention) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_targetEntityMention).getCode();

  }
}



    