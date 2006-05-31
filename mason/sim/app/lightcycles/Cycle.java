/*
  Copyright 2006 by Daniel Kuebrich
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.lightcycles;
import sim.util.*;
import sim.engine.*;

// The cycle itself...  A steppable which either proceeds while being manipulated by 
// user input or directed by a very minimal AI.

public class Cycle implements Steppable
    {
    // Properties
    public int dir, my_id;
    boolean alive;
    boolean cpu;
    
    // Stop stepping the cycle when it dies
    Stoppable stopper;
    
    // accessors for inspectors - direction
    public int getdir() { return dir; }
    public void setdir(int ndir) { dir = ndir; }

    // accessors for inspectors - id    
    public int getmy_id() { return my_id; }
    public void setmy_id(int nid) { my_id = nid; }
    
    // accessors for inspectors - alive?    
    public boolean getalive() { return alive; }
    public void setalive(boolean nalive) { alive = nalive; }
    
    // accessors for inspectors - player control - cpu
    public boolean getcpu() { return cpu; }
    public void setcpu(boolean ncpu) { cpu = ncpu; }
    
    public Cycle(int id, int ndir)
        {
        my_id = id;
        dir = ndir;
        
        alive = true;
        cpu = true;
        }

    public void step( final SimState state )
        {
        // if this cycle has crashed,
        // stop rescheduling this cycle's step function
        if(!alive)
            {
            if(stopper!=null) stopper.stop();
            return;
            }
        
        LightCycles lc = (LightCycles)state;
        
        int dirs[] = new int[5];
        int i=0, n=0;
        
        Int2D location = lc.cycleGrid.getObjectLocation(this); // find x and y
        int x = location.x;
        int y = location.y;
        
        // if cpu controlled, use ai for direction change action
        if(cpu)
            {
            // cycle through directions and find direction with longest open path in any one direction -- (simplistic, no?)
            for(i = 1; i<5; i++)
                {
                // make sure direction is even vaguely viable (eg not on edge of grid)
                if( (i == 1 && y == 0) ||
                    (i == 2 && y == lc.gridHeight-1) ||
                    (i == 3 && x == 0) ||
                    (i == 4 && x == lc.gridWidth-1) )
                    continue;
                    
                n=1;
                switch(i)
                    {
                    case 1:
                        while(lc.grid.field[x][y-n] == 0 && y-(n+1) > 0)
                            n++;
                        break;
                    case 2:
                        while(lc.grid.field[x][y+n] == 0 && y+(n+1) < lc.gridHeight)
                            n++;
                        break;
                    case 3:
                        while(lc.grid.field[x-n][y] == 0 && x-(n+1) > 0)
                            n++;
                        break;
                    case 4:
                        while(lc.grid.field[x+n][y] == 0 && x+(n+1) < lc.gridWidth)
                            n++;
                        break;
                    }
                dirs[i] = n;
                }
            
            int bestDir = 0;
            int maxN = 0;
            
            // pick best direction (longest path)
            for(i = 1; i<5; i++)
                {
                if(dirs[i] > maxN || (dirs[i]==maxN && state.random.nextBoolean()))
                    {
                    maxN = dirs[i];
                    bestDir = i;
                    }
                }
            
            if(maxN == 1)
                alive = false;
            
            dir = bestDir;
            }
        
        // if player or cpu, no matter, still moves
        if(dir == 1) // up
            y -= 1;
        if(dir == 2) // down
            y += 1;
        if(dir == 3) // left
            x -= 1;
        if(dir == 4) // right
            x += 1;
        
        // see if going off edge
        if(x < 0 || x >= lc.gridWidth || y < 0 || y >= lc.gridHeight)
            alive = false;
        else
            {
            // see if crashing
            if(lc.grid.field[x][y] != 0)
                alive = false;
            else        // else you're good
                {
                lc.cycleGrid.setObjectLocation(this,x,y);
                lc.grid.field[x][y] = my_id;
                }
            }
        } // end Step
    } // end class Cycle
