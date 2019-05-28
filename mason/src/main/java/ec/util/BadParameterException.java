/*
  Copyright 2006 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/


package ec.util;

/*
 * BadParameterException.java
 * Created: Sat Aug  7 12:06:49 1999
 */


/**
 *
 * Thrown when you attempt to create a Parameter from bad path items.
 *
 * @author Sean Luke
 * @version 1.0
 */

public class BadParameterException extends RuntimeException
    {
    public BadParameterException(String s) { super("\n"+s); }
    }
