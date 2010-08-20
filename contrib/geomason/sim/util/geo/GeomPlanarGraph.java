/*
 * Network.java
 *
 * $Id: GeomPlanarGraph.java,v 1.2 2010-08-20 18:28:04 mcoletti Exp $
 */
package sim.util.geo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.planargraph.DirectedEdge;
import com.vividsolutions.jts.planargraph.Edge;
import com.vividsolutions.jts.planargraph.Node;
import com.vividsolutions.jts.planargraph.PlanarGraph;
import sim.field.geo.GeomVectorField;
import sim.field.network.Network;
import sim.util.Bag;

/** A factory class for creating a JTS PlanarGraph from a GeomVectorField
 *
 * This class is used to compute intersections between line segments.
 * <p>
 * That is, a given data set may be comprised of a set of lines that you want to move
 * an agent along.  Presumably some of those line segments will intersect;
 * however, those intersections may not be represented in the original data,
 * so they will have to be computed.  This class can be used to compute those
 * intersections.<p>
 *
 * @see sim.app.geo.networkworld and sim.app.geo.campusworld
 *
 */
public class GeomPlanarGraph extends PlanarGraph
{

    public GeomPlanarGraph()
    {
        super();
    }

    /** populate network with lines from a GeomVectorField
     *
     * @param field containing line segments that need intersections computed
     *
     * Assumes that GeomVectorField contains linear objects
     *
     * @todo May want to use a filter to pull out linear geometry regardless
     * of geometry type.
     */
    public void createFromGeomField(GeomVectorField field)
    {
        Bag geometries = field.getGeometries();

        for (int i = 0; i < geometries.numObjs; i++)
        {
            if (((MasonGeometry) geometries.get(i)).geometry instanceof LineString)
            {
                LineString lineString = (LineString) ((MasonGeometry) geometries.get(i)).geometry;

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

        Node nStart = getNode(startPt); // nodes added as necessary side-effect
        Node nEnd = getNode(endPt);

        Edge edge = new GeomPlanarGraphEdge(line);

        GeomPlanarGraphDirectedEdge de0 = new GeomPlanarGraphDirectedEdge(nStart, nEnd, linePts[1], true);
        GeomPlanarGraphDirectedEdge de1 = new GeomPlanarGraphDirectedEdge(nEnd, nStart, linePts[linePts.length - 2], false);

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

    /** Create a MASON Network from this planar graph
     *
     * XXX Unfortunately we need this since JTS planar graphs do not support
     * shortest distance and other common graph traversals.
     */
    public Network getNetwork()
    {
        Network network = new Network(false); // false == not directed

        for ( Object object : getEdges() )
        {
            DirectedEdge edge = (DirectedEdge) object;

            network.addEdge(edge.getFromNode(), edge.getToNode(), edge);
        }

        return network;
    }

}
