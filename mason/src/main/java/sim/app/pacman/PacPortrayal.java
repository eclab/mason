/*
  Copyright 2009  by Sean Luke and Vittorio Zipparo
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.pacman;
import java.awt.*;
import sim.field.grid.*;
import sim.portrayal.*;
import sim.portrayal.grid.*;
import java.awt.geom.*;
import sim.util.*;

/**
   PacPortrayal draws the PacMan as an Arc2D which changes its angle
   depending on the current step of the game.
*/

public class PacPortrayal extends SimplePortrayal2D
    {
    private static final long serialVersionUID = 1;

    protected Color color;
    PacMan pacman;
    public static final int MOUTH_RATE = 10;
    public static final double MAXIMUM_MOUTH_ANGLE = 60.0;
        
    public PacPortrayal(PacMan pacman, Color color) 
        {this.pacman = pacman; this.color = color;}
        
    Arc2D.Double arc = new Arc2D.Double();
    public void draw(Object object, Graphics2D g, DrawInfo2D info)
        {
        Pac pac = (Pac)object;
                
        int time = (int)(pacman.schedule.getTime());
        int step = time % (MOUTH_RATE * 2);
        if (step > MOUTH_RATE)
            step = MOUTH_RATE - (step - MOUTH_RATE);  // close mouth
        // now step cleverly goes from 0 *through* MOUTH_RATE
                
        double x = info.draw.x;
        double y = info.draw.y;
        double w = info.draw.width * 0.8;
                
        double a = 0;  // Pac.E
        switch (pac.lastAction)
            {
            case Pac.N: a = 90; break;
            case Pac.E: a = 0; break;
            case Pac.S: a = -90; break;
            case Pac.W: a = 180; break;
            case Pac.NOTHING: a = 0; break;
            default:
                throw new RuntimeException("default case should never occur");
            }
                        
        double starta = a - MAXIMUM_MOUTH_ANGLE * step / MOUTH_RATE;
        double enda = MAXIMUM_MOUTH_ANGLE * 2 * step / MOUTH_RATE - 360;
                
        arc.setArcByCenter(x, y, w, starta, enda, Arc2D.PIE);
        g.setColor(color);
        g.fill(arc);
        }
    }