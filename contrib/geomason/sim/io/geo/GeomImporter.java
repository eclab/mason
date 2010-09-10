package sim.io.geo;
import java.io.FileNotFoundException;
import sim.field.geo.GeomVectorField;
import sim.util.Bag; 
import sim.util.geo.*; 
import java.util.ArrayList;

/** 
    A GeomImportor reads a file for geometric info and adds new objects to the GeomVectorField.  In addition, it sets up the
    attribute information for inspection.  The attributes are sorted alphabetically by name.  Currently, attributes must be 
    either integer, decimal, string or boolean data types.   
*/

public abstract class GeomImporter {
	
	final static int POINT = 1; 
	final static int LINE = 3; 
	final static int POLYGON = 5; 
	
    /** 
        Read geospatial data into the GeomVectorField.  

		@param fileName The filename of the shape file to be opened, <b>without</b> the extension, and 
		  			relative to the calling class.  
		@param referenceClass The class from which ingest is called.  The fileName should be relative to this class's 
				location in the filesystem.  
		@param field The GeomVectorField to store the GIS information in 
		@param masked  The Bag contains a subset of attribute names to display in the
        inspector.  The names must exactly match those in the data file.  If masked is null, then all attributes are 
        displayed, and if masked is an empty Bag, then no attributes are displayed.  
    */
	public void ingest(String fileName, Class<?> referenceClass, GeomVectorField field, Bag masked) throws FileNotFoundException {}
    
    /** This version does not display any attribute information in the inspector.  See 
      {@link #ingest(String, Class, GeomVectorField, Bag)} for more details. 
    */ 
	public void ingest(String fileName, Class<?> referenceClass, GeomVectorField field) throws FileNotFoundException 
	{
		ingest(fileName, referenceClass, field, new Bag()); 
	}

	
    /** Holds attribute information from the underlying GIS files */
    public ArrayList<AttributeField> attributeInfo = new ArrayList<AttributeField>(); 
    
    
    /** Type of MasonGeometry to use.  This allows the user to use custom MasonGeometry objects instead of the default */ 
    public Class<?> masonGeometryClass = MasonGeometry.class; 
}
