/*
  Copyright 2006 by Ankur Desai, Sean Luke, and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.pso;

import ec.util.MersenneTwisterFast;
import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.Continuous2D;
import sim.util.MutableDouble2D;

/**
   @author Ankur Desai and Joey Harrison
*/
public class PSO extends SimState 
    {       
    public Continuous2D space;
        
    public double width = 10.24;
    public double height = 10.24;
    public Particle[] particles;
    int prevSuccessCount = -1;      
        
    // public modifier values
    public int numParticles = 1000;
    public int getNumParticles() { return numParticles; }
    public void setNumParticles(int val) { if (val >= 0) numParticles = val; }

    public int neighborhoodSize = 10;
    public int getNeighborhoodSize() { return neighborhoodSize; }
    public void setNeighborhoodSize(int val) { if ((val >= 0) && (val <= numParticles)) neighborhoodSize = val; }

    public double initialVelocityRange = 1.0;
    public double getInitialVelocityRange() { return initialVelocityRange; }
    public void setInitialVelocityRange(double val) { if (val >= 0.0) initialVelocityRange = val; }
    
    public double velocityScalar = 2.7;
    public double getVelocityScalar() { return velocityScalar; }
    public void setVelocityScalar(double val) { if (val >= 0.0) velocityScalar = val; }

    public int fitnessFunction = 0;
    public int getFitnessFunction() { return fitnessFunction; }
    public void setFitnessFunction(int val) { fitnessFunction = val; }
    public Object domFitnessFunction() 
        { 
        return new String[] { "Booth", "Rastrigin", "Griewangk", "Rosenbrock" };
        }
            
    private Evaluatable mapFitnessFunction(int val)
        {
        switch (val)
            {
            case 0: return new Booth();
            case 1: return new Rastrigin();
            case 2: return new Griewangk();
            case 3: return new Rosenbrock();
            }
        
        return new Booth();
        }

    public double[] fitnessFunctionLowerBound = 
        {
        920,
        950,
        998,
        200
        };
    
    public double successThreshold = 1.0e-8;
    public double getSuccessThreshold() { return successThreshold; }
    public void setSuccessThreshold(double val) { if (val >= 0.0) successThreshold = val; }
    
    public double bestVal = 0;
    MutableDouble2D bestPosition = new MutableDouble2D();
        
    public PSO(long seed)
        {
        super(seed);
        }

    public PSO(MersenneTwisterFast random) 
        {
        super(random);
        }

    public PSO(MersenneTwisterFast random, Schedule schedule) 
        {
        super(random, schedule);
        }
        
    public void updateBest(double currVal, double currX, double currY)
        {
        if (currVal > bestVal)
            {
            bestVal = currVal;
            bestPosition.setTo(currX, currY);
            }               
        }
        
    public double getNeighborhoodBest(int index, MutableDouble2D pos)
        {
        double bv = Double.NEGATIVE_INFINITY;
        Particle p;     

        int start = (index - neighborhoodSize / 2);
        if (start < 0)
            start += numParticles;
                
        for (int i = 0; i < neighborhoodSize; i++)
            {
            p = particles[(start + i) % numParticles];
            if (p.bestVal > bv)
                {
                bv = p.bestVal;
                pos.setTo(p.bestPosition);
                }
            }
        return 1.0;             
        }
        
    public void start()
        {
        // reset the global best
        bestVal = 0;
                
        super.start();
        particles = new Particle[numParticles];
        space = new Continuous2D(height, width, height);
        Evaluatable f = mapFitnessFunction(fitnessFunction);            
                        
        for (int i = 0; i < numParticles; i++)
            {
            double x = (random.nextDouble() * width) - (width * 0.5);
            double y = (random.nextDouble() * height) - (height * 0.5);
            double vx = (random.nextDouble() * initialVelocityRange) - (initialVelocityRange * 0.5);
            double vy = (random.nextDouble() * initialVelocityRange) - (initialVelocityRange * 0.5);
                        
            final Particle p = new Particle(x, y, vx, vy, this, f, i);
            particles[i] = p;
                        
            schedule.scheduleRepeating(Schedule.EPOCH,1,new Steppable()
                {
                public void step(SimState state) { p.stepUpdateFitness(); }
                });
                        
            schedule.scheduleRepeating(Schedule.EPOCH,2,new Steppable()
                {
                public void step(SimState state) { p.stepUpdateVelocity(); }
                });
                        
            schedule.scheduleRepeating(Schedule.EPOCH,3,new Steppable()
                {
                public void step(SimState state) { p.stepUpdatePosition(); }
                });
            }                       
                
        schedule.scheduleRepeating(Schedule.EPOCH, 4, new Steppable()
            {
            public void step(SimState state)
                {
                int successCount = 0;
                for (int i = 0; i < space.allObjects.numObjs; i++)
                    {
                    Particle p = (Particle)space.allObjects.get(i);
                                                        
                    if (Math.abs(p.getFitness() - 1000) <= successThreshold)
                        successCount++;                                                 
                    }
                if (successCount != prevSuccessCount)
                    {
                    prevSuccessCount = successCount;
                    //System.out.println("SuccessCount = " + successCount);                                         
                    if (successCount == numParticles)
                        state.kill();
                    }
                }
            });             
        }

    public static void main(String[] args) 
        {
        doLoop(PSO.class, args);
        System.exit(0);
        }

    }
