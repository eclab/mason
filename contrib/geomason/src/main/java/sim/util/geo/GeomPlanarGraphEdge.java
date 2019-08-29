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

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.planargraph.Edge;
import java.util.Map;

/** A planar graph edge that wraps a LineString
 *
 * XXX Should consider making this an internal class to GeomPlanarGraph?
 */
public class GeomPlanarGraphEdge extends Edge
{
    /** Optional set of attributes associated with this edge
     *  <p>
     *  Typically a link to the corresponding MasonGeometry from which
     * this line was derived.
     *
     * @see MasonGeometry
     */
    private Map<String, AttributeValue> attributes;

    private LineString line; // line that corresponds to this edge

    public GeomPlanarGraphEdge(LineString line)
    {
        this.line = line;
    }
  
    public LineString getLine() { return line; }


    /**
     * @return true iff there are any attributes associated with this edge
     */
    public boolean hasAttributes()
    {
        return attributes != null && ! attributes.isEmpty();
    }

    public void setAttributes(final Map<String,AttributeValue> attributes )
    {
        // Yes, copying this would be safer, but we want to be in sync with
        // any attribute changes that occur in the originating MasonGeometry
        // from wence the line, and its corresponding attributes, came.
        this.attributes = attributes;
    }


    /**
     *
     * @param name of attribute for which we're looking
     * @return true if this edge has an attribute of that name
     */
    public boolean hasAttribute(final String name)
    {
        return this.attributes.containsKey(name);
    }


    public Object getAttribute(final String name)
    {
        return this.attributes.get(name);
    }

    public Integer getIntegerAttribute(final String name)
    {
        return this.attributes.get(name).getInteger();
    }

    public Double getDoubleAttribute(final String name)
    {
        return this.attributes.get(name).getDouble();
    }

    public String getStringAttribute(final String name)
    {
        return this.attributes.get(name).getString();
    }


}
