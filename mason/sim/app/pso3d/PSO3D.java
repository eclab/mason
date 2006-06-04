/*
  Copyright 2006 by Ankur Desai, Sean Luke, and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.pso3d;

/**
   @author Ankur Desai and Joey Harrison
*/
import ec.util.MersenneTwisterFast;
import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.Continuous3D;
import sim.util.MutableDouble3D;

public class PSO3D extends SimState 
    {       
    public Continuous3D space; // the final frontier
        
    public int numParticlesPerNeighborhood = 10;
    public int numNeighborhoods = 10;
    public double width = 10.24;
    public double height = 10.24;
    public double length = 10.24;   

    // public modifier values
        
    public int getNumParticlesPerNeighborhood() { return numParticlesPerNeighborhood; }
    public void setNumParticlesPerNeighborhood(int val) { if (val > 0) numParticlesPerNeighborhood = val; }
        
    public int getNumNeighborhoods() { return numNeighborhoods; }
    public void setNumNeighborhoods(int val) { if (val > 0) numNeighborhoods = val; }
                
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
        return new String[] { "Rastrigin", "Griewangk", "Rosenbrock" };
        }
    
    public double successThreshold = 1.0e-8;
    public double getSuccessThreshold() { return successThreshold; }
    public void setSuccessThreshold(double val) { if (val >= 0.0) successThreshold = val; }
    
    
    private Evaluatable3D mapFitnessFunction(int val)
        {
        switch (val)
            {
            case 0: return new Rastrigin3D();
            case 1: return new Griewangk3D();
            case 2: return new Rosenbrock3D();
            }
        
        return new Rastrigin3D();
        }

    public double bestVal = 0;
    MutableDouble3D bestPosition = new MutableDouble3D();
        
    public PSO3D(long seed)
        {
        super(seed);
        }

    public PSO3D(MersenneTwisterFast random) 
        {
        super(random);
        }

    public PSO3D(MersenneTwisterFast random, Schedule schedule) 
        {
        super(random, schedule);
        }
        
    public void updateBest(double currVal, double currX, double currY, double currZ)
        {
        if (currVal > bestVal)
            {
            bestVal = currVal;
            bestPosition.setTo(currX, currY, currZ);
            }               
        }
        
    int previousSuccessCount = -1;
    public void start()
        {
        // reset the global best
        bestVal = 0;
        //TODO fix this
                
        super.start();
        space = new Continuous3D(1,length, width, height);
        //fakeSpace = new Continuous2D(height, width, height);
        Evaluatable3D f = mapFitnessFunction(fitnessFunction);
                
        for (int i = 0; i < numNeighborhoods; i++)
            {
            Neighborhood3D n = new Neighborhood3D(this);
                        
            for (int j = 0; j < numParticlesPerNeighborhood; j++)
                {
                double x = (random.nextDouble() * width) - (width * 0.5);
                double y = (random.nextDouble() * height) - (height * 0.5);
                double z = (random.nextDouble() * height) - (height * 0.5);
                double vx = (random.nextDouble() * initialVelocityRange) - (initialVelocityRange * 0.5);
                double vy = (random.nextDouble() * initialVelocityRange) - (initialVelocityRange * 0.5);
                double vz = (random.nextDouble() * initialVelocityRange) - (initialVelocityRange * 0.5);
                                
                final Particle3D p = new Particle3D(x, y, z, vx, vy, vz, n, f);
                                
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
            }
                
        schedule.scheduleRepeating(Schedule.EPOCH, 4, new Steppable()
            {
            public void step(SimState state)
                {
                int successCount = 0;
                for (int i = 0; i < space.allObjects.numObjs; i++)
                    {
                    Particle3D p = (Particle3D)space.allObjects.get(i);
                                                        
                    if (Math.abs(p.getFitness() - 1000) <= successThreshold)
                        successCount++;                                                 
                    }
                if (successCount != previousSuccessCount) 
                    {
                    //System.out.println("SuccessCount = " + successCount);
                    previousSuccessCount = successCount;
                    }
                if (successCount == (numNeighborhoods * numParticlesPerNeighborhood))
                    state.kill();
                }
            });
                
        // init a best watcher
        //space.setObjectLocation()
        }

    /**
     * @param args
     */

    public static void main(String[] args) 
        {
        doLoop(PSO3D.class, args);
        System.exit(0);
        }

    }
