/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import sim.util.*;
import java.util.*;

/** 
    A storage for CountableResources (or subclasses such as UncountableResources or Money etc.) with
    a maximum resource value.  Lock and Unlock are used to seize and release resources to/from Pools.
    The maximum for a standard CountableResource is by default CountableResource.MAXIMUM_INTEGER.
    The maximum for an UncountableResource is by default Double.POSITIVE_INFINITY.
*/


public class Pool
    {
    CountableResource resource;
    double maximum;
        
    public Pool(CountableResource resource, double maximum)
        {
        this.maximum = maximum;
        this.resource = resource;
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
        
    public Pool()
        {
        this(0);
        }
        
    public CountableResource getResource() { return resource; }
    public void setResource(CountableResource val) { resource = val; }
        
    public double getMaximum() { return maximum; }
    public void setMaximum(double val) { maximum = val; }

    public String getName()
        {
        return "Pool(" + resource + ", " + (long)maximum + ")";
        }               
    }
