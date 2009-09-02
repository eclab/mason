/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.simple;
import sim.portrayal.*;
import java.awt.*;
import java.awt.geom.*;

/**
   A simple portrayal for 2D visualization of hexagons. It extends the SimplePortrayal2D and
   it manages the drawing and hit-testing for hexagonal shapes. If the DrawInfo2D parameter
   received by draw and hitObject functions is an instance of HexaDrawInfo2D, better information
   is extracted and used to make everthing look better. Otherwise, hexagons may be created from
   information stored in simple DrawInfo2D objects, but overlapping or extra empty spaces may be
   observed (especially when increasing the scale).
*/

public class HexagonalPortrayal2D extends ShapePortrayal2D
    {
    final static double stretch = 1 / (2.0 * Math.sin(Math.PI/3));
    public HexagonalPortrayal2D() { this(Color.gray,1.0,true); }
    public HexagonalPortrayal2D(Paint paint)  { this(paint,1.0,true); }
    public HexagonalPortrayal2D(double scale) { this(Color.gray,scale,true); }
    public HexagonalPortrayal2D(boolean filled) { this(Color.gray,1.0,filled); }
    public HexagonalPortrayal2D(Paint paint, double scale)  { this(paint,scale,true); }
    public HexagonalPortrayal2D(Paint paint, boolean filled)  { this(paint,1.0,filled); }
    public HexagonalPortrayal2D(double scale, boolean filled)  { this(Color.gray,scale,filled); }
    public HexagonalPortrayal2D(Paint paint, double scale, boolean filled)
        {
        /*        4|5
         *         |
         *-------3-+-0-----
         *         |
         *        2|1
         */
        super(new double[]{1*stretch,0.5*stretch,-0.5*stretch,-1*stretch,-0.5*stretch,0.5*stretch},
            new double[]{0,-0.5,-0.5,0,0.5,0.5}, paint, scale, filled);
        }
    }
