/*
  Copyright 2006 by Ankur Desai, Sean Luke, and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.pso3d;


import sim.util.Double3D;
import sim.util.MutableDouble3D;

/**
   @author Ankur Desai and Joey Harrison
*/
public class Particle3D
    {       
    static final public long serialVersionUID = 15L;
    double bestVal = 0;     
    MutableDouble3D bestPosition = new MutableDouble3D();

    MutableDouble3D position = new MutableDouble3D();
    MutableDouble3D velocity = new MutableDouble3D();       
                
    private PSO3D pso;
    private Evaluatable3D fitnessFunction;
    private int index;      // this kludge is necessary because the particles are individually scheduled
        
    public Particle3D() 
        {
        super();
        }
        
    public Particle3D(double x, double y, double z, double vx, double vy, double vz, PSO3D pso, Evaluatable3D f, int index)
        {
        super();

        this.position.setTo(x, y, z);
        this.velocity.setTo(vx, vy, vz);
                
        this.pso = pso;
        this.fitnessFunction = f;
        pso.space.setObjectLocation(this,new Double3D(position));
        this.index = index;             
        }

    public void updateBest(double currVal, double currX, double currY, double currZ)
        {
        if (currVal > bestVal)
            {
            bestVal = currVal;
            bestPosition.setTo(currX, currY, currZ);
                        
            pso.updateBest(currVal, currX, currY, currZ);
            }
        }
        
    public double getFitness()
        {
        return fitnessFunction.calcFitness(position.x,position.y,position.z);
        }
        
    public void stepUpdateFitness()
        {               
        updateBest(getFitness(), position.x, position.y, position.z);
        }

    public void stepUpdateVelocity()
        {
        double x = position.x;
        double y = position.y;
        double z = position.z;

        MutableDouble3D nBestPosition = new MutableDouble3D(); 
        pso.getNeighborhoodBest(index, nBestPosition);  // updates the location of nBestPos
                
        // calc new velocity
        // calc x component
        double inertia = velocity.x;
        double pDelta = bestPosition.x - x;
        double nDelta = nBestPosition.x - x;
        double gDelta = pso.bestPosition.x - x;
        double pWeight = Math.random() + 0.4;
        double nWeight = Math.random() + 0.4;
        double gWeight = Math.random() + 0.4;
        double vx = (0.9*inertia + pWeight*pDelta + nWeight*nDelta + gWeight*gDelta) / (1+pWeight+nWeight+gWeight);
                 
        // calc y component
        inertia = velocity.y;
        pDelta = bestPosition.y - y;
        nDelta = nBestPosition.y - y;
        gDelta = pso.bestPosition.y - y;
        pWeight = Math.random() + 0.4;
        nWeight = Math.random() + 0.4;
        gWeight = Math.random() + 0.4;
        double vy = (0.9*inertia + pWeight*pDelta + nWeight*nDelta + gWeight*gDelta) / (1+pWeight+nWeight+gWeight);

        // calc z component
        inertia = velocity.z;
        pDelta = bestPosition.z - z;
        nDelta= nBestPosition.z - z;
        gDelta = pso.bestPosition.z - z;
        pWeight = Math.random() + 0.4;
        nWeight = Math.random() + 0.4;
        gWeight = Math.random() + 0.4;
        double vz = (0.9*inertia + pWeight*pDelta + nWeight*nDelta + gWeight*gDelta) / (1+pWeight+nWeight+gWeight);

        vx *= pso.velocityScalar;
        vy *= pso.velocityScalar;
        vz *= pso.velocityScalar;
                
        // update velocity
        velocity.setTo(vx, vy, vz);             
        }
        
    public void stepUpdatePosition()
        {
        position.addIn(velocity);
        pso.space.setObjectLocation(this, new Double3D(position));
        }

    }
