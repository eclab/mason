/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.flockers;
import sim.engine.*;
import sim.field.continuous.*;
import sim.util.*;
import ec.util.*;

public class Flocker implements Steppable, sim.portrayal.Oriented2D
    {
    public Double2D loc = new Double2D(0,0);
    public Double2D lastd = new Double2D(0,0);
    public Continuous2D flockers;
    public Flockers theFlock;
    public boolean dead = false;
    
    public Flocker(Double2D location) { loc = location; }
    
    public Bag getNeighbors()
        {
        return flockers.getObjectsExactlyWithinDistance(loc, theFlock.neighborhood, true);
        }
    
    public double getOrientation() { return orientation2D(); }
    public boolean isDead() { return dead; }
    public void setDead(boolean val) { dead = val; }
    
    public double orientation2D()
        {
        if (lastd.x == 0 && lastd.y == 0) return 0;
        return Math.atan2(lastd.y, lastd.x);
        }
    
    public Double2D momentum()
        {
        return lastd;
        }

    public Double2D consistency(Bag b, Continuous2D flockers)
        {
        if (b==null || b.numObjs == 0) return new Double2D(0,0);
        
        double x = 0; 
        double y= 0;
        int i =0;
        int count = 0;
        for(i=0;i<b.numObjs;i++)
            {
            Flocker other = (Flocker)(b.objs[i]);
            if (!other.dead)
                {
                double dx = flockers.tdx(loc.x,other.loc.x);
                double dy = flockers.tdy(loc.y,other.loc.y);
                Double2D m = ((Flocker)b.objs[i]).momentum();
                count++;
                x += m.x;
                y += m.y;
                }
            }
        if (count > 0) { x /= count; y /= count; }
        return new Double2D(x,y);
        }
    
    public Double2D cohesion(Bag b, Continuous2D flockers)
        {
        if (b==null || b.numObjs == 0) return new Double2D(0,0);
        
        double x = 0; 
        double y= 0;        

        int count = 0;
        int i =0;
        for(i=0;i<b.numObjs;i++)
            {
            Flocker other = (Flocker)(b.objs[i]);
            if (!other.dead)
                {
                double dx = flockers.tdx(loc.x,other.loc.x);
                double dy = flockers.tdy(loc.y,other.loc.y);
                count++;
                x += dx;
                y += dy;
                }
            }
        if (count > 0) { x /= count; y /= count; }
        return new Double2D(-x/10,-y/10);
        }
 
    public Double2D avoidance(Bag b, Continuous2D flockers)
        {
        if (b==null || b.numObjs == 0) return new Double2D(0,0);
        double x = 0;
        double y = 0;
        
        int i=0;
        int count = 0;

        for(i=0;i<b.numObjs;i++)
            {
            Flocker other = (Flocker)(b.objs[i]);
            if (other != this )
                {
                double dx = flockers.tdx(loc.x,other.loc.x);
                double dy = flockers.tdy(loc.y,other.loc.y);
                double lensquared = dx*dx+dy*dy;
                count++;
                x += dx/(lensquared*lensquared + 1);
                y += dy/(lensquared*lensquared + 1);
                }
            }
        if (count > 0) { x /= count; y /= count; }
        return new Double2D(400*x,400*y);      
        }
        
    public Double2D randomness(MersenneTwisterFast r)
        {
        double x = r.nextDouble() * 2 - 1.0;
        double y = r.nextDouble() * 2 - 1.0;
        double l = Math.sqrt(x * x + y * y);
        return new Double2D(0.05*x/l,0.05*y/l);
        }
    
    public void step(SimState state)
        {        
        final Flockers flock = (Flockers)state;
        loc = flock.flockers.getObjectLocation(this);

        if (dead) return;
        
        Bag b = getNeighbors();
            
        Double2D avoid = avoidance(b,flock.flockers);
        Double2D cohe = cohesion(b,flock.flockers);
        Double2D rand = randomness(flock.random);
        Double2D cons = consistency(b,flock.flockers);
        Double2D mome = momentum();

        double dx = flock.cohesion * cohe.x + flock.avoidance * avoid.x + flock.consistency* cons.x + flock.randomness * rand.x + flock.momentum * mome.x;
        double dy = flock.cohesion * cohe.y + flock.avoidance * avoid.y + flock.consistency* cons.y + flock.randomness * rand.y + flock.momentum * mome.y;
                
        // renormalize to the given step size
        double dis = Math.sqrt(dx*dx+dy*dy);
        if (dis>0)
            {
            dx = dx / dis * flock.jump;
            dy = dy / dis * flock.jump;
            }
        
        lastd = new Double2D(dx,dy);
        loc = new Double2D(flock.flockers.stx(loc.x + dx), flock.flockers.sty(loc.y + dy));
        flock.flockers.setObjectLocation(this, loc);
        }
 
    }
