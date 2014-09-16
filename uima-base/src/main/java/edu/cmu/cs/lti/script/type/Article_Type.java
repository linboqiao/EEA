
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
public class Article_Type extends ComponentAnnotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Article_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Article_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Article(addr, Article_Type.this);
  			   Article_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Article(addr, Article_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Article.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.Article");
 
  /** @generated */
  final Feature casFeat_articleName;
  /** @generated */
  final int     casFeatCode_articleName;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getArticleName(int addr) {
        if (featOkTst && casFeat_articleName == null)
      jcas.throwFeatMissing("articleName", "edu.cmu.cs.lti.script.type.Article");
    return ll_cas.ll_getStringValue(addr, casFeatCode_articleName);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArticleName(int addr, String v) {
        if (featOkTst && casFeat_articleName == null)
      jcas.throwFeatMissing("articleName", "edu.cmu.cs.lti.script.type.Article");
    ll_cas.ll_setStringValue(addr, casFeatCode_articleName, v);}
    
  
 
  /** @generated */
  final Feature casFeat_language;
  /** @generated */
  final int     casFeatCode_language;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getLanguage(int addr) {
        if (featOkTst && casFeat_language == null)
      jcas.throwFeatMissing("language", "edu.cmu.cs.lti.script.type.Article");
    return ll_cas.ll_getStringValue(addr, casFeatCode_language);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setLanguage(int addr, String v) {
        if (featOkTst && casFeat_language == null)
      jcas.throwFeatMissing("language", "edu.cmu.cs.lti.script.type.Article");
    ll_cas.ll_setStringValue(addr, casFeatCode_language, v);}
    
  
 
  /** @generated */
  final Feature casFeat_articleDate;
  /** @generated */
  final int     casFeatCode_articleDate;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getArticleDate(int addr) {
        if (featOkTst && casFeat_articleDate == null)
      jcas.throwFeatMissing("articleDate", "edu.cmu.cs.lti.script.type.Article");
    return ll_cas.ll_getStringValue(addr, casFeatCode_articleDate);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setArticleDate(int addr, String v) {
        if (featOkTst && casFeat_articleDate == null)
      jcas.throwFeatMissing("articleDate", "edu.cmu.cs.lti.script.type.Article");
    ll_cas.ll_setStringValue(addr, casFeatCode_articleDate, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Article_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_articleName = jcas.getRequiredFeatureDE(casType, "articleName", "uima.cas.String", featOkTst);
    casFeatCode_articleName  = (null == casFeat_articleName) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_articleName).getCode();

 
    casFeat_language = jcas.getRequiredFeatureDE(casType, "language", "uima.cas.String", featOkTst);
    casFeatCode_language  = (null == casFeat_language) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_language).getCode();

 
    casFeat_articleDate = jcas.getRequiredFeatureDE(casType, "articleDate", "uima.cas.String", featOkTst);
    casFeatCode_articleDate  = (null == casFeat_articleDate) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_articleDate).getCode();

  }
}



    