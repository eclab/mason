/*
 * NetworkDirectedEdge.java
 *
 * $Id: NetworkDirectedEdge.java,v 1.1 2010-04-02 16:53:54 mcoletti Exp $
 */

package sim.util.geo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.planargraph.DirectedEdge;
import com.vividsolutions.jts.planargraph.Node;

/**
 *
 * @author mcoletti
 */
public class NetworkDirectedEdge extends DirectedEdge {

    public
    NetworkDirectedEdge(Node from, Node to, Coordinate directionPt,
                        boolean edgeDirection)
    {
        super(from, to, directionPt, edgeDirection);
    }
    

}
