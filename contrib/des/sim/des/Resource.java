/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import java.util.concurrent.atomic.*;

/**
   The top level abstract superclass of things that can be handed from one node to another.
   Resouces have TYPES (unique integers) and NAMES (which you can specify and which don't
   have to be unique).  You create a new kind of resource with new Resource(name).  You
   create more than one of the same kind of resoure by duplicating an existing Resource
   or using a copy constuctor found in a resource subclass.
        
   <p>Note that type allocation is not threadsafe.
   <!-- unless DSimState.isMultiThreaded() returns true. -->
**/


public abstract class Resource implements java.io.Serializable
    {
    private static final long serialVersionUID = 1;

    static int typeCounter = 0;
    //static final AtomicInteger threadSafeCounter = new AtomicInteger();
         
    int type;
    String name;
        
    /** Creates a new resource type. */
    static int getNextType() 
        { 
        //if (DSimState.isMultiThreaded())
        //      return threadSafeCounter.getAndIncrement();
        //else
        return typeCounter++;
        }
        
    /** 
        Returns a new kind of Resource with a given name, and initial amount.
        The name is informal: It's legal, though unwise, for two different kinds of resources 
        to have the same name.  Resource types are distinguished internally using
        unique integers. 
    */
    public Resource(String name)
        {
        this.name = name;
        this.type = getNextType();
        }
                
    /** Constructs a Resource but does not set the name nor the type. 
        This exists primarily for copy constructors to call super()
        rather than super(name).  */
    protected Resource()
        {
        }
        
    /** Clears the resource.  For countable/uncountable resources, this sets its amount to zero.
        For entities, this removes any stored elements but does not change the Info object. */
    public abstract void clear();
        
    /**
       Prints the resource out in a pleasing manner. 
    */
    public abstract String toString();

    /** Does comparison by pointer, that is, ==.  This is because CountableResources are mutable,
    	and so equality testing by value will break hash tables.  Furthermore, it
    	makes it work properly with remove(). */
    public boolean equals(Object other)
        {
        return (this == other);
        }
    
    /** Hashes by pointer. */
    public int hashCode()
        {
        return System.identityHashCode(this);
        }

    /** 
        Returns the amount of the resource.
    */
    public abstract double getAmount();

    /** 
        Returns true if the two objects are both Entities with the same type.
    */
    public boolean isSameType(Resource other)
        {
        if (other == this) return true;
        if (other == null) return false;
        if (other instanceof Resource)
            {
            Resource res = (Resource) other;
            return (res.type == type);
            }
        else return false;
        }
        

    /** Makes an exact copy of this resource.  */
    public abstract Resource duplicate();

    /** 
        Exactly copies the resource TIMES times.  Returns an array of the new resources.
    */
    public Resource[] duplicate(int times)
        {
        Resource[] res = new Resource[times];                   // this will throw an exception for us
        for(int i = 0; i < times; i++)
            {
            res[i] = duplicate();
            }
        return res;
        }
        
    /**
       Returns the assigned entity type.
    */
    public int getType()
        {
        return type;
        }
                
    /**
       Returns the assigned entity name.
    */
    public String getName()
        {
        return name;
        }

    protected void setName(String name)
        {
        this.name = name;
        }
    }
