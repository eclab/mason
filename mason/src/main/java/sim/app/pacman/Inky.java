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

/** Inky is the cyan ghost.  He starts in the box and initially waiting.
    His target (see Ghost.java) is complex: it's on the opposite side 
    of Pinky's target than the location of Blinky.  So if Pinky's target is
    the vector p, and Blinky is at b, then Inky's target is p + (p - b).
*/
        
public class Inky extends Pinky
    {
    private static final long serialVersionUID = 1;

    Blinky blinky;
        
    public Double2D getStartLocation() { return new Double2D(13.5, 16); }

    public Inky(PacMan pacman, Blinky blinky) 
        {
        super(pacman);
        this.blinky = blinky;
        }
        
    public Double2D getTarget()
        {
        Double2D target = super.getTarget();
        MutableDouble2D blinkyLoc = blinky.location;
        Continuous2D agents = pacman.agents;
        return new Double2D(agents.stx(2 * blinkyLoc.x - target.x), agents.sty(2 * blinkyLoc.y - target.y));
        }
    }
