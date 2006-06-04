/*
  Copyright 2006 by Ankur Desai, Sean Luke, and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.pso3d;

/**
   @author Ankur Desai and Joey Harrison
*/
public class Griewangk3D implements Evaluatable3D 
    {
    private final double sqrt2 = Math.sqrt(2);
    private final double sqrt3 = Math.sqrt(3);
        
    public double calcFitness(double x, double y, double z) 
        {
        return (1000 - (1 + (x*x) / 4000 + (y*y) / 4000 + (z*z) / 4000 - Math.cos(x) * Math.cos(y / sqrt2) * Math.cos(z / sqrt3)));
        }
    }
