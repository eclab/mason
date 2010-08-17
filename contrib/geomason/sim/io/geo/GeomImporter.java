package sim.io.geo;
import java.io.FileNotFoundException;
import sim.field.geo.GeomField;
import sim.util.Bag; 
import sim.util.geo.*; 

import java.util.ArrayList;
import java.util.Comparator;

/** 
    A GeomImportor reads a file for geometric info and adds new objects to the GeomField.  In addition, it sets up the 
    attribute information for inspection.  The attributes are sorted alphabetically by name.  Currently, attributes must be 
    either integer, decimal, string or boolean data types.   
*/

public abstract class GeomImporter {
    /** 
        Read geospatial data into the GeomField.  The Bag contains a subset of attribute names to display in the 
        inspector.  The names must exactly match those in the data file.  If masked is null, then all attributes are 
        displayed, and if masked is an empty Bag, then no attributes are displayed.  
    */
    public void ingest(String input, GeomField field, Bag masked) throws FileNotFoundException {} 
    
    /** This version does not display any attribute information in the inspector. */
    public void ingest(String input, GeomField field) throws FileNotFoundException { ingest(input, field, new Bag()); } 
    
    /** Holds attribute information from the underlying GIS files */
    public ArrayList<AttributeField> attributeInfo = new ArrayList<AttributeField>(); 
    
    
    /** Type of MasonGeometry to use.  This allows the user to use custom MasonGeometry objects instead of the default */ 
    public Class<?> masonGeometryClass = MasonGeometry.class; 
    
    /** Comparator to sort attribute <name, value> pairs by name */
    public Comparator<AttributeField> attrFieldCompartor = new Comparator<AttributeField>() { 
    		public int compare(AttributeField af1, AttributeField af2)
    		{
    			return af1.name.compareTo(af2.name) ;
    		}
    };
}
