package sim.io.geo; 
import java.util.*; 

/**
 GeometryInfo holds metadata about a Geometry, which is used by the 
 GeometryInspector.  The <code>fields</code> TreeMap stores underlying metadata in <name, value> pairs, 
 sorted alphabetically by name.  Each subclass determines how to population the <code>fields</code> TreeMap.  
 */

public abstract class GeometryInfo { 

	/** All metadata <key, value> pairs.  Any subclass must determine which fields to display.  */
	public TreeMap fields; 
	
	/** Default constructor.  All subclasses must call this to instantiate the TreeMap.   */
	GeometryInfo() { fields = new TreeMap(); } 
}