package sim.util.geo; 
import sim.io.geo.*; 
import sim.util.*; 
import java.util.*; 
import sim.util.geo.*; 

/** 
 A simple class for inspecting MasonGeometry objects.  Unlike sim.util.SimpleProperties, GeometricProperties
 only displays Strings, as the underlying meta-data is assumed to be unmodifiable.   As such, we do not look 
 for getXXX and setXXX methods, but rather create the inspector fields from an underlying TreeMap, which contains 
 all the <Name, Value> pairs.  We assume the user added all metadata of interest prior to using MASON.    
 
 Note that each object can have a different number of display fields.  
 */

public class GeometricProperties extends sim.util.Properties {
	
	/** Holds the <Name, Value> pairs to be displayed. */ 
	TreeMap attrMethods = new TreeMap(); 
	
	/** Default constructor.  */ 
	public GeometricProperties(GeometryInfo info)
	{
		if (info != null)
			attrMethods = info.fields;
	}
	
	/**  Always returns null since we assume the underlying meta-data is static. */ 
	protected Object _setValue(int index, Object value) 
	{  
		if (index < 0 || index > numProperties()) return null; 
		Set keys = attrMethods.keySet(); 
		Iterator iter = keys.iterator(); 
		for (int i=0; i <= index-1; i++) iter.next();
		String key = (String)iter.next(); 
		
		AttributeField field = (AttributeField) attrMethods.get(key); 
		field.value = value; 
		attrMethods.put(key, field); 
		return getValue(index); 
	}
	
	/** 
	 Returns the name of the given property.  Returns null if the index is out of the range [0 ... numProperties() - 1 ]
	 */ 
	public String getName(int index)
	{
		if (index < 0 || index > numProperties()) return null; 
		Set keys = attrMethods.keySet(); 
		Iterator iter = keys.iterator(); 
		for (int i=0; i <= index-1; i++) iter.next();
		return (String)iter.next(); 
	}
	
	/**
	 Returns the value of the given property.  Returns null if the index is out of the range [0 ... numProperties() - 1 ]
	 */
	public Object getValue(int index)
	{
		if (index < 0 || index > numProperties()) return null; 
		Set keys = attrMethods.keySet(); 
		Iterator iter = keys.iterator(); 
		for (int i=0; i <= index-1; i++) iter.next();
		AttributeField f = (AttributeField) attrMethods.get(iter.next());
		return f.value; 
	}
	
	/* Returns the number of properties */
	public int numProperties()
	{
		return attrMethods.size(); 
	}
	
	/** Always returns false since we assume the underlying metadata is static */ 
	public boolean isVolatile() { return false;  }
	
	/** Always return String.class since we assume all properties are of type String */ 
	public Class getType(int index) 
	{  
		if (index < 0 || index > numProperties()) return null; 
		Set keys = attrMethods.keySet(); 
		Iterator iter = keys.iterator(); 
		for (int i=0; i <= index-1; i++) iter.next();
		AttributeField f = (AttributeField) attrMethods.get(iter.next()); 
		return getTypeConversion(f.value.getClass()); 
	}
	
	/** Always returns false, since we assume the underlying metadata is static */
	public boolean isReadWrite(int index) { return true; }
}
