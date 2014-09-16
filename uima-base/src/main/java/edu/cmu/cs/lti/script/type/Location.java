

/* First created by JCasGen Mon Sep 15 15:04:26 EDT 2014 */
package edu.cmu.cs.lti.script.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.StringList;


/** 
 * Updated by JCasGen Tue Sep 16 00:38:29 EDT 2014
 * XML source: /Users/zhengzhongliu/Documents/projects/uimafied-tools/uima-base/src/main/resources/TypeSystem.xml
 * @generated */
public class Location extends EntityBasedComponent {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Location.class);
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
  protected Location() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Location(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Location(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Location(JCas jcas, int begin, int end) {
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
  //* Feature: locationType

  /** getter for locationType - gets 
   * @generated
   * @return value of the feature 
   */
  public String getLocationType() {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_locationType == null)
      jcasType.jcas.throwFeatMissing("locationType", "edu.cmu.cs.lti.script.type.Location");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Location_Type)jcasType).casFeatCode_locationType);}
    
  /** setter for locationType - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLocationType(String v) {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_locationType == null)
      jcasType.jcas.throwFeatMissing("locationType", "edu.cmu.cs.lti.script.type.Location");
    jcasType.ll_cas.ll_setStringValue(addr, ((Location_Type)jcasType).casFeatCode_locationType, v);}    
   
    
  //*--------------*
  //* Feature: country

  /** getter for country - gets 
   * @generated
   * @return value of the feature 
   */
  public String getCountry() {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_country == null)
      jcasType.jcas.throwFeatMissing("country", "edu.cmu.cs.lti.script.type.Location");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Location_Type)jcasType).casFeatCode_country);}
    
  /** setter for country - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setCountry(String v) {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_country == null)
      jcasType.jcas.throwFeatMissing("country", "edu.cmu.cs.lti.script.type.Location");
    jcasType.ll_cas.ll_setStringValue(addr, ((Location_Type)jcasType).casFeatCode_country, v);}    
   
    
  //*--------------*
  //* Feature: city

  /** getter for city - gets 
   * @generated
   * @return value of the feature 
   */
  public String getCity() {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_city == null)
      jcasType.jcas.throwFeatMissing("city", "edu.cmu.cs.lti.script.type.Location");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Location_Type)jcasType).casFeatCode_city);}
    
  /** setter for city - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setCity(String v) {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_city == null)
      jcasType.jcas.throwFeatMissing("city", "edu.cmu.cs.lti.script.type.Location");
    jcasType.ll_cas.ll_setStringValue(addr, ((Location_Type)jcasType).casFeatCode_city, v);}    
   
    
  //*--------------*
  //* Feature: longitude

  /** getter for longitude - gets 
   * @generated
   * @return value of the feature 
   */
  public float getLongitude() {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_longitude == null)
      jcasType.jcas.throwFeatMissing("longitude", "edu.cmu.cs.lti.script.type.Location");
    return jcasType.ll_cas.ll_getFloatValue(addr, ((Location_Type)jcasType).casFeatCode_longitude);}
    
  /** setter for longitude - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLongitude(float v) {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_longitude == null)
      jcasType.jcas.throwFeatMissing("longitude", "edu.cmu.cs.lti.script.type.Location");
    jcasType.ll_cas.ll_setFloatValue(addr, ((Location_Type)jcasType).casFeatCode_longitude, v);}    
   
    
  //*--------------*
  //* Feature: longitudePosition

  /** getter for longitudePosition - gets East or west
   * @generated
   * @return value of the feature 
   */
  public String getLongitudePosition() {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_longitudePosition == null)
      jcasType.jcas.throwFeatMissing("longitudePosition", "edu.cmu.cs.lti.script.type.Location");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Location_Type)jcasType).casFeatCode_longitudePosition);}
    
  /** setter for longitudePosition - sets East or west 
   * @generated
   * @param v value to set into the feature 
   */
  public void setLongitudePosition(String v) {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_longitudePosition == null)
      jcasType.jcas.throwFeatMissing("longitudePosition", "edu.cmu.cs.lti.script.type.Location");
    jcasType.ll_cas.ll_setStringValue(addr, ((Location_Type)jcasType).casFeatCode_longitudePosition, v);}    
   
    
  //*--------------*
  //* Feature: latitude

  /** getter for latitude - gets 
   * @generated
   * @return value of the feature 
   */
  public float getLatitude() {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_latitude == null)
      jcasType.jcas.throwFeatMissing("latitude", "edu.cmu.cs.lti.script.type.Location");
    return jcasType.ll_cas.ll_getFloatValue(addr, ((Location_Type)jcasType).casFeatCode_latitude);}
    
  /** setter for latitude - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLatitude(float v) {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_latitude == null)
      jcasType.jcas.throwFeatMissing("latitude", "edu.cmu.cs.lti.script.type.Location");
    jcasType.ll_cas.ll_setFloatValue(addr, ((Location_Type)jcasType).casFeatCode_latitude, v);}    
   
    
  //*--------------*
  //* Feature: latitudePosition

  /** getter for latitudePosition - gets North or south
   * @generated
   * @return value of the feature 
   */
  public String getLatitudePosition() {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_latitudePosition == null)
      jcasType.jcas.throwFeatMissing("latitudePosition", "edu.cmu.cs.lti.script.type.Location");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Location_Type)jcasType).casFeatCode_latitudePosition);}
    
  /** setter for latitudePosition - sets North or south 
   * @generated
   * @param v value to set into the feature 
   */
  public void setLatitudePosition(String v) {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_latitudePosition == null)
      jcasType.jcas.throwFeatMissing("latitudePosition", "edu.cmu.cs.lti.script.type.Location");
    jcasType.ll_cas.ll_setStringValue(addr, ((Location_Type)jcasType).casFeatCode_latitudePosition, v);}    
   
    
  //*--------------*
  //* Feature: names

  /** getter for names - gets 
   * @generated
   * @return value of the feature 
   */
  public StringList getNames() {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_names == null)
      jcasType.jcas.throwFeatMissing("names", "edu.cmu.cs.lti.script.type.Location");
    return (StringList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Location_Type)jcasType).casFeatCode_names)));}
    
  /** setter for names - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setNames(StringList v) {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_names == null)
      jcasType.jcas.throwFeatMissing("names", "edu.cmu.cs.lti.script.type.Location");
    jcasType.ll_cas.ll_setRefValue(addr, ((Location_Type)jcasType).casFeatCode_names, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: historicalEvents

  /** getter for historicalEvents - gets 
   * @generated
   * @return value of the feature 
   */
  public StringList getHistoricalEvents() {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_historicalEvents == null)
      jcasType.jcas.throwFeatMissing("historicalEvents", "edu.cmu.cs.lti.script.type.Location");
    return (StringList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Location_Type)jcasType).casFeatCode_historicalEvents)));}
    
  /** setter for historicalEvents - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setHistoricalEvents(StringList v) {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_historicalEvents == null)
      jcasType.jcas.throwFeatMissing("historicalEvents", "edu.cmu.cs.lti.script.type.Location");
    jcasType.ll_cas.ll_setRefValue(addr, ((Location_Type)jcasType).casFeatCode_historicalEvents, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: entitiesLocactedIn

  /** getter for entitiesLocactedIn - gets 
   * @generated
   * @return value of the feature 
   */
  public StringList getEntitiesLocactedIn() {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_entitiesLocactedIn == null)
      jcasType.jcas.throwFeatMissing("entitiesLocactedIn", "edu.cmu.cs.lti.script.type.Location");
    return (StringList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Location_Type)jcasType).casFeatCode_entitiesLocactedIn)));}
    
  /** setter for entitiesLocactedIn - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setEntitiesLocactedIn(StringList v) {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_entitiesLocactedIn == null)
      jcasType.jcas.throwFeatMissing("entitiesLocactedIn", "edu.cmu.cs.lti.script.type.Location");
    jcasType.ll_cas.ll_setRefValue(addr, ((Location_Type)jcasType).casFeatCode_entitiesLocactedIn, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: locatedIn

  /** getter for locatedIn - gets 
   * @generated
   * @return value of the feature 
   */
  public StringList getLocatedIn() {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_locatedIn == null)
      jcasType.jcas.throwFeatMissing("locatedIn", "edu.cmu.cs.lti.script.type.Location");
    return (StringList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Location_Type)jcasType).casFeatCode_locatedIn)));}
    
  /** setter for locatedIn - sets  
   * @generated
   * @param v value to set into the feature 
   */
  public void setLocatedIn(StringList v) {
    if (Location_Type.featOkTst && ((Location_Type)jcasType).casFeat_locatedIn == null)
      jcasType.jcas.throwFeatMissing("locatedIn", "edu.cmu.cs.lti.script.type.Location");
    jcasType.ll_cas.ll_setRefValue(addr, ((Location_Type)jcasType).casFeatCode_locatedIn, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    