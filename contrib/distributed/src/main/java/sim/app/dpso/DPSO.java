/*
  Copyright 2022 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/
        
package sim.app.dpso;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import sim.engine.DSimState;
import sim.engine.DSteppable;
import sim.engine.Schedule;
import sim.engine.SimState;
import sim.field.continuous.DContinuous2D;
import sim.util.Double2D;
import sim.util.MutableDouble2D;
import sim.util.Timing;

public class DPSO extends DSimState {
        
    private static final long serialVersionUID = 1;



    
    public int width = 10;
    public int height = 10;
    
    public DContinuous2D<DParticle> space = new DContinuous2D<DParticle>(getPartition().getAOI(), this);;

    //public DParticle[] particles; //should we have this master list here?
    int prevSuccessCount = -1; 
    
    // public modifier values
    public int numParticles = 100;
    public int getNumParticles() { return numParticles; }
    public void setNumParticles(int val) { if (val >= 0) numParticles = val; }

    public int neighborhoodSize = 10;
    public int getNeighborhoodSize() { return neighborhoodSize; }
    public void setNeighborhoodSize(int val) { if ((val >= 0) && (val <= numParticles)) neighborhoodSize = val; }

    public double initialVelocityRange = 0.1; //1.0;
    public double getInitialVelocityRange() { return initialVelocityRange; }
    public void setInitialVelocityRange(double val) { if (val >= 0.0) initialVelocityRange = val; }
    
    public double velocityScalar = 0.27; //2.7;
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
    
    //will we have a version of this per partition?  Do we need to globalize this?
    public double bestVal = 0;
    //MutableDouble2D bestPosition = new MutableDouble2D();
    double best_x = 0.0;
    double best_y = 0.0;
        
    public DPSO(long seed)
        {
        super(seed, 10, 10, 1, false);  //what should these be
        }
    
	@Override
	public void preSchedule()
	{
		// TODO Auto-generated method stub
		super.preSchedule(); // do not forget this line

		System.out.println("Size of agents in proc " + getPID() + " " + space.getAllAgentsInStorage().size()+space.getHaloBounds());

	}
	
    public void updateBest(double currVal, double currX, double currY)
        {
    

    
        if (currVal > bestVal)
            {
            bestVal = currVal;
            //bestPosition.setTo(currX, currY);
            best_x = currX;
            best_y = currY;
            }
    
    
    
    
        }
    
    
    public double getNeighborhoodBest(int index, MutableDouble2D pos)
        {
        double bv = Double.NEGATIVE_INFINITY;
        DParticle p;     


        int start = (index - neighborhoodSize / 2);
        if (start < 0)
            start += numParticles;

        //List<DParticle> particle_list = space.getStorage().getObjects(space.getStorage().getShape());
        List<DParticle> particle_list = space.getAllAgentsInStorage();

    
    
    
        for (int i = 0; i < neighborhoodSize; i++)
            {
            //p = particles[(start + i) % numParticles]; //access storage instead?  won't have a master list in distributed, I believe
            p = particle_list.get((start + i) % particle_list.size());
            if (p.bestVal > bv)
                {
                bv = p.bestVal;
                pos.setTo(p.bestPosition);
                }
            }
        return 1.0;             
        }
    
    @Override
    protected void startRoot() {

                
        DParticle[] particles = new DParticle[numParticles];
        Evaluatable f = mapFitnessFunction(fitnessFunction);            

        for (int i = 0; i < numParticles; i++)
        {
 

        
            double x = (random.nextDouble() * width) - (width * 0.5);
            double y = (random.nextDouble() * height) - (height * 0.5);
            double vx = (random.nextDouble() * initialVelocityRange) - (initialVelocityRange * 0.5);
            double vy = (random.nextDouble() * initialVelocityRange) - (initialVelocityRange * 0.5);
                    
            final DParticle p = new DParticle(x, y, vx, vy, f, i);
        
            particles[i] = p;
            System.out.println(particles[i]+" "+particles[i].position);
                


            }
        
        sendRootInfoToAll("particles", particles);

            
        
        }
        
    @Override
    public void start() {
                
                
                
        // TODO Auto-generated method stub
        super.start(); // do not forget this line

        bestVal = 0;  //how do I keep track of global?
        //System.out.println("best set to 0");
        
        DParticle[] particles = (DParticle[]) getRootInfo("particles");
                
        for (Object p : particles) {
            DParticle a = (DParticle) p;
            //System.out.println(a.bestVal);
                        
            Double2D storagePos = problemSpaceToMasonStorageBounds(a.position);
            if (partition.getLocalBounds().contains(storagePos)) {
                                
                this.space.addAgent(storagePos, a, 0, 0, 1);
                }
            }
                
                
        schedule.scheduleRepeating(Schedule.EPOCH, 1, new DSteppable()
            {
            public void step(SimState state)
                {
                int successCount = 0;
                //List<DParticle> particle_list = space.getStorage().getObjects(space.getStorage().getShape());
                List<DParticle> particle_list = space.getAllAgentsInStorage();

                for (DParticle p: particle_list)            
                    {
                                                    
                    if (Math.abs(p.getFitness() - 1000) <= successThreshold)
                        successCount++;                                                 
                    }
                if (successCount != prevSuccessCount)
                    {
                    prevSuccessCount = successCount;
                    //System.out.println("SuccessCount = " + successCount);                                         
                    if (successCount == numParticles)
                    {
                        //state.kill();
                    }
                    
                    }
            


                }
            }); 
        
        
        
        
        }
        
    //Mason bounds top left is 0,0, while problems in dpso usually have 0,0 in the middle.
    //input real problem value (center is 0.0), outputs storage location (top left is 0.0)
    public Double2D problemSpaceToMasonStorageBounds(MutableDouble2D p) {
                
        Double2D newPoint = new Double2D(p.getX()+ (width * 0.5), p.getY() + (height * 0.5));
        return newPoint;
                
        }
        
        
    //we want to keep track of best point, so each partion gives an array where:
    //index 0 is the value
    //index 1 and 2 are position values (x and y)
    //we want to pick the best index 0 and its corresponding x and y

    protected Serializable[] arbitrateGlobals(ArrayList<Serializable[]> global)
        {
        int chosen_index = 0;
        Object chosen_item = global.get(0)[0];

        double best_val = (double) chosen_item; // make type invariant

        for (int i = 0; i < partition.getNumProcessors(); i++)
            {
            if ((double) global.get(i)[0] > best_val)
                {
                best_val = (double) global.get(i)[0];
                chosen_index = i;
                }
            }

        return global.get(chosen_index);
        }
        
        
    protected Serializable[] getPartitionGlobals() {
        
        //first element is score
        //second element is x
        //third element is y
        
        Serializable[] o = new Serializable[3];
        o[0] = bestVal;
        o[1] = best_x;
        o[2] = best_y;
        
 
        
        return o;
        
        }

    
    protected void setPartitionGlobals(Serializable[] o) {
        
        bestVal = (double) o[0];
        best_x = (double) o[1];
        best_y = (double) o[2];
        

        
        }   
        
    public static void main(final String[] args) {
        Timing.setWindow(20);
        doLoopDistributed(DPSO.class, args);
        System.exit(0);
        }

    }
