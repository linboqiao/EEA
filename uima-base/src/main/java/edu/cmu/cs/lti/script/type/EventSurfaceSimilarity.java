

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Tue Sep 16 00:38:29 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class EventSurfaceSimilarity extends ComponentTOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(EventSurfaceSimilarity.class);
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
  protected EventSurfaceSimilarity() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public EventSurfaceSimilarity(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public EventSurfaceSimilarity(JCas jcas) {
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
  //* Feature: wordNetWuPalmer

  /** getter for wordNetWuPalmer - gets 
   * @generated
   * @return value of the feature 
   */
  public double getWordNetWuPalmer() {
    if (EventSurfaceSimilarity_Type.featOkTst && ((EventSurfaceSimilarity_Type)jcasType).casFeat_wordNetWuPalmer == null)
      jcasType.jcas.throwFeatMissing("wordNetWuPalmer", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    return jcasType.ll_cas.ll_getDoubleValue(addr, ((EventSurfaceSimilarity_Type)jcasType).casFeatCode_wordNetWuPalmer);}
    
  /** setter for wordNetWuPalmer - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setWordNetWuPalmer(double v) {
    if (EventSurfaceSimilarity_Type.featOkTst && ((EventSurfaceSimilarity_Type)jcasType).casFeat_wordNetWuPalmer == null)
      jcasType.jcas.throwFeatMissing("wordNetWuPalmer", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    jcasType.ll_cas.ll_setDoubleValue(addr, ((EventSurfaceSimilarity_Type)jcasType).casFeatCode_wordNetWuPalmer, v);}    
   
    
  //*--------------*
  //* Feature: eventMentionI

  /** getter for eventMentionI - gets 
   * @generated
   * @return value of the feature 
   */
  public EventMention getEventMentionI() {
    if (EventSurfaceSimilarity_Type.featOkTst && ((EventSurfaceSimilarity_Type)jcasType).casFeat_eventMentionI == null)
      jcasType.jcas.throwFeatMissing("eventMentionI", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    return (EventMention)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventSurfaceSimilarity_Type)jcasType).casFeatCode_eventMentionI)));}
    
  /** setter for eventMentionI - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEventMentionI(EventMention v) {
    if (EventSurfaceSimilarity_Type.featOkTst && ((EventSurfaceSimilarity_Type)jcasType).casFeat_eventMentionI == null)
      jcasType.jcas.throwFeatMissing("eventMentionI", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventSurfaceSimilarity_Type)jcasType).casFeatCode_eventMentionI, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: eventMentionJ

  /** getter for eventMentionJ - gets 
   * @generated
   * @return value of the feature 
   */
  public EventMention getEventMentionJ() {
    if (EventSurfaceSimilarity_Type.featOkTst && ((EventSurfaceSimilarity_Type)jcasType).casFeat_eventMentionJ == null)
      jcasType.jcas.throwFeatMissing("eventMentionJ", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    return (EventMention)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((EventSurfaceSimilarity_Type)jcasType).casFeatCode_eventMentionJ)));}
    
  /** setter for eventMentionJ - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEventMentionJ(EventMention v) {
    if (EventSurfaceSimilarity_Type.featOkTst && ((EventSurfaceSimilarity_Type)jcasType).casFeat_eventMentionJ == null)
      jcasType.jcas.throwFeatMissing("eventMentionJ", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    jcasType.ll_cas.ll_setRefValue(addr, ((EventSurfaceSimilarity_Type)jcasType).casFeatCode_eventMentionJ, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: sennaSimilarity

  /** getter for sennaSimilarity - gets 
   * @generated
   * @return value of the feature 
   */
  public double getSennaSimilarity() {
    if (EventSurfaceSimilarity_Type.featOkTst && ((EventSurfaceSimilarity_Type)jcasType).casFeat_sennaSimilarity == null)
      jcasType.jcas.throwFeatMissing("sennaSimilarity", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    return jcasType.ll_cas.ll_getDoubleValue(addr, ((EventSurfaceSimilarity_Type)jcasType).casFeatCode_sennaSimilarity);}
    
  /** setter for sennaSimilarity - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setSennaSimilarity(double v) {
    if (EventSurfaceSimilarity_Type.featOkTst && ((EventSurfaceSimilarity_Type)jcasType).casFeat_sennaSimilarity == null)
      jcasType.jcas.throwFeatMissing("sennaSimilarity", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    jcasType.ll_cas.ll_setDoubleValue(addr, ((EventSurfaceSimilarity_Type)jcasType).casFeatCode_sennaSimilarity, v);}    
   
    
  //*--------------*
  //* Feature: diceCoefficient

  /** getter for diceCoefficient - gets 
   * @generated
   * @return value of the feature 
   */
  public double getDiceCoefficient() {
    if (EventSurfaceSimilarity_Type.featOkTst && ((EventSurfaceSimilarity_Type)jcasType).casFeat_diceCoefficient == null)
      jcasType.jcas.throwFeatMissing("diceCoefficient", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    return jcasType.ll_cas.ll_getDoubleValue(addr, ((EventSurfaceSimilarity_Type)jcasType).casFeatCode_diceCoefficient);}
    
  /** setter for diceCoefficient - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setDiceCoefficient(double v) {
    if (EventSurfaceSimilarity_Type.featOkTst && ((EventSurfaceSimilarity_Type)jcasType).casFeat_diceCoefficient == null)
      jcasType.jcas.throwFeatMissing("diceCoefficient", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    jcasType.ll_cas.ll_setDoubleValue(addr, ((EventSurfaceSimilarity_Type)jcasType).casFeatCode_diceCoefficient, v);}    
   
    
  //*--------------*
  //* Feature: morphalizedWuPalmer

  /** getter for morphalizedWuPalmer - gets 
   * @generated
   * @return value of the feature 
   */
  public double getMorphalizedWuPalmer() {
    if (EventSurfaceSimilarity_Type.featOkTst && ((EventSurfaceSimilarity_Type)jcasType).casFeat_morphalizedWuPalmer == null)
      jcasType.jcas.throwFeatMissing("morphalizedWuPalmer", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    return jcasType.ll_cas.ll_getDoubleValue(addr, ((EventSurfaceSimilarity_Type)jcasType).casFeatCode_morphalizedWuPalmer);}
    
  /** setter for morphalizedWuPalmer - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setMorphalizedWuPalmer(double v) {
    if (EventSurfaceSimilarity_Type.featOkTst && ((EventSurfaceSimilarity_Type)jcasType).casFeat_morphalizedWuPalmer == null)
      jcasType.jcas.throwFeatMissing("morphalizedWuPalmer", "edu.cmu.cs.lti.script.type.EventSurfaceSimilarity");
    jcasType.ll_cas.ll_setDoubleValue(addr, ((EventSurfaceSimilarity_Type)jcasType).casFeatCode_morphalizedWuPalmer, v);}    
  }

    