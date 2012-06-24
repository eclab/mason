/* 
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 *
 * $Id$
*/
package sim.util.geo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;

/** 
 *  A helper class to move a point to a new Coordinate.  JTS geometries require a CoordinateSequenceFilter to 
 *  apply a transform.  
 *
 */
public class PointMoveTo implements CoordinateSequenceFilter, java.io.Serializable
{
    private static final long serialVersionUID = -2029180922944093196L;
    
	Coordinate newValue = null;
    boolean isDone = false;
    boolean geometryChanged = false;
    
    public PointMoveTo() { super(); }
    
    public PointMoveTo(Coordinate c)
    {
        super();
        newValue = c;
    }

    public void setCoordinate(Coordinate newValue)
    {
        this.newValue = newValue;
    }

    public void filter(CoordinateSequence coords, int pos)
    {
        coords.setOrdinate(pos, 0, newValue.x);
        coords.setOrdinate(pos, 1, newValue.y);
        isDone = true;
        geometryChanged = true;
    }

    public boolean isDone() { return isDone; }

    public boolean isGeometryChanged() {  return geometryChanged; } 
}
