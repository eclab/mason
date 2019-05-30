/*
  Copyright 2006 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/


package ec.util;
import java.io.Serializable;

/*
 * Parameter.java
 * Created: Sat Aug  7 12:06:49 1999
 */

/**
 *
 * <p>A Parameter is an object which the ParameterDatabase class
 * uses as a key to associate with strings, forming a key-value pair.
 * Parameters are designed to be hierarchical in nature, consisting
 * of "path items" separated by a path separator.
 * Parameters are created either from a single path item, from an array
 * of path items, or both.  For example, a parameter with the path
 * foo.bar.baz might be created from 
 * <tt>new Parameter(new String[] {"foo","bar","baz"})</tt>
 * 
 * <p>Parameters are not mutable -- but once a parameter is created, path 
 * items may be pushed an popped from it, forming a new parameter.
 * For example, if a parameter p consists of the path foo.bar.baz,
 * p.pop() results in a new parameter whose path is foo.bar
 * This pushing and popping isn't cheap, so be sparing.
 *
 * <p>Because this system internally uses "." as its path separator, you should
 * not use that character in parts of the path that you provide; however
 * if you need some other path separator, you can change the delimiter in
 * the code trivially.
 * In fact, you can create a new Parameter with a path foo.bar.baz simply
 * by calling <tt>new Parameter("foo.bar.baz")</tt> but you'd better know
 * what you're doing.
 *
 * <p>Additionally, parameters must not contain "#", "=", non-ascii values,
 * or whitespace.  Yes, a parameter path item may be empty.
 *
 * @author Sean Luke
 * @version 1.0
 */


public class Parameter implements Serializable
    {
    public String param;
    public static final char delimiter = '.';

    /** Creates a new parameter by joining the path items in s into a single path. */
    public Parameter(String[] s) throws ec.util.BadParameterException
        {
        if (s.length==0)
            throw new BadParameterException("Parameter created with length 0");
        for (int x=0;x<s.length;x++)
            {
            if (s[x]==null)
                throw new BadParameterException("Parameter created with null string");
            if ( x == 0) param = s[x];
            else param += ( delimiter + s[x] );
            }
        }


    /** Creates a new parameter from the single path item in s. */
    public Parameter (String s) throws BadParameterException
        {
        if (s==null)
            throw new BadParameterException("Parameter created with null string");
        param = s;
        }


    /** Creates a new parameter from the path item in s, plus the path items in s2.  s2 may be null or empty, but not s */
    public Parameter(String s, String[] s2)
        {
        if (s==null)
            throw new BadParameterException("Parameter created with null string");
        param = s;
        for (int x=0;x<s2.length;x++)
            {
            if (s2[x]==null)
                throw new BadParameterException("Parameter created with null string");
            else param += ( delimiter + s2[x] );
            }
        }



    /** Returns a new parameter with s added to the end of the current path items. 
        If s is empty, nothing is pushed on (and no delimiter is added to the end). */ 
    public Parameter push(String s)
        {
        if (s==null)
            throw new BadParameterException("Parameter pushed with null string");
        if (s.equals("")) return new Parameter(param);
        return new Parameter ( param + delimiter + s );
        }


    /** Returns a new parameter with the path items in s added to the end of the current path items. */ 
    public Parameter push(String[] s)
        {
        return new Parameter(param,s);
        }

    /** Returns a new parameter with one path item popped off the end.  If this would result in a parameter with an empty collection of path items, null is returned. */ 
    public Parameter pop()
        {
        int x = param.lastIndexOf(delimiter);
        if (x==-1) // there's nothing left.
            return null;
        else return new Parameter(param.substring(0,x));
        }

    /** Returns a new parameter with n path items popped off the end.  If this would result in a parameter with an empty collection of path items, null is returned. */ 
    public Parameter popn(int n)
        {
        String s = param;

        for (int y=0;y<n;y++)
            {
            int x = s.lastIndexOf(delimiter);
            if (x==-1) // there's nothing left
                return null;
            else s = param.substring(0,x);
            }
        return new Parameter(s);
        }


    /** Returns the path item at the far end of the parameter. */ 
    public String top ()
        {
        int x = param.lastIndexOf(delimiter);
        if (x==-1) return param;
        else return param.substring(x+1);
        }
    
    public String toString()
        {
        return param;
        }

    }
