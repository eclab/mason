/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal.network;
import sim.portrayal.*;
import java.awt.geom.*;
import java.awt.*;
import sim.display.*;

/**
   An extension of DrawInfo2D for dealing with edges in visualizing network fields.
*/

public class EdgeDrawInfo2D extends DrawInfo2D
    {
    /** A pre-scaled point to draw to. */
    public Point2D.Double secondPoint;
    
    public EdgeDrawInfo2D(GUIState state, FieldPortrayal2D fieldPortrayal, RectangularShape draw, RectangularShape clip, Point2D.Double secondPoint)
        {
        super(state, fieldPortrayal, draw,clip);
        this.secondPoint = secondPoint;
        }
                
    public EdgeDrawInfo2D(DrawInfo2D other, double translateX, double translateY, Point2D.Double secondPoint)
        {
        super(other, translateX, translateY);
        this.secondPoint = secondPoint;
        }

    public EdgeDrawInfo2D(DrawInfo2D other, Point2D.Double secondPoint)
        {
        super(other);
        this.secondPoint = secondPoint;
        }        

    public EdgeDrawInfo2D(EdgeDrawInfo2D other)
        {
        this(other, new Point2D.Double(other.secondPoint.x, other.secondPoint.y));
        }        

    public String toString() 
        {
        return "EdgeDrawInfo2D[ Draw: " + draw + " Clip: " + clip +  " Precise: " + precise + " Location : " + location + " 2nd: " + secondPoint + "]";
        }
    }
