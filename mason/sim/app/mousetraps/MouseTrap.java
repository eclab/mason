/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.mousetraps;
import sim.engine.*;

public class MouseTrap implements Steppable
    {
    public int posx, posy;
    public MouseTrap( int x, int y) 
        {
        this.posx = x;
        this.posy = y;
        }
    public void step( final SimState state )
        {
        ((MouseTraps)state).triggerTrap(posx, posy);
        }
    }
