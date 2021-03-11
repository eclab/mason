package sim.field.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import sim.engine.DObject;
import sim.util.*;

public class ContinuousStorage<T extends DObject> extends GridStorage<T> {
	private static final long serialVersionUID = 1L;

	// note that this is the DISCRETIZED WIDTH (and the DISCRETIZED HEIGHT is height
	// in GridStorage)
	int width;
	int discretization;
	public HashMap<Long, Double2D> m;
	public HashMap<Long, T>[] storage;
	public boolean removeEmptyBags = true;

	public ContinuousStorage(final IntRect2D shape, final IntRect2D haloBounds, int discretization) {
		super(shape, haloBounds);
		this.discretization = discretization;
		clear();
	}

	public HashMap<Long, Double2D> getStorageMap() {
		return m;
	}

	public int getDiscretization() {
		return discretization;
	}

	public String toString() {
		final StringBuffer string = new StringBuffer(String.format("ContStorage-%s\n", shape));

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				HashMap<Long, T> cell = getCelldp(x, y);
				if (cell.size() > 0)
					string.append("Cell (" + x + ", " + y + "):\t" + cell + "\n");
			}
		}

		return string.toString();
	}

	/**
	 * Discretizes the given point, in world coordinates, into the cell which holds
	 * the point. Warning: does not check to see if the point is inside the local
	 * boundary.
	 */
	public Int2D discretize(final Double2D p) {

		final double[] offsets = shape.ul().getOffsets(p);

		int[] ans = new int[2];
		for (int i = 0; i < offsets.length; i++) {
			ans[i] = 0 - (int) (offsets[i] / (double) discretization);
		}
		return new Int2D(ans);
	}

	void setCelldp(final Int2D p, HashMap<Long, T> cell) {
		storage[getFlatIdx(p.x, p.y)] = cell;
	}

	/** Sets the given discretized cell. */
	void setCelldp(int x, int y, HashMap<Long, T> cell) {
		storage[getFlatIdx(x, y)] = cell;
	}

	/**
	 * Sets the cell which contains the given world point. Does not check to see if
	 * the point is out of bounds.
	 */
	public void setCell(final Double2D p, HashMap<Long, T> cell) {
		setCelldp(discretize(p), cell);
	}

	/** Returns the given discretized cell. */
	HashMap<Long, T> getCelldp(final Int2D p) {

		return storage[getFlatIdx(p.x, p.y)];
	}

	/** Returns the given discretized cell. */
	public HashMap<Long, T> getCelldp(int x, int y) {
		return storage[getFlatIdx(x, y)];
	}

	/**
	 * Returns the cell which contains the given world point. Does not check to see
	 * if the point is out of bounds.
	 */
	public HashMap<Long, T> getCell(final Double2D p) {
		return getCelldp(discretize(p));
	}

	/** Returns the location of the given object. */
	public Double2D getObjectLocation(final T obj) {
		return m.get(obj.ID());
	}

	/** Returns the location of the object with the given ID. */
	public Double2D getObjectLocation(final long id) {
		return m.get(id);
	}

	///// GRIDSTORAGE METHODS

	// Put the object to the given point
	public void addObject(Double2D p, T obj) {
		final Double2D old = m.put(obj.ID(), p);
		if (old != null)
			getCell(old).remove(obj);
		getCell(p).put(obj.ID(), obj);
	}

	public void addObject(Int2D p, T obj) {
		addObject(new Double2D(p.x, p.y), obj);
	}

	public void addObjectUsingGlobalLoc(Double2D p, final T t) {
		addObject(toLocalPoint(p), t);
	}

	public T getObject(Double2D p, long id) {
		HashMap<Long, T> cell = getCell(p);
		if (cell == null)
			return null;
		else
			return cell.get(id);
	}

	public T getObject(Int2D p, long id) {
		return getObject(new Double2D(p.x, p.y), id);
	}

	public T getObjectUsingGlobalLoc(Double2D p, long id) {
		return getObject(toLocalPoint(p), id);
	}

	// Get all the objects at exactly the given point
	public ArrayList<T> getAllObjects(final Double2D p) {
		final ArrayList<T> objects = new ArrayList<>();
		HashMap<Long, T> cell = getCell(p);

		if (cell != null) {
			for (final T t : cell.values()) {
				if (m.get(t.ID()).equals(p))
					objects.add(t);
			}
		}
		return objects;
	}

	public ArrayList<T> getAllObjects(final Int2D p) {
		return getAllObjects(new Double2D(p.x, p.y));
	}

	public ArrayList<T> getAllObjectsUsingGlobalLoc(final Double2D p) {
		return getAllObjects(toLocalPoint(p));
	}

	public boolean removeObject(long id) {
		// p is ignored.
		Double2D loc = m.remove(id);
		if (loc == null)
			return false;
		getCell(loc).remove(id);
		return true;
	}

	public boolean removeObject(Double2D p, long id) {
		// p is ignored.
		return removeObject(id);
	}

	public boolean removeObject(Int2D p, long id) {
		// p is ignored.
		return removeObject(id);
	}

	public boolean removeObjectUsingGlobalLoc(Double2D p, long id) {
		return removeObject(id);
	}

	// Remove all the objects at the given point
	public void clear(Double2D p) {
		HashMap<Long, T> cell = getCell(p);
		for (T obj : cell.values()) {
			if (m.get(obj.ID()).equals(p))
				cell.remove(obj);
		}
	}

	public void clear(Int2D p) {
		clear(new Double2D(p.x, p.y));
	}

	public void clearUsingGlobalLoc(Double2D p) {
		clear(toLocalPoint(p));
	}

	public void clear() {
		width = (int) Math.ceil(shape.getSizes()[0] / (double) discretization) + 1;
		height = (int) Math.ceil(shape.getSizes()[1] / (double) discretization) + 1;
		this.m = new HashMap<>();

		storage = new HashMap[width * height];
		for (int i = 0; i < storage.length; i++) {
			storage[i] = new HashMap<>();

		}
	}

	/// METHODS FOR PACKING AND UNPACKING

//	void removeObject(long id) {
//		getCell(m.remove(id)).remove(id);
//	}

	// Remove all the objects inside the given rectangle
	void removeObjects(final IntRect2D r) {
		for (T obj : getObjects(r)) {
			removeObject(obj.ID());
		}
	}

	// Get all the objects inside the given rectangle
	public ArrayList<T> getObjects(final IntRect2D r) {
		final ArrayList<T> objs = new ArrayList<T>();
		int[] offset = { 1, 1 };

		final Int2D ul = discretize(new Double2D(r.ul()));
		final Int2D br = discretize(new Double2D(r.br())).add(offset);

		// I believe this code is just doing:

		for (int x = ul.x; x < br.x; x++) {
			for (int y = ul.y; y < br.y; y++) {

				HashMap<Long, T> cell = getCelldp(x, y);

				if (cell != null) {

					for (T obj : cell.values()) { // need to offset/discretize!
						if (r.contains(m.get(obj.ID()))) {
							objs.add(obj);
						}
					}
				}
			}
		}

		return objs;
	}

	// This returns a list of a list of dissimilar Objects
	// They are either of type T or of type Double2D
	public Serializable pack(final MPIParam mp) {
		final ArrayList<ArrayList<Serializable>> ret = new ArrayList<>();

		for (final IntRect2D rect : mp.rects) {
			final ArrayList<Serializable> objs = new ArrayList<>();
			for (final T obj : getObjects(rect.shift(shape.ul().toArray()))) {
				objs.add(obj);
				// Append the object's location relative to the rectangle
				objs.add(m.get(obj.ID()).subtract(shape.ul().toArray()).subtract(rect.ul().toArray()));
			}
			ret.add(objs);
		}

		return ret;
	}

	public int unpack(final MPIParam mp, final Serializable buf) {
		final ArrayList<ArrayList<Serializable>> objs = (ArrayList<ArrayList<Serializable>>) buf;

		// Remove any objects that are in the unpack area (overwrite the area)
		// shift the rect with local coordinates back to global coordinates
		for (final IntRect2D rect : mp.rects)
			removeObjects(rect.shift(shape.ul().toArray()));

		for (int k = 0; k < mp.rects.size(); k++)
			for (int i = 0; i < objs.get(k).size(); i += 2)
				addObject(
						//// FIXME: This looks VERY inefficient, with lots of array allocations
						((Double2D) objs.get(k).get(i + 1)).add(mp.rects.get(k).ul().toArray())
								.add(shape.ul().toArray()),
						(T) objs.get(k).get(i));

		int sum = 0;
		for (int i = 0; i < objs.size(); i++) {
			sum += objs.get(i).size();
		}

		return sum;
	}

	public boolean checkNull() {

		for (int i = 0; i < storage.length; i++) {
			if (storage[i] == null) {
				System.out.println(i + " is null");
				return false;
			}

		}

		return true;
	}

	/**
	 * Shifts point p to give location on the local partition
	 * 
	 * @param p
	 * @return location on the local partition
	 */
	public Int2D toLocalPoint(final Int2D p) {
		return p;
	}

	/**
	 * Shifts point p to give location on the local partition
	 * 
	 * @param p
	 * @return location on the local partition
	 */
	public Double2D toLocalPoint(final Double2D p) {
		return p;
	}
}
