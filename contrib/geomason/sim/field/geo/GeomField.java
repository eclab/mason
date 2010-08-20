package sim.field.geo;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Super class for GeomVectorField and GeomGridField.
 *
 */
public abstract class GeomField implements java.io.Serializable
{
    private static final long serialVersionUID = 5356444334673155514L;

	/** The minimum bounding rectangle (MBR) of all the stored geometries. */
    public Envelope MBR;
    
    /** Holds the origin for drawing; used to handle zooming and scrolling */
    public double drawX, drawY;

    public GeomField()
    {
        MBR = new Envelope();
        drawX = drawY = 0;
    }

    /** delete contents */
    public void clear()
    {
        MBR = new Envelope();
        drawX = drawY = 0;
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

    /** Set the MBR */
    public void setMBR(Envelope MBR)
    {
        this.MBR = MBR;
    }
}
