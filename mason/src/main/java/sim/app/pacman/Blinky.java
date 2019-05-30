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

/** Blinky is the red ghost.  He starts outside of the box (and is not initially waiting).
    His target (see Ghost.java) is the Pac himself.  */
        
public class Blinky extends Ghost
    {
    private static final long serialVersionUID = 1;

    public Double2D getStartLocation() { return new Double2D(13.5, 13); }

    public Blinky(PacMan pacman) 
        {
        super(pacman);
        waiting = 0;  // not waiting.
        }
        
    public Double2D getTarget()
        {
        return new Double2D(pacman.pacClosestTo(location).location);  
        }
    }
