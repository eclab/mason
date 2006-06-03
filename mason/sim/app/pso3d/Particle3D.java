/*
  Copyright 2006 by Ankur Desai, Sean Luke, and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.pso3d;

/**
   @author Ankur Desai and Joey Harrison
*/
import sim.util.Double3D;
import sim.util.MutableDouble3D;

public class Particle3D
{	
	static final public long serialVersionUID = 15L;
   	double bestVal = 0;   	
   	MutableDouble3D bestPosition = new MutableDouble3D();

   	MutableDouble3D position = new MutableDouble3D();
   	MutableDouble3D velocity = new MutableDouble3D();  	
   	   	
   	Neighborhood3D n;
   	Evaluatable3D fitnessFunction;
   	
	public Particle3D() 
	{
		super();
	}
	
	public Particle3D(double x, double y, double z, double vx, double vy, double vz, Neighborhood3D n, Evaluatable3D f)
	{
		super();

		this.position.setTo(x, y, z);
		this.velocity.setTo(vx, vy, vz);
		
		this.n = n;
		this.fitnessFunction = f;
		n.po.space.setObjectLocation(this,new Double3D(position));
	}

	public void updateBest(double currVal, double currX, double currY, double currZ)
	{
		if (currVal > bestVal)
		{
			bestVal = currVal;
			bestPosition.setTo(currX, currY, currZ);
		}
	}
	
	public double getFitness()
	{
		return fitnessFunction.calcFitness(position.x,position.y,position.z);
	}
	
	public void stepUpdateFitness()
	{
		double fitness = getFitness();
		
		updateBest(fitness, position.x, position.y, position.z);
		n.updateBest(fitness, position.x, position.y, position.z);
	}

	public void stepUpdateVelocity()
	{
		double x = position.x;
		double y = position.y;
		double z = position.z;
		
		// calc new velocity
		// calc x component
		double inertia = velocity.x;
		double pBest = bestPosition.x - x;
		double nBest = n.bestPosition.x - x;
		double gBest = n.po.bestPosition.x - x;
		double pWeight = Math.random() + 0.4;
		double nWeight = Math.random() + 0.4;
		double gWeight = Math.random() + 0.4;
		double vx = (0.9*inertia + pWeight*pBest + nWeight*nBest + gWeight*gBest) / (1+pWeight+nWeight+gWeight);
		 
		// calc y component
		inertia = velocity.y;
		pBest = bestPosition.y - y;
		nBest = n.bestPosition.y - y;
		gBest = n.po.bestPosition.y - y;
		pWeight = Math.random() + 0.4;
		nWeight = Math.random() + 0.4;
		gWeight = Math.random() + 0.4;
		double vy = (0.9*inertia + pWeight*pBest + nWeight*nBest + gWeight*gBest) / (1+pWeight+nWeight+gWeight);

		// calc z component
		inertia = velocity.z;
		pBest = bestPosition.z - z;
		nBest = n.bestPosition.z - z;
		gBest = n.po.bestPosition.z - z;
		pWeight = Math.random() + 0.4;
		nWeight = Math.random() + 0.4;
		gWeight = Math.random() + 0.4;
		double vz = (0.9*inertia + pWeight*pBest + nWeight*nBest + gWeight*gBest) / (1+pWeight+nWeight+gWeight);

		vx *= n.po.velocityScalar;
		vy *= n.po.velocityScalar;
		vz *= n.po.velocityScalar;
		
		// update velocity
		velocity.setTo(vx, vy, vz);		
	}
	
	public void stepUpdatePosition()
	{
		position.addIn(velocity);
		n.po.space.setObjectLocation(this, new Double3D(position));
	}

}
