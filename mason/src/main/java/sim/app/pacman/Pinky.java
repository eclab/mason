/*
  Copyright 2009  by Sean Luke and Vittorio Zipparo
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.pacman;
import sim.engine.*;
import sim.field.continuous.*;
import sim.util.*;
import ec.util.*;

/** Pinky is the pink ghost.  He starts in the box and initially waiting.
    His target (see Ghost.java) is four steps ahead of where the Pac is facing.  
    In the real Pac Man there's a bug in Pinky's actions which causes him to
    have a slightly different target when the Pac is facing up.  We do not
    reproduce this bug.  We compute "four ahead" toroidally.    
*/
        
public class Pinky extends Ghost
    {
    private static final long serialVersionUID = 1;

    public static final int DIST = 4;
        
    public Double2D getStartLocation() { return new Double2D(12.5, 16); }

    public Pinky(PacMan pacman) 
        {
        super(pacman);
        }
        
    public Double2D getTarget()
        {
        Pac pac = pacman.pacClosestTo(location);
        MutableDouble2D loc = pac.location;
        Continuous2D agents = pacman.agents;
        switch (pac.lastAction)
            {
            case N: return new Double2D(loc.x, agents.sty(loc.y - DIST));
            case E: return new Double2D(agents.stx(loc.x + DIST), loc.y);
            case S: return new Double2D(loc.x, agents.sty(loc.y + DIST));
            case W: return new Double2D(agents.stx(loc.x - DIST), loc.y);
            }
        return new Double2D(loc.x, loc.y);
        }
    }
