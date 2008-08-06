/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.util;

/* 
 * HeapSort.java
 * 
 * Created: Fri Jan 25 2002
 * By: Liviu Panait
 */

/**
 * Implementations of Heap functions in Java.  This code is derived
 * from the HeapSort example algorithm in <i>Introduction to algorithms</i> by
 * Cormen, Leiserson and Rivest.  Intentionally very simple.
 *
 * @author Liviu Panait and Sean Luke
 * @version 2.0 
 */


public class Heap implements java.io.Serializable
    {
    // the keys
    Comparable[] keys = null;

    // the information associated with the keys
    Object[] objects = null;

    int numElem = 0;

    // constructs the heap
    public Heap()
        {
        this(new Comparable[0], new Object[0], 0);
        }

    // constructs the heap
    public Heap( Comparable[] keys, Object[] objects, int numElem )
        {
        if (keys.length != objects.length)
            throw new IllegalArgumentException("keys and objects must be of the same length");
        this.keys = keys;
        this.objects = objects;
        this.numElem = numElem;
        buildHeap();
        }

    // builds the heap
    void buildHeap()
        {
        for( int i = numElem/2 ; i >= 1 ; i-- )
            heapify( i, numElem );
        }
        
    void heapify( int i, int heapsize )
        {
        // make local
        Object[] objects = this.objects;
        Comparable[] keys = this.keys;
        
        while( true )
            {
            int l = 2*i;
            int r = 2*i+1;
            int smallest;
            if( l <= heapsize && keys[l-1].compareTo(keys[i-1]) < 0 )    //keys[l-1] < keys[i-1] )
                smallest = l;
            else
                smallest = i;
            if( r <= heapsize && keys[r-1].compareTo(keys[smallest-1]) < 0)    // keys[r-1] < keys[smallest-1] )
                smallest = r;
            if( smallest != i )
                {
                // swap keys
                Comparable tempkey = keys[i-1];
                keys[i-1] = keys[smallest-1];
                keys[smallest-1] = tempkey;
                // swap info
                Object temp = objects[i-1];
                objects[i-1] = objects[smallest-1];
                objects[smallest-1] = temp;
                // recursive call.... :)
                i = smallest;
                }
            else
                return;
            }
        }

    /** Returns the key value of the current min element.  Returns null if there is no such element.  Does not extract the element. */
    public Comparable getMinKey()
        {
        if (numElem == 0) return null;
        return keys[1-1];
        }
        
    /** Removes elements in order and adds them to a Bag, so long as the provided
        Comparable object is equal to their keys.  As soon as this is not true, the Bag is returned.
        You may provide a Bag -- putInHere -- to be filled in. */
    public Bag extractMin(Comparable comparable, Bag putInHere)
        {
        if (putInHere == null) putInHere = new Bag();
        while( true )
            {
            Comparable comp = getMinKey();
            if (comp == null || // ran out
                comparable.compareTo(comp) != 0)  // not the same value
                return putInHere;
            putInHere.add(extractMin());
            }
        }
                
    /** Removes all key-equal minimum elements and adds them to a Bag, which is then is returned.
        You may provide a Bag -- putInHere -- to be filled in. */
    public Bag extractMin(Bag putInHere)
        {
        Comparable min = getMinKey();
        if (min==null)
            {
            if (putInHere == null) return new Bag(0);
            else return putInHere;
            }
                
        if (putInHere==null) putInHere = new Bag();
        putInHere.add(extractMin());
        return extractMin(min, putInHere);
        }

    /** Removes the minimum element and its key from the heap, and returns the minimum element.  Will return null if the heap is empty */
    public Object extractMin()
        {
        // make local
        int numElem = this.numElem;
        Object[] objects = this.objects;
        Comparable[] keys = this.keys;
        
        if( numElem == 0 )
            return null;
        // remove the key
        keys[1-1] = keys[numElem-1];
        keys[numElem-1] = null;           // 0;
        // remove the info
        Object result = objects[1-1];
        objects[1-1] = objects[numElem-1];
        objects[numElem-1] = null;
        numElem--;
        // rebuild heap
        if (numElem > 1) heapify( 1, numElem );    // no need to heapify if there's only zero or one element!
        // return the info with min key (which was also removed from the heap)
        
        // put back
        this.numElem = numElem;
        return result;
        }

    /** Adds an element to the heap with the given key. */
    public void add( Object elem, Comparable key )
        {
        // make local
        int numElem = this.numElem;
        Object[] objects = this.objects;
        Comparable[] keys = this.keys;
                
        numElem++;
        if( (numElem-1) >= objects.length )
            {
            Object[] temp = new Object[ objects.length * 2 + 1];
            System.arraycopy( objects, 0, temp, 0, objects.length );
            objects = temp;
            Comparable[] temptemp = new Comparable[ keys.length * 2 + 1];
            System.arraycopy( keys, 0, temptemp, 0, keys.length );
            keys = temptemp;

            // objects and keys may have changed
            this.objects = objects;
            this.keys = keys;
            }
        int i = numElem;

        if (i > 1)  // no need to bubble up if there's only zero or one element!
            {
            while ( i > 1 &&  key.compareTo(keys[i/2-1]) < 0 )    // keys[i/2-1] > key )
                {
                objects[i-1] = objects[i/2-1];
                keys[i-1] = keys[i/2-1];
                i = i/2;
                }
            }
        keys[i-1] = key;
        objects[i-1] = elem;
        
        // put back
        this.numElem = numElem;
        }

    public boolean isEmpty()
        {
        return (numElem==0);
        }

    public void clear()
        {
        int len = numElem;
                
        // let go of the objects so they GC... perhaps we should just replace the arrays?  dunno.
        Object[] objects = this.objects;
        Comparable[] keys = this.keys;
        for(int x=0;x<len;x++)
            {
            objects[x] = null;
            keys[x] = null;
            }
                        
        numElem = 0;
        }

    }
