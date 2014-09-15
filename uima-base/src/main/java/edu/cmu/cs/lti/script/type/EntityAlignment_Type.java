
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
public class EntityAlignment_Type extends ComponentTOP_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (EntityAlignment_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = EntityAlignment_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new EntityAlignment(addr, EntityAlignment_Type.this);
  			   EntityAlignment_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new EntityAlignment(addr, EntityAlignment_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = EntityAlignment.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.EntityAlignment");
 
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
      jcas.throwFeatMissing("sourceViewName", "edu.cmu.cs.lti.script.type.EntityAlignment");
    return ll_cas.ll_getStringValue(addr, casFeatCode_sourceViewName);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setSourceViewName(int addr, String v) {
        if (featOkTst && casFeat_sourceViewName == null)
      jcas.throwFeatMissing("sourceViewName", "edu.cmu.cs.lti.script.type.EntityAlignment");
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
      jcas.throwFeatMissing("targetViewName", "edu.cmu.cs.lti.script.type.EntityAlignment");
    return ll_cas.ll_getStringValue(addr, casFeatCode_targetViewName);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTargetViewName(int addr, String v) {
        if (featOkTst && casFeat_targetViewName == null)
      jcas.throwFeatMissing("targetViewName", "edu.cmu.cs.lti.script.type.EntityAlignment");
    ll_cas.ll_setStringValue(addr, casFeatCode_targetViewName, v);}
    
  
 
  /** @generated */
  final Feature casFeat_sourceEntity;
  /** @generated */
  final int     casFeatCode_sourceEntity;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getSourceEntity(int addr) {
        if (featOkTst && casFeat_sourceEntity == null)
      jcas.throwFeatMissing("sourceEntity", "edu.cmu.cs.lti.script.type.EntityAlignment");
    return ll_cas.ll_getRefValue(addr, casFeatCode_sourceEntity);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setSourceEntity(int addr, int v) {
        if (featOkTst && casFeat_sourceEntity == null)
      jcas.throwFeatMissing("sourceEntity", "edu.cmu.cs.lti.script.type.EntityAlignment");
    ll_cas.ll_setRefValue(addr, casFeatCode_sourceEntity, v);}
    
  
 
  /** @generated */
  final Feature casFeat_targetEntity;
  /** @generated */
  final int     casFeatCode_targetEntity;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getTargetEntity(int addr) {
        if (featOkTst && casFeat_targetEntity == null)
      jcas.throwFeatMissing("targetEntity", "edu.cmu.cs.lti.script.type.EntityAlignment");
    return ll_cas.ll_getRefValue(addr, casFeatCode_targetEntity);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTargetEntity(int addr, int v) {
        if (featOkTst && casFeat_targetEntity == null)
      jcas.throwFeatMissing("targetEntity", "edu.cmu.cs.lti.script.type.EntityAlignment");
    ll_cas.ll_setRefValue(addr, casFeatCode_targetEntity, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public EntityAlignment_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_sourceViewName = jcas.getRequiredFeatureDE(casType, "sourceViewName", "uima.cas.String", featOkTst);
    casFeatCode_sourceViewName  = (null == casFeat_sourceViewName) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_sourceViewName).getCode();

 
    casFeat_targetViewName = jcas.getRequiredFeatureDE(casType, "targetViewName", "uima.cas.String", featOkTst);
    casFeatCode_targetViewName  = (null == casFeat_targetViewName) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_targetViewName).getCode();

 
    casFeat_sourceEntity = jcas.getRequiredFeatureDE(casType, "sourceEntity", "edu.cmu.cs.lti.script.type.Entity", featOkTst);
    casFeatCode_sourceEntity  = (null == casFeat_sourceEntity) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_sourceEntity).getCode();

 
    casFeat_targetEntity = jcas.getRequiredFeatureDE(casType, "targetEntity", "edu.cmu.cs.lti.script.type.Entity", featOkTst);
    casFeatCode_targetEntity  = (null == casFeat_targetEntity) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_targetEntity).getCode();

  }
}



    