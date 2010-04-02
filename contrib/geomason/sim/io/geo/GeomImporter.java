package sim.io.geo;
import com.vividsolutions.jts.geom.Geometry;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.field.geo.GeomField;
import sim.util.Bag; 
import sim.util.geo.GeomWrapper;

/** 
 A GeomImportor reads a file for geometric info and adds new objects to the GeomField.  
  */

public abstract class GeomImporter {

    /** Used to create GeomWrapper
     *
     * The user may have created a GeomWrapper subclass.  This allows the user
     * to specify that that class be used instead of the default GeomWrapper
     * when reading geospatial data.
     *
     * By default this is set to null indicating that GeomWrapper instances
     * should be used instead.
     */
    private java.lang.Class geomWrapperClass = null;

    public
    Class getGeomWrapperClass()
    {
        return geomWrapperClass;
    }

    public
    void setGeomWrapperClass(Class geomWrapperClass)
    {
        this.geomWrapperClass = geomWrapperClass;
    }

    GeomImporter()
    {
        geomWrapperClass = null;
    }

    GeomImporter(java.lang.Class wrapper)
    {
        // TODO: add check that wrapper is-a subclass of GeomWrapper
        geomWrapperClass = wrapper;
    }

    /** factory function for creating GeomWrappers
     *
     * Since it's possible for a user to specify a GeomWrapper subclass be used
     * instead of the default GeomWrapper for imported geometry, this function
     * ensures that the desired GeomWrapper is created and returned during
     * ingest()
     *
     * @param g
     * @param gi
     * @return newly created GeomWrapper or subclass of same
     */
    protected GeomWrapper makeGeomWrapper(Geometry g, GeometryInfo gi)
    {
        if (this.geomWrapperClass == null)
        {
            return new GeomWrapper(g,gi);
        }

        GeomWrapper geomWrapper = null;

        try
        {
            geomWrapper = (GeomWrapper) geomWrapperClass.newInstance();
            geomWrapper.geometry = g;
            geomWrapper.geoInfo = gi;
        }
        catch (InstantiationException ex)
        {
            Logger.getLogger(GeomImporter.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (IllegalAccessException ex)
        {
            Logger.getLogger(GeomImporter.class.getName()).log(Level.SEVERE, null, ex);
        }

        return geomWrapper;
    }

    /** 
	 Read geospatial data into the GeomField.  The Bag contains only the 
	 fields to display in the Inspector, set masked to null to display 
	 all fields contained in the GIS metadata. 
     */
   public abstract void ingest(String input, GeomField field, Bag masked) throws FileNotFoundException;
}
