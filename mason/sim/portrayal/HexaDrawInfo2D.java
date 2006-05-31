/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal;
import java.awt.geom.*;

/**
   An extension of DrawInfo2D that contains 6 (x,y) points that indicate
   the exact coordinates of the vertexes of the hexagon (after rounding to int values).
*/

public class HexaDrawInfo2D extends DrawInfo2D
    {
    // the pre-scaled exact pixel values to plot with
    public int[] xPoints;
    public int[] yPoints;

    public HexaDrawInfo2D(Rectangle2D.Double draw, Rectangle2D.Double clip, int[] xPoints, int[] yPoints)
        {
        super( draw, clip );
        this.xPoints = xPoints;
        this.yPoints = yPoints;
        }
        
    public String toString() { return "HexaDrawInfo2D[ Draw: " + draw + " Clip: " + clip + " Points: {"+
                                   " (" + xPoints[0] + "," + yPoints[0] + ") " +
                                   "(" + xPoints[1] + "," + yPoints[1] + ") " +
                                   "(" + xPoints[2] + "," + yPoints[2] + ") " +
                                   "(" + xPoints[3] + "," + yPoints[3] + ") " +
                                   "(" + xPoints[4] + "," + yPoints[4] + ") " +
                                   "(" + xPoints[5] + "," + yPoints[5] + ") " +
                                   "} ]"; }
    }
    
