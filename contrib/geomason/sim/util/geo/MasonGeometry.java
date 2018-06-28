/**
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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;



/** 
 * A MasonGeometry is a wrapper for a JTS geometry and an associated userData 
 * field.  The userData field can be any MASON object, or general Java object,
 * which will be included in the inspector by default.
 *
 * <p> MasonGeometry implements sim.util.Proxiable to allow the hiding of 
 * various getXXX and setXXX methods from the inspectors.
 *
 * TODO may have to move new AttributeValue convenience functions to inner
 * proxy class.
 */
public class MasonGeometry implements sim.util.Proxiable, java.io.Serializable
{

    private static final long serialVersionUID = 6290810736517692387L;

    /** Internal JTS geometry object */
    public Geometry geometry;

    /** Optional attribute-value pairs associated with this geometry
     */
    private Map<String,AttributeValue> attributes;


    /**
     * XXX This might be deprecated because no longer support hidden attributes
     *
     * @return true iff this.attributes contains any hidden attributes
     */
    public boolean hasHiddenAttributes()
    {
        for (AttributeValue value : this.attributes.values())
        {
            if ( value.isHidden() )
            {
                return true;
            }
        }
        return false;
    }


    /**
     * @return true iff there are any attributes associated with this geometry
     */
    public boolean hasAttributes()
    {
        return ! attributes.isEmpty();
    }


    /**
     *
     * @param name of attribute for which we're looking
     * @return true if this geometry has an attribute of that name
     */
    public boolean hasAttribute(final String name)
    {
        return this.attributes.containsKey(name);
    }

    
    /**
     * @return attributes associated with this geometry
     */
    public Map<String,AttributeValue> getAttributes()
    {
//        return Collections.unmodifiableMap(attributes);
        return attributes;
    }


    /** Java2D shape corresponding to this Geometry. Used to 
     * speed up drawing.
     */
    public GeneralPath shape;

    /**
     *
     */
    public AffineTransform transform = new AffineTransform();



    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 19 * hash + (this.geometry != null ? this.geometry.hashCode() : 0);
        return hash;
    }



    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }

        final MasonGeometry other = (MasonGeometry) obj;

        if (this.geometry != other.geometry && (this.geometry == null || !this.geometry.equals(other.geometry)))
        {
            return false;
        }

        if (this.attributes != other.attributes && (this.attributes == null || !this.attributes.equals(other.attributes)))
        {
            return false;
        }
        
        return true;
    }


    /** A cached, optimized version of my Geometry.  Used for fast intersection, union, etc. operations,
     * This instance is not serializable, thus we declare it transient */
    public transient PreparedGeometry preparedGeometry;
    
    private void writeObject(ObjectOutputStream out) throws IOException {
    	// just do not write preparedGeometry to stream
    	out.defaultWriteObject();
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    	in.defaultReadObject();
    	// reconstruct preparedGeometry from geometry
    	if (geometry != null)
        {
            preparedGeometry = PreparedGeometryFactory.prepare(geometry);
        }
    }

    /** Does this MasonGeometry move? i.e., dynamically change location */
    public boolean isMovable = false;


    /** Default constructors */
    public MasonGeometry()
    {
        this(null, null);
    }



    public MasonGeometry(Geometry g)
    {
        this(g, null);
    }



    public MasonGeometry(Geometry g, Object o)
    {
        geometry = g;
        shape = null;
        transform = new AffineTransform();
        preparedGeometry = null;
        
        attributes = new HashMap<String,AttributeValue>();

        if (geometry != null)
        {
            preparedGeometry = PreparedGeometryFactory.prepare(geometry);
        }

        if ( o != null )
        {
            setUserData(o);
        }
    }


    // TODO add test for this in harness
    public void addAttributes(final Map<String,AttributeValue> attributes )
    {
        this.attributes.putAll(attributes);
    }

    public void addAttribute(final String name, final Object value)
    {
        this.attributes.put(name, new AttributeValue(value));
    }

    public Object getAttribute(final String name)
    {
        return this.attributes.get(name);
    }

    public void addIntegerAttribute(final String name, int value)
    {
        this.attributes.put(name, new AttributeValue(value));
    }

    public Integer getIntegerAttribute(final String name)
    {
        return this.attributes.get(name).getInteger();
    }

    public void addDoubleAttribute(final String name, double value)
    {
        this.attributes.put(name, new AttributeValue(value));
    }

    public Double getDoubleAttribute(final String name)
    {
        return this.attributes.get(name).getDouble();
    }

    public void addStringAttribute(final String name, final String value)
    {
        this.attributes.put(name, new AttributeValue(value));
    }

    public String getStringAttribute(final String name)
    {
        return this.attributes.get(name).getString();
    }


    /** Set the userData field with an arbitrary object
     *
     * @param o is user supplied object to attach to this geometry
     */
    final public void setUserData(Object o)
    {
        geometry.setUserData(o);
    }



    public Object getUserData()
    {
        return geometry.getUserData();
    }



    /** 
     * @return geometry type and coordinates
     */
    @Override
    public String toString()
    {
        return geometry.toString();
    }



    /** @return the JTS geometry object.
     */
    public Geometry getGeometry()
    {
        return geometry;
    }



    /** Inner class allows us to prevent certain getXXX and setXXX methods from
     * appearing in the Inspector
     */
    public class GeomWrapperProxy
    {

        /**
         * @return the area of the internal JTS geometry object.  The units are
         * the same as same as the internal JTS geometry object
         */
        public double getArea()
        {
            return geometry.getArea();
        }



        /** @return the length of the perimeter of the internal JTS geometry 
         * object. The units are the same as same as the internal JTS geometry
         * object
         */
        public double getPerimeter()
        {
            return geometry.getLength();
        }



        /**
         * @return the number of vertices which make up the geometry
         */
        public int getNumVertices()
        {
            return geometry.getNumPoints();
        }
    }



    public Object propertiesProxy()
    {
        return new GeomWrapperProxy();
    }

}
