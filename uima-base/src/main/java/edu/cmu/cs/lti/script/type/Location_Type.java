
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
public class Location_Type extends EntityBasedComponent_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Location_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Location_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Location(addr, Location_Type.this);
  			   Location_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Location(addr, Location_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Location.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.cmu.cs.lti.script.type.Location");
 
  /** @generated */
  final Feature casFeat_locationType;
  /** @generated */
  final int     casFeatCode_locationType;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getLocationType(int addr) {
        if (featOkTst && casFeat_locationType == null)
      jcas.throwFeatMissing("locationType", "edu.cmu.cs.lti.script.type.Location");
    return ll_cas.ll_getStringValue(addr, casFeatCode_locationType);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setLocationType(int addr, String v) {
        if (featOkTst && casFeat_locationType == null)
      jcas.throwFeatMissing("locationType", "edu.cmu.cs.lti.script.type.Location");
    ll_cas.ll_setStringValue(addr, casFeatCode_locationType, v);}
    
  
 
  /** @generated */
  final Feature casFeat_country;
  /** @generated */
  final int     casFeatCode_country;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getCountry(int addr) {
        if (featOkTst && casFeat_country == null)
      jcas.throwFeatMissing("country", "edu.cmu.cs.lti.script.type.Location");
    return ll_cas.ll_getStringValue(addr, casFeatCode_country);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setCountry(int addr, String v) {
        if (featOkTst && casFeat_country == null)
      jcas.throwFeatMissing("country", "edu.cmu.cs.lti.script.type.Location");
    ll_cas.ll_setStringValue(addr, casFeatCode_country, v);}
    
  
 
  /** @generated */
  final Feature casFeat_city;
  /** @generated */
  final int     casFeatCode_city;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getCity(int addr) {
        if (featOkTst && casFeat_city == null)
      jcas.throwFeatMissing("city", "edu.cmu.cs.lti.script.type.Location");
    return ll_cas.ll_getStringValue(addr, casFeatCode_city);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setCity(int addr, String v) {
        if (featOkTst && casFeat_city == null)
      jcas.throwFeatMissing("city", "edu.cmu.cs.lti.script.type.Location");
    ll_cas.ll_setStringValue(addr, casFeatCode_city, v);}
    
  
 
  /** @generated */
  final Feature casFeat_longitude;
  /** @generated */
  final int     casFeatCode_longitude;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public float getLongitude(int addr) {
        if (featOkTst && casFeat_longitude == null)
      jcas.throwFeatMissing("longitude", "edu.cmu.cs.lti.script.type.Location");
    return ll_cas.ll_getFloatValue(addr, casFeatCode_longitude);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setLongitude(int addr, float v) {
        if (featOkTst && casFeat_longitude == null)
      jcas.throwFeatMissing("longitude", "edu.cmu.cs.lti.script.type.Location");
    ll_cas.ll_setFloatValue(addr, casFeatCode_longitude, v);}
    
  
 
  /** @generated */
  final Feature casFeat_longitudePosition;
  /** @generated */
  final int     casFeatCode_longitudePosition;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getLongitudePosition(int addr) {
        if (featOkTst && casFeat_longitudePosition == null)
      jcas.throwFeatMissing("longitudePosition", "edu.cmu.cs.lti.script.type.Location");
    return ll_cas.ll_getStringValue(addr, casFeatCode_longitudePosition);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setLongitudePosition(int addr, String v) {
        if (featOkTst && casFeat_longitudePosition == null)
      jcas.throwFeatMissing("longitudePosition", "edu.cmu.cs.lti.script.type.Location");
    ll_cas.ll_setStringValue(addr, casFeatCode_longitudePosition, v);}
    
  
 
  /** @generated */
  final Feature casFeat_latitude;
  /** @generated */
  final int     casFeatCode_latitude;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public float getLatitude(int addr) {
        if (featOkTst && casFeat_latitude == null)
      jcas.throwFeatMissing("latitude", "edu.cmu.cs.lti.script.type.Location");
    return ll_cas.ll_getFloatValue(addr, casFeatCode_latitude);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setLatitude(int addr, float v) {
        if (featOkTst && casFeat_latitude == null)
      jcas.throwFeatMissing("latitude", "edu.cmu.cs.lti.script.type.Location");
    ll_cas.ll_setFloatValue(addr, casFeatCode_latitude, v);}
    
  
 
  /** @generated */
  final Feature casFeat_latitudePosition;
  /** @generated */
  final int     casFeatCode_latitudePosition;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getLatitudePosition(int addr) {
        if (featOkTst && casFeat_latitudePosition == null)
      jcas.throwFeatMissing("latitudePosition", "edu.cmu.cs.lti.script.type.Location");
    return ll_cas.ll_getStringValue(addr, casFeatCode_latitudePosition);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setLatitudePosition(int addr, String v) {
        if (featOkTst && casFeat_latitudePosition == null)
      jcas.throwFeatMissing("latitudePosition", "edu.cmu.cs.lti.script.type.Location");
    ll_cas.ll_setStringValue(addr, casFeatCode_latitudePosition, v);}
    
  
 
  /** @generated */
  final Feature casFeat_names;
  /** @generated */
  final int     casFeatCode_names;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getNames(int addr) {
        if (featOkTst && casFeat_names == null)
      jcas.throwFeatMissing("names", "edu.cmu.cs.lti.script.type.Location");
    return ll_cas.ll_getRefValue(addr, casFeatCode_names);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setNames(int addr, int v) {
        if (featOkTst && casFeat_names == null)
      jcas.throwFeatMissing("names", "edu.cmu.cs.lti.script.type.Location");
    ll_cas.ll_setRefValue(addr, casFeatCode_names, v);}
    
  
 
  /** @generated */
  final Feature casFeat_historicalEvents;
  /** @generated */
  final int     casFeatCode_historicalEvents;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getHistoricalEvents(int addr) {
        if (featOkTst && casFeat_historicalEvents == null)
      jcas.throwFeatMissing("historicalEvents", "edu.cmu.cs.lti.script.type.Location");
    return ll_cas.ll_getRefValue(addr, casFeatCode_historicalEvents);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setHistoricalEvents(int addr, int v) {
        if (featOkTst && casFeat_historicalEvents == null)
      jcas.throwFeatMissing("historicalEvents", "edu.cmu.cs.lti.script.type.Location");
    ll_cas.ll_setRefValue(addr, casFeatCode_historicalEvents, v);}
    
  
 
  /** @generated */
  final Feature casFeat_entitiesLocactedIn;
  /** @generated */
  final int     casFeatCode_entitiesLocactedIn;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getEntitiesLocactedIn(int addr) {
        if (featOkTst && casFeat_entitiesLocactedIn == null)
      jcas.throwFeatMissing("entitiesLocactedIn", "edu.cmu.cs.lti.script.type.Location");
    return ll_cas.ll_getRefValue(addr, casFeatCode_entitiesLocactedIn);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setEntitiesLocactedIn(int addr, int v) {
        if (featOkTst && casFeat_entitiesLocactedIn == null)
      jcas.throwFeatMissing("entitiesLocactedIn", "edu.cmu.cs.lti.script.type.Location");
    ll_cas.ll_setRefValue(addr, casFeatCode_entitiesLocactedIn, v);}
    
  
 
  /** @generated */
  final Feature casFeat_locatedIn;
  /** @generated */
  final int     casFeatCode_locatedIn;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getLocatedIn(int addr) {
        if (featOkTst && casFeat_locatedIn == null)
      jcas.throwFeatMissing("locatedIn", "edu.cmu.cs.lti.script.type.Location");
    return ll_cas.ll_getRefValue(addr, casFeatCode_locatedIn);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setLocatedIn(int addr, int v) {
        if (featOkTst && casFeat_locatedIn == null)
      jcas.throwFeatMissing("locatedIn", "edu.cmu.cs.lti.script.type.Location");
    ll_cas.ll_setRefValue(addr, casFeatCode_locatedIn, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Location_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_locationType = jcas.getRequiredFeatureDE(casType, "locationType", "uima.cas.String", featOkTst);
    casFeatCode_locationType  = (null == casFeat_locationType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_locationType).getCode();

 
    casFeat_country = jcas.getRequiredFeatureDE(casType, "country", "uima.cas.String", featOkTst);
    casFeatCode_country  = (null == casFeat_country) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_country).getCode();

 
    casFeat_city = jcas.getRequiredFeatureDE(casType, "city", "uima.cas.String", featOkTst);
    casFeatCode_city  = (null == casFeat_city) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_city).getCode();

 
    casFeat_longitude = jcas.getRequiredFeatureDE(casType, "longitude", "uima.cas.Float", featOkTst);
    casFeatCode_longitude  = (null == casFeat_longitude) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_longitude).getCode();

 
    casFeat_longitudePosition = jcas.getRequiredFeatureDE(casType, "longitudePosition", "uima.cas.String", featOkTst);
    casFeatCode_longitudePosition  = (null == casFeat_longitudePosition) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_longitudePosition).getCode();

 
    casFeat_latitude = jcas.getRequiredFeatureDE(casType, "latitude", "uima.cas.Float", featOkTst);
    casFeatCode_latitude  = (null == casFeat_latitude) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_latitude).getCode();

 
    casFeat_latitudePosition = jcas.getRequiredFeatureDE(casType, "latitudePosition", "uima.cas.String", featOkTst);
    casFeatCode_latitudePosition  = (null == casFeat_latitudePosition) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_latitudePosition).getCode();

 
    casFeat_names = jcas.getRequiredFeatureDE(casType, "names", "uima.cas.StringList", featOkTst);
    casFeatCode_names  = (null == casFeat_names) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_names).getCode();

 
    casFeat_historicalEvents = jcas.getRequiredFeatureDE(casType, "historicalEvents", "uima.cas.StringList", featOkTst);
    casFeatCode_historicalEvents  = (null == casFeat_historicalEvents) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_historicalEvents).getCode();

 
    casFeat_entitiesLocactedIn = jcas.getRequiredFeatureDE(casType, "entitiesLocactedIn", "uima.cas.StringList", featOkTst);
    casFeatCode_entitiesLocactedIn  = (null == casFeat_entitiesLocactedIn) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_entitiesLocactedIn).getCode();

 
    casFeat_locatedIn = jcas.getRequiredFeatureDE(casType, "locatedIn", "uima.cas.StringList", featOkTst);
    casFeatCode_locatedIn  = (null == casFeat_locatedIn) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_locatedIn).getCode();

  }
}



    