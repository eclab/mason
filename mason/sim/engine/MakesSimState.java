/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

/** An interface for classes capable of creating SimState subclasses.
    Typically you wouldn't use this interface; but rather it is used
    internaly in the SimState.doLoop methods. */
        
public interface MakesSimState
    {
    /** Creates a SimState subclass with the given random number seed
        and command-line arguments passed into main(...). */
    public SimState newInstance(long seed, String[] args);
        
    /** Returns the class of the SimState subclass that will be generated. */
    public Class simulationClass();
    }
