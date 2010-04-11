/*
  Copyright 2010 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal;

/**
   An Orientable2D object can have its orientation changed in radians.
    
   <p>Objects which define the setOrientation2D(val) method can have their orientation changed
   by AdjustablePortrayal2D.
*/

public interface Orientable2D extends Oriented2D
    {
    public void setOrientation2D(double val);
    }
