/**
 *  PointMoveTo.java
 *
 * $Id: PointMoveTo.java,v 1.3 2010-08-20 18:04:36 kemsulli Exp $
 */

package sim.util.geo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFilter;



/** Moves a Point to a new coordinate
 *
 * @author mcoletti
 */
public class PointMoveTo implements CoordinateSequenceFilter, java.io.Serializable
{

    private static final long serialVersionUID = -2029180922944093196L;
	private Coordinate newValue_ = null;
    private boolean isDone_ = false;
    private boolean geometryChanged_ = false;
    
    public PointMoveTo(Coordinate c)
    {
        super();
        newValue_ = c;
    }

    public PointMoveTo()
    {
        
    }

    public
        void setCoordinate(Coordinate newValue_)
    {
        this.newValue_ = newValue_;
    }

    public
        void filter(CoordinateSequence coords, int pos)
    {
        coords.setOrdinate(pos, 0, newValue_.x);
        coords.setOrdinate(pos, 1, newValue_.y);
        isDone_ = true;
        geometryChanged_ = true;
    }

    public
        boolean isDone()
    {
        return isDone_;
    }

    public
        boolean isGeometryChanged()
    {
        return geometryChanged_;
    }
}
