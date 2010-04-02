/*
 * Network.java
 *
 * $Id: Network.java,v 1.1 2010-04-02 16:53:54 mcoletti Exp $
 */
package sim.util.geo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.planargraph.Edge;
import com.vividsolutions.jts.planargraph.Node;
import com.vividsolutions.jts.planargraph.PlanarGraph;
import sim.field.geo.GeomField;
import sim.util.Bag;

/** A factory class for create a JTS PlanarGraph from a GeomField
 *
 * @author mcoletti
 */
public class Network extends PlanarGraph
{

    public Network()
    {
    }

    /** populate network with lines from a GeomField
     *
     * @param field
     *
     * Assumes that GeomField contains linear objects
     *
     * @todo May want to use a filter to pull out linear geometry regardless
     * of geometry type.
     */
    public void createFromGeomField(GeomField field)
    {
        Bag geometries = field.getGeometry();

        for (int i = 0; i < geometries.numObjs; i++)
        {
            if (((GeomWrapper)geometries.get(i)).geometry instanceof LineString)
            {
                LineString lineString = (LineString) ((GeomWrapper)geometries.get(i)).geometry;

                addLineString(lineString);
            }
        }

    }

    /** Add the given line to the graph
     * 
     * @param line
     *
     * @note Some code copied from JTS PolygonizeGraph.addEdge() and hacked
     * to fit
     */
    private void addLineString(LineString line)
    {
        if (line.isEmpty())
        {
            return;
        }

        Coordinate[] linePts = CoordinateArrays.removeRepeatedPoints(line.getCoordinates());

        if (linePts.length < 2)
        {
            return;
        }

        Coordinate startPt = linePts[0];
        Coordinate endPt = linePts[linePts.length - 1];

        Node nStart = getNode(startPt); // nodes added as necessary as side-effect
        Node nEnd = getNode(endPt);

        Edge edge = new NetworkEdge(line);

        NetworkDirectedEdge de0 = new NetworkDirectedEdge(nStart, nEnd, linePts[1], true);
        NetworkDirectedEdge de1 = new NetworkDirectedEdge(nEnd, nStart, linePts[linePts.length - 2], false);

        edge.setDirectedEdges(de0, de1);
        
        add(edge);

    }


    /** get the node corresponding to the coordinate
     *
     * @param startPt
     * @return graph node associated with point
     *
     * Will create a new Node if one does not exist.
     *
     * @note Some code copied from JTS PolygonizeGraph.getNode() and hacked to fit
     */
    private Node getNode(Coordinate pt)
    {
        Node node = findNode(pt);
        if (node == null)
        {
            node = new Node(pt);
            // ensure node is only added once to graph
            add(node);
        }
        return node;
    }

}
