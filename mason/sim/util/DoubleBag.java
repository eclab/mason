/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;

/** Maintains a simple array (objs) of doubles and the number of doubles (numObjs) in the array
    (the array can be bigger than this number).  You are encouraged to access the doubles directly;
    they are stored in positions [0 ... numObjs-1].  If you wish to extend the array, you should call
    the resize method.
    
    <p>DoubleBag is approximately to double what Bag is to Object.  However, for obvious reasons, DoubleBag is not
    a java.util.Collection subclass and is purposely simple (it doesn't have an Iterator for example).
    
    <p>DoubleBag is not synchronized, and so should not be accessed from different threads without locking on it
    or some appropriate lock double first.  DoubleBag also has an unusual, fast method for removing doubles
    called remove(...), which removes the double simply by swapping the topmost double into its
    place.  This means that after remove(...) is called, the DoubleBag may no longer have the same order
    (hence the reason it's called a "DoubleBag" rather than some variant on "Vector" or "Array" or "List").  You can
    guarantee order by calling removeNondestructively(...) instead if you wish, but this is O(n) in the worst case.
*/

public class DoubleBag implements java.io.Serializable, Cloneable, Indexed
    {
    public double[] objs;
    public int numObjs;
    
    /** Creates a DoubleBag with a given initial capacity. */
    public DoubleBag(int capacity) { numObjs = 0; objs = new double[capacity]; }

    public DoubleBag() { numObjs = 0; objs = new double[1]; }
    
    /** Adds the doubles from the other DoubleBag without copying them.  The size of the
        new DoubleBag is the minimum necessary size to hold the doubles. */
    public DoubleBag(final DoubleBag other)
        {
        if (other==null) { numObjs = 0; objs = new double[1]; }
        numObjs = other.numObjs;
        objs = new double[numObjs];
        System.arraycopy(other.objs,0,objs,0,numObjs);
        }
    
    public int size()
        {
        return numObjs;
        }
    
    public boolean isEmpty()
        {
        return (numObjs<=0);
        }
    
    public boolean addAll(final int index, final double[] other)
        {
        // throws NullPointerException if other == null,
        // ArrayIndexOutOfBoundsException if index < 0,
        // and IndexOutOfBoundsException if index > numObjs
        if (index > numObjs) { throwIndexOutOfBoundsException(index); }
        if (other.length == 0) return false;
        // make DoubleBag big enough
        if (numObjs+other.length > objs.length)
            resize(numObjs+other.length);
        if (index != numObjs)   // make room
            System.arraycopy(objs,index,objs,index+other.length,other.length);
        System.arraycopy(other,0,objs,index,other.length);
        numObjs += other.length;
        return true;
        }
    
    public boolean addAll(final DoubleBag other) { return addAll(numObjs,other); }

    public boolean addAll(final int index, final DoubleBag other)
        {
        // throws NullPointerException if other == null,
        // ArrayIndexOutOfBoundsException if index < 0,
        // and IndexOutOfBoundsException if index > numObjs
        if (index > numObjs) { throwIndexOutOfBoundsException(index); }
        if (other.numObjs <= 0) return false;
        // make DoubleBag big enough
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
        DoubleBag b = (DoubleBag)(super.clone());
        b.objs = (double[]) objs.clone();
        return b;
        }
        
    public void resize(int toAtLeast)
        {
        if (objs.length >= toAtLeast)  // already at least as big as requested
            return;

        if (objs.length * 2 > toAtLeast)  // worth doubling
            toAtLeast = objs.length * 2;

        // now resize
        double[] newobjs = new double[toAtLeast];
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
        double[] newobjs = new double[desiredLength];
        System.arraycopy(objs,0,newobjs,0,numObjs);
        objs = newobjs;
        }
    
    
    /** Returns 0 if the DoubleBag is empty, else returns the topmost double. */
    public double top()
        {
        if (numObjs<=0) return 0;
        else return objs[numObjs-1];
        }
    
    /** Returns 0 if the DoubleBag is empty, else removes and returns the topmost double. */
    public double pop()
        {
        // this curious arrangement makes me small enough to be inlined (35 bytes; right at the limit)
        int numObjs = this.numObjs;
        if (numObjs<=0) return 0;
        double ret = objs[--numObjs];
        this.numObjs = numObjs;
        return ret;
        }
    
    /** Synonym for add(obj) -- try to use add instead unless you
        want to think of the DoubleBag as a stack. */
    public boolean push(final double obj)
        {
        // this curious arrangement makes me small enough to be inlined (33 bytes)
        int numObjs = this.numObjs;
        if (numObjs >= objs.length) doubleCapacityPlusOne();
        objs[numObjs] = obj;
        this.numObjs = numObjs + 1;
        return true;
        }
        
    public boolean add(final double obj)
        {
        // this curious arrangement makes me small enough to be inlined (33 bytes)
        int numObjs = this.numObjs;
        if (objs.length <= numObjs) doubleCapacityPlusOne();
        objs[numObjs] = obj;
        this.numObjs = numObjs + 1;
        return true;
        }
    
    // private function used by add and push in order to get them below
    // 35 bytes -- always doubles the capacity and adds one
    void doubleCapacityPlusOne()
        {
        double[] newobjs = new double[numObjs*2+1];
        System.arraycopy(objs,0,newobjs,0,numObjs);
        objs=newobjs;
        }

    public boolean contains(final double o)
        {
        int numObjs = this.numObjs;
        double[] objs = this.objs;
        for(int x=0;x<numObjs;x++)
            if (o==objs[x]) return true;
        return false;
        }
        
    public double get(final int index)
        {
        if (index>=numObjs) //  || index < 0)
            throwIndexOutOfBoundsException(index);
        return objs[index];
        }

    public Object getValue(final int index)
        {
        return new Double(get(index));
        }

    public double set(final int index, final double element)
        {
        if (index>=numObjs) // || index < 0)
            throwIndexOutOfBoundsException(index);
        double returnval = objs[index];
        objs[index] = element;
        return returnval;
        }

    public Object setValue(final int index, final Object value)
        {
        Double old = new Double(get(index));
        Double newval = null;
        try { newval = (Double)value; }
        catch (ClassCastException e) { throw new IllegalArgumentException("Expected a Double"); }
        set(index,newval.doubleValue());
        return old;
        }

    /** Removes the double at the given index, shifting the other doubles down. */
    public double removeNondestructively(final int index)
        {
        if (index>=numObjs) // || index < 0)
            throwIndexOutOfBoundsException(index);
        double ret = objs[index];
        if (index < numObjs - 1)  // it's not the topmost double, must swap down
            System.arraycopy(objs, index+1, objs, index, numObjs - index - 1);
        numObjs--;
        return ret;
        }
    
    /** Removes the double at the given index, moving the topmost double into its position. */
    public double remove(final int index)
        {
        int _numObjs = numObjs;
        if (index>=_numObjs) // || index < 0)
            throwIndexOutOfBoundsException(index);
        double[] _objs = this.objs;
        double ret = _objs[index];
        _objs[index] = _objs[_numObjs-1];
        numObjs--;
        return ret;
        }
        
    /** Sorts the doubles into ascending numerical order. */
    public void sort() {java.util.Arrays.sort(objs, 0, numObjs);}


    /** Replaces all elements in the bag with the provided object. */
    public void fill(double o)
        {
        // teeny bit faster
        double[] objs = this.objs;
        int numObjs = this.numObjs;
        
        for(int x=0; x < numObjs; x++)
            objs[x] = o;
        }

    /** Shuffles (randomizes the order of) the DoubleBag */
    public void shuffle(java.util.Random random)
        {
        // teeny bit faster
        double[] objs = this.objs;
        int numObjs = this.numObjs;
        double obj;
        int rand;
        
        for(int x=numObjs-1; x >= 1 ; x--)
            {
            rand = random.nextInt(x+1);
            obj = objs[x];
            objs[x] = objs[rand];
            objs[rand] = obj;
            }
        }
    
    /** Shuffles (randomizes the order of) the DoubleBag */
    public void shuffle(ec.util.MersenneTwisterFast random)
        {
        // teeny bit faster
        double[] objs = this.objs;
        int numObjs = this.numObjs;
        double obj;
        int rand;
        
        for(int x=numObjs-1; x >= 1 ; x--)
            {
            rand = random.nextInt(x+1);
            obj = objs[x];
            objs[x] = objs[rand];
            objs[rand] = obj;
            }
        }
    
    /** Reverses order of the elements in the DoubleBag */
    public void reverse()
        {
        // teeny bit faster
        double[] objs = this.objs;
        int numObjs = this.numObjs;
        int l = numObjs / 2;
        double obj;
        for(int x=0; x < l; x++)
            {
            obj = objs[x];
            objs[x] = objs[numObjs - x - 1];
            objs[numObjs - x - 1] = obj;
            }
        }

    protected void throwIndexOutOfBoundsException(final int index)
        {
        throw new IndexOutOfBoundsException(""+index);
        }
        
    /** Removes all numbers in the DoubleBag.  This is done by clearing the internal array but 
        not replacing it with a new, smaller one. */
    public void clear()
        {
        numObjs = 0;
        }
        
    public double[] toArray()
        {
        double[] o = new double[numObjs];
        System.arraycopy(objs,0,o,0,numObjs);
        return o;
        }
        
    public Double[] toDoubleArray()
        {
        Double[] o = new Double[numObjs];
        for(int i = 0; i < numObjs; i++)
            o[i] = new Double(objs[i]);
        return o;
        }

    public Class componentType()
        {
        return Double.TYPE;
        }
    }
