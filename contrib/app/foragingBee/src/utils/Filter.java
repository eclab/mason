/*
 * Bee foraging simulation. Copyright by Joerg Hoehne.
 * For suggestions or questions email me at hoehne@thinktel.de
 */

package utils;

import foragingBee.IMovingAgent;

/**
 * A class containing filter methods.
 * <p>
 * Copyright 2009 Joerg Hoehne
 * 
 * @author hoehne (<a href="mailto:hoehne@thinktel.de">J&ouml;rg H&ouml;hne</a>)
 */
public class Filter {
	/**
	 * A simple filter that returns a (new) array that contains only the
	 * specified type. The provided array will be altered! If the no object is
	 * rejected the same array is returned.
	 * 
	 * @param objects
	 * @param theClass
	 * @return The objects that meets the criteria.
	 */
	public static final Object[] filter(Object[] objects, Class<?> theClass) {
		Object o;
		int i, k;
		for (i = 0, k = 0; i < objects.length; i++) {
			o = objects[i];
			// FIXME what will happen if subclasses are used because this checks
			// for identity?
			if (o.getClass() == theClass) {
				objects[k] = o;
				k++;
			}
		}

		// assume no object has bee rejected
		Object[] filteredObjects = objects;
		// if an object has been rejected create new array and copy information
		if (i != k) {
			filteredObjects = new IMovingAgent[k];
			System.arraycopy(objects, 0, filteredObjects, 0, k);
		}

		// return the filtered array
		return filteredObjects;
	}
}
