

/* First created by JCasGen Sat Sep 13 18:23:45 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSList;


/** Cluster of entity mentions
 * Updated by JCasGen Sun Sep 14 21:48:57 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class EntityCoreferenceCluster extends ComponentTOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(EntityCoreferenceCluster.class);
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
  protected EntityCoreferenceCluster() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public EntityCoreferenceCluster(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public EntityCoreferenceCluster(JCas jcas) {
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
  //* Feature: entityMentions

  /** getter for entityMentions - gets 
   * @generated
   * @return value of the feature 
   */
  public FSList getEntityMentions() {
    if (EntityCoreferenceCluster_Type.featOkTst && ((EntityCoreferenceCluster_Type)jcasType).casFeat_entityMentions == null)
      jcasType.jcas.throwFeatMissing("entityMentions", "edu.cmu.cs.lti.script.type.EntityCoreferenceCluster");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EntityCoreferenceCluster_Type)jcasType).casFeatCode_entityMentions)));}
    
  /** setter for entityMentions - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEntityMentions(FSList v) {
    if (EntityCoreferenceCluster_Type.featOkTst && ((EntityCoreferenceCluster_Type)jcasType).casFeat_entityMentions == null)
      jcasType.jcas.throwFeatMissing("entityMentions", "edu.cmu.cs.lti.script.type.EntityCoreferenceCluster");
    jcasType.ll_cas.ll_setRefValue(addr, ((EntityCoreferenceCluster_Type)jcasType).casFeatCode_entityMentions, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: representativeMention

  /** getter for representativeMention - gets 
   * @generated
   * @return value of the feature 
   */
  public EntityMention getRepresentativeMention() {
    if (EntityCoreferenceCluster_Type.featOkTst && ((EntityCoreferenceCluster_Type)jcasType).casFeat_representativeMention == null)
      jcasType.jcas.throwFeatMissing("representativeMention", "edu.cmu.cs.lti.script.type.EntityCoreferenceCluster");
    return (EntityMention)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EntityCoreferenceCluster_Type)jcasType).casFeatCode_representativeMention)));}
    
  /** setter for representativeMention - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setRepresentativeMention(EntityMention v) {
    if (EntityCoreferenceCluster_Type.featOkTst && ((EntityCoreferenceCluster_Type)jcasType).casFeat_representativeMention == null)
      jcasType.jcas.throwFeatMissing("representativeMention", "edu.cmu.cs.lti.script.type.EntityCoreferenceCluster");
    jcasType.ll_cas.ll_setRefValue(addr, ((EntityCoreferenceCluster_Type)jcasType).casFeatCode_representativeMention, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    