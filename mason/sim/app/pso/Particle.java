/*
  Copyright 2006 by Ankur Desai, Sean Luke, and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.pso;

import sim.util.Double2D;
import sim.util.MutableDouble2D;

/**
   @author Ankur Desai and Joey Harrison
*/
public class Particle
    {       
    static final public long serialVersionUID = 15L;
    double bestVal = 0;     
    MutableDouble2D bestPosition = new MutableDouble2D();

    MutableDouble2D position = new MutableDouble2D();
    MutableDouble2D velocity = new MutableDouble2D();       
                
    PSO pso;
    Evaluatable fitnessFunction;
    int index;  // this kludge is necessary because the particles are individually scheduled
        
    public Particle() 
        {
        super();
        }
        
    public Particle(double x, double y, double vx, double vy, PSO pso, Evaluatable f, int index)
        {
        super();

        this.position.setTo(x, y);
        this.velocity.setTo(vx, vy);
                
        this.pso = pso;
        this.fitnessFunction = f;
        pso.space.setObjectLocation(this,new Double2D(position));
        this.index = index;
        }

    public void updateBest(double currVal, double currX, double currY)
        {
        if (currVal > bestVal)
            {
            bestVal = currVal;
            bestPosition.setTo(currX, currY);
                        
            pso.updateBest(currVal, currX, currY);
            }
        }
        
    public double getFitness()
        {
        return fitnessFunction.calcFitness(position.x,position.y);
        }
        
    public void stepUpdateFitness()
        {
        updateBest(getFitness(), position.x, position.y);
        }

    public void stepUpdateVelocity()
        {
        double x = position.x;
        double y = position.y;
                
        MutableDouble2D nBestPos = new MutableDouble2D(); 
        pso.getNeighborhoodBest(index, nBestPos);       // updates the location of nBestPos
                
        // calc new velocity
        // calc x component
        double inertia = velocity.x;
        double pDelta = bestPosition.x - x;
        double nDelta = nBestPos.x - x;
        double gDelta = pso.bestPosition.x - x;
        double pWeight = Math.random() + 0.4;
        double nWeight = Math.random() + 0.4;
        double gWeight = Math.random() + 0.4;
        double vx = (0.9*inertia + pWeight*pDelta + nWeight*nDelta + gWeight*gDelta) / (1+pWeight+nWeight+gWeight);
                 
        // calc y component
        inertia = velocity.y;
        pDelta = bestPosition.y - y;
        nDelta = nBestPos.y - y;
        gDelta = pso.bestPosition.y - y;
        pWeight = Math.random() + 0.4;
        nWeight = Math.random() + 0.4;
        gWeight = Math.random() + 0.4;
        double vy = (0.9*inertia + pWeight*pDelta + nWeight*nDelta + gWeight*gDelta) / (1+pWeight+nWeight+gWeight);

        vx *= pso.velocityScalar;
        vy *= pso.velocityScalar;
                
        // update velocity
        velocity.setTo(vx, vy);         
        }
        
    public void stepUpdatePosition()
        {
        //System.out.println(
        //              "Best: " + n.bestVal + " (" + n.bestPosition.x + ", " + n.bestPosition.y + ")");
        position.addIn(velocity);
        pso.space.setObjectLocation(this, new Double2D(position));
        }

    }
