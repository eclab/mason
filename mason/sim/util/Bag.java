/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;
import java.util.*;

/** Maintains a simple array (objs) of Objects and the number of objects (numObjs) in the array
    (the array can be bigger than this number).  Unlike Vector or ArrayList, Bag is designed
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
    
    <p>Bag is not synchronized, and so should not be accessed from different threads without locking on it
    or some appropriate lock object first.  Bag also has an unusual, fast method for removing objects
    called remove(...), which removes the object simply by swapping the topmost object into its
    place.  This means that after remove(...) is called, the Bag may no longer have the same order
    (hence the reason it's called a "Bag" rather than some variant on "Vector" or "Array" or "List").  You can
    guarantee order by calling removeNondestructively(...) instead if you wish, but this is O(n) in the worst case.

    <p>Bags provide iterators but you are strongly encouraged to just access the array instead.  Iterators
    are slow.  Bag's iterator performs its remove operation by calling removeNondestructively().  
    Like array access, iterator usage is undefined if objects are placed into the Bag or 
    removed from the Bag in the middle of the iterator usage (except by using the iterator's remove 
    operation of course).
*/

public class Bag implements java.util.Collection, java.io.Serializable, Cloneable, Indexed
    {
    public Object[] objs;
    public int numObjs;
    
    public Bag() { numObjs = 0; objs = new Object[1]; }
    
    /** Creates a Bag with a given initial capacity. */
    public Bag(int capacity) { numObjs = 0; objs = new Object[capacity]; }
        
    /** Adds the objects from the other Bag without copying them.  The size of the
        new Bag is the minimum necessary size to hold the objects. */
    public Bag(final Bag other)
        {
        if (other==null) { numObjs = 0; objs = new Object[1]; }
        numObjs = other.numObjs;
        objs = new Object[numObjs];
        System.arraycopy(other.objs,0,objs,0,numObjs);
        }
    
    public int size()
        {
        return numObjs;
        }
    
    public boolean isEmpty()
        {
        return (numObjs<= 0);
        }
    
    public boolean addAll(final Collection other) 
        { 
        if (other instanceof Bag) return addAll((Bag)other);  // avoid an array build
        return addAll(numObjs, other.toArray()); 
        }

    public boolean addAll(final int index, final Collection other)
        {
        if (other instanceof Bag) return addAll(index, (Bag)other);  // avoid an array build
        return addAll(index, other.toArray());
        }

    public boolean addAll(final int index, final Object[] other)
        {
        // throws NullPointerException if other == null,
        // ArrayIndexOutOfBoundsException if index < 0,
        // and IndexOutOfBoundsException if index > numObjs
        if (index > numObjs) { throwIndexOutOfBoundsException(index); }
        if (other.length == 0) return false;
        // make Bag big enough
        if (numObjs+other.length > objs.length)
            resize(numObjs+other.length);
        if (index != numObjs)   // make room
            System.arraycopy(objs,index,objs,index+other.length,other.length);
        System.arraycopy(other,0,objs,index,other.length);
        numObjs += other.length;
        return true;
        }
    
    public boolean addAll(final Bag other) { return addAll(numObjs,other); }

    public boolean addAll(final int index, final Bag other)
        {
        // throws NullPointerException if other == null,
        // ArrayIndexOutOfBoundsException if index < 0,
        // and IndexOutOfBoundsException if index > numObjs
        if (index > numObjs) { throwIndexOutOfBoundsException(index); }
        if (other.numObjs <= 0) return false;
        // make Bag big enough
        if (numObjs+other.numObjs > objs.length)
            resize(numObjs+other.numObjs);
        if (index != numObjs)    // make room
            System.arraycopy(objs,index,objs,index+other.numObjs,other.numObjs);
        System.arraycopy(other.objs,0,objs,index,other.numObjs);
        numObjs += other.numObjs;
        return true;
        }

    public Object clone() throws CloneNotSupportedException
        {
        Bag b = (Bag)(super.clone());
        b.objs = (Object[]) objs.clone();
        return b;
        }
    
    /** Resizes the internal array to at least the requested size. */
    public void resize(int toAtLeast)
        {
        if (objs.length >= toAtLeast)  // already at least as big as requested
            return;

        if (objs.length * 2 > toAtLeast)  // worth doubling
            toAtLeast = objs.length * 2;

        // now resize
        Object[] newobjs = new Object[toAtLeast];
        System.arraycopy(objs,0,newobjs,0,numObjs);
        objs=newobjs;
        }
        
    /** Resizes the objs array to max(numObjs, desiredLength), unless that value is greater than or equal to objs.length,
        in which case no resizing is done (this operation only shrinks -- use resize() instead).
        This is an O(n) operation, so use it sparingly. */
    public void shrink(int desiredLength)
        {
        if (desiredLength < numObjs) desiredLength = numObjs;
        if (desiredLength >= objs.length) return;  // no reason to bother
        Object[] newobjs = new Object[desiredLength];
        System.arraycopy(objs,0,newobjs,0,numObjs);
        objs = newobjs;
        }
    
    /** Returns null if the Bag is empty, else returns the topmost object. */
    public Object top()
        {
        if (numObjs<= 0) return null;
        else return objs[numObjs-1];
        }
    
    /** Returns null if the Bag is empty, else removes and returns the topmost object. */
    public Object pop()
        {
        // this curious arrangement makes me small enough to be inlined (35 bytes; right at the limit)
        int numObjs = this.numObjs;
        if (numObjs<= 0) return null;
        Object ret = objs[--numObjs];
        objs[numObjs] = null; // let GC
        this.numObjs = numObjs;
        return ret;
        }
    
    /** Synonym for add(obj) -- stylistically, you should add instead unless you
        want to think of the Bag as a stack. */
    public boolean push(final Object obj)
        {
        // this curious arrangement makes me small enough to be inlined (35 bytes)
        int numObjs = this.numObjs;
        if (numObjs >= objs.length) doubleCapacityPlusOne();
        objs[numObjs] = obj;
        this.numObjs = numObjs+1;
        return true;
        }
        
    public boolean add(final Object obj)
        {
        // this curious arrangement makes me small enough to be inlined (35 bytes)
        int numObjs = this.numObjs;
        if (numObjs >= objs.length) doubleCapacityPlusOne();
        objs[numObjs] = obj;
        this.numObjs = numObjs+1;
        return true;
        }
        
    // private function used by add and push in order to get them below
    // 35 bytes -- always doubles the capacity and adds one
    void doubleCapacityPlusOne()
        {
        Object[] newobjs = new Object[numObjs*2+1];
        System.arraycopy(objs,0,newobjs,0,numObjs);
        objs=newobjs;
        }

    public boolean contains(final Object o)
        {
        int numObjs = this.numObjs;
        Object[] objs = this.objs;
        for(int x=0;x<numObjs;x++)
            if (o==null ?  objs[x]==null :  o==objs[x] || o.equals(objs[x])) return true;
        return false;
        }
        
    public boolean containsAll(final Collection c)
        {
        Iterator iterator = c.iterator();
        while(iterator.hasNext())
            if (!contains(iterator.next())) return false;
        return true;
        }

    public Object get(final int index)
        {
        if (index>=numObjs) // || index < 0)
            throwIndexOutOfBoundsException(index);
        return objs[index];
        }

    /** identical to get(index) */
    public Object getValue(final int index)
        {
        if (index>=numObjs) // || index < 0)
            throwIndexOutOfBoundsException(index);
        return objs[index];
        }

    public Object set(final int index, final Object element)
        {
        if (index>=numObjs) // || index < 0)
            throwIndexOutOfBoundsException(index);
        Object returnval = objs[index];
        objs[index] = element;
        return returnval;
        }

    /** identical to set(index, element) */
    public Object setValue(final int index, final Object element)
        {
        if (index>=numObjs) // || index < 0)
            throwIndexOutOfBoundsException(index);
        Object returnval = objs[index];
        objs[index] = element;
        return returnval;
        }

    public boolean removeAll(final Collection c)
        {
        boolean flag = false;
        Iterator iterator = c.iterator();
        while(iterator.hasNext())
            if (remove(iterator.next())) flag = true;
        return flag;
        }

    public boolean retainAll(final Collection c)
        {
        boolean flag = false;
        for(int x=0;x<numObjs;x++)
            if (!c.contains(objs[x]))
                {
                flag = true;
                remove(x);
                x--; // consider the newly-swapped-in item
                }
        return flag;
        }

    /** Removes the object at the given index, shifting the other objects down. */
    public Object removeNondestructively(final int index)
        {
        if (index>=numObjs) // || index < 0)
            throwIndexOutOfBoundsException(index);
        Object ret = objs[index];
        if (index < numObjs - 1)  // it's not the topmost object, must swap down
            System.arraycopy(objs, index+1, objs, index, numObjs - index - 1);
        objs[numObjs-1] = null;  // let GC
        numObjs--;
        return ret;
        }
    
    /** Removes the object, moving the topmost object into its position. */
    public boolean remove(final Object o)
        {
        int numObjs = this.numObjs;
        Object[] objs = this.objs;
        for(int x=0;x<numObjs;x++)
            if (o==null ?  objs[x]==null :  o==objs[x] || o.equals(objs[x])) 
                {
                remove(x);
                return true;
                }
        return false;
        }
        
    /** Removes multiple instantiations of an object */
    public boolean removeMultiply(final Object o)
        {
        int numObjs = this.numObjs;
        Object[] objs = this.objs;
        boolean flag = false;
        for(int x=0;x<numObjs;x++)
            if (o==null ?  objs[x]==null :  o==objs[x] || o.equals(objs[x])) 
                {
                flag = true;
                remove(x);
                x--;  // to check the next item swapped in...
                }
        return flag;
        }

    /** Removes the object at the given index, moving the topmost object into its position. */
    public Object remove(final int index)
        {
        int _numObjs = numObjs;
        if (index >= _numObjs) // || index < 0)
            throwIndexOutOfBoundsException(index);
        Object[] _objs = this.objs;
        Object ret = _objs[index];
        _objs[index] = _objs[_numObjs-1];
        _objs[_numObjs-1] = null;  // let GC
        numObjs--;
        return ret;
        }
        
    protected void throwIndexOutOfBoundsException(final int index)
        {
        throw new IndexOutOfBoundsException(""+index);
        }

    /** Removes all objects in the Bag.  This is done by clearing the internal array but 
        not replacing it with a new, smaller one. */
    public void clear()
        {
        // local variables are faster
        int len = numObjs;
        Object[] o = objs;
        
        for(int i = 0; i < len; i++)
            o[i] = null;  // let GC
                
        numObjs = 0;
        }
        
    public Object[] toArray()
        {
        Object[] o = new Object[numObjs];
        System.arraycopy(objs,0,o,0,numObjs);
        return o;
        }
    
    // ArrayList.toArray(Object[]) generates an error if the array passed in is null.
    // So I do the same thing.
    public Object[] toArray(Object[] o)
        {
        if (o.length < numObjs)
            // make a new array of same type
            o = (Object[]) java.lang.reflect.Array.newInstance(o.getClass().getComponentType(), numObjs);
        // load it up
        System.arraycopy(objs,0,o,0,numObjs);
        // insert null if we need it
        if (o.length > numObjs)
            o[numObjs] = null;
        return null;
        }

    /** NOT fail-fast.  Use this method only if you're
        concerned about accessing numObjs and objs directly.  */
    public Iterator iterator()
        {
        return new BagIterator(this);
        }
    
    /** Always returns null.  This method is to adhere to Indexed. */
    public Class componentType()
        {
        return null;
        }

    /** Sorts the bag according to the provided comparator */
    public void sort(Comparator c) 
        {
        Arrays.sort(objs, 0, numObjs, c);
        }

    /** Replaces all elements in the bag with the provided object. */
    public void fill(Object o)
        {
        // teeny bit faster
        Object[] objs = this.objs;
        int numObjs = this.numObjs;
        
        for(int x=0; x < numObjs; x++)
            objs[x] = o;
        }

    /** Shuffles (randomizes the order of) the Bag */
    public void shuffle(Random random)
        {
        // teeny bit faster
        Object[] objs = this.objs;
        int numObjs = this.numObjs;
        Object obj;
        int rand;
        
        for(int x=numObjs-1; x >= 1 ; x--)
            {
            rand = random.nextInt(x+1);
            obj = objs[x];
            objs[x] = objs[rand];
            objs[rand] = obj;
            }
        }
    
    /** Shuffles (randomizes the order of) the Bag */
    public void shuffle(ec.util.MersenneTwisterFast random)
        {
        // teeny bit faster
        Object[] objs = this.objs;
        int numObjs = this.numObjs;
        Object obj;
        int rand;
        
        for(int x=numObjs-1; x >= 1 ; x--)
            {
            rand = random.nextInt(x+1);
            obj = objs[x];
            objs[x] = objs[rand];
            objs[rand] = obj;
            }
        }
    
    /** Reverses order of the elements in the Bag */
    public void reverse()
        {
        // teeny bit faster
        Object[] objs = this.objs;
        int numObjs = this.numObjs;
        int l = numObjs / 2;
        Object obj;
        for(int x=0; x < l; x++)
            {
            obj = objs[x];
            objs[x] = objs[numObjs - x - 1];
            objs[numObjs - x - 1] = obj;
            }
        }

    static class BagIterator implements Iterator, java.io.Serializable
        {
        int obj = 0;
        Bag bag;
        boolean canRemove = false;
        
        public BagIterator(Bag bag) { this.bag = bag; }
        
        public boolean hasNext()
            {
            return (obj < bag.numObjs);
            }
        public Object next()
            {
            if (obj >= bag.numObjs) throw new NoSuchElementException("No More Elements");
            canRemove = true;
            return bag.objs[obj++];
            }
        public void remove()
            {
            if (!canRemove) throw new IllegalStateException("remove() before next(), or remove() called twice");
            // more consistent with the following line than 'obj > bag.numObjs' would be...
            if (obj - 1 >=  bag.numObjs) throw new NoSuchElementException("No More Elements");
            bag.removeNondestructively(obj-1);
            obj--;
            canRemove = false;
            }
        // static inner class -- no need to add a serialVersionUID
        }
    }
