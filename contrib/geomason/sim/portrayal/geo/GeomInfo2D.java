/*
 *
 * Copyright 2011 by Mark Coletti, Keith Sullivan, Sean Luke, and
 * George Mason University Mason University Licensed under the Academic
 * Free License version 3.0
 *
 * See the file "LICENSE" for more information
 *
 * $Id$
 *
 */
package sim.portrayal.geo;

import java.awt.geom.AffineTransform;
import sim.portrayal.DrawInfo2D;



public class GeomInfo2D extends DrawInfo2D
{
    public AffineTransform transform;

    public GeomInfo2D(DrawInfo2D info, AffineTransform t)
    {
        super(info);
        transform = t;
    }
}