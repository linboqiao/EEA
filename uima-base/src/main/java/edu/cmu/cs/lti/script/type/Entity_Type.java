
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

/** An entity refers to an underlying entity, it might corresponding to multiple entity mentions. It can be viewed that we assign the attributes to each cluster
 * Updated by JCasGen Tue Sep 16 00:38:29 EDT 2014
 * @generated */
public class Entity_Type extends ComponentTOP_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Entity_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Entity_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Entity(addr, Entity_Type.this);
  			   Entity_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Entity(addr, Entity_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Entity.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.Entity");
 
  /** @generated */
  final Feature casFeat_entityType;
  /** @generated */
  final int     casFeatCode_entityType;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getEntityType(int addr) {
        if (featOkTst && casFeat_entityType == null)
      jcas.throwFeatMissing("entityType", "edu.cmu.cs.lti.script.type.Entity");
    return ll_cas.ll_getStringValue(addr, casFeatCode_entityType);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEntityType(int addr, String v) {
        if (featOkTst && casFeat_entityType == null)
      jcas.throwFeatMissing("entityType", "edu.cmu.cs.lti.script.type.Entity");
    ll_cas.ll_setStringValue(addr, casFeatCode_entityType, v);}
    
  
 
  /** @generated */
  final Feature casFeat_entitySubtype;
  /** @generated */
  final int     casFeatCode_entitySubtype;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getEntitySubtype(int addr) {
        if (featOkTst && casFeat_entitySubtype == null)
      jcas.throwFeatMissing("entitySubtype", "edu.cmu.cs.lti.script.type.Entity");
    return ll_cas.ll_getStringValue(addr, casFeatCode_entitySubtype);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEntitySubtype(int addr, String v) {
        if (featOkTst && casFeat_entitySubtype == null)
      jcas.throwFeatMissing("entitySubtype", "edu.cmu.cs.lti.script.type.Entity");
    ll_cas.ll_setStringValue(addr, casFeatCode_entitySubtype, v);}
    
  
 
  /** @generated */
  final Feature casFeat_entityClass;
  /** @generated */
  final int     casFeatCode_entityClass;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getEntityClass(int addr) {
        if (featOkTst && casFeat_entityClass == null)
      jcas.throwFeatMissing("entityClass", "edu.cmu.cs.lti.script.type.Entity");
    return ll_cas.ll_getStringValue(addr, casFeatCode_entityClass);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEntityClass(int addr, String v) {
        if (featOkTst && casFeat_entityClass == null)
      jcas.throwFeatMissing("entityClass", "edu.cmu.cs.lti.script.type.Entity");
    ll_cas.ll_setStringValue(addr, casFeatCode_entityClass, v);}
    
  
 
  /** @generated */
  final Feature casFeat_annotationId;
  /** @generated */
  final int     casFeatCode_annotationId;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getAnnotationId(int addr) {
        if (featOkTst && casFeat_annotationId == null)
      jcas.throwFeatMissing("annotationId", "edu.cmu.cs.lti.script.type.Entity");
    return ll_cas.ll_getStringValue(addr, casFeatCode_annotationId);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setAnnotationId(int addr, String v) {
        if (featOkTst && casFeat_annotationId == null)
      jcas.throwFeatMissing("annotationId", "edu.cmu.cs.lti.script.type.Entity");
    ll_cas.ll_setStringValue(addr, casFeatCode_annotationId, v);}
    
  
 
  /** @generated */
  final Feature casFeat_entityMentions;
  /** @generated */
  final int     casFeatCode_entityMentions;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getEntityMentions(int addr) {
        if (featOkTst && casFeat_entityMentions == null)
      jcas.throwFeatMissing("entityMentions", "edu.cmu.cs.lti.script.type.Entity");
    return ll_cas.ll_getRefValue(addr, casFeatCode_entityMentions);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEntityMentions(int addr, int v) {
        if (featOkTst && casFeat_entityMentions == null)
      jcas.throwFeatMissing("entityMentions", "edu.cmu.cs.lti.script.type.Entity");
    ll_cas.ll_setRefValue(addr, casFeatCode_entityMentions, v);}
    
   /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @return value at index i in the array 
   */
  public int getEntityMentions(int addr, int i) {
        if (featOkTst && casFeat_entityMentions == null)
      jcas.throwFeatMissing("entityMentions", "edu.cmu.cs.lti.script.type.Entity");
    if (lowLevelTypeChecks)
      return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_entityMentions), i, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_entityMentions), i);
  return ll_cas.ll_getRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_entityMentions), i);
  }
   
  /** @generated
   * @param addr low level Feature Structure reference
   * @param i index of item in the array
   * @param v value to set
   */ 
  public void setEntityMentions(int addr, int i, int v) {
        if (featOkTst && casFeat_entityMentions == null)
      jcas.throwFeatMissing("entityMentions", "edu.cmu.cs.lti.script.type.Entity");
    if (lowLevelTypeChecks)
      ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_entityMentions), i, v, true);
    jcas.checkArrayBounds(ll_cas.ll_getRefValue(addr, casFeatCode_entityMentions), i);
    ll_cas.ll_setRefArrayValue(ll_cas.ll_getRefValue(addr, casFeatCode_entityMentions), i, v);
  }
 
 
  /** @generated */
  final Feature casFeat_representativeString;
  /** @generated */
  final int     casFeatCode_representativeString;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getRepresentativeString(int addr) {
        if (featOkTst && casFeat_representativeString == null)
      jcas.throwFeatMissing("representativeString", "edu.cmu.cs.lti.script.type.Entity");
    return ll_cas.ll_getStringValue(addr, casFeatCode_representativeString);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setRepresentativeString(int addr, String v) {
        if (featOkTst && casFeat_representativeString == null)
      jcas.throwFeatMissing("representativeString", "edu.cmu.cs.lti.script.type.Entity");
    ll_cas.ll_setStringValue(addr, casFeatCode_representativeString, v);}
    
  
 
  /** @generated */
  final Feature casFeat_entityFeatures;
  /** @generated */
  final int     casFeatCode_entityFeatures;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getEntityFeatures(int addr) {
        if (featOkTst && casFeat_entityFeatures == null)
      jcas.throwFeatMissing("entityFeatures", "edu.cmu.cs.lti.script.type.Entity");
    return ll_cas.ll_getRefValue(addr, casFeatCode_entityFeatures);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEntityFeatures(int addr, int v) {
        if (featOkTst && casFeat_entityFeatures == null)
      jcas.throwFeatMissing("entityFeatures", "edu.cmu.cs.lti.script.type.Entity");
    ll_cas.ll_setRefValue(addr, casFeatCode_entityFeatures, v);}
    
  
 
  /** @generated */
  final Feature casFeat_representativeMention;
  /** @generated */
  final int     casFeatCode_representativeMention;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getRepresentativeMention(int addr) {
        if (featOkTst && casFeat_representativeMention == null)
      jcas.throwFeatMissing("representativeMention", "edu.cmu.cs.lti.script.type.Entity");
    return ll_cas.ll_getRefValue(addr, casFeatCode_representativeMention);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setRepresentativeMention(int addr, int v) {
        if (featOkTst && casFeat_representativeMention == null)
      jcas.throwFeatMissing("representativeMention", "edu.cmu.cs.lti.script.type.Entity");
    ll_cas.ll_setRefValue(addr, casFeatCode_representativeMention, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Entity_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_entityType = jcas.getRequiredFeatureDE(casType, "entityType", "uima.cas.String", featOkTst);
    casFeatCode_entityType  = (null == casFeat_entityType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_entityType).getCode();

 
    casFeat_entitySubtype = jcas.getRequiredFeatureDE(casType, "entitySubtype", "uima.cas.String", featOkTst);
    casFeatCode_entitySubtype  = (null == casFeat_entitySubtype) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_entitySubtype).getCode();

 
    casFeat_entityClass = jcas.getRequiredFeatureDE(casType, "entityClass", "uima.cas.String", featOkTst);
    casFeatCode_entityClass  = (null == casFeat_entityClass) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_entityClass).getCode();

 
    casFeat_annotationId = jcas.getRequiredFeatureDE(casType, "annotationId", "uima.cas.String", featOkTst);
    casFeatCode_annotationId  = (null == casFeat_annotationId) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_annotationId).getCode();

 
    casFeat_entityMentions = jcas.getRequiredFeatureDE(casType, "entityMentions", "uima.cas.FSArray", featOkTst);
    casFeatCode_entityMentions  = (null == casFeat_entityMentions) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_entityMentions).getCode();

 
    casFeat_representativeString = jcas.getRequiredFeatureDE(casType, "representativeString", "uima.cas.String", featOkTst);
    casFeatCode_representativeString  = (null == casFeat_representativeString) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_representativeString).getCode();

 
    casFeat_entityFeatures = jcas.getRequiredFeatureDE(casType, "entityFeatures", "uima.cas.StringList", featOkTst);
    casFeatCode_entityFeatures  = (null == casFeat_entityFeatures) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_entityFeatures).getCode();

 
    casFeat_representativeMention = jcas.getRequiredFeatureDE(casType, "representativeMention", "edu.cmu.cs.lti.script.type.EntityMention", featOkTst);
    casFeatCode_representativeMention  = (null == casFeat_representativeMention) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_representativeMention).getCode();

  }
}



    