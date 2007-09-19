/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/
package sim.portrayal3d.network;

import java.awt.*;
import java.util.Enumeration;

import javax.vecmath.*;
import javax.media.j3d.*;

/**
 * This is an extension of <code>GenericEdgePortrayal3D</code> for Shape3D
 * edge models in particular, as I can pass the locationWrapper to the
 * geometries myself, instead of relying on the user (like in the general case).
 * 
 * @author Gabriel Balan
 */

public class Shape3DEdgePortrayal3D extends GenericEdgePortrayal3D
    {
    public Shape3DEdgePortrayal3D(Shape3D model)
        {
        super(model);
        }

    public Shape3DEdgePortrayal3D(Shape3D model, Color labelColor)
        {
        super(model, labelColor);
        }

    public Shape3DEdgePortrayal3D(Shape3D model, Color labelColor, Font labelFont)
        {
        super(model, labelColor, labelFont);
        }

    protected void passWrapperToGeometries(Object drawInfo)
        {
        Shape3D shape = (Shape3D) edgeModel;
        setPickableFlags(shape);
        shape.setUserData(drawInfo);
        Enumeration en = shape.getAllGeometries();
        while (en.hasMoreElements())
            {
            Geometry g = (Geometry) en.nextElement();
            setPickableFlags(g);
            g.setUserData(drawInfo);

            }
        }
    }
