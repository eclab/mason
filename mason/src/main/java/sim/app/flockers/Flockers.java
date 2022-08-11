/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.flockers;
import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;

public class Flockers extends SimState
    {
    private static final long serialVersionUID = 1;

    public Continuous2D flockers;
    public double width = 150;
    public double height = 150;
    public int numFlockers = 200;
    public double cohesion = 1.0;
    public double avoidance = 1.0;
    public double randomness = 1.0;
    public double consistency = 1.0;
    public double momentum = 1.0;
    public double deadFlockerProbability = 0.1;
    public double neighborhood = 10;
    public double jump = 0.7;  // how far do we move in a timestep?
    
    public double getCohesion() { return cohesion; }
    public void setCohesion(double val) { if (val >= 0.0) cohesion = val; }
    public double getAvoidance() { return avoidance; }
    public void setAvoidance(double val) { if (val >= 0.0) avoidance = val; }
    public double getRandomness() { return randomness; }
    public void setRandomness(double val) { if (val >= 0.0) randomness = val; }
    public double getConsistency() { return consistency; }
    public void setConsistency(double val) { if (val >= 0.0) consistency = val; }
    public double getMomentum() { return momentum; }
    public void setMomentum(double val) { if (val >= 0.0) momentum = val; }
    public int getNumFlockers() { return numFlockers; }
    public void setNumFlockers(int val) { if (val >= 1) numFlockers = val; }
    public double getWidth() { return width; }
    public void setWidth(double val) { if (val > 0) width = val; }
    public double getHeight() { return height; }
    public void setHeight(double val) { if (val > 0) height = val; }
    public double getNeighborhood() { return neighborhood; }
    public void setNeighborhood(double val) { if (val > 0) neighborhood = val; }
    public double getDeadFlockerProbability() { return deadFlockerProbability; }
    public void setDeadFlockerProbability(double val) { if (val >= 0.0 && val <= 1.0) deadFlockerProbability = val; }
        
    public Double2D[] getLocations()
        {
        if (flockers == null) return new Double2D[0];
        Bag b = flockers.getAllObjects();
        if (b==null) return new Double2D[0];
        Double2D[] locs = new Double2D[b.numObjs];
        for(int i =0; i < b.numObjs; i++)
            locs[i] = flockers.getObjectLocation(b.objs[i]);
        return locs;
        }

    // this is included to demonstrate MASON's parameter sweep facility, see the manual
    public double getMeanXLocation()
        {
        Double2D[] locations = getLocations();
        double avg = 0;
        for(int i = 0; i < locations.length; i++)
            {
            avg += locations[i].x;
            }
        if (locations.length > 0) 
            avg /= locations.length;
        return avg;
        }
    
    public Double2D[] getInvertedLocations()
        {
        if (flockers == null) return new Double2D[0];
        Bag b = flockers.getAllObjects();
        if (b==null) return new Double2D[0];
        Double2D[] locs = new Double2D[b.numObjs];
        for(int i =0; i < b.numObjs; i++)
            {
            locs[i] = flockers.getObjectLocation(b.objs[i]);
            locs[i] = new Double2D(locs[i].y, locs[i].x);
            }
        return locs;
        }

    /** Creates a Flockers simulation with the given random number seed. */
    public Flockers(long seed)
        {
        super(seed);
        }
    
    public void start()
        {
        super.start();
        
        // set up the flockers field.  It looks like a discretization
        // of about neighborhood / 1.5 is close to optimal for us.  Hmph,
        // that's 16 hash lookups! I would have guessed that 
        // neighborhood * 2 (which is about 4 lookups on average)
        // would be optimal.  Go figure.
        flockers = new Continuous2D(neighborhood/1.5,width,height);
        
        // make a bunch of flockers and schedule 'em.  A few will be dead
        for(int x=0;x<numFlockers;x++)
            {
            Double2D location = new Double2D(random.nextDouble()*width, random.nextDouble() * height);
            Flocker flocker = new Flocker(location);
            if (random.nextBoolean(deadFlockerProbability)) flocker.dead = true;
            flockers.setObjectLocation(flocker, location);
            flocker.flockers = flockers;
            flocker.theFlock = this;
            schedule.scheduleRepeating(flocker);
            }
        }

    public static void main(String[] args)
        {
        doLoop(Flockers.class, args);
        System.exit(0);
        }    



    public double[] assess(int numObjectives)
        {
        double[] assessment = new double[numObjectives];
        
        double avgNeighbors = 0;
        
        Bag flock = flockers.getAllObjects();
        for(int i = 0; i < flock.numObjs; i++)
            {
            Flocker f = (Flocker)(flock.objs[i]);
            avgNeighbors += f.getNeighbors().numObjs;
            }
                
        avgNeighbors /= (double)flock.numObjs;
        assessment[0] = avgNeighbors;
        System.err.println(assessment[0]);
        return assessment;
        }
    }
