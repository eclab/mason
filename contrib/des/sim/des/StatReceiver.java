/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;

/**
   Statistics facilities for Receivers.  Only certain receivers implement this.
*/

public interface StatReceiver extends Receiver
    {
    /** Returns the total amount of received (and accepted) resource */
    public double getTotalReceivedResource();
    
    /** Returns the received (and accepted) resource rate. */
    // Implement this as 
    // double time = state.schedule.getTime(); if (time <= 0) return 0; else return totalReceivedResource / time;
    public double getReceiverResourceRate();
    
    /** Resets the received (and accepted) resource amount to 0, among other possible things. */
    public void reset(SimState state);
    }
