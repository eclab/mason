package sim.engine;

import java.io.Serializable;

/**
	PROMISE is an implementation of the Promised interface for local promised data
	on your own partition.  For remotely promised data, see REMOTE PROMISE
**/

public class Promise implements Promised
{

	boolean ready = false;
	Serializable object = null;

	/** Returns TRUE if the promised data is ready, else FALSE. */
	public boolean isReady()
	{
		return ready;
	}

	/** Returns the data. This data is only valid if isReady() is TRUE. */
	public Serializable get()
	{
		return object;
	}

	/**
	 * Returns the data, which should be an integer. This data is only valid if
	 * isReady() is TRUE.
	 */
	public int getInt()
	{
		return (Integer) object;
	}

	/**
	 * Returns the data, which should be an double. This data is only valid if
	 * isReady() is TRUE.
	 */
	public double getDouble()
	{
		return (Double) object;
	}

	/** Provides the data and makes the promise ready. */
	public void fulfill(Serializable object)
	{
		ready = true;
		this.object = object;
	}

	/** Copies the data and readiness from another promise. */
	public void setTo(Promise promise)
	{
		ready = promise.ready;
		object = promise.object;
	}

	/**
	 * Constructs an unfulfilled promise
	 */
	public Promise()
	{
	}

	/**
	 * Constructs an already fulfilled promise
	 */
	public Promise(Serializable object)
	{
		this.object = object;
		ready = true;
	}

	/**
	 * Constructs an already fulfilled promise
	 */
	public Promise(int value)
	{
		this.object = Integer.valueOf(value);
		ready = true;
	}

	/**
	 * Constructs an already fulfilled promise
	 */
	public Promise(double value)
	{
		this.object = Double.valueOf(value);
		ready = true;
	}
}
