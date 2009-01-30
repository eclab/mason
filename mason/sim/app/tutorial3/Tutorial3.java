/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.tutorial3;
import sim.engine.*;
import sim.field.grid.*;
import sim.util.*;


public class Tutorial3 extends SimState
    {
    public DoubleGrid2D trails;
    public SparseGrid2D particles;
    
    public int gridWidth = 100;
    public int gridHeight = 100;
    public int numParticles = 500;
    
    public Tutorial3(long seed)
        {
        super(seed);
        }

    public void start()
        {
        super.start();
        trails = new DoubleGrid2D(gridWidth, gridHeight);
        particles = new SparseGrid2D(gridWidth, gridHeight);
        
        Particle p;
        
        for(int i=0 ; i<numParticles ; i++)
            {
            p = new Particle(random.nextInt(3) - 1, random.nextInt(3) - 1);  // random direction
            schedule.scheduleRepeating(p);
            particles.setObjectLocation(p,
                new Int2D(random.nextInt(gridWidth),random.nextInt(gridHeight)));  // random location
            }
        
        // Schedule the decreaser
        Steppable decreaser = new Steppable()
            {
            public void step(SimState state)
                {
                // decrease the trails
                trails.multiply(0.9);
                }
            static final long serialVersionUID = 6330208160095250478L;
            };
            
        schedule.scheduleRepeating(Schedule.EPOCH,2,decreaser,1);
        }

    public static void main(String[] args)
        {
        doLoop(Tutorial3.class, args);
        System.exit(0);
        }    

    static final long serialVersionUID = 9115981605874680023L;    
    }
