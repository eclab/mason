/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.des;

import sim.engine.*;
import java.util.*;

/**
   A subclass that defines Resources which are atomic and cannot be subdivided.  You can think
   of Entities as tokens which cannot be broken into smaller tokens: this might be used to model
   things like cars, or bricks, or telephones.  Entities can also be COMPOSITE, meaning that they
   hold a one or more things (normally Resources) in their bellies.  A composite Entity might be thought of as a
   cargo shipping container: you can't break it into two smaller containers, but you can open it
   up and get at the things inside it.  
**/

public class Entity extends Resource
    {
    Resource[] storage;
    
    /** 
        Creates an Entity of a new, unique type.
    */
    public Entity(String name)
        {
        super(name);
        }
        
    /** 
        Returns a Entity of the same type, name, and amount as the provided Entity.
        Note that the copy of the Entity's storage is just a pointer copy: you may
        want to copy the storage more properly.
    */
    public Entity(Entity other)
        {
        super();
        this.name = other.name;
        this.type = other.type;
        this.storage = other.storage;
        }
                
    /** Returns the current storage composed in the Entity, or null if there is none. */
    public Resource[] getStorage()
        {
        return storage;
        }
        
    /** Sets the current storage composed in the Entity, or null if there is none. */
    public void setStorage(Resource[] val)
        {
        storage = val;
        }
        
    /** Returns true if the Entity is composite. */
	public boolean isComposite()
		{
		return (storage != null);
		}
		
    /** Removes the Entity's storage. */
    public void clear()
        {
        storage = null;
        }
    
    /** Returns 1.0 always. */  
    public double getAmount()
        {
        return 1.0;
        }

	/** Returns a Entity of the same type, name, and amount as the provided Entity.
        Note that the copy of the Entity's storage is just a pointer copy: you may
        want to copy the storage more properly. */
    public Resource duplicate()
        {
        return new Entity(this);
        }

    public String toString()
        {
        return "Entity[" + name + " (" + type + ")]";
        }

    public boolean equals(Object other)
        {
        if (other == this) return true;
        if (other == null) return false;
        if (other instanceof Entity)
            {
            Entity res = (Entity) other;
            return (res.type == type);
            }
        else return false;
        }
        
    public final int hashCode()
        {
        return type;
        }
    }
