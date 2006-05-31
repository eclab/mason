/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;
import java.awt.geom.Dimension2D;

/** 
    One real oddity of Java 1.3.1 and 1.4.1 is the lack of
    a concrete subclass of Dimension2D which uses doubles or floats;
    the only one provided (java.awt.Dimension) uses ints!  This is particularly
    weird given that Java has Rectangle2D.Double, Rectangle2D.Float, and Rectangle (int),
    plus Point2D.Double, Point2D.Float, and Point (int), etc.  Inexplicable.
    
    <p>This class is a simple concrete subclass of Dimension2D with public width
    and height members.  You're welcome.
*/

public class DoubleDimension2D extends Dimension2D
    {
    public double width;
    public double height;
    public DoubleDimension2D(double width, double height)
        {
        this.width = width; this.height = height;
        }
    public double getHeight() { return height; }
    public double getWidth() { return width; }
    public void setSize(double width, double height) 
        {
        this.width = width; this.height = height; 
        }
    }
