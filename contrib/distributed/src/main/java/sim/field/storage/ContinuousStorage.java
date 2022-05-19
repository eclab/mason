/*
  Copyright 2022 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/
        
package sim.field.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import sim.engine.DObject;
import sim.util.Double2D;
import sim.util.Int2D;
import sim.util.IntRect2D;
import sim.util.MPIParam;
import sim.util.Number2D;

public class ContinuousStorage<T extends DObject> extends GridStorage<T>
    {
    private static final long serialVersionUID = 1L;

    // note that this is the DISCRETIZED WIDTH (and the DISCRETIZED HEIGHT is height in GridStorage)
    int width;
    int discretization;
    public HashMap<Long, Double2D> locations; // id -> DObject's location
    public HashMap<Long, T>[] storage; // id -> DObject
    public boolean removeEmptyBags = true;

    public ContinuousStorage(final IntRect2D shape, int discretization)
        {
        super(shape);
        this.discretization = discretization;
        clear();
        }

    public HashMap<Long, Double2D> getLocations()
        {
        return locations;
        }

    public int getDiscretization()
        {
        return discretization;
        }

    public String toString()
        {
        final StringBuffer string = new StringBuffer(String.format("ContStorage-%s\n", shape));

        for (int x = 0; x < width; x++)
            {
            for (int y = 0; y < height; y++)
                {
                HashMap<Long, T> cell = getDiscretizedCell(x, y);
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
    public Int2D discretize(Double2D p)
        {
        double[] offsets = shape.ul().getOffsets(p);

        int[] ans = new int[2];
        for (int i = 0; i < offsets.length; i++)
            {
            ans[i] = 0 - (int) (offsets[i] / (double) discretization);
            }
        return new Int2D(ans);
        }
        
    //takes storage index and creates IntRect2D 
    public IntRect2D getCellBounds(int index)
        {
        int xLow = index / this.height;
        int yLow = index % this.height;
                
        int xHigh = xLow + 1;
        int yHigh = yLow + 1;
                
        //undiscetize
        xLow = xLow * discretization;
        yLow = yLow * discretization;
        xHigh = xHigh * discretization;
        yHigh = yHigh * discretization;
                
        //offset
        Int2D cellUL = new Int2D(xLow, yLow); //include
        Int2D cellBR = new Int2D(xHigh, yHigh);  //don't include
                
        Int2D finalCellUL = cellUL.add(shape.ul());
        Int2D finalCellBR = cellBR.add(shape.ul());
        
        //System.out.println(this.getShape()+" index "+index+"ul "+finalCellUL+"br "+finalCellBR);

        return new IntRect2D(finalCellUL,finalCellBR);
        }

    void setCelldp(final Int2D p, HashMap<Long, T> cell)
        {
        storage[getFlatIndex(p.x, p.y)] = cell;
        }

    /** Sets the given discretized cell. */
    void setCelldp(int x, int y, HashMap<Long, T> cell)
        {
        storage[getFlatIndex(x, y)] = cell;
        }

    /**
     * Sets the cell which contains the given world point. Does not check to see if
     * the point is out of bounds.
     */
    public void setCell(final Double2D p, HashMap<Long, T> cell)
        {
        setCelldp(discretize(p), cell);
        }

    /** Returns the given discretized cell. */
    HashMap<Long, T> getDiscretizedCell(final Int2D p)
        {
        return storage[getFlatIndex(p.x, p.y)];
        }

    /** Returns the given discretized cell. */
    public HashMap<Long, T> getDiscretizedCell(int x, int y)
        {
        return storage[getFlatIndex(x, y)];
        }

    /**
     * Returns the cell which contains the given world point. Does not check to see
     * if the point is out of bounds.
     */
    public HashMap<Long, T> getCell(final Double2D p)
        {
        return getDiscretizedCell(discretize(p));
        }

    /**
     * Returns the cell which contains the given world point. Does not check to see
     * if the point is out of bounds.
     */
    public HashMap<Long, T> getCell(final Number2D p)
        {
        return getDiscretizedCell(discretize(buildDouble2D(p)));
        }

    /** Returns the location of the given object. */
    public Double2D getObjectLocation(final T obj)
        {
        return locations.get(obj.ID());
        }

    /** Returns the location of the object with the given ID. */
    public Double2D getObjectLocation(final long id)
        {
        return locations.get(id);
        }

    ///// GRIDSTORAGE METHODS

    // Put the object to the given point
    public void addObject(Number2D p, T obj)
        {
        Double2D pDouble = buildDouble2D(p);
//              System.out.println("add Object: " + m + "; " + obj);
        final Double2D old = locations.put(obj.ID(), pDouble);

        if (old != null)
            getCell(old).remove(obj.ID());
        getCell(pDouble).put(obj.ID(), obj);
        }

    public T getObject(Number2D p, long id)
        {
        Double2D pDouble = buildDouble2D(p);

        HashMap<Long, T> cell = getCell(pDouble);
        if (cell == null)
            return null;
        else
            return cell.get(id);
        }

    // Get all the objects at exactly the given point
    public ArrayList<T> getAllObjects(final Number2D p)
        {
        Double2D pDouble = buildDouble2D(p);

        final ArrayList<T> objects = new ArrayList<>();
        HashMap<Long, T> cell = getCell(pDouble);

        if (cell != null)
            for (final T t : cell.values())
                if (locations.get(t.ID()).equals(pDouble))
                    objects.add(t);

        return objects;
        }

    public boolean removeObject(Number2D p, long id)
        {
        // p is ignored.

        Double2D loc = locations.remove(id);
        if (loc == null)
            return false;
        getCell(loc).remove(id);
        return true;
        }

/*
// Get all the objects at the given point
ArrayList<T> getObjects(NumberND p)
{
Double2D pDouble = buildDouble2D(p);

final ArrayList<T> objects = new ArrayList<>();

if (getCell(pDouble) != null)
for (final T t : getCell(pDouble).values())
if (locations.get(t.ID()).equals(pDouble))
objects.add(t);

return objects;
}
*/

    // Remove all the objects at the given point
    public void clear(Number2D p)
        {
        Double2D pDouble = buildDouble2D(p);
        HashMap<Long, T> cell = getCell(pDouble);

        ArrayList<Long> removeList = new ArrayList<Long>();
        for (Long key : cell.keySet())
            if (locations.get(key).equals(pDouble)) {
                removeList.add(key);
                //cell.remove(key); #concurrent exception issues
                }
                
        for (int i=0; i<removeList.size(); i++) {
            cell.remove(removeList.get(i));
            }
        }

    @SuppressWarnings("unchecked")
    public void clear()
        {  
        width = (int) Math.ceil(shape.getWidth() / (double) discretization) + 1;
        height = (int) Math.ceil(shape.getHeight() / (double) discretization) + 1;
        this.locations = new HashMap<>();

        storage = new HashMap[width * height];
        for (int i = 0; i < storage.length; i++)
            {
            storage[i] = new HashMap<>();
            }
        }

    /// METHODS FOR PACKING AND UNPACKING

    void removeObject(long id)
        {
        getCell(locations.remove(id)).remove(id);
        }

    // Remove all the objects inside the given rectangle
    void removeObjects(final IntRect2D r)
        {
        for (T obj : getObjects(r))
            {
            removeObject(obj.ID());
            }
        }

    static final Int2D OFFSET = new Int2D(1,1);
        
    // Get all the objects inside the given rectangle
    protected ArrayList<T> getObjects(final IntRect2D r)
        {
        final ArrayList<T> objs = new ArrayList<T>();
//              int[] offset = { 1, 1 };

        final Int2D ul = discretize(new Double2D(r.ul()));
        final Int2D br = discretize(new Double2D(r.br())).add(OFFSET);

        // I believe this code is just doing:

        for (int x = ul.x; x < br.x; x++)
            {
            for (int y = ul.y; y < br.y; y++)
                {

                HashMap<Long, T> cell = getDiscretizedCell(x, y);

                if (cell != null)
                    {
                    for (T obj : cell.values()) // need to offset/discretize!
                        {
                        if (r.contains(locations.get(obj.ID())))
                            {
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
    public Serializable pack(final MPIParam mp)
        {
        final ArrayList<ArrayList<Serializable>> ret = new ArrayList<>();

        for (final IntRect2D rect : mp.rects)
            {
            final ArrayList<Serializable> objs = new ArrayList<>();
            for (final T obj : getObjects(rect.add(shape.ul())))
                {
                objs.add(obj);
                // Append the object's location relative to the rectangle
                objs.add(locations.get(obj.ID()).subtract(shape.ul()).subtract(rect.ul()));
                }
            ret.add(objs);
            }

        return ret;
        }

    public void unpack(final MPIParam mp, final Serializable buf)
        {
        final ArrayList<ArrayList<Serializable>> objs = (ArrayList<ArrayList<Serializable>>) buf;

        // Remove any objects that are in the unpack area (overwrite the area)
        // shift the rect with local coordinates back to global coordinates
        for (final IntRect2D rect : mp.rects)
            removeObjects(rect.add(shape.ul()));
        
        for (int k = 0; k < mp.rects.size(); k++)
            for (int i = 0; i < objs.get(k).size(); i += 2)
                addObject(
                    //// FIXME: This looks VERY inefficient, with lots of array allocations
                    ((Double2D) objs.get(k).get(i + 1)).add(mp.rects.get(k).ul()).add(shape.ul()),
                    (T) objs.get(k).get(i));
                

/*
  int sum = 0;
  for (int i = 0; i < objs.size(); i++)
  {
  sum += objs.get(i).size();
  }
  return sum;
*/
        }

/*
  public boolean checkNull()
  {
  for (int i = 0; i < storage.length; i++)
  {
  if (storage[i] == null)
  {
  System.out.println(i + " is null");
  return false;
  }
  }
  return true;
  }
*/

    protected Double2D buildDouble2D(Number2D p)
        {
        if (p instanceof Double2D)
            return (Double2D) p;
        return new Double2D(p.getVal(0), p.getVal(1));
        }

    public Int2D toLocalPoint(final Int2D p)
        {
        return p;
        }

    public Double2D toLocalPoint(final Double2D p)
        {
        return p;
        }

    public Int2D toGlobalPoint(final Int2D p)
        {
        return p;
        }

    public Double2D toGlobalPoint(final Double2D p)
        {
        return p;
        }
    }
