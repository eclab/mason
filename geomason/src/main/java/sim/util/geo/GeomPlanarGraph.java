/* 
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 *
 * $Id$
 * 
 */
package sim.util.geo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.planargraph.DirectedEdge;
import com.vividsolutions.jts.planargraph.Edge;
import com.vividsolutions.jts.planargraph.Node;
import com.vividsolutions.jts.planargraph.PlanarGraph;
import java.util.Iterator;
import sim.field.geo.GeomVectorField;
import sim.field.network.Network;
import sim.util.Bag;

/** A JTS PlanarGraph
 *
 * Planar graph useful for exploiting network topology.
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
     * @param field containing line segments
     *
     * Assumes that 'field' contains co-planar linear objects
     *
     */
    public void createFromGeomField(GeomVectorField field)
    {
        Bag geometries = field.getGeometries();

        for (int i = 0; i < geometries.numObjs; i++)
        {
            if (((MasonGeometry) geometries.get(i)).geometry instanceof LineString)
            {
                addLineString((MasonGeometry)geometries.get(i));
            }
        }

        // Abandoned work on rectifying non-planar data. MAC 8/24/10.
//        field.clear();
//
//        Collection<LineString> lines = lineMerger.getMergedLineStrings();
//
//        PrecisionModel pm = new PrecisionModel(PrecisionModel.FLOATING);
//
//        GeometryNoder noder = new GeometryNoder(pm);
//
//        Collection<LineString> more_lines = noder.node(lines);
//
//        // Now add nodes and edges to graph
//
//        for (LineString line : more_lines)
//        {
//            addLineString(line);
//            field.addGeometry(new MasonGeometry(line));
        //}

    }

    /** Add the given line to the graph
     * 
     * @param wrappedLine is MasonGeometry wrapping a JTS line
     *
     * @note Some code copied from JTS PolygonizeGraph.addEdge() and hacked
     * to fit
     */
    private void addLineString(MasonGeometry wrappedLine)
    {
        LineString line = (LineString) wrappedLine.geometry;

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

        GeomPlanarGraphEdge edge = new GeomPlanarGraphEdge(line);

        GeomPlanarGraphDirectedEdge de0 = new GeomPlanarGraphDirectedEdge(nStart, nEnd, linePts[1], true);
        GeomPlanarGraphDirectedEdge de1 = new GeomPlanarGraphDirectedEdge(nEnd, nStart, linePts[linePts.length - 2], false);

        edge.setDirectedEdges(de0, de1);

        edge.setAttributes(wrappedLine.getAttributes());

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

        for (Iterator it = dirEdges.iterator(); it.hasNext();)
        {
            Object object = it.next();
            GeomPlanarGraphDirectedEdge edge = (GeomPlanarGraphDirectedEdge) object;
            network.addEdge(edge.getFromNode(), edge.getToNode(), edge);
        }

        return network;
    }

}
