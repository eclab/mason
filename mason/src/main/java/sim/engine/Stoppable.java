/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

/** Stoppable objects can be prevented from being stepped any further by calling their stop() method. */

public interface Stoppable extends java.io.Serializable
    {
    public void stop();
    }
