/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import java.util.*;


/**
   A Sink accepts all incoming offers of resources matching a given type, then throws them away.
*/

public class Sink implements Receiver, StatReceiver
    {
    protected SimState state;
    Resource typical;
        
    double totalReceivedResource;
    public double getTotalReceivedResource() { return totalReceivedResource; }
    public double getReceiverResourceRate() { double time = state.schedule.getTime(); if (time <= 0) return 0; else return totalReceivedResource / time; }

	@Deprecated
    public Resource getTypical() { return getTypicalReceived(); }
        
    public Resource getTypicalReceived() { return typical; }
	public boolean hideTypicalReceived() { return true; }

    void throwUnequalTypeException(Resource resource)
        {
        throw new RuntimeException("Expected resource type " + this.typical.getName() + "(" + this.typical.getType() + ")" +
            " but got resource type " + resource.getName() + "(" + resource.getType() + ")" );
        }

    void throwInvalidAtLeastAtMost(double atLeast, double atMost)
        {
        throw new RuntimeException("Requested resource amounts are between " + atLeast + " and " + atMost + ", which is out of bounds.");
        }

    public Sink(SimState state, Resource typical)
        {
        this.state = state;
        this.typical = typical;
        }

    public boolean accept(Provider provider, Resource resource, double atLeast, double atMost)
        {
        if (!typical.isSameType(resource)) throwUnequalTypeException(resource);
        
        if (!(atLeast >= 0 && atMost >= atLeast))
        	throwInvalidAtLeastAtMost(atLeast, atMost);

        if (resource instanceof CountableResource) 
            {
			totalReceivedResource += atMost;
            ((CountableResource) resource).reduce(atMost);
            return true;
            }
        else
            {
			totalReceivedResource += 1.0;
            return true;
            }
        }

    public String toString()
        {
        return "Sink@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + ", " + typical.getName() + ")";
        }               

    public void step(SimState state)
        {
        // do nothing
        }

    String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
   	public boolean hideName() { return true; }

    public void reset(SimState state) { totalReceivedResource = 0; }
 	}
