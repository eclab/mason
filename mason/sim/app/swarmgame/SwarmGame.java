/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.swarmgame;
import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;

public class SwarmGame extends SimState
    {
    public Continuous2D agents;
    public double width = 100;
    public double height = 100;
    public int numAgents = 100;
    public double jump = 0.5;  // how far do we move in a timestep?
    
    public double stalker_v = 0.5;
    public double avoider_v = 0.5;
    public double defender_v = 0.0;
    public double aggressor_v = 0.0;
    public double random_v = 0.0;
    
    // some properties to appear in the inspector
    public double getGoTowardsA() { return stalker_v; }
    public void setGoTowardsA(double val) { stalker_v = val; }
    public double getGoAwayFromB() { return avoider_v; }
    public void setGoAwayFromB(double val) { avoider_v = val; }
    public double getGoBetweenAAndB() { return defender_v; }
    public void setGoBetweenAAndB(double val) { defender_v = val; }
    public double getGetBehindBFromA() { return aggressor_v; }
    public void setGetBehindBFromA(double val) { aggressor_v = val; }
    public double getMoveRandomly() { return random_v; }
    public void setMoveRandomly(double val) { random_v = val; }

    /** Creates a SwarmGame simulation with the given random number seed. */
    public SwarmGame(long seed)
        {
        super(seed);
        }
    
    public void start()
        {
        super.start();
        
        // set up the agents field
        agents = new Continuous2D(width,width,height);
        
        // make a bunch of agents and schedule 'em
        for(int x=0;x<numAgents;x++)
            {
            Agent agent = new Agent();
            agents.setObjectLocation(agent, 
                new Double2D(random.nextDouble()*width, random.nextDouble() * height));
            schedule.scheduleRepeating(agent);
            }
                
        // have them pick their A and B targets
        for(int x=0;x<agents.allObjects.numObjs;x++)
            {
            ((Agent)(agents.allObjects.objs[x])).pick(this);
            }
        }

    public static void main(String[] args)
        {
        doLoop(SwarmGame.class, args);
        System.exit(0);
        }    

    }
