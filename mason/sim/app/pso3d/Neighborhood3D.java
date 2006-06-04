/*
  Copyright 2006 by Ankur Desai, Sean Luke, and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.pso3d;

/**
   @author Ankur Desai and Joey Harrison
*/
import sim.util.*;

public class Neighborhood3D
    {
    public double bestVal = 0;
    MutableDouble3D bestPosition = new MutableDouble3D();
        
    public PSO3D po;
        
    public Neighborhood3D(PSO3D p)
        {
        po = p;
        }
        
    public void updateBest(double currVal, double currX, double currY, double currZ)
        {
        if (currVal > bestVal)
            {
            bestVal = currVal;
            bestPosition.setTo(currX, currY, currZ);

            po.updateBest(currVal, currX, currY, currZ);
            }               
        }       
    }

