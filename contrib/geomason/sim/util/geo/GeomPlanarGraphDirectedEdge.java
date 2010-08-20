/*
 * NetworkDirectedEdge.java
 *
 * $Id: GeomPlanarGraphDirectedEdge.java,v 1.2 2010-08-20 18:28:04 mcoletti Exp $
 */

package sim.util.geo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.planargraph.DirectedEdge;
import com.vividsolutions.jts.planargraph.Node;

/**
 *
 */
public class GeomPlanarGraphDirectedEdge extends DirectedEdge
{

    public GeomPlanarGraphDirectedEdge(Node from, Node to,
            Coordinate directionPt,
            boolean edgeDirection)
    {
        super(from, to, directionPt, edgeDirection);
    }


}
