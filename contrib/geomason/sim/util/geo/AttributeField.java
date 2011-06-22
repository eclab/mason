/* 
Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
George Mason University Mason University Licensed under the Academic
Free License version 3.0

See the file "LICENSE" for more information
*/
package sim.util.geo; 

/**
 * A utility class to hold information about each attribute of a JTS geometry.  Attributes consist of a name, value, 
 * whether to display this attribute in the inspector or not, and the number of bytes needed to store the value.  We store 
 * the number of bytes since the attributes are formatted similar to a relational database table where each column has a 
 * fixed width (not necessarily the same width as the data) defined by the user.  The number of bytes is used to pad the data
 * when writing to disk.  
 */

public class AttributeField implements java.io.Serializable { 

	private static final long serialVersionUID = -2342742107342686581L;

	/** Attribute name */ 
    public String name;  
    
    /** Attribute value */ 
    public Object value; 
    
    /** Whether the attribute is displayed in the inspector or not */
    public boolean hidden;  
        
    /** Attributes are stored in format similar to a relational database table, so we 
     * need to save the size of the field for exporting.  
     */
    public int fieldSize; 
    
    public AttributeField(String n) { this (n, null, 0, false); }
    public AttributeField(String n, Object v) { this(n, v, 0, false); }
    public AttributeField(String n, Object v, int f, boolean h)
    {
        name = n; 
        value = v;  
        hidden = h;
        fieldSize = f; 
    }
        
    /** Human readable form */ 
    public String toString()
    {
        return "Name: " + name +  " Value: " + value + " Field size: " + fieldSize + " Hidden: " + hidden; 
    }
        
    /** Simple, shallow clone */ 
    public Object clone()
    {
        AttributeField a = new AttributeField(name, value, fieldSize, hidden); 
        return a; 
    }
}
