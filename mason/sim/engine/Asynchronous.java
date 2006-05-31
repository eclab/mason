/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;

/** Asynchronous objects can be started, stopped, paused, and resumed. */

public interface Asynchronous extends Steppable, Stoppable
    {
    public void pause();
    public void resume();
    }
