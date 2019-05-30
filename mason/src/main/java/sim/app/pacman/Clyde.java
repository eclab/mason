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

/** Clyde is the orange ghost.  He starts in the box and initially waiting.
    His target (see Ghost.java) is the Pac himself, just like Blinky.  However
    If Clyde is within 8 tiles of the Pac, he changes his target to be the
    bottom left corner of the screen (Clyde's "scatter target").
*/

public class Clyde extends Blinky
    {
    private static final long serialVersionUID = 1;

    public static final int DIST = 8;
        
    public Double2D getStartLocation() { return new Double2D(14.5, 16); }

    public Double2D scatterTarget;  // only Clyde uses (and sets) this for now

    public Clyde(PacMan pacman) 
        {
        super(pacman);
        waiting = INITIAL_WAITING_PERIOD;
        this.scatterTarget = new Double2D(0, 32);  // bottom left
        }
        
    public Double2D getTarget()
        {
        if (pacman.agents.tds(new Double2D(location), new Double2D(pacman.pacClosestTo(location).location)) > DIST * DIST)
            return super.getTarget();
        else return scatterTarget;
        }
    }
