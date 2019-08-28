/*
  Copyright 2010 by Sean Luke and George Mason University
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
 * <p>This code uses doubles as keys; as opposed to sim.util.Heap, which uses
 * Comparables as keys.
 *
 * @author Liviu Panait
 * @version 1.0 
 */


public class DoubleHeap implements java.io.Serializable
    {
    // the keys
    double[] keys = null;

    // the information associated with the keys
    Object[] objects = null;

    int numElem = 0;

    // constructs the heap
    public DoubleHeap()
        {
        this(new double[0], new Object[0], 0);
        }

    // constructs the heap
    public DoubleHeap( double[] keys, Object[] objects, int numElem )
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
        double[] keys = this.keys;
        
        while( true )
            {
            int l = 2*i;
            int r = 2*i+1;
            int smallest;
            if( l <= heapsize && keys[l-1] < keys[i-1] )
                smallest = l;
            else
                smallest = i;
            if( r <= heapsize && keys[r-1] < keys[smallest-1] )
                smallest = r;
            if( smallest != i )
                {
                // swap keys
                double tempkey = keys[i-1];
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

    /** Returns the key value of the current min element.  Does not extract the element. */
    public double getMinKey()
        {
        return keys[1-1];
        }

    /** Removes the minimum element and its key from the heap, and returns the minimum element.  Will return null if the heap is empty */
    public Object extractMin()
        {
        // make local
        int numElem = this.numElem;
        Object[] objects = this.objects;
        double[] keys = this.keys;
        
        if( numElem == 0 )
            return null;
        // remove the key
        keys[1-1] = keys[numElem-1];
        keys[numElem-1] = 0;
        // remove the info
        Object result = objects[1-1];
        objects[1-1] = objects[numElem-1];
        objects[numElem-1]=null;
        numElem--;
        // rebuild heap
        heapify( 1, numElem );
        // return the info with min key (which was also removed from the heap)
        
        // put back
        this.numElem = numElem;
        return result;
        }

    /** Adds an element to the heap with the given key. */
    public void add( Object elem, double key )
        {
        // make local
        int numElem = this.numElem;
        Object[] objects = this.objects;
        double[] keys = this.keys;
                
        numElem++;
        if( (numElem-1) >= objects.length )
            {
            Object[] temp = new Object[ objects.length * 2 + 1];
            System.arraycopy( objects, 0, temp, 0, objects.length );
            objects = temp;
            double[] temptemp = new double[ keys.length * 2 + 1];
            System.arraycopy( keys, 0, temptemp, 0, keys.length );
            keys = temptemp;

            // objects and keys may have changed
            this.objects = objects;
            this.keys = keys;
            }
        int i = numElem;
        while ( i > 1 && keys[i/2-1] > key )
            {
            objects[i-1] = objects[i/2-1];
            keys[i-1] = keys[i/2-1];
            i = i/2;
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
        numElem = 0;
        }

    }
