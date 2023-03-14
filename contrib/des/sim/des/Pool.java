/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.des.portrayal.DESPortrayal;
import sim.engine.*;
import sim.portrayal.*;
import sim.portrayal.simple.*;

/** 
    A storage for CountableResources (or subclasses such as UncountableResources or Money etc.) with
    a maximum resource value.  Lock and Unlock are used to seize and release resources to/from Pools.
    The maximum for a standard CountableResource is by default CountableResource.MAXIMUM_INTEGER.
    The maximum for an UncountableResource is by default Double.POSITIVE_INFINITY.
*/


public class Pool extends DESPortrayal implements Named, Resettable
    {
	
    private static final long serialVersionUID = -4048334284332068371L;

	public SimplePortrayal2D buildDefaultPortrayal(double scale){
    	return new ShapePortrayal2D(ShapePortrayal2D.POLY_PARALLELOGRAM, 
    			getFillPaint(), getStrokePaint(), getStrokeWidth(), scale);
    }
	
    CountableResource resource;
    CountableResource initial;
    double maximum;
        
    public Pool(CountableResource resource, double maximum)
        {
        this.maximum = maximum;
        this.resource = resource;
        this.initial = (CountableResource)(resource.duplicate());
        }

    public Pool(CountableResource resource)
        {
        if (resource instanceof UncountableResource)
            this.maximum = Double.POSITIVE_INFINITY;
        else
            this.maximum =  CountableResource.MAXIMUM_INTEGER;
        }
        
    public Pool(int initialResourceAllocation)
        {
        this(new CountableResource("", initialResourceAllocation));
        }
        
    public Pool(int initialResourceAllocation, double maximum)
        {
        this(new CountableResource("", initialResourceAllocation));
        this.maximum = maximum;
        }
        
    public Pool()
        {
        this(0);
        }
        
    public CountableResource getResource() { return resource; }
    public void setResource(CountableResource val) { resource = val; }
        
    public double getMaximum() { return maximum; }
    public void setMaximum(double val) { maximum = val; }

    public String toString()
        {
        return "Pool@" + System.identityHashCode(this) + "(" + (getName() == null ? "" : getName()) + ", " + resource + ", " + (long)maximum + ")";
        }               

    String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    /** Resets the pool's resource to its initial value */
    public void reset(SimState state) 
        { 
        this.resource = (CountableResource)(initial.duplicate());
        }
    }