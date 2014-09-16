

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Tue Sep 16 00:38:29 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class Article extends ComponentAnnotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Article.class);
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
  protected Article() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Article(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Article(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Article(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
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
  //* Feature: articleName

  /** getter for articleName - gets 
   * @generated
   * @return value of the feature 
   */
  public String getArticleName() {
    if (Article_Type.featOkTst && ((Article_Type)jcasType).casFeat_articleName == null)
      jcasType.jcas.throwFeatMissing("articleName", "edu.cmu.cs.lti.script.type.Article");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Article_Type)jcasType).casFeatCode_articleName);}
    
  /** setter for articleName - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArticleName(String v) {
    if (Article_Type.featOkTst && ((Article_Type)jcasType).casFeat_articleName == null)
      jcasType.jcas.throwFeatMissing("articleName", "edu.cmu.cs.lti.script.type.Article");
    jcasType.ll_cas.ll_setStringValue(addr, ((Article_Type)jcasType).casFeatCode_articleName, v);}    
   
    
  //*--------------*
  //* Feature: language

  /** getter for language - gets 
   * @generated
   * @return value of the feature 
   */
  public String getLanguage() {
    if (Article_Type.featOkTst && ((Article_Type)jcasType).casFeat_language == null)
      jcasType.jcas.throwFeatMissing("language", "edu.cmu.cs.lti.script.type.Article");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Article_Type)jcasType).casFeatCode_language);}
    
  /** setter for language - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLanguage(String v) {
    if (Article_Type.featOkTst && ((Article_Type)jcasType).casFeat_language == null)
      jcasType.jcas.throwFeatMissing("language", "edu.cmu.cs.lti.script.type.Article");
    jcasType.ll_cas.ll_setStringValue(addr, ((Article_Type)jcasType).casFeatCode_language, v);}    
   
    
  //*--------------*
  //* Feature: articleDate

  /** getter for articleDate - gets 
   * @generated
   * @return value of the feature 
   */
  public String getArticleDate() {
    if (Article_Type.featOkTst && ((Article_Type)jcasType).casFeat_articleDate == null)
      jcasType.jcas.throwFeatMissing("articleDate", "edu.cmu.cs.lti.script.type.Article");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Article_Type)jcasType).casFeatCode_articleDate);}
    
  /** setter for articleDate - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setArticleDate(String v) {
    if (Article_Type.featOkTst && ((Article_Type)jcasType).casFeat_articleDate == null)
      jcasType.jcas.throwFeatMissing("articleDate", "edu.cmu.cs.lti.script.type.Article");
    jcasType.ll_cas.ll_setStringValue(addr, ((Article_Type)jcasType).casFeatCode_articleDate, v);}    
  }

    