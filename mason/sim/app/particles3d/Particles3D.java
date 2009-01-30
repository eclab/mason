/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.particles3d;

import sim.engine.*;
import sim.field.grid.*;
import sim.util.*;

public class Particles3D extends SimState
    {
    static public int gridWidth = 30;
    static public int gridHeight = 30;
    static public int gridLength = 30; 

    public SparseGrid3D particles;
    public DoubleGrid3D trails = new DoubleGrid3D(gridWidth, gridHeight, gridLength); 
        
    public int numParticles = 20;
    
    public Particles3D(long seed)
        {
        super(seed);
        }

    public void start()
        {
        super.start();
        particles = new SparseGrid3D(gridWidth, gridHeight, gridLength);
        trails = new DoubleGrid3D(gridWidth, gridHeight, gridLength);
        
        Particle p;
        
        for(int i=0 ; i<numParticles ; i++)
            {
            p = new Particle(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1); 
            schedule.scheduleRepeating(p);
            particles.setObjectLocation(p, new Int3D(random.nextInt(gridWidth), random.nextInt(gridHeight), 
                    random.nextInt(gridLength))); 
            }
        
        // Schedule the decreaser
        Steppable decreaser = new Steppable()
            {
            public void step(SimState state)
                {
                trails.multiply(0.9f); 
                }
            static final long serialVersionUID = 6330208160095250478L;
            };
            
        schedule.scheduleRepeating(Schedule.EPOCH,2,decreaser,1);
        }

    public static void main(String[] args)
        {
        doLoop(Particles3D.class, args);
        System.exit(0);
        }    

    static final long serialVersionUID = 9115981605874680023L;    
    }
