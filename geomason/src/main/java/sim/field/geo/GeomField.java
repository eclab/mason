/*
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 * 
 * $Id$
*/
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

    public GeomField() { this(0,0); } 
    
    public GeomField(int w, int h)
    {
        MBR = new Envelope();
        drawX = drawY = 0;
        fieldHeight = h; 
        fieldWidth = w; 
    }
    
    /** The field dimensions
    *
    * Used for computing scale.
    *
    */
   public int fieldWidth, fieldHeight; 
   
   public int getFieldWidth() { return fieldWidth; } 
   public int getFieldHeight() { return fieldHeight; }

   public void setFieldWidth(int fw ) { fieldWidth = fw; }
   public void setFieldHeight(int fh) { fieldHeight = fh; }
   
    

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
