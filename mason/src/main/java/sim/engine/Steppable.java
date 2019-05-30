/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

/** Something that can be stepped */

public interface Steppable extends java.io.Serializable
    {
    public void step(SimState state);
    }
