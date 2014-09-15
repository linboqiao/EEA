
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

/** Annotate numbers and their normalized form
 * Updated by JCasGen Sun Sep 14 23:46:31 EDT 2014
 * @generated */
public class NumberAnnotation_Type extends ComponentAnnotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (NumberAnnotation_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = NumberAnnotation_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new NumberAnnotation(addr, NumberAnnotation_Type.this);
  			   NumberAnnotation_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new NumberAnnotation(addr, NumberAnnotation_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = NumberAnnotation.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.NumberAnnotation");
 
  /** @generated */
  final Feature casFeat_normalizedString;
  /** @generated */
  final int     casFeatCode_normalizedString;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getNormalizedString(int addr) {
        if (featOkTst && casFeat_normalizedString == null)
      jcas.throwFeatMissing("normalizedString", "edu.cmu.cs.lti.script.type.NumberAnnotation");
    return ll_cas.ll_getStringValue(addr, casFeatCode_normalizedString);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setNormalizedString(int addr, String v) {
        if (featOkTst && casFeat_normalizedString == null)
      jcas.throwFeatMissing("normalizedString", "edu.cmu.cs.lti.script.type.NumberAnnotation");
    ll_cas.ll_setStringValue(addr, casFeatCode_normalizedString, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public NumberAnnotation_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_normalizedString = jcas.getRequiredFeatureDE(casType, "normalizedString", "uima.cas.String", featOkTst);
    casFeatCode_normalizedString  = (null == casFeat_normalizedString) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_normalizedString).getCode();

  }
}



    