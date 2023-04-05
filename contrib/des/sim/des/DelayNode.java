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
    Provider provider;
    // next is used internally by Delay to do a little optimization within the heap.  SimpleDelay instead just uses LinkedList
    DelayNode next = null;
    boolean dead = false;
                
    public DelayNode(Resource resource, double timestamp, Provider provider)
        {
        this.resource = resource;
        this.timestamp = timestamp;
        this.provider = provider;
        }
    
    public String toString() { return "DelayNode[" + System.identityHashCode(this) + ": " + resource + ", " + timestamp + ", " + provider + "]"; }
    public Resource getResource() { return resource; }
    public double getTimestamp() { return timestamp; }
    public Provider getProvider() { return provider; }
    
    /** Returns whether the node has been marked dead.  When its delay is finished, 
    	a dead node will not be processed and added to the resources, but instead will
    	simply be discareded. */
	public boolean isDead() { return dead; }
	
    /** Marks a node as dead.  When its delay is finished, 
    	a dead node will not be processed and added to the resources, but instead will
    	simply be discareded. */
	public void setDead(boolean val) { dead = val; }
    }
        
