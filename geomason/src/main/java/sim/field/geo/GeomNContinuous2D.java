package sim.field.geo;

import java.io.Serializable;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;

import mpi.MPIException;
import sim.field.continuous.NContinuous2D;
import sim.field.geo.GeomField;
import sim.field.grid.Grid2D;
import sim.util.Double2D;
import sim.util.NdPoint;

public class GeomNContinuous2D <T extends Serializable>
{
	/** The minimum bounding rectangle (MBR) of all the stored geometries. */
    public Envelope MBR;
    
    /** Holds the origin for drawing; used to handle zooming and scrolling */
    public double drawX, drawY;
    
    public NContinuous2D<T> field = null;
    
    /** width of grid point in projection coordinate system
    *
    * @see GeomGridField.setGrid()
    * @see GeomGridField.setMBR()
    */
   private double pixelWidth = 0.0;

   /** height of grid point in projection coordinate system
    *
    * @see GeomGridField.setGrid()
    * @see GeomGridField.setMBR()
    */
   private double pixelHeight = 0.0;

    public GeomNContinuous2D()
    {
        MBR = new Envelope();
        drawX = drawY = 0;
    }
    
    public GeomNContinuous2D(NContinuous2D<T> continuous2d)
    {
    	this();
        setField(continuous2d);
    }
    
	/** The field dimensions
    *
    * Used for computing scale.
    *
    */
   
   public double getFieldWidth() { 
	   return (double)field.getWidth();
   } 
   
   public double getFieldHeight() {
	   return (double)field.getHeight();
   }

    

    /** delete contents */
    public void clear()
    {
        MBR = new Envelope();
        drawX = drawY = 0;
        field = null;
    }

    /** Returns the width of the MBR. */
    public double getWidth()
    {
        return MBR.getWidth();
    }

    /** Returns the height of the MBR. */
    public double getHeight()
    {
        return MBR.getHeight();
    }

    /** Returns the minimum bounding rectangle (MBR) */
    public final Envelope getMBR()
    {
        return MBR;
    }

    
    
    /** Height of pixels in units of the underlying coordinate reference system */
    public double getPixelHeight()
    {
        return pixelHeight;
    }

    /** Set heigh of pixels in units of the underlying coordinate reference system */
    public void setPixelHeight(double pixelHeight)
    {
        this.pixelHeight = pixelHeight;
    }

    /** Width of pixels in units of the underlying coordinate reference system */
    public double getPixelWidth()
    {
        return pixelWidth;
    }

    /** Set pixel width in units of underlying coordinate reference system */
    public void setPixelWidth(double pixelWidth)
    {
        this.pixelWidth = pixelWidth;
    }
   

    public final NContinuous2D<T> getField()
    {
        return field;
    }

    
    public final void setField(NContinuous2D<T> newField)
    {
        field = newField;

        setPixelWidth(field.getWidth());
        setPixelHeight(field.getHeight());
    }
    
    
    /** Set the MBR */
    public void setMBR(Envelope MBR)
    {
    	this.MBR = MBR;

        // update pixelWidth and pixelHeight iff grid is set
        if (field != null)
        {
            setPixelWidth(MBR.getWidth() / getFieldWidth());
            setPixelHeight(MBR.getHeight() / getFieldHeight());
        }
    }
    
    /**
     * @param p point
     * @return x grid coordinate for cell 'p' is in
     */
    public double toXCoord(final Point p)
    {
        return (p.getX() - getMBR().getMinX()) / getPixelWidth();
    }

    /**
     *
     * @param x Coordinate in base projection
     * @return x grid coordinate for cell 'x'
     */
    public double toXCoord(final double x)
    {
        return (x - getMBR().getMinX()) / getPixelWidth();
    }


    /**
     * @param p point
     * @return y grid coordinate for cell 'p' is in
     */
    public double toYCoord(final Point p)
    {
        // Note that we have to flip the y coordinate because the origin in
        // MASON is in the upper left corner.
        return (getMBR().getMaxY() - p.getY()) / getPixelHeight();
    }

    /**
     *
     * @param y coordinate in base projection
     * @return y grid coordinate for cell 'y' is in
     */
    public double toYCoord(final double y)
    {
        // Note that we have to flip the y coordinate because the origin in
        // MASON is in the upper left corner.
        return (getMBR().getMaxY() - y) / getPixelHeight();
    }

    
    public void setLocation(T obj, NdPoint loc)
    {
    	field.setLocation(obj, loc);
    }
    
    public void removeObject(T obj)
    {
    	field.removeObject(obj);
    }
    
    public void sync() throws MPIException 
    {
    	field.sync();
    }
}
