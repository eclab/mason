/*
  Copyright 2006 by Daniel Kuebrich
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

// Class Segment
package sim.app.lsystem;
import java.awt.*;
import sim.portrayal.*;
import sim.util.*;

public /*strictfp*/ class Segment extends SimplePortrayal2D
    {
    public double x,y,x2,y2;
    
    public Segment(double x, double y, double dist, double theta)
        {
        this.x = x;
        this.y = y;
        this.x2 = /*Strict*/Math.cos(theta)*dist;
        this.y2 = /*Strict*/Math.sin(theta)*dist;
        }
    
    public void draw(Object object,  final Graphics2D g, final DrawInfo2D info )
        {
        g.setColor(new Color(0,127,0));
        g.drawLine((int)info.draw.x,
            (int)info.draw.y,
            (int)(info.draw.x) + (int)(info.draw.width*x2),
            (int)(info.draw.y) + (int)(info.draw.height*y2));
        }

    public void hitObjects(DrawInfo2D range, Bag putInHere)
        {
        // don't want to have any inspectors
        }
    }
