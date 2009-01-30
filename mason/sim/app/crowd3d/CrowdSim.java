/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.crowd3d;
import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;


public class CrowdSim extends SimState
    {
    public double spaceHeight = 20;
    public double spaceWidth = 20;
    public double spaceDepth = 20;
    public int boidCount =0;
    public int STEPS_BETWEEN_INSERTS = 200;

    public Continuous3D boidSpace = new Continuous3D(Agent.SIGHT, spaceWidth, spaceHeight, spaceDepth);
                
    public static void main(String[] args)
        {
        doLoop(CrowdSim.class, args);
        System.exit(0);
        }    

    public CrowdSim(long seed)
        {
        super(seed);
        Agent.MAX_FN_VAL = Math.min(spaceHeight, Math.min(spaceWidth, spaceDepth))/2;
        }
    
    private void spawnBoid()
        {
        Agent boid = new Agent();
        boidSpace.setObjectLocation(boid,
            new Double3D(       random.nextDouble()*spaceWidth,
                random.nextDouble()*spaceHeight,
                random.nextDouble()*spaceDepth));
        boid.setStopper(schedule.scheduleRepeating(boid));
        boidCount++;
        }

    protected void killBoid()
        {
        Agent victim = (Agent)boidSpace.allObjects.objs[(int)(random.nextDouble()*boidCount)];
        victim.stop();
        boidSpace.remove(victim);
                
        }
        
    public void start()
        {
        super.start();  // clear out the schedule
        
        boidSpace = new Continuous3D(Agent.SIGHT, spaceWidth, spaceHeight, spaceDepth);
//*
        Steppable spawner = new Steppable(){public void step(SimState state){spawnBoid();}};
//        Steppable killer = new Steppable(){public void step(SimState state){killBoid();}};
        schedule.scheduleRepeating(Schedule.EPOCH,1,spawner,STEPS_BETWEEN_INSERTS);
/*/
  for(int i=0;i<10;++i)
  spawnBoid();
//*/
        }
    }
    
    
    
    
    
