/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.particles3d;
import sim.engine.*;
import sim.util.*;

/** A bouncing particle. */

public class Particle implements Steppable
    {
    public boolean randomize = false;
    public int xdir;  // -1, 0, or 1
    public int ydir;  // -1, 0, or 1
    public int zdir; 

    IntBag xPos = new IntBag(); 
    IntBag yPos = new IntBag(); 
    IntBag zPos = new IntBag(); 

    public Particle(int xdir, int ydir, int zdir)
        {
        this.xdir = xdir;
        this.ydir = ydir;
        this.zdir = zdir;
        }

    public void step(SimState state)
        {
        Particles3D tut = (Particles3D)state;
        
        Int3D location = tut.particles.getObjectLocation(this); 
        tut.trails.field[location.x][location.y][location.z] = 1.0f; 

        // Randomize my direction if requested
        if (randomize)
            {
            xdir = tut.random.nextInt(3) - 1;
            ydir = tut.random.nextInt(3) - 1;
            zdir = tut.random.nextInt(3) - 1;
            randomize = false;
            }
        
        // move
        int newx = location.x + xdir;
        int newy = location.y + ydir;
        int newz = location.z + zdir;
        
        // reverse course if hitting boundary
        if (newx < 0) { newx++; xdir = -xdir; }
        else if (newx >= tut.particles.getWidth()) {newx--; xdir = -xdir; }
        if (newy < 0) { newy++ ; ydir = -ydir; }
        else if (newy >= tut.particles.getHeight()) {newy--; ydir = -ydir; }
        if (newz < 0) { newz++ ; zdir = -zdir; }
        else if (newz >= tut.particles.getLength()) {newz--; zdir = -zdir; }
        
        // set my new location 
        Int3D newLoc = new Int3D(newx,newy,newz); 
        tut.particles.setObjectLocation(this, newLoc); 

        // randomize everyone at that location if need be
        Bag p = tut.particles.getObjectsAtLocation(newLoc); 
        if (p.numObjs > 1)
            {
            for(int x=0;x<p.numObjs;x++)
                ((Particle)(p.objs[x])).randomize = true;
            }
        }
    }
