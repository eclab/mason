/* 
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 *
 * $Id: GeomImporter.java,v 1.10 2010-09-22 01:20:08 mcoletti Exp $
 */

package sim.io.geo;

import java.io.FileNotFoundException;
import sim.field.geo.GeomVectorField;
import sim.util.Bag; 
import sim.util.geo.*; 
import java.util.ArrayList;
import sim.field.geo.GeomGridField;

/** 
    A GeomImportor reads a file for geometric info and adds new objects to the GeomVectorField.  In addition, it sets up the
    attribute information for inspection.  The attributes are sorted alphabetically by name.  Currently, attributes must be 
    either integer, decimal, string or boolean data types.   
*/
public abstract class GeomImporter
{

	
    /** Read geospatial data into the GeomVectorField.  

		@param fileName The filename of the shape file to be opened and relative to the calling class.
		@param referenceClass The class from which this is called.  The fileName should be relative to this class's
				location in the filesystem.  
		@param field The GeomVectorField to store the GIS information in 
		@param masked  The Bag contains a subset of attribute names to display in the
        inspector.  The names must exactly match those in the data file.  If masked is null, then all attributes are 
     * displayed, and if masked is an empty Bag, then no attributes are displayed.
     * @throws FileNotFoundException
    */
    public void ingest(String fileName, Class<?> referenceClass, GeomVectorField field, Bag masked) throws FileNotFoundException
    {
        String filePath = null;

        try
        {
            filePath = referenceClass.getResource(fileName).getPath();
        } catch (NullPointerException np1)
        {
            // getResource() was unable to find the file.  This is probably
            // because 'fileName' doesn't have a '.shp' extension.  Try again
            // after adding the '.shp' suffix.

            try
            {
                filePath = referenceClass.getResource(fileName + ".shp").getPath();
            } catch (NullPointerException np2)
            {
                throw new FileNotFoundException(fileName);
            }
        }
        
        ingest(filePath, field, masked);
    }


    /** Read geospatial data into the given field.
     *
     * @param fileName is name of the data file
     * @param field into which the data is read
     * @param masked contains subset of attribute names to read and must match those in the datafile.  If null then all
     * attributes are read.  If the bag is empty then no attributes are read.
     * @throws FileNotFoundException
     */
	public void ingest(String fileName, GeomVectorField field, Bag masked) throws FileNotFoundException {}
    
    
    /** This version does not display any attribute information in the inspector.
     *
     * @throws FileNotFoundException
     *
     * @see GeomImporter#ingest(java.lang.String, java.lang.Class, sim.field.geo.GeomVectorField, sim.util.Bag)
     */
	public void ingest(String fileName, Class<?> referenceClass, GeomVectorField field) throws FileNotFoundException 
	{
		ingest(fileName, referenceClass, field, new Bag()); 
	}


    /**
     * Used to determine the GeomGridField storage type.
     *
     * @see ingest()
     */
    public enum GridDataType {INTEGER, DOUBLE}

	/** Read geospatial grid data from inputFile into field
     *
     * This only reads the first band of data.
     *
     * @param type denotes the base type as either integer or double-based
     * @param field is field to populate
     * @throws FileNotFoundException if 'inputFile' not found
     * @thrown RuntimeException if unable to read data
     */
    public void ingest(String fileName, GridDataType type, GeomGridField field) throws FileNotFoundException {}

    
    /** Type of MasonGeometry to use.  This allows the user to use custom MasonGeometry objects instead of the default */
    public Class<?> masonGeometryClass = MasonGeometry.class; 
}
