/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.network;
import sim.portrayal.*;
import java.awt.geom.*;

/**
   An extension of DrawInfo2D for dealing with edges in visualizing network fields.
*/

public class EdgeDrawInfo2D extends DrawInfo2D
    {
    /** A pre-scaled point to draw to. */
    public Point2D.Double secondPoint;
    
    public EdgeDrawInfo2D(Rectangle2D.Double draw, Rectangle2D.Double clip, Point2D.Double secondPoint)
        {
        super(draw,clip);
        this.secondPoint = secondPoint;
        }        

    public String toString() 
        {
        return "EdgeDrawInfo2D[ Draw: " + draw + " Clip: " + clip + " 2nd: " + secondPoint + "]";
        }
    }
