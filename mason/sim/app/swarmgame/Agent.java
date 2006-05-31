/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.swarmgame;
import sim.engine.*;
import sim.util.*;

public class Agent implements Steppable, sim.portrayal.Oriented2D
    {
    public Agent a;
    public Agent b;
    public Double2D lastLoc = new Double2D(0,0);
    public Double2D loc = new Double2D(0,0);
    
    public double orientation2D()
        {
        return Math.atan2(loc.y - lastLoc.y, loc.x - lastLoc.x);
        }

    public void pick(SwarmGame swarm)
        {
        final Bag agents = swarm.agents.allObjects;
        do
            {
            a = (Agent)(agents.objs[swarm.random.nextInt(agents.numObjs)]);
            }
        while (a == this);
        do
            {
            b = (Agent)(agents.objs[swarm.random.nextInt(agents.numObjs)]);
            }
        while (b == a || b == this);
        }
    
    public void step(SimState state)
        {
        final SwarmGame swarm = (SwarmGame)state;
        Double2D aLoc = swarm.agents.getObjectLocation(a);
        Double2D bLoc = swarm.agents.getObjectLocation(b);
        loc = swarm.agents.getObjectLocation(this);
        
        double dis=0,dx=0,dy=0,dx0=0,dy0=0,dx1=0,dy1=0,dx2=0,dy2=0,dx3=0,dy3=0,dx4=0,dy4=0;
        
        // OP_STALKER
        dx = (aLoc.x - loc.x);
        dy = (aLoc.y - loc.y);

        // renormalize
        dis = Math.sqrt(dx*dx+dy*dy);
        if (dis>0)
            {
            dx0 = dx / dis;
            dy0 = dy / dis;
            }

        // OP_AVOIDER

        dx = (loc.x - bLoc.x);
        dy = (loc.y - bLoc.y);

        // renormalize
        dis = Math.sqrt(dx*dx+dy*dy);
        if (dis>0)
            {
            dx1 = dx / dis;
            dy1 = dy / dis;
            }
                    
        // OP_DEFENDER
        // go to the mid-point between a and b
        dx = ((aLoc.x + bLoc.x)/2 - loc.x);
        dy = ((aLoc.y + bLoc.y)/2 - loc.y);

        // renormalize
        dis = Math.sqrt(dx*dx+dy*dy);
        if (dis>0)
            {
            dx2 = dx / dis;
            dy2 = dy / dis;
            }

        // OP_AGGRESSOR
        // go to the opposite of a from b
        dx = (aLoc.x + (aLoc.x - bLoc.x) - loc.x);
        dy = (aLoc.y + (aLoc.y - bLoc.y) - loc.y);

        // renormalize
        dis = Math.sqrt(dx*dx+dy*dy);
        if (dis>0)
            {
            dx3 = dx / dis;
            dy3 = dy / dis;
            }
        // RANDOM
        dx = state.random.nextDouble()-0.5;
        dy = state.random.nextDouble()-0.5;

        // renormalize
        dis = Math.sqrt(dx*dx+dy*dy);
        if (dis>0)
            {
            dx4 = dx / dis;
            dy4 = dy / dis;
            }
        
        // add 'em up
        dx = swarm.stalker_v * dx0 + 
            swarm.avoider_v * dx1 + 
            swarm.defender_v * dx2 + 
            swarm.aggressor_v * dx3 +
            swarm.random_v * dx4;
             
        dy = swarm.stalker_v * dy0 + 
            swarm.avoider_v * dy1 + 
            swarm.defender_v * dy2 + 
            swarm.aggressor_v * dy3 +
            swarm.random_v * dy4;
             
        // renormalize to the given step size
        dis = Math.sqrt(dx*dx+dy*dy);
        dx = dx / dis * swarm.jump;
        dy = dy / dis * swarm.jump;
        
        lastLoc = loc;
        loc = new Double2D(loc.x + dx, loc.y + dy);
        swarm.agents.setObjectLocation(this, loc);
        }
 
    }
