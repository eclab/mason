package sim.io.geo; 
import org.gdal.ogr.*;
import sim.util.Bag; 
import sim.util.geo.*; 

/**
 OGRInfo holds a verion of GeometryInfo that uses the OGR JNI interface to 
 populate the <name, value> pairs.  
  */
public class OGRInfo extends GeometryInfo { 
	
	/** Basic contructor. If the Bag is null, then all the fields are displayed in the Inspector.  
	 Otherwise, only field names inside the Bag are displayed by the Inspector. */ 
	public OGRInfo (Feature feature, Bag masked)
	{
		super(); 
		
		String  key=""; 
		Object val; 
		for (int i=0 ; i < feature.GetFieldCount(); i++) { 
			FieldDefn fieldDef = feature.GetFieldDefnRef(i); 
			key = fieldDef.GetNameRef(); 
			if (masked == null || masked.contains(key)) { 
				
				val = null; 
				char type; 
				int fieldType = fieldDef.GetFieldType(); 
				if (fieldType == ogrConstants.OFTString) { 
					type = 'C'; 
					val = new String(feature.GetFieldAsString(i)); 
				}
				else if (fieldType == ogrConstants.OFTInteger) { 
					type = 'N'; 
					val = new Integer(feature.GetFieldAsInteger(i)); 
				}
				else { //fieldType == ogrConstants.OFTReal 
					type = 'N'; 
					val = new Double(feature.GetFieldAsDouble(i)); 
				}
										
				AttributeField attr = new AttributeField(key, type, fieldDef.GetWidth()); 
				attr.value = val; 
				fields.put(key, attr); 
			}
		}
	}
}



