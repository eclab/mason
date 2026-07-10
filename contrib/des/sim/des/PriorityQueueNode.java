/*
  Copyright 2026 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

/** 
    Used by PriorityQueue to return resources along with their Comparables. 
*/

public class PriorityQueueNode implements Comparable<PriorityQueueNode>
    {
    Entity entity;
    Comparable key;
        
    public PriorityQueueNode(Entity entity, Comparable key) 
        { 
        this.entity = entity; 
        this.key = key; 
        }
        
    public Entity getEntity() 
        { 
        return entity; 
        }
        
    public Comparable getKey()
        {
        return key;
        }
                
    /** Compares the PriorityQueueNode to other PriorityQueueNodes by Comparable. */
    public int compareTo(PriorityQueueNode other)
        {
        if (other == null) throw new NullPointerException();
        else return key.compareTo(other.key);
        }
    }
        
