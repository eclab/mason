/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.woims;

import java.awt.*;
import sim.portrayal.simple.*;

public class Obstacle extends OvalPortrayal2D
    {
    public final static Paint obstacleColor = new Color(192,255,192);
    // gradient obstacles!  Try it!  Slower but fun!
    // public final static Paint obstacleColor = new GradientPaint(0,0,Color.red,10,10,Color.green,true);
    
    public Obstacle(double diam)
        {
        super(obstacleColor,diam);
        }
    }
