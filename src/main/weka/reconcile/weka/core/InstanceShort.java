/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    Instance.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package reconcile.weka.core;

import java.io.Serializable;
import java.util.Enumeration;

/**
 * Class for handling an instance. All values (numeric, nominal, or
 * string) are internally stored as floating-point numbers. If an
 * attribute is nominal (or a string), the stored value is the index
 * of the corresponding nominal (or string) value in the attribute's
 * definition. We have chosen this approach in favor of a more elegant
 * object-oriented approach because it is much faster. <p>
 *
 * Typical usage (code from the main() method of this class): <p>
 *
 * <code>
 * ... <br>
 *      
 * // Create empty instance with three attribute values <br>
 * Instance inst = new Instance(3); <br><br>
 *     
 * // Set instance's values for the attributes "length", "weight", and "position"<br>
 * inst.setValue(length, 5.3); <br>
 * inst.setValue(weight, 300); <br>
 * inst.setValue(position, "first"); <br><br>
 *   
 * // Set instance's dataset to be the dataset "race" <br>
 * inst.setDataset(race); <br><br>
 *   
 * // Print the instance <br>
 * System.out.println("The instance: " + inst); <br>
 *
 * ... <br>
 * </code><p>
 *
 * All methods that change an instance are safe, ie. a change of an
 * instance does not affect any other instances. All methods that
 * change an instance's attribute values clone the attribute value
 * vector before it is changed. If your application heavily modifies
 * instance values, it may be faster to create a new instance from scratch.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $ 
 */
public class InstanceShort implements Copyable, Serializable {
  
  /** Constant representing a missing value. */
  protected static final short MISSING_VALUE = Short.MIN_VALUE;

  /** 
   * The dataset the instance has access to.  Null if the instance
   * doesn't have access to any dataset.  Only if an instance has
   * access to a dataset, it knows about the actual attribute types.  
   */
  protected /*@spec_public@*/ InstancesShort m_Dataset;

  /** The instance's attribute values. */
  protected /*@spec_public non_null@*/ short[] m_AttValues;

  /** The instance's weight. */
  protected float m_Weight;

  /**
   * Constructor that copies the attribute values and the weight from
   * the given instance. Reference to the dataset is set to null.
   * (ie. the instance doesn't have access to information about the
   * attribute types)
   *
   * @param instance the instance from which the attribute
   * values and the weight are to be copied 
   */
  //@ ensures m_Dataset == null;
  protected InstanceShort(/*@non_null@*/ InstanceShort instance) {
    
    m_AttValues = instance.m_AttValues;
    m_Weight = instance.m_Weight;
    m_Dataset = null;
  }

  /**
   * Constructor that inititalizes instance variable with given
   * values. Reference to the dataset is set to null. (ie. the instance
   * doesn't have access to information about the attribute types)
   *
   * @param weight the instance's weight
   * @param attValues a vector of attribute values 
   */
  //@ ensures m_Dataset == null;
  public InstanceShort(float weight,  /*@non_null@*/ short[]attValues){
    
    m_AttValues = attValues;
    m_Weight = weight;
    m_Dataset = null;
  }

  /**
   * Constructor of an instance that sets weight to one, all values to
   * be missing, and the reference to the dataset to null. (ie. the instance
   * doesn't have access to information about the attribute types)
   *
   * @param numAttributes the size of the instance 
   */
  //@ requires numAttributes > 0;    // Or maybe == 0 is okay too?
  //@ ensures m_Dataset == null;
  public InstanceShort(int numAttributes) {
    
    m_AttValues = new short[numAttributes];
    for (int i = 0; i < m_AttValues.length; i++) {
      m_AttValues[i] = MISSING_VALUE;
    }
    m_Weight = 1;
    m_Dataset = null;
  }

  /**
   * Returns the attribute with the given index.
   *
   * @param index the attribute's index
   * @return the attribute at the given position
   * @exception UnassignedDatasetException if instance doesn't have access to a
   * dataset
   */ 
  //@ requires m_Dataset != null;
  public /*@pure@*/ AttributeShort attribute(int index) {
   
    if (m_Dataset == null) {
      throw new UnassignedDatasetException("Instance doesn't have access to a dataset!");
    }
    return m_Dataset.attribute(index);
  }

  /**
   * Returns the attribute with the given index. Does the same
   * thing as attribute().
   *
   * @param indexOfIndex the index of the attribute's index 
   * @return the attribute at the given position
   * @exception UnassignedDatasetException if instance doesn't have access to a
   * dataset
   */ 
  //@ requires m_Dataset != null;
  public /*@pure@*/ AttributeShort attributeSparse(int indexOfIndex) {
   
    if (m_Dataset == null) {
      throw new UnassignedDatasetException("Instance doesn't have access to a dataset!");
    }
    return m_Dataset.attribute(indexOfIndex);
  }

  /**
   * Returns class attribute.
   *
   * @return the class attribute
   * @exception UnassignedDatasetException if the class is not set or the
   * instance doesn't have access to a dataset
   */
  //@ requires m_Dataset != null;
  public /*@pure@*/ AttributeShort classAttribute() {

    if (m_Dataset == null) {
      throw new UnassignedDatasetException("Instance doesn't have access to a dataset!");
    }
    return m_Dataset.classAttribute();
  }

  /**
   * Returns the class attribute's index.
   *
   * @return the class index as an integer 
   * @exception UnassignedDatasetException if instance doesn't have access to a dataset 
   */
  //@ requires m_Dataset != null;
  //@ ensures  \result == m_Dataset.classIndex();
  public /*@pure@*/ int classIndex() {
    
    if (m_Dataset == null) {
      throw new UnassignedDatasetException("Instance doesn't have access to a dataset!");
    }
    return m_Dataset.classIndex();
  }

  /**
   * Tests if an instance's class is missing.
   *
   * @return true if the instance's class is missing
   * @exception UnassignedClassException if the class is not set or the instance doesn't
   * have access to a dataset
   */
  //@ requires classIndex() >= 0;
  public /*@pure@*/ boolean classIsMissing() {

    if (classIndex() < 0) {
      throw new UnassignedClassException("Class is not set!");
    }
    return isMissing(classIndex());
  }

  /**
   * Returns an instance's class value in internal format. (ie. as a
   * floating-point number)
   *
   * @return the corresponding value as a double (If the 
   * corresponding attribute is nominal (or a string) then it returns the 
   * value's index as a double).
   * @exception UnassignedClassException if the class is not set or the instance doesn't
   * have access to a dataset 
   */
  //@ requires classIndex() >= 0;
  public /*@pure@*/ short classValue() {
    
    if (classIndex() < 0) {
      throw new UnassignedClassException("Class is not set!");
    }
    return value(classIndex());
  }

  /**
   * Produces a shallow copy of this instance. The copy has
   * access to the same dataset. (if you want to make a copy
   * that doesn't have access to the dataset, use 
   * <code>new Instance(instance)</code>
   *
   * @return the shallow copy
   */
  //@ also ensures \result != null;
  //@ also ensures \result instanceof Instance;
  //@ also ensures ((Instance)\result).m_Dataset == m_Dataset;
  public /*@pure@*/ Object copy() {
    InstanceShort result = new InstanceShort(this);
    result.m_Dataset = m_Dataset;
    return result;
  }

  /**
   * Returns the dataset this instance has access to. (ie. obtains
   * information about attribute types from) Null if the instance
   * doesn't have access to a dataset.
   *
   * @return the dataset the instance has accesss to
   */
  //@ ensures \result == m_Dataset;
  public /*@pure@*/ InstancesShort dataset() {

    return m_Dataset;
  }

  /**
   * Deletes an attribute at the given position (0 to 
   * numAttributes() - 1). Only succeeds if the instance does not
   * have access to any dataset because otherwise inconsistencies
   * could be introduced.
   *
   * @param pos the attribute's position
   * @exception RuntimeException if the instance has access to a
   * dataset 
   */
  //@ requires m_Dataset != null;
  public void deleteAttributeAt(int position) {

    if (m_Dataset != null) {
      throw new RuntimeException("Instance has access to a dataset!");
    }
    forceDeleteAttributeAt(position);
  }

  /**
   * Returns an enumeration of all the attributes.
   *
   * @return enumeration of all the attributes
   * @exception UnassignedDatasetException if the instance doesn't
   * have access to a dataset 
   */
  //@ requires m_Dataset != null;
  public /*@pure@*/ Enumeration enumerateAttributes() {

    if (m_Dataset == null) {
      throw new UnassignedDatasetException("Instance doesn't have access to a dataset!");
    }
    return m_Dataset.enumerateAttributes();
  }

  /**
   * Tests if the headers of two instances are equivalent.
   *
   * @param instance another instance
   * @return true if the header of the given instance is 
   * equivalent to this instance's header
   * @exception UnassignedDatasetException if instance doesn't have access to any
   * dataset
   */
  //@ requires m_Dataset != null;
  public /*@pure@*/ boolean equalHeaders(InstanceShort inst) {

    if (m_Dataset == null) {
      throw new UnassignedDatasetException("Instance doesn't have access to a dataset!");
    }
    return m_Dataset.equalHeaders(inst.m_Dataset);
  }

  /**
   * Tests whether an instance has a missing value. Skips the class attribute if set.
   * @return true if instance has a missing value.
   * @exception UnassignedDatasetException if instance doesn't have access to any
   * dataset
   */
  //@ requires m_Dataset != null;
  public /*@pure@*/ boolean hasMissingValue() {
    
    if (m_Dataset == null) {
      throw new UnassignedDatasetException("Instance doesn't have access to a dataset!");
    }
    for (int i = 0; i < numAttributes(); i++) {
      if (i != classIndex()) {
	if (isMissing(i)) {
	  return true;
	}
      }
    }
    return false;
  }

  /**
   * Returns the index of the attribute stored at the given position.
   * Just returns the given value.
   *
   * @param position the position 
   * @return the index of the attribute stored at the given position
   */
  public /*@pure@*/ int index(int position) {

    return position;
  }

  /**
   * Inserts an attribute at the given position (0 to 
   * numAttributes()). Only succeeds if the instance does not
   * have access to any dataset because otherwise inconsistencies
   * could be introduced.
   *
   * @param pos the attribute's position
   * @exception RuntimeException if the instance has accesss to a
   * dataset
   * @exception IllegalArgumentException if the position is out of range
   */
  //@ requires m_Dataset == null;
  //@ requires 0 <= position && position <= numAttributes();
  public void insertAttributeAt(int position) {

    if (m_Dataset != null) {
      throw new RuntimeException("Instance has accesss to a dataset!");
    }
    if ((position < 0) ||
	(position > numAttributes())) {
      throw new IllegalArgumentException("Can't insert attribute: index out "+
                                         "of range");
    }
    forceInsertAttributeAt(position);
  }

  /**
   * Tests if a specific value is "missing".
   *
   * @param attIndex the attribute's index
   */
  public /*@pure@*/ boolean isMissing(int attIndex) {

    if (Short.MIN_VALUE == (m_AttValues[attIndex])) {
      return true;
    }
    return false;
  }

  /**
   * Tests if a specific value is "missing". Does
   * the same thing as isMissing() if applied to an Instance.
   *
   * @param indexOfIndex the index of the attribute's index 
   */
  public /*@pure@*/ boolean isMissingSparse(int indexOfIndex) {

    if (Short.MIN_VALUE == (m_AttValues[indexOfIndex])) {
      return true;
    }
    return false;
  }

  /**
   * Tests if a specific value is "missing".
   * The given attribute has to belong to a dataset.
   *
   * @param att the attribute
   */
  public /*@pure@*/ boolean isMissing(AttributeShort att) {

    return isMissing(att.index());
  }

  /**
   * Tests if the given value codes "missing".
   *
   * @param val the value to be tested
   * @return true if val codes "missing"
   */
  public static /*@pure@*/ boolean isMissingValue(double val) {

    return Double.isNaN(val);
  }

  /**
   * Merges this instance with the given instance and returns
   * the result. Dataset is set to null.
   *
   * @param inst the instance to be merged with this one
   * @return the merged instances
   */
  public InstanceShort mergeInstance(InstanceShort inst) {

    int m = 0;
    short [] newVals = new short[numAttributes() + inst.numAttributes()];
    for (int j = 0; j < numAttributes(); j++, m++) {
      newVals[m] = value(j);
    }
    for (int j = 0; j < inst.numAttributes(); j++, m++) {
      newVals[m] = inst.value(j);
    }
    return new InstanceShort(1, newVals);
  }

  /**
   * Returns the double that codes "missing".
   *
   * @return the double that codes "missing"
   */
  public /*@pure@*/ static short missingValue() {

    return MISSING_VALUE;
  }

  /**
   * Returns the number of attributes.
   *
   * @return the number of attributes as an integer
   */
  //@ ensures \result == m_AttValues.length;
  public /*@pure@*/ int numAttributes() {

    return m_AttValues.length;
  }

  /**
   * Returns the number of class labels.
   *
   * @return the number of class labels as an integer if the 
   * class attribute is nominal, 1 otherwise.
   * @exception UnassignedDatasetException if instance doesn't have access to any
   * dataset
   */
  //@ requires m_Dataset != null;
  public /*@pure@*/ int numClasses() {
    
    if (m_Dataset == null) {
      throw new UnassignedDatasetException("Instance doesn't have access to a dataset!");
    }
    return m_Dataset.numClasses();
  }

  /**
   * Returns the number of values present. Always the same as numAttributes().
   *
   * @return the number of values
   */
  //@ ensures \result == m_AttValues.length;
  public /*@pure@*/ int numValues() {

    return m_AttValues.length;
  }

  /** 
   * Replaces all missing values in the instance with the
   * values contained in the given array. A deep copy of
   * the vector of attribute values is performed before the
   * values are replaced.
   *
   * @param array containing the means and modes
   * @exception IllegalArgumentException if numbers of attributes are unequal
   */
  public void replaceMissingValues(short[] array) {
	 
    if ((array == null) || 
	(array.length != m_AttValues.length)) {
      throw new IllegalArgumentException("Unequal number of attributes!");
    }
    freshAttributeVector();
    for (int i = 0; i < m_AttValues.length; i++) {
      if (isMissing(i)) {
	m_AttValues[i] = array[i];
      }
    }
  }

  /**
   * Sets the class value of an instance to be "missing". A deep copy of
   * the vector of attribute values is performed before the
   * value is set to be missing.
   *
   * @exception UnassignedClassException if the class is not set
   * @exception UnassignedDatasetException if the instance doesn't
   * have access to a dataset
   */
  //@ requires classIndex() >= 0;
  public void setClassMissing() {

    if (classIndex() < 0) {
      throw new UnassignedClassException("Class is not set!");
    }
    setMissing(classIndex());
  }

  /**
   * Sets the class value of an instance to the given value (internal
   * floating-point format).  A deep copy of the vector of attribute
   * values is performed before the value is set.
   *
   * @param value the new attribute value (If the corresponding
   * attribute is nominal (or a string) then this is the new value's
   * index as a double).  
   * @exception UnassignedClassException if the class is not set
   * @exception UnaddignedDatasetException if the instance doesn't
   * have access to a dataset 
   */
  //@ requires classIndex() >= 0;
  public void setClassValue(short value) {

    if (classIndex() < 0) {
      throw new UnassignedClassException("Class is not set!");
    }
    setValue(classIndex(), value);
  }

  /**
   * Sets the class value of an instance to the given value. A deep
   * copy of the vector of attribute values is performed before the
   * value is set.
   *
   * @param value the new class value (If the class
   * is a string attribute and the value can't be found,
   * the value is added to the attribute).
   * @exception UnassignedClassException if the class is not set
   * @exception UnassignedDatasetException if the dataset is not set
   * @exception IllegalArgumentException if the attribute is not
   * nominal or a string, or the value couldn't be found for a nominal
   * attribute 
   */
  //@ requires classIndex() >= 0;
  public final void setClassValue(String value) {

    if (classIndex() < 0) {
      throw new UnassignedClassException("Class is not set!");
    }
    setValue(classIndex(), value);
  }

  /**
   * Sets the reference to the dataset. Does not check if the instance
   * is compatible with the dataset. Note: the dataset does not know
   * about this instance. If the structure of the dataset's header
   * gets changed, this instance will not be adjusted automatically.
   *
   * @param instances the reference to the dataset 
   */
  public final void setDataset(InstancesShort instances) {
    
    m_Dataset = instances;
  }

  /**
   * Sets a specific value to be "missing". Performs a deep copy
   * of the vector of attribute values before the value is set to
   * be missing.
   *
   * @param attIndex the attribute's index
   */
  public final void setMissing(int attIndex) {

    setValue(attIndex, MISSING_VALUE);
  }

  /**
   * Sets a specific value to be "missing". Performs a deep copy
   * of the vector of attribute values before the value is set to
   * be missing. The given attribute has to belong to a dataset.
   *
   * @param att the attribute
   */
  public final void setMissing(AttributeShort att) {

    setMissing(att.index());
  }

  /**
   * Sets a specific value in the instance to the given value 
   * (internal floating-point format). Performs a deep copy
   * of the vector of attribute values before the value is set.
   *
   * @param attIndex the attribute's index 
   * @param value the new attribute value (If the corresponding
   * attribute is nominal (or a string) then this is the new value's
   * index as a double).  
   */
  public void setValue(int attIndex, short value) {
    
    freshAttributeVector();
    m_AttValues[attIndex] = value;
  }

  /**
   * Sets a specific value in the instance to the given value 
   * (internal floating-point format). Performs a deep copy
   * of the vector of attribute values before the value is set.
   * Does exactly the same thing as setValue().
   *
   * @param indexOfIndex the index of the attribute's index 
   * @param value the new attribute value (If the corresponding
   * attribute is nominal (or a string) then this is the new value's
   * index as a double).  
   */
  public void setValueSparse(int indexOfIndex, short value) {
    
    freshAttributeVector();
    m_AttValues[indexOfIndex] = value;
  }

  /**
   * Sets a value of a nominal or string attribute to the given
   * value. Performs a deep copy of the vector of attribute values
   * before the value is set.
   *
   * @param attIndex the attribute's index
   * @param value the new attribute value (If the attribute
   * is a string attribute and the value can't be found,
   * the value is added to the attribute).
   * @exception UnassignedDatasetException if the dataset is not set
   * @exception IllegalArgumentException if the selected
   * attribute is not nominal or a string, or the supplied value couldn't 
   * be found for a nominal attribute 
   */
  //@ requires m_Dataset != null;
  public final void setValue(int attIndex, String value) {
    
    short valIndex;

    if (m_Dataset == null) {
      throw new UnassignedDatasetException("Instance doesn't have access to a dataset!");
    }
    if (!attribute(attIndex).isNominal() &&
	!attribute(attIndex).isString()) {
      throw new IllegalArgumentException("Attribute neither nominal nor string!");
    }
    valIndex = attribute(attIndex).indexOfValue(value);
    if (valIndex == -1) {
      if (attribute(attIndex).isNominal()) {
	throw new IllegalArgumentException("Value not defined for given nominal attribute!");
      } else {
	attribute(attIndex).forceAddValue(value);
	valIndex = attribute(attIndex).indexOfValue(value);
      }
    }
    setValue(attIndex, (short)valIndex); 
  }

  /**
   * Sets a specific value in the instance to the given value
   * (internal floating-point format). Performs a deep copy of the
   * vector of attribute values before the value is set, so if you are
   * planning on calling setValue many times it may be faster to
   * create a new instance using toDoubleArray.  The given attribute
   * has to belong to a dataset.
   *
   * @param att the attribute 
   * @param value the new attribute value (If the corresponding
   * attribute is nominal (or a string) then this is the new value's
   * index as a double).  
   */
  public final void setValue(AttributeShort att, short value) {

    setValue(att.index(), value);
  }

  /**
   * Sets a value of an nominal or string attribute to the given
   * value. Performs a deep copy of the vector of attribute values
   * before the value is set, so if you are planning on calling setValue many
   * times it may be faster to create a new instance using toDoubleArray.
   * The given attribute has to belong to a dataset.
   *
   * @param att the attribute
   * @param value the new attribute value (If the attribute
   * is a string attribute and the value can't be found,
   * the value is added to the attribute).
   * @exception IllegalArgumentException if the the attribute is not
   * nominal or a string, or the value couldn't be found for a nominal
   * attribute 
   */
  public final void setValue(AttributeShort att, String value) {

    if (!att.isNominal() &&
	!att.isString()) {
      throw new IllegalArgumentException("Attribute neither nominal nor string!");
    }
    int valIndex = att.indexOfValue(value);
    if (valIndex == -1) {
      if (att.isNominal()) {
	throw new IllegalArgumentException("Value not defined for given nominal attribute!");
      } else {
	att.forceAddValue(value);
	valIndex = att.indexOfValue(value);
      }
    }
    setValue(att.index(), (short)valIndex);
  }

  /**
   * Sets the weight of an instance.
   *
   * @param weight the weight
   */
  public final void setWeight(float weight) {

    m_Weight = weight;
  }

  /** 
   * Returns the string value of a nominal, string, or date attribute
   * for the instance.
   *
   * @param attIndex the attribute's index
   * @return the value as a string
   * @exception IllegalArgumentException if the attribute is not a nominal,
   * string, or date attribute.
   * @exception UnassignedDatasetException if the instance doesn't belong
   * to a dataset.
   */
  //@ requires m_Dataset != null;
  public final /*@pure@*/ String stringValue(int attIndex) {

    if (m_Dataset == null) {
      throw new UnassignedDatasetException("Instance doesn't have access to a dataset!");
    } 
    return stringValue(m_Dataset.attribute(attIndex));
  }

  /** 
   * Returns the string value of a nominal, string, or date attribute
   * for the instance.
   *
   * @param att the attribute
   * @return the value as a string
   * @exception IllegalArgumentException if the attribute is not a nominal,
   * string, or date attribute.
   * @exception UnassignedDatasetException if the instance doesn't belong
   * to a dataset.
   */
  public final /*@pure@*/ String stringValue(AttributeShort att) {

    int attIndex = att.index();
    switch (att.type()) {
    case AttributeShort.NOMINAL:
    case AttributeShort.STRING:
      return att.value((int) value(attIndex));
    case AttributeShort.DATE:
      return att.formatDate(value(attIndex));
    default:
      throw new IllegalArgumentException("Attribute isn't nominal, string or date!");
    }
  }

  /**
   * Returns the values of each attribute as an array of doubles.
   *
   * @return an array containing all the instance attribute values
   */
  public short[] toShortArray() {

    short[] newValues = new short[m_AttValues.length];
    System.arraycopy(m_AttValues, 0, newValues, 0, 
		     m_AttValues.length);
    return newValues;
  }
  
  /**
   * Return the array of original values
   * 
   * WARNING: watch out for side effects
   */
  
  public short[] getValueArray() {
    return m_AttValues;
  }

  /**
   * Returns the description of one instance. If the instance
   * doesn't have access to a dataset, it returns the internal
   * floating-point values. Quotes string
   * values that contain whitespace characters.
   *
   * @return the instance's description as a string
   */
  public String toString() {

    StringBuffer text = new StringBuffer();
    for (int i = 0; i < m_AttValues.length; i++) {
      if (i > 0) text.append(",");
      text.append(toString(i));
    }

    return text.toString();
  }

  /**
   * Returns the description of one value of the instance as a 
   * string. If the instance doesn't have access to a dataset, it 
   * returns the internal floating-point value. Quotes string
   * values that contain whitespace characters, or if they
   * are a question mark.
   *
   * @param attIndex the attribute's index
   * @return the value's description as a string
   */
  public final /*@pure@*/ String toString(int attIndex) {

   StringBuffer text = new StringBuffer();
   
   if (isMissing(attIndex)) {
     text.append("?");
   } else {
     if (m_Dataset == null) {
       text.append(Utils.doubleToString(m_AttValues[attIndex],6));
     } else {
       switch (m_Dataset.attribute(attIndex).type()) {
       case AttributeShort.NOMINAL:
       case AttributeShort.STRING:
       case AttributeShort.DATE:
         text.append(Utils.quote(stringValue(attIndex)));
         break;
       case AttributeShort.NUMERIC:
	 text.append(Utils.doubleToString(m_Dataset.attribute(attIndex).getOriginalValue(new Short(value(attIndex))),6));
         break;
       default:
         throw new IllegalStateException("Unknown attribute type");
       }
     }
   }
   return text.toString();
  }

  /**
   * Returns the description of one value of the instance as a 
   * string. If the instance doesn't have access to a dataset it 
   * returns the internal floating-point value. Quotes string
   * values that contain whitespace characters, or if they
   * are a question mark.
   * The given attribute has to belong to a dataset.
   *
   * @param att the attribute
   * @return the value's description as a string
   */
  public final String toString(AttributeShort att) {
   
   return toString(att.index());
  }

  /**
   * Returns an instance's attribute value in internal format.
   *
   * @param attIndex the attribute's index
   * @return the specified value as a double (If the corresponding
   * attribute is nominal (or a string) then it returns the value's index as a 
   * double).
   */
  public /*@pure@*/ short value(int attIndex) {

    return m_AttValues[attIndex];
  }

  /**
   * Returns an instance's attribute value in internal format.
   * Does exactly the same thing as value() if applied to an Instance.
   *
   * @param indexOfIndex the index of the attribute's index
   * @return the specified value as a double (If the corresponding
   * attribute is nominal (or a string) then it returns the value's index as a 
   * double).
   */
  public /*@pure@*/ float valueSparse(int indexOfIndex) {

    return m_AttValues[indexOfIndex];
  }  

  /**
   * Returns an instance's attribute value in internal format.
   * The given attribute has to belong to a dataset.
   *
   * @param att the attribute
   * @return the specified value as a double (If the corresponding
   * attribute is nominal (or a string) then it returns the value's index as a
   * double).
   */
  public /*@pure@*/ short value(AttributeShort att) {

    return value(att.index());
  }

  /**
   * Returns the instance's weight.
   *
   * @return the instance's weight as a double
   */
  public final /*@pure@*/ float weight() {

    return m_Weight;
  }

  /**
   * Deletes an attribute at the given position (0 to 
   * numAttributes() - 1).
   *
   * @param pos the attribute's position
   */

  void forceDeleteAttributeAt(int position) {

    short[] newValues = new short[m_AttValues.length - 1];

    System.arraycopy(m_AttValues, 0, newValues, 0, position);
    if (position < m_AttValues.length - 1) {
      System.arraycopy(m_AttValues, position + 1, 
		       newValues, position, 
		       m_AttValues.length - (position + 1));
    }
    m_AttValues = newValues;
  }

  /**
   * Inserts an attribute at the given position
   * (0 to numAttributes()) and sets its value to be missing. 
   *
   * @param pos the attribute's position
   */
  void forceInsertAttributeAt(int position)  {

    short[] newValues = new short[m_AttValues.length + 1];

    System.arraycopy(m_AttValues, 0, newValues, 0, position);
    newValues[position] = MISSING_VALUE;
    System.arraycopy(m_AttValues, position, newValues, 
		     position + 1, m_AttValues.length - position);
    m_AttValues = newValues;
  }

  /**
   * Private constructor for subclasses. Does nothing.
   */
  protected InstanceShort() {
  }

  /**
   * Clones the attribute vector of the instance and
   * overwrites it with the clone.
   */
  private void freshAttributeVector() {

    m_AttValues = toShortArray();
  }

  /**
   * Main method for testing this class.
   */
  //@ requires options != null;
  public static void main(String[] options) {

    try {

      // Create numeric attributes "length" and "weight"
      AttributeShort length = new AttributeShort("length");
      AttributeShort weight = new AttributeShort("weight");
      
      // Create vector to hold nominal values "first", "second", "third" 
      FastVector my_nominal_values = new FastVector(3); 
      my_nominal_values.addElement("first"); 
      my_nominal_values.addElement("second"); 
      my_nominal_values.addElement("third"); 
      
      // Create nominal attribute "position" 
      AttributeShort position = new AttributeShort("position", my_nominal_values);
      
      // Create vector of the above attributes 
      FastVector attributes = new FastVector(3);
      attributes.addElement(length);
      attributes.addElement(weight);
      attributes.addElement(position);
      
      // Create the empty dataset "race" with above attributes
      InstancesShort race = new InstancesShort("race", attributes, 0);
      
      // Make position the class attribute
      race.setClassIndex(position.index());
      
      // Create empty instance with three attribute values
      InstanceShort inst = new InstanceShort(3);
      
      // Set instance's values for the attributes "length", "weight", and "position"
      inst.setValue(length, (short)5);
      inst.setValue(weight, (short)300);
      inst.setValue(position, "first");
      
      // Set instance's dataset to be the dataset "race"
      inst.setDataset(race);
      
      // Print the instance
      System.out.println("The instance: " + inst);
      
      // Print the first attribute
      System.out.println("First attribute: " + inst.attribute(0));
      
      // Print the class attribute
      System.out.println("Class attribute: " + inst.classAttribute());
      
      // Print the class index
      System.out.println("Class index: " + inst.classIndex());
      
      // Say if class is missing
      System.out.println("Class is missing: " + inst.classIsMissing());
      
      // Print the instance's class value in internal format
      System.out.println("Class value (internal format): " + inst.classValue());
      
      // Print a shallow copy of this instance
      InstanceShort copy = (InstanceShort) inst.copy();
      System.out.println("Shallow copy: " + copy);
      
      // Set dataset for shallow copy
      copy.setDataset(inst.dataset());
      System.out.println("Shallow copy with dataset set: " + copy);
      
      // Unset dataset for copy, delete first attribute, and insert it again
      copy.setDataset(null);
      copy.deleteAttributeAt(0);
      copy.insertAttributeAt(0);
      copy.setDataset(inst.dataset());
      System.out.println("Copy with first attribute deleted and inserted: " + copy); 
      
      // Enumerate attributes (leaving out the class attribute)
      System.out.println("Enumerating attributes (leaving out class):");
      Enumeration enu = inst.enumerateAttributes();
      while (enu.hasMoreElements()) {
	AttributeShort att = (AttributeShort) enu.nextElement();
	System.out.println(att);
      }
      
      // Headers are equivalent?
      System.out.println("Header of original and copy equivalent: " +
			 inst.equalHeaders(copy));

      // Test for missing values
      System.out.println("Length of copy missing: " + copy.isMissing(length));
      System.out.println("Weight of copy missing: " + copy.isMissing(weight.index()));
      System.out.println("Length of copy missing: " + 
			 InstanceShort.isMissingValue(copy.value(length)));
      System.out.println("Missing value coded as: " + InstanceShort.missingValue());

      // Prints number of attributes and classes
      System.out.println("Number of attributes: " + copy.numAttributes());
      System.out.println("Number of classes: " + copy.numClasses());

      // Replace missing values
      short[] meansAndModes = {2, 3, 0};
      copy.replaceMissingValues(meansAndModes);
      System.out.println("Copy with missing value replaced: " + copy);

      // Setting and getting values and weights
      copy.setClassMissing();
      System.out.println("Copy with missing class: " + copy);
      copy.setClassValue((short)0);
      System.out.println("Copy with class value set to first value: " + copy);
      copy.setClassValue("third");
      System.out.println("Copy with class value set to \"third\": " + copy);
      copy.setMissing(1);
      System.out.println("Copy with second attribute set to be missing: " + copy);
      copy.setMissing(length);
      System.out.println("Copy with length set to be missing: " + copy);
      copy.setValue(0, (short)0);
      System.out.println("Copy with first attribute set to 0: " + copy);
      copy.setValue(weight, (short)1);
      System.out.println("Copy with weight attribute set to 1: " + copy);
      copy.setValue(position, "second");
      System.out.println("Copy with position set to \"second\": " + copy);
      copy.setValue(2, "first");
      System.out.println("Copy with last attribute set to \"first\": " + copy);
      System.out.println("Current weight of instance copy: " + copy.weight());
      copy.setWeight(2);
      System.out.println("Current weight of instance copy (set to 2): " + copy.weight());
      System.out.println("Last value of copy: " + copy.toString(2));
      System.out.println("Value of position for copy: " + copy.toString(position));
      System.out.println("Last value of copy (internal format): " + copy.value(2));
      System.out.println("Value of position for copy (internal format): " + 
			 copy.value(position));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
