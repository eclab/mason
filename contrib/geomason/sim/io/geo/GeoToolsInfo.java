package sim.io.geo; 

import sim.util.Bag; 
import org.opengis.feature.simple.*; 
import org.opengis.feature.type.*; 
import org.opengis.filter.*; 
import java.util.*; 
import sim.util.geo.*; 

/** 
 Uses the GeoTools API to polutate the underlying <name, value> pairs.  
 */

public class GeoToolsInfo extends GeometryInfo { 

	/** Default constructor.  The SimpleFeature object contains all the metadata for one object.  
	 If the Bag is null, then all the fields are displayed in the Inspector.  Otherwise, only 
	 field names inside the Bag are displayed by the Inspector.  
	 */ 
	public GeoToolsInfo(SimpleFeature feature, Bag masked)
	{
		super(); 
		String key="", val=""; 
		SimpleFeatureType type = feature.getFeatureType(); 
		
		for (int i=0; i < feature.getAttributeCount(); i++) { 
			AttributeDescriptor desc = type.getDescriptor(i); 
			key = desc.getLocalName(); 
			if (key.equals("the_geom")) continue;
			
			if (masked == null || masked.contains(key)) {
				AttributeType attrType = desc.getType(); 
				Class binding = attrType.getBinding(); 
				String className = binding.getName(); 
				
				char t='C'; 
				if (className.indexOf("String") != -1)
					t = 'C'; 
				else if (className.indexOf("Double") != -1)
					t = 'N'; 
				else if (className.indexOf("Integer") != -1) 
					t = 'N'; 
				
				List l = attrType.getRestrictions(); 
				Iterator iter = l.iterator(); 
				int len = 20;
				while (iter.hasNext()) { 
					Filter f = (Filter)iter.next(); 
					if (f instanceof BinaryComparisonOperator) { 
						BinaryComparisonOperator b = (BinaryComparisonOperator) f; 
						String ex1 = b.getExpression1().toString(); 
						if (ex1.indexOf("length") != -1) { 
							String ex2 = b.getExpression2().toString(); 
							len = Integer.valueOf(ex2).intValue(); 
							break; 
						}
					}
				}
				
				AttributeField attrField = new AttributeField(key, t, len);
				attrField.value = feature.getAttribute(i); 
				fields.put(key, attrField); 
			}
		}
	}
}
