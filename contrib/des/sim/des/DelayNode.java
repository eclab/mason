/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

/** 
    Used by SimpleDelay to store resources along with their timestamps. 
*/

public class DelayNode
    {
    Resource resource;
    double timestamp;
                
    public DelayNode(Resource resource, double timestamp)
        {
        this.resource = resource;
        this.timestamp = timestamp;
        }
        
    public Resource getResource() { return resource; }
    public double getTimestamp() { return timestamp; }
    }
        
