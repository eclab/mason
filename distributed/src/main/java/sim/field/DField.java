package sim.field;

import java.io.Serializable;

import sim.engine.IterativeRepeat;
import sim.engine.Steppable;
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
	 * @param t Object
	 */
	void remove(final P p);

	/**
	 * The location can be remote
	 *
	 * @param fromP Move from Location
	 * @param toP   Move to Location
	 * @param t
	 */
	void move(final P fromP, final P toP, final T t);

	/**
	 * Adds and schedules an agent. The location can be remote
	 *
	 * @param p location
	 * @param t Must be of type Steppable
	 *
	 * @throws IllegalArgumentException if t is not a Steppable
	 */
	void addAgent(final P p, final T t);

	/**
	 * Adds and schedules an agent. The location can be remote
	 *
	 * @param p        location
	 * @param t        Must be of type Steppable
	 * @param ordering
	 * @param time
	 *
	 * @throws IllegalArgumentException if t is not a Steppable
	 */
	void addAgent(final P p, final T t, int ordering, double time);

	/**
	 * Moves and schedules an agent. The toP location can be remote
	 *
	 * @param fromP Move from Location (must be local to the field)
	 * @param toP   Move to Location
	 * @param t     Must be of type Steppable
	 *
	 * @throws IllegalArgumentException if the fromP location is not local or if t
	 *                                  is not Steppable
	 */
	void moveAgent(final P fromP, final P toP, final T t);

	/**
	 * Moves and schedules an agent. The toP location can be remote
	 *
	 * @param fromP    Move from Location (must be local to the field)
	 * @param toP      Move to Location
	 * @param t        Must be of type Steppable
	 * @param ordering
	 * @param time
	 *
	 * @throws IllegalArgumentException if the fromP location is not local or if t
	 *                                  is not Steppable
	 */
	void moveAgent(final P fromP, final P toP, final T t, final int ordering, final double time);

	/**
	 * Moves and schedules a repeating agent. The toP location can be remote
	 *
	 * @param fromP           Move from Location (must be local to the field)
	 * @param toP             Move to Location
	 * @param toP
	 * @param iterativeRepeat must contain a Steppable of type T
	 *
	 * @throws IllegalArgumentException if the fromP location is not local
	 */
	void moveRepeatingAgent(final P fromP, final P toP, final IterativeRepeat iterativeRepeat);

	void addLocal(final P p, final T t);

	void removeLocal(final P p, final T t);

	void removeLocal(final P p);

	default void moveLocal(final P fromP, final P toP, final T t) {
		removeLocal(fromP, t);
		addLocal(toP, t);
	}

}
