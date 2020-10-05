/*
  Copyright 2020 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;
import mpi.*;

/** 
	A superclass for objects that may be accessed and queried remotely.  To do this,
	such objects need a unique system-wide ID.  This ID is generated through a combination
	of the PID on which the object was first created, and a counter special to that PID.
	DObject implements a basic form of equals and hashCode based on comparing these IDs.
*/

public abstract class DObject implements java.io.Serializable
    {
    private static final long serialVersionUID = 1;
	static int pid;
	
	static
		{
    	try
    		{
			pid = MPI.COMM_WORLD.getRank();	
    		}
    	catch (MPIException ex)
    		{
    		throw new RuntimeException(ex);
    		}
		}

    /** Returns the current PID on which this object resides. */ 
    public static int getPID() 
    	{ 
    	return pid;
    	}
    	
    static int idCounter = 0;
	static int nextCounter()	// FIXME If we need to be multithreaded, we need to figure out how to make this synchronized yet fast
		{
		return idCounter++;
		}
	
    final int firstpid;			// this is the PID of the *original* processor on which this object was created
    final int localid;			// this is a unique ID special to processor PID
    
    public DObject()
    	{
    	firstpid = pid;						// called originally to get the FIRST PID
    	localid = nextCounter();
    	}
    
    public boolean equals(Object other)
    	{
    	if (other == null) return false;
    	else if (other == this) return true;
    	else if (!(other instanceof DObject)) return false;
    	else return (firstpid == ((DObject)other).firstpid && localid == ((DObject)other).localid);
    	}
    	
    public final int hashCode()
        {
        // stolen from Int2D.hashCode()
        int y = this.firstpid;
        y += ~(y << 15);
        y ^=  (y >>> 10);
        y +=  (y << 3);
        y ^=  (y >>> 6);
        y += ~(y << 11);
        y ^=  (y >>> 16);
        return localid ^ y;
        }
    
    /** Returns a unique system-wideID to this object. */ 
    public long getID() { return (((long)firstpid) << 32) | localid; }
    
    /** Returns a string consisting of the original pid, followed by the unique local id of the object.
    	Together these form a unique, permanent systemwide id. */
    public final String getIDString() { return firstpid + "/" + localid; }
    
    /** Returns a string consisting of the form CLASSNAME:UNIQUEID@CURRENTPID */
    public final String toString() { return this.getClass().getName() + ":" + getIDString() + "@" + getPID(); }
    }
