/*
  Copyright 2006 by Ankur Desai, Sean Luke, and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.pso3d;

/**
   @author Ankur Desai and Joey Harrison   
*/
public interface Evaluatable3D 
    {
    public double calcFitness(double x, double y, double z);
    }
