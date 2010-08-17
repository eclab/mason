package sim.util.geo; 

/**
 * A utility class to hold information about each attribute of a JTS geometry. 
 *
 */

public class AttributeField { 

	/** Attribute name */ 
    public String name;  
    
    /** Attribute value */ 
    public Object value; 
    
    /** Whether the attribute is displayed in the inspector or not */
    public boolean hidden;  
        
    public AttributeField(String n) { this (n, null, false); }
    public AttributeField(String n, Object v) { this(n, v, false); }
    public AttributeField(String n, Object v, boolean h)
    {
        name = n; 
        value = v;  
        hidden = h;
    }
        
    /** Human readable form */ 
    public String toString()
    {
        return "Name: " + name +  " Value: " + value + "Hidden: " + hidden; 
    }
        
    /** Simple, shallow clone */ 
    public Object clone()
    {
        AttributeField a = new AttributeField(name, value, hidden); 
        return a; 
    }
}
