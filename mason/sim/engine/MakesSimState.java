/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

public interface MakesSimState
    {
    public SimState newInstance(long seed, String[] args);
    public Class simulationClass();
    }
