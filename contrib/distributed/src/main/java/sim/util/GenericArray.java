package sim.util;

import java.io.Serializable;
import java.util.Arrays;


public class GenericArray<E> implements Serializable
{
    private final Object[] arr;
    public final int length;
 
    // constructor
    public GenericArray(int length)
    {
        // Creates a new object array of the specified length
        arr = new Object[length];
        this.length = length;
    }
 
    // Method to get object present at index `i` in the array
    public E get(int i) {
        @SuppressWarnings("unchecked")
        final E e = (E)arr[i];
        return e;
    }
 
    // Method to set a value `e` at index `i` in the array
    public void set(int i, E e) {
        arr[i] = e;
    }
    
    public Object[] getArr() {
    	return arr;
    }
 
    @Override
    public String toString() {
        return Arrays.toString(arr);
    }
}