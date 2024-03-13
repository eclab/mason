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
   
   <p>An Entity can also contain an INFO object.  This object can be anything you like.  The idea
   is that the entity might be of a given type but have certain features (such as, say, a batch number
   or an expiration date, or a VIN number).   
**/

public class Entity extends Resource
    {
    private static final long serialVersionUID = 1;

    Resource[] storage;
    Object info;
    
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
        want to copy the storage more properly.  Similarly, the copy of the info is
        just a pointer copy, though this might be more appropriate.
    */
    public Entity(Entity other)
        {
        super();
        this.name = other.name;
        this.type = other.type;
        this.storage = other.storage;
        this.info = info;
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
        
    /** Returns the current info object in the Entity, or null if there is none. */
    public Object getInfo()
        {
        return info;
        }
        
    /** Sets the current info object in the Entity, or null if there is none. */
    public void setInfo(Object val)
        {
        info = val;
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
        Note that the copy of the Entity's storage and info are just pointer copies: 
        you may want to copy them more properly. */
    public Resource duplicate()
        {
        return new Entity(this);
        }

    public String toString()
        {
        return "Entity[" + name + " (" + type + ")]";
        }
    }
