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

public abstract class DObject 
    {
    private static final long serialVersionUID = 1;

    static int idCounter = 0;
    final int firstpid;			// this is the PID of the *original* processor on which this object was created
    final int localid;			// this is a unique ID special to processor PID
    
    public DObject()
    	{
    	try
    		{
			firstpid = MPI.COMM_WORLD.getRank();	
    		}
    	catch (MPIException ex)
    		{
    		throw new RuntimeException(ex);
    		}
    	localid = idCounter++;
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
    }
