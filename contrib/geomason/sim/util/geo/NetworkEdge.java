/*
 * NetworkEdge.java
 *
 * $Id: NetworkEdge.java,v 1.2 2010-04-10 18:27:38 kemsulli Exp $
 */

package sim.util.geo;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.planargraph.Edge;

/** A planar graph edge that wraps a LineString
 *
 * @author mcoletti
 */
public class NetworkEdge extends Edge {

    private LineString line; // line that corresponds to this edge

    public NetworkEdge(LineString line)
    {
        this.line = line;
    }
  
    public LineString getLine() { return line; }
}
