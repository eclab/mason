/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal;

/**
   An Oriented2D object provides an orientation in radians.
    
   <p>Objects which define the orientation2D() method can have their orientation shown
   by Oriented2D Portrayals.
*/


public interface Oriented2D
    {
    public double orientation2D();
    }
