package sim.engine.mpi;

import java.io.Serializable;

import sim.engine.*;
import sim.util.*;

//This class is not supposed to be used by the modelers
/**
 * Wrapper for transporting objects to remote processors<br>
 * Used Internally
 *
 */
public class PayloadWrapper extends MigratableObject
{
	private static final long serialVersionUID = 1L;

	/** Set the INTERVAL to this to indicate that the object is NOT a REPEATING AGENT */
	public static final int NON_REPEATING_INTERVAL = -1;

	/** Set the TIME to this to indicate that the object is NOT an AGENT */
	public static final double NON_AGENT_TIME = -1;

	/** Set the ORDERING to this to indicate that the object is NOT an AGENT (though it doesn't matter) */
	public static final int NON_AGENT_ORDERING = 0;		// doesn't really matter
	
	/**
	 * The is the Object to be transported<br>
	 * Required to be set by the caller
	 */
	public Serializable payload;

	/**
	 * The is the pId of the destination<br>
	 * Required to be set by the caller
	 */
	public int destination;

	/**
	 * Location of the Object in the field <br>
	 * Optional field, only needed it the payload is to be added to a field as well
	 * <br>
	 * <br>
	 * Default: null
	 */
	public Number2D loc;

	/**
	 * Internal field, do not set it explicitly <br>
	 * It is set by the field, if a field is used to migrate an Object <br>
	 */
	public int fieldIndex;

	/**
	 * Ordering for the scheduler <br>
	 * Optional field <br>
	 * <br>
	 * Default: 1
	 */
	public int ordering;

	/**
	 * time for the scheduler. Values less than zero are considered invalid <br>
	 * Optional field <br>
	 * <br>
	 * Default: -1.0
	 */
	public double time;
	
	public double interval; 

	public PayloadWrapper(Serializable obj, int dst, Number2D loc, int fieldIndex, int ordering, double time, double interval)
	{
		this.destination = dst;
		this.payload = obj;
		this.loc = loc;
		this.fieldIndex = fieldIndex;
		this.ordering = ordering;
		this.time = time;
		this.interval = interval;
	}
	
	public boolean isRepeating()
		{
		return (interval != NON_REPEATING_INTERVAL);
		}

	public boolean isAgent()
		{
		return (time != NON_AGENT_TIME);
		}

	public String toString()
	{
		return "PayloadWrapper<" + payload + ", to=" + destination + ", at=" + loc + ", field=" + fieldIndex + 
			(isAgent() ? ", time=" + time + ", ord=" + ordering + 
				(isRepeating() ? ", int=" + interval : "") : "" ) + ">";
	}

}
