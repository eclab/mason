/*
  Copyright 2021 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

	
package sim.util;

import java.io.Serializable;
import java.util.Arrays;


/** A wrapper class for a fixed-length array of type E.
	Why is this needed?  Because Java's generics are largely
	broken with respect to arrays: you cannot easily make
	or copy arrays of a generic type.  This allows us to get
	around it in certain situations. */
	
public class GenericArray<E> implements Serializable
{
    Object[] arr;
    public final int length;
 
    public GenericArray(int length)
    {
        // Creates a new object array of the specified length
        arr = new Object[length];
        this.length = length;
    }
    
    public int size()
    	{
    	return length;
    	}
 
    public E get(int i) 
    	{
        @SuppressWarnings("unchecked")
        final E e = (E)arr[i];
        return e;
    }
 
    public void set(int i, E e) 
    	{
        arr[i] = e;
    	}
    
    public Object[] getArray() 
    	{
    	return arr;
    	}
 
    public String toString() 
    	{
        return Arrays.toString(arr);
    	}
}