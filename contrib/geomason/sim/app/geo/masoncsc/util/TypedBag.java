/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package masoncsc.util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;

import sim.util.Indexed;

/** Maintains a simple array (objs) of Objects and the number of objects (numObjs) in the array
    (the array can be bigger than this number).  Unlike Vector or ArrayList, TypedBag is designed
    to encourage direct access of the array.  If you access the objects directly, they are
    stored in positions [0 ... numObjs-1].  If you wish to extend the array, you should call
    the resize method.
    
    <p>By providing direct access to the array, Bags are about three and a half times faster than ArrayLists
    (whose get/set methods unfortunately at present contain un-inlinable range bounds checks) and four times faster 
    than Vectors (whose methods additionally are synchronized).  Even Bag's built-in get() and set() methods, 
    complete with range bounds checks, are twice the speed of ArrayLists.  To get faster 
    than a Bag, you'd have to go to a raw fixed-length array of the specific class type of your objects.
    Accessing a Bag's Object array and casting its Objects into the appropriate class is about 50% slower
    than accessing a fixed-length array of that class in the first place.
    
    <p>TypedBag is not synchronized, and so should not be accessed from different threads without locking on it
    or some appropriate lock object first.  TypedBag also has an unusual, fast method for removing objects
    called remove(...), which removes the object simply by swapping the topmost object into its
    place.  This means that after remove(...) is called, the TypedBag may no longer have the same order
    (hence the reason it's called a "TypedBag" rather than some variant on "Vector" or "Array" or "List").  You can
    guarantee order by calling removeNondestructively(...) instead if you wish, but this is O(n) in the worst case.

    <p>Bags provide iterators but you are strongly encouraged to just access the array instead.  Iterators
    are slow.  TypedBag's iterator performs its remove operation by calling removeNondestructively().  
    Like array access, iterator usage is undefined if objects are placed into the TypedBag or 
    removed from the TypedBag in the middle of the iterator usage (except by using the iterator's remove 
    operation of course).
*/

public class TypedBag<T> extends ArrayList<T> implements Indexed
    {
    private static final long serialVersionUID = 1;
        
    /** Returns null if the TypedBag is empty, else returns the topmost object. */
    public T top()
        {
        if (isEmpty()) return null;
        else return this.get(size() - 1);
        }
    
    /** Returns null if the TypedBag is empty, else removes and returns the topmost object. */
    public T pop()
        {
    	if (isEmpty())
    		return null;
    	
    	return this.remove(size() - 1);
        }
    
    /** Synonym for add(obj) -- stylistically, you should add instead unless you
        want to think of the TypedBag as a stack. */
    public boolean push(T obj)
        {
    	return add(obj);
        }
        
    public boolean add(T obj)
        {
    	return add(obj);
        }
      
    /** identical to get(index) */
    public T getValue(int index)
        {
    	return get(index);
        }

    /** identical to set(index, element) */
    @SuppressWarnings("unchecked")
	public T setValue(int index, Object element)
        {
    	return set(index, (T)element);
        }

    /** Removes the object at the given index, shifting the other objects down. */
    public T removeNondestructively(int index)
        {
    	return remove(index);
        }
    
    /** Removes the object, shifting the other objects down. */
    public boolean removeNondestructively(T o)
        {
    	return remove(o);
        }
        
    /** Removes multiple instantiations of an object */
    public boolean removeMultiply(T o)
        {
        boolean flag = false;
        Iterator<T> iter = this.iterator();
        while (iter.hasNext()) 
        	{
        	T item = iter.next();
        	if (o == null ? item == null : o==item || o.equals(item)) 
        		{
        		iter.remove();
        		flag = true;
        		}
        	}
        return flag;
        }


    /**    
        Copies 'len' elements from the TypedBag into the provided array.
        The 'len' elements start at index 'fromStart' in the TypedBag, and
        are copied into the provided array starting at 'toStat'.
    */ 
    public void copyIntoArray(int fromStart, T[] to, int toStart, int len)
        {
		T[] array = (T[])this.toArray();
        System.arraycopy(array, fromStart, to, toStart, len);
        }
    
    /** Always returns null.  This method is to adhere to Indexed. */
    public Class<?> componentType()
        {
        return null;
        }

    /** Sorts the bag according to the provided comparator */
    /*public void sort(Comparator<T> c)
        {
    	Collections.sort(this, c);
        }*/

    /** Sorts the bag under the assumption that all objects stored within are Comparable. */
    public void sort() 
        {
    	Collections.sort(this, new Comparator<T>() {
			public int compare(T arg0, T arg1) {
				return ((Comparable)arg0).compareTo(arg1);
				}
			});
        }

    /** Replaces all elements in the bag with the provided object. */
    public void fill(T o)
        {
//        // teeny bit faster
//        T[] objs = this.objs;
//        int numObjs = this.size();
//        
//        for(int x=0; x < numObjs; x++)
//            objs[x] = o;
//        
        for (T t : this)
        	t = o;
        }

    /** Shuffles (randomizes the order of) the TypedBag */
    public void shuffle(Random random)
        {
//        // teeny bit faster
//        T[] objs = this.objs;
//        int numObjs = this.numObjs;
//        T obj;
//        int rand;
//        
//        for(int x=numObjs-1; x >= 1 ; x--)
//            {
//            rand = random.nextInt(x+1);
//            obj = objs[x];
//            objs[x] = objs[rand];
//            objs[rand] = obj;
//            }
//        
        Collections.shuffle(this, random);
        }
    
    /** Shuffles (randomizes the order of) the TypedBag */
    public void shuffle(ec.util.MersenneTwisterFast random)
        {
        int numObjs = this.size();
        T obj;
        int rand;
        
        for(int x=numObjs-1; x >= 1 ; x--)
            {
            rand = random.nextInt(x+1);
            obj = get(x);        
            this.set(x, this.get(rand));
            this.set(rand, obj);
            }
        }
    
    /** Reverses order of the elements in the TypedBag */
    public void reverse()
        {
        int numObjs = this.size();
        int l = size() / 2;
        T obj;
        for(int x=0; x < l; x++)
            {
            obj = get(x);
            set(x, get(numObjs - x - 1));
            set(numObjs - x - 1, obj);
            }
        }
    }
