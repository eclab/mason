/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.tutorial4;
import sim.engine.*;
import sim.field.grid.*;
import sim.util.*;

public class Tutorial4 extends SimState
    {
    public DoubleGrid2D trails;
    public SparseGrid2D particles;

    public int collisions;
    public double collisionRate;
    public double getCollisionRate() { return collisionRate; }
    
    public int gridWidth = 100;
    public int gridHeight = 100;
    public int numParticles = 500;
    
    public int getWidth() { return gridWidth; }
    public void setWidth(int val) { if (val > 0 ) gridWidth = val; }
    public int getHeight() { return gridHeight; }
    public void setHeight(int val) { if (val > 0 ) gridHeight = val; }
    public int getNumParticles() { return numParticles; }
    public void setNumParticles(int val) { if (val >= 0) numParticles = val; }

    public Tutorial4(long seed)
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
        
        // Schedule the "Big Particle"
        BigParticle b = new BigParticle(random.nextInt(3) - 1, random.nextInt(3) - 1);
        particles.setObjectLocation(b,
            new Int2D(random.nextInt(gridWidth),random.nextInt(gridHeight)));
        schedule.scheduleRepeating(Schedule.EPOCH,1,b,5);
        
        // Schedule the decreaser
        Steppable decreaser = new Steppable()
            {
            public void step(SimState state)
                {
                // decrease the trails
                trails.multiply(0.9);
                
                // compute and reset the collision info
                collisionRate = collisions / (double)numParticles;
                collisions = 0;
                }
                
            // anonymous class -- here's the serialVersionUID (see tutorial3)
            // by the way, notice it's DIFFERENT from tutorial3's due to new names of stuff.
            // No biggie -- as long as you have <i>some</i> serialVersionUID...
            static final long serialVersionUID = 6976157378487763326L;
            };
            
        schedule.scheduleRepeating(Schedule.EPOCH,2,decreaser,1);
        }

    public static void main(String[] args)
        {
        doLoop(Tutorial4.class, args);
        System.exit(0);
        }    
    
    // contains an anonymous class -- here's the serialVersionUI (see tutorial3)
    // by the way, notice it's DIFFERENT from tutorial3's due to new names of stuff.
    // No biggie -- as long as you have <i>some</i> serialVersionUID...
    static final long serialVersionUID = 6930440709111220430L;
    }
