

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringList;


/** An entity refers to an underlying entity, it might corresponding to multiple entity mentions. It can be viewed that we assign the attributes to each cluster
 * Updated by JCasGen Tue Sep 16 00:38:29 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class Entity extends ComponentTOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Entity.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected Entity() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Entity(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Entity(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: entityType

  /** getter for entityType - gets 
   * @generated
   * @return value of the feature 
   */
  public String getEntityType() {
    if (Entity_Type.featOkTst && ((Entity_Type)jcasType).casFeat_entityType == null)
      jcasType.jcas.throwFeatMissing("entityType", "edu.cmu.cs.lti.script.type.Entity");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Entity_Type)jcasType).casFeatCode_entityType);}
    
  /** setter for entityType - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEntityType(String v) {
    if (Entity_Type.featOkTst && ((Entity_Type)jcasType).casFeat_entityType == null)
      jcasType.jcas.throwFeatMissing("entityType", "edu.cmu.cs.lti.script.type.Entity");
    jcasType.ll_cas.ll_setStringValue(addr, ((Entity_Type)jcasType).casFeatCode_entityType, v);}    
   
    
  //*--------------*
  //* Feature: entitySubtype

  /** getter for entitySubtype - gets This feature corresponds to the ACE corpus annotation
   * @generated
   * @return value of the feature 
   */
  public String getEntitySubtype() {
    if (Entity_Type.featOkTst && ((Entity_Type)jcasType).casFeat_entitySubtype == null)
      jcasType.jcas.throwFeatMissing("entitySubtype", "edu.cmu.cs.lti.script.type.Entity");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Entity_Type)jcasType).casFeatCode_entitySubtype);}
    
  /** setter for entitySubtype - sets This feature corresponds to the ACE corpus annotation 
   * @generated
   * @param v value to set into the feature 
   */
  public void setEntitySubtype(String v) {
    if (Entity_Type.featOkTst && ((Entity_Type)jcasType).casFeat_entitySubtype == null)
      jcasType.jcas.throwFeatMissing("entitySubtype", "edu.cmu.cs.lti.script.type.Entity");
    jcasType.ll_cas.ll_setStringValue(addr, ((Entity_Type)jcasType).casFeatCode_entitySubtype, v);}    
   
    
  //*--------------*
  //* Feature: entityClass

  /** getter for entityClass - gets This feature corresponds to the ACE corpus annotation, see http://www.itl.nist.gov/iad/mig/tests/ace/2005/doc/ace05-evalplan.v2a.pdf
   * @generated
   * @return value of the feature 
   */
  public String getEntityClass() {
    if (Entity_Type.featOkTst && ((Entity_Type)jcasType).casFeat_entityClass == null)
      jcasType.jcas.throwFeatMissing("entityClass", "edu.cmu.cs.lti.script.type.Entity");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Entity_Type)jcasType).casFeatCode_entityClass);}
    
  /** setter for entityClass - sets This feature corresponds to the ACE corpus annotation, see http://www.itl.nist.gov/iad/mig/tests/ace/2005/doc/ace05-evalplan.v2a.pdf 
   * @generated
   * @param v value to set into the feature 
   */
  public void setEntityClass(String v) {
    if (Entity_Type.featOkTst && ((Entity_Type)jcasType).casFeat_entityClass == null)
      jcasType.jcas.throwFeatMissing("entityClass", "edu.cmu.cs.lti.script.type.Entity");
    jcasType.ll_cas.ll_setStringValue(addr, ((Entity_Type)jcasType).casFeatCode_entityClass, v);}    
   
    
  //*--------------*
  //* Feature: annotationId

  /** getter for annotationId - gets 
   * @generated
   * @return value of the feature 
   */
  public String getAnnotationId() {
    if (Entity_Type.featOkTst && ((Entity_Type)jcasType).casFeat_annotationId == null)
      jcasType.jcas.throwFeatMissing("annotationId", "edu.cmu.cs.lti.script.type.Entity");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Entity_Type)jcasType).casFeatCode_annotationId);}
    
  /** setter for annotationId - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setAnnotationId(String v) {
    if (Entity_Type.featOkTst && ((Entity_Type)jcasType).casFeat_annotationId == null)
      jcasType.jcas.throwFeatMissing("annotationId", "edu.cmu.cs.lti.script.type.Entity");
    jcasType.ll_cas.ll_setStringValue(addr, ((Entity_Type)jcasType).casFeatCode_annotationId, v);}    
   
    
  //*--------------*
  //* Feature: entityMentions

  /** getter for entityMentions - gets 
   * @generated
   * @return value of the feature 
   */
  public FSArray getEntityMentions() {
    if (Entity_Type.featOkTst && ((Entity_Type)jcasType).casFeat_entityMentions == null)
      jcasType.jcas.throwFeatMissing("entityMentions", "edu.cmu.cs.lti.script.type.Entity");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Entity_Type)jcasType).casFeatCode_entityMentions)));}
    
  /** setter for entityMentions - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEntityMentions(FSArray v) {
    if (Entity_Type.featOkTst && ((Entity_Type)jcasType).casFeat_entityMentions == null)
      jcasType.jcas.throwFeatMissing("entityMentions", "edu.cmu.cs.lti.script.type.Entity");
    jcasType.ll_cas.ll_setRefValue(addr, ((Entity_Type)jcasType).casFeatCode_entityMentions, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for entityMentions - gets an indexed value - 
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public EntityMention getEntityMentions(int i) {
    if (Entity_Type.featOkTst && ((Entity_Type)jcasType).casFeat_entityMentions == null)
      jcasType.jcas.throwFeatMissing("entityMentions", "edu.cmu.cs.lti.script.type.Entity");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Entity_Type)jcasType).casFeatCode_entityMentions), i);
    return (EntityMention)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Entity_Type)jcasType).casFeatCode_entityMentions), i)));}

  /** indexed setter for entityMentions - sets an indexed value - 
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setEntityMentions(int i, EntityMention v) { 
    if (Entity_Type.featOkTst && ((Entity_Type)jcasType).casFeat_entityMentions == null)
      jcasType.jcas.throwFeatMissing("entityMentions", "edu.cmu.cs.lti.script.type.Entity");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((Entity_Type)jcasType).casFeatCode_entityMentions), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((Entity_Type)jcasType).casFeatCode_entityMentions), i, jcasType.ll_cas.ll_getFSRef(v));}
   
    
  //*--------------*
  //* Feature: representativeString

  /** getter for representativeString - gets 
   * @generated
   * @return value of the feature 
   */
  public String getRepresentativeString() {
    if (Entity_Type.featOkTst && ((Entity_Type)jcasType).casFeat_representativeString == null)
      jcasType.jcas.throwFeatMissing("representativeString", "edu.cmu.cs.lti.script.type.Entity");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Entity_Type)jcasType).casFeatCode_representativeString);}
    
  /** setter for representativeString - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setRepresentativeString(String v) {
    if (Entity_Type.featOkTst && ((Entity_Type)jcasType).casFeat_representativeString == null)
      jcasType.jcas.throwFeatMissing("representativeString", "edu.cmu.cs.lti.script.type.Entity");
    jcasType.ll_cas.ll_setStringValue(addr, ((Entity_Type)jcasType).casFeatCode_representativeString, v);}    
   
    
  //*--------------*
  //* Feature: entityFeatures

  /** getter for entityFeatures - gets 
   * @generated
   * @return value of the feature 
   */
  public StringList getEntityFeatures() {
    if (Entity_Type.featOkTst && ((Entity_Type)jcasType).casFeat_entityFeatures == null)
      jcasType.jcas.throwFeatMissing("entityFeatures", "edu.cmu.cs.lti.script.type.Entity");
    return (StringList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Entity_Type)jcasType).casFeatCode_entityFeatures)));}
    
  /** setter for entityFeatures - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEntityFeatures(StringList v) {
    if (Entity_Type.featOkTst && ((Entity_Type)jcasType).casFeat_entityFeatures == null)
      jcasType.jcas.throwFeatMissing("entityFeatures", "edu.cmu.cs.lti.script.type.Entity");
    jcasType.ll_cas.ll_setRefValue(addr, ((Entity_Type)jcasType).casFeatCode_entityFeatures, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: representativeMention

  /** getter for representativeMention - gets 
   * @generated
   * @return value of the feature 
   */
  public EntityMention getRepresentativeMention() {
    if (Entity_Type.featOkTst && ((Entity_Type)jcasType).casFeat_representativeMention == null)
      jcasType.jcas.throwFeatMissing("representativeMention", "edu.cmu.cs.lti.script.type.Entity");
    return (EntityMention)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Entity_Type)jcasType).casFeatCode_representativeMention)));}
    
  /** setter for representativeMention - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setRepresentativeMention(EntityMention v) {
    if (Entity_Type.featOkTst && ((Entity_Type)jcasType).casFeat_representativeMention == null)
      jcasType.jcas.throwFeatMissing("representativeMention", "edu.cmu.cs.lti.script.type.Entity");
    jcasType.ll_cas.ll_setRefValue(addr, ((Entity_Type)jcasType).casFeatCode_representativeMention, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    