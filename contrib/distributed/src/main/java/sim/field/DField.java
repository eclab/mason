package sim.field;

import java.io.Serializable;

import sim.engine.IterativeRepeat;
import sim.util.NdPoint;

/**
 * Implemented be fields in Distributed MASON
 *
 *
 * @param <P> The Type of NdPoint to use
 * @param <T> The Type of Object in the field
 */
public interface DField<T extends Serializable, P extends NdPoint> {
	// We did not declare get methods because fields may want to return
	// T[], ArrayList<T>, T or even primitives like int and double

	/**
	 * Adds Object t to location p <br>
	 * The location can be remote
	 *
	 * @param p location
	 * @param t Object
	 */
	void add(final P p, final T t);

	/**
	 * Removes Object t from location p <br>
	 * The location can be remote
	 *
	 * @param p location
	 * @param t Object
	 */
	void remove(final P p, final T t);

	/**
	 * Removes all Objects from location p <br>
	 * The location can be remote
	 *
	 * @param p location
	 */
	void remove(final P p);

	/**
	 * The location can be remote
	 *
	 * @param fromP Move from Location
	 * @param toP   Move to Location
	 * @param t Object to be moved
	 */
	void move(final P fromP, final P toP, final T t);

	/**
	 * Adds and schedules an agent. The location can be remote
	 *
	 * @param p location
	 * @param t Must be of type Stopping
	 *
	 * @throws IllegalArgumentException if t is not a Stopping
	 */
	void addAgent(final P p, final T t);

	/**
	 * Adds and schedules an agent. The location can be remote
	 *
	 * @param p        location
	 * @param t        Must be of type Stopping
	 * @param ordering
	 * @param time
	 *
	 * @throws IllegalArgumentException if t is not a Stopping
	 */
	void addAgent(final P p, final T t, int ordering, double time);

	/**
	 * Moves and schedules an agent. The toP location can be remote
	 *
	 * @param fromP Move from Location (must be local to the field)
	 * @param toP   Move to Location
	 * @param t     Must be of type Stopping
	 *
	 * @throws IllegalArgumentException if the fromP location is not local or if t
	 *                                  is not Stopping
	 */
	void moveAgent(final P fromP, final P toP, final T t);

	/**
	 * Moves and schedules an agent. The toP location can be remote
	 *
	 * @param fromP    Move from Location (must be local to the field)
	 * @param toP      Move to Location
	 * @param t        Must be of type Stopping
	 * @param ordering
	 * @param time
	 *
	 * @throws IllegalArgumentException if the fromP location is not local or if t
	 *                                  is not Stopping
	 */
	void moveAgent(final P fromP, final P toP, final T t, final int ordering, final double time);

	/**
	 * Adds, schedules and registers a repeating agent. The location can be remote
	 *
	 * @param p        add to Location
	 * @param t        Must be of type Stopping
	 * @param time
	 * @param ordering
	 * @param interval
	 *
	 * @throws IllegalArgumentException if t is not Stopping
	 */
	void addRepeatingAgent(final P p, final T t, final double time, final int ordering, final double interval);

	/**
	 * Adds, schedules and registers a repeating agent. The location can be remote
	 *
	 * @param p        add to Location
	 * @param t        Must be of type Stopping
	 * @param ordering
	 * @param interval
	 *
	 * @throws IllegalArgumentException if t is not Stopping
	 */
	void addRepeatingAgent(final P p, final T t, final int ordering, final double interval);

//	/**
//	 * Adds and registers a repeating agent. The location can be remote <br>
//	 * <br>
//	 * <b>This does not schedule a repeating agent.</b>
//	 *
//	 * @param p               add to Location
//	 * @param iterativeRepeat must contain a Stopping of type T
//	 */
//	void addRepeatingAgent(final P p, final IterativeRepeat iterativeRepeat);

	/**
	 * Removes and stops a repeating agent. The location can be remote <br>
	 *
	 * @param p remove from Location (must be local to the field)
	 * @param t Must be of type Stopping
	 *
	 * @throws IllegalArgumentException if the fromP location is not local or if t
	 *                                  is not Stopping
	 */
	void removeAndStopRepeatingAgent(final P p, final T t);

	/**
	 * Removes and stops a repeating agent. The location can be remote <br>
	 *
	 * @param p               remove from Location (must be local to the field)
	 * @param iterativeRepeat must contain a Stopping of type T
	 *
	 * @throws IllegalArgumentException if the fromP location is not local
	 */
	void removeAndStopRepeatingAgent(final P p, final IterativeRepeat iterativeRepeat);

	/**
	 * Moves and schedules a repeating agent. The toP location can be remote
	 *
	 * @param fromP Move from Location (must be local to the field)
	 * @param toP   Move to Location
	 * @param t     Must be of type Stopping
	 *
	 * @throws IllegalArgumentException if the fromP location is not local
	 */
	void moveRepeatingAgent(final P fromP, final P toP, final T t);

	/**
	 * Moves and schedules a repeating agent. The toP location can be remote
	 *
	 * @param fromP           Move from Location (must be local to the field)
	 * @param toP             Move to Location
	 * @param iterativeRepeat must contain a Stopping of type T
	 *
	 * @throws IllegalArgumentException if the fromP location is not local
	 */
	void moveRepeatingAgent(final P fromP, final P toP, final IterativeRepeat iterativeRepeat);

	void addLocal(final P p, final T t);

	void removeLocal(final P p, final T t);

	void removeLocal(final P p);

	void moveLocal(final P fromP, final P toP, final T t);

}
