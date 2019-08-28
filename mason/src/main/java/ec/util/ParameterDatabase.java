/*
  Copyright 2006 by Sean Luke
  With modifications by Ananya Dhawan
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/


package ec.util;

import java.io.*;
import java.util.*;
import javax.swing.tree.*;
import java.net.*;

/* 
 * ParameterDatabase.java
 * Created: Sat Aug  7 12:09:19 1999
 */

/**
 * 
 * <p>
 * This extension of the Properties class allows you to set, get, and delete
 * Parameters in a hierarchical tree-like database. The database consists of a
 * list of Parameters, plus an array of "parent databases" which it falls back
 * on when it can't find the Parameter you're looking for. Parents may also have
 * arrays of parents, and so on..
 * 
 * <p>
 * The parameters are loaded from a Java property-list file, which is basically
 * a collection of parameter=value pairs, one per line. Empty lines and lines
 * beginning with # are ignored. These parameters and their values are
 * <b>case-sensitive </b>, and whitespace is trimmed I believe.
 * 
 * <p>
 * An optional set of parameters, "parent. <i>n </i>", where <i>n </i> are
 * consecutive integers starting at 0, define the filenames of the database's
 * parents.
 * 
 * <p>
 * An optional set of parameters, "print-params", specifies whether or not
 * parameters should be printed as they are used (through one of the get(...)
 * methods). If print-params is unset, or set to false or FALSE, nothing is
 * printed. If set to non-false, then the parameters are printed prepended with a "P:"
 * when their values are requested,  "E:" when their existence is tested.  Prior to the
 * "P:" or "E:" you may see a "!" (meaning that the parameter isn't in the database),
 * or a "&lt;" (meaning that the parameter was a default parameter which was never
 * looked up because the primary parameter contained the value).
 * 
 * <p>
 * <p>
 * When you create a ParameterDatabase using new ParameterDatabase(), it is
 * created thus:
 * 
 * <p>
 * <table border=0 cellpadding=0 cellspacing=0>
 * <tr>
 * <td><tt>DATABASE:</tt></td>
 * <td><tt>&nbsp;database</tt></td>
 * </tr>
 * <tr>
 * <td><tt>FROM:</tt></td>
 * <td><tt>&nbsp;(empty)</tt></td>
 * </tr>
 * </table>
 * 
 * 
 * <p>
 * When you create a ParameterDatabase using new ParameterDatabase( <i>file
 * </i>), it is created by loading the database file, and its parent file tree,
 * thus:
 * 
 * <p>
 * <table border=0 cellpadding=0 cellspacing=0>
 * <tr>
 * <td><tt>DATABASE:</tt></td>
 * <td><tt>&nbsp;database</tt></td>
 * <td><tt>&nbsp;-&gt;</tt></td>
 * <td><tt>&nbsp;parent0</tt></td>
 * <td><tt>&nbsp;+-&gt;</tt></td>
 * <td><tt>&nbsp;parent0</tt></td>
 * <td><tt>&nbsp;+-&gt;</tt></td>
 * <td><tt>&nbsp;parent0</tt></td>
 * <td><tt>&nbsp;+-&gt;</tt></td>
 * <td><tt>&nbsp;....</tt></td>
 * </tr>
 * <tr>
 * <td><tt>FROM:</tt></td>
 * <td><tt>&nbsp;(empty)</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;(file)</tt></td>
 * <td><tt>&nbsp;|</tt></td>
 * <td><tt>&nbsp;(parent.0)</tt></td>
 * <td><tt>&nbsp;|</tt></td>
 * <td><tt>&nbsp;(parent.0)</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;....</tt></td>
 * </tr>
 * <tr>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;|</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;+-&gt;</tt></td>
 * <td><tt>&nbsp;parent1</tt></td>
 * <td><tt>&nbsp;+-&gt;</tt></td>
 * <td><tt>&nbsp;....</tt></td>
 * </tr>
 * <tr>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;|</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;|</tt></td>
 * <td><tt>&nbsp;(parent.1)</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * </tr>
 * <tr>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;|</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;....</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * </tr>
 * <tr>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;|</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * </tr>
 * <tr>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;+-&gt;</tt></td>
 * <td><tt>&nbsp;parent1</tt></td>
 * <td><tt>&nbsp;+-&gt;</tt></td>
 * <td><tt>&nbsp;....</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * </tr>
 * <tr>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;|</tt></td>
 * <td><tt>&nbsp;(parent.1)</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * </tr>
 * <tr>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;....</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * </tr>
 * </table>
 * 
 * 
 * <p>
 * When you create a ParameterDatabase using new ParameterDatabase( <i>file,argv
 * </i>), the preferred way, it is created thus:
 * 
 * 
 * <p>
 * <table border=0 cellpadding=0 cellspacing=0>
 * <tr>
 * <td><tt>DATABASE:</tt></td>
 * <td><tt>&nbsp;database</tt></td>
 * <td><tt>&nbsp;-&gt;</tt></td>
 * <td><tt>&nbsp;parent0</tt></td>
 * <td><tt>&nbsp;+-&gt;</tt></td>
 * <td><tt>&nbsp;parent0</tt></td>
 * <td><tt>&nbsp;+-&gt;</tt></td>
 * <td><tt>&nbsp;parent0</tt></td>
 * <td><tt>&nbsp;+-&gt;</tt></td>
 * <td><tt>&nbsp;parent0</tt></td>
 * <td><tt>&nbsp;+-&gt;</tt></td>
 * <td><tt>&nbsp;....</tt></td>
 * </tr>
 * <tr>
 * <td><tt>FROM:</tt></td>
 * <td><tt>&nbsp;(empty)</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>(argv)</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;(file)</tt></td>
 * <td><tt>&nbsp;|</tt></td>
 * <td><tt>&nbsp;(parent.0)</tt></td>
 * <td><tt>&nbsp;|</tt></td>
 * <td><tt>&nbsp;(parent.0)</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;....</tt></td>
 * </tr>
 * <tr>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;|</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;+-&gt;</tt></td>
 * <td><tt>&nbsp;parent1</tt></td>
 * <td><tt>&nbsp;+-&gt;</tt></td>
 * <td><tt>&nbsp;....</tt></td>
 * </tr>
 * <tr>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;|</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;|</tt></td>
 * <td><tt>&nbsp;(parent.1)</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * </tr>
 * <tr>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;|</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;....</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * </tr>
 * <tr>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;|</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * </tr>
 * <tr>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;+-&gt;</tt></td>
 * <td><tt>&nbsp;parent1</tt></td>
 * <td><tt>&nbsp;+-&gt;</tt></td>
 * <td><tt>&nbsp;....</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * </tr>
 * <tr>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;|</tt></td>
 * <td><tt>&nbsp;(parent.1)</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * </tr>
 * <tr>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;....</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * <td><tt>&nbsp;</tt></td>
 * </tr>
 * </table>
 * 
 * 
 * <p>
 * ...that is, the actual top database is empty, and stores parameters added
 * programmatically; its parent is a database formed from arguments passed in on
 * the command line; <i>its </i> parent is the parameter database which actually
 * loads from foo. This allows you to programmatically add parameters which
 * override those in foo, then delete them, thus bringing foo's parameters back
 * in view.
 * 
 * <p>
 * Once a parameter database is loaded, you query it with the <tt>get</tt>
 * methods. The database, then its parents, are searched until a match is found
 * for your parameter. The search rules are thus: (1) the root database is
 * searched first. (2) If a database being searched doesn't contain the data, it
 * searches its parents recursively, starting with parent 0, then moving up,
 * until all searches are exhausted or something was found. (3) No database is
 * searched twice.
 *
 * <p>The various <tt>get</tt> methods all take two parameters.  The first
 * parameter is fetched and retrieved first.  If that fails, the second one
 * (known as the <i>default parameter</i>) is fetched and retrieved.  You
 * can pass in <tt>null</tt> for the default parameter if you don't have one.
 *
 * <p>You can test a parameter for existence with the <tt>exists</tt> methods.
 * 
 * <p>
 * You can set a parameter (in the topmost database <i>only </i> with the
 * <tt>set</tt> command. The <tt>remove</tt> command removes a parameter
 * from the topmost database only. The <tt>removeDeeply</tt> command removes
 * that parameter from every database.
 * 
 * <p>
 * The values stored in a parameter database must not contain "#", "=",
 * non-ascii values, or whitespace.
 *
 * <p>The get... methods can also handle special <i>macro parameters</i>.
 * Macro parameter names will end in <b>default</b> or <b>alias</b>, which
 * means you <i>cannot have any parameter names which end with these two words.</i>
 * The idea behind a macro parameter is that it can substitute one substring for another
 * among your parameter names, making your parameters potentially much simpler.
 * Macros work along period boundaries.  
 *
 * <p>The <b>alias</b> parameter macro works as follows.  A parameter entry of the form:
 *
 * <p><tt>hello.there.alias = foo</tt>
 *
 * <p>Means that parameters which <i>start with</i> <tt>hello.there</tt> should have
 * the <tt>hello.there</tt> portion replace with <tt>foo</tt>.  Thus
 *
 * <p><tt>hello.there.mom.how.are.you</tt>
 *
 * <p>... becomes ...
 * 
 * <p><tt>foo.mom.how.are.you</tt>
 * 
 * <p> and
 *
 * <p><tt>hello.there</tt>
 *
 * <p>... becomes ...
 * 
 * <p><tt>foo</tt>
 *
 * <p> but <tt>hello.thereyo</tt> is unchanged, and <tt>yohello.there.how.are.you</tt> is unchanged.
 *
 * <p>Furthermore if you already have a parameter entered for a given name, it takes precedence 
 * over a macro, and more specific macros take precedence over more general ones.  Thus
 * imagine you have  <tt>a.b.alias = quux</tt>.  Now when you query <tt>a.b.c.d</tt> this
 * will get converted to <tt>quux.c.d</tt> and that parameter will get looked up instead.
 * If you also had <tt>a.b.c.alias = bar</tt>, when when you query <tt>a.b.c.d</tt>, this takes
 * precedence, so now it'll get converted to <tt>bar.d</tt> and that'll get looked up.
 * Finally, if you had <tt>a.b.c.d = foo</tt>, when querying <tt>a.b.c.d</tt> will simply result 
 * in <tt>foo</tt>.
 * 
 * Additionally there is the <b>default</b> macro.  This works just like the <b>alias</b> macro
 * except that it allows anything to be in the last parameter position prior to the default.
 * That is:
 *
 * <p><tt>hello.there.default = foo</tt>
 *
 * <p>Means that parameters which <i>start with</i> <tt>hello.*</tt> , where "*" can be
 * any single parameter element, will have that portion replaced with <tt>foo</tt>.  Thus
 *
 * <p><tt>hello.there.mom.how.are.you</tt>
 *
 * <p>... becomes ...
 * 
 * <p><tt>foo.mom.how.are.you</tt>
 * 
 * <p> and
 *
 * <p><tt>hello.yo.whatever</tt>
 *
 * <p>... becomes ...
 * 
 * <p><tt>foo.whatever</tt>
 *
 * <p> and
 *
 * <p><tt>hello.blah</tt>
 *
 * <p>... becomes ...
 * 
 * <p><tt>foo</tt>
 *
 * This second macro is particularly useful for replacing groups of parameters which differ
 * based on some number.
 * <p>
 * <b>Note for JDK 1.1 </b>. Finally recovering from stupendous idiocy, JDK 1.2
 * included parseDouble() and parseFloat() commands; now you can READ A FLOAT
 * FROM A STRING without having to create a Float object first! Anyway, you will
 * need to modify the getFloat() method below if you're running on JDK 1.1, but
 * understand that large numbers of calls to the method may be inefficient.
 * Sample JDK 1.1 code is given with those methods, but is commented out.
 * 
 * 
 * @author Sean Luke
 * @version 1.0
 */

public class ParameterDatabase implements Serializable 
    {
    public static final String C_HERE = "$";
    public static final String C_CLASS = "@";
    public static final String V_ALIAS = "alias";
    public static final String V_DEFAULT = "default";
    public static final String UNKNOWN_VALUE = "";
    public static final String PRINT_PARAMS = "print-params";
    public static final int PS_UNKNOWN = -1;
    public static final int PS_NONE = 0;
    public static final int PS_PRINT_PARAMS = 1;
    public int printState = PS_UNKNOWN;
    
    // keeps track of the popped parameter parts while searching the database
    private String popped = "";
    private Hashtable aliases = new Hashtable();

    // A descriptive name of the parameter database
    String label;

    // the parents of this database
    Vector parents;
    
    // If the database was loaded via a file, this holds the directory of the database
    File directory;
    
    // a checkbox (unchecked by uncheck()) for not hitting the same database twice in a graph search
    boolean checked;

    // List of parameters which were requested and ones which furthermore were fulfilled
    Hashtable gotten;
    Hashtable accessed;

    // If the database was loaded via getResource(), this holds the class and relative path
    // used in that load
    Class relativeClass;
    String relativePath;

    Properties properties;

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a full Class name, and the class must be a descendent of but not
     * equal to <i>mustCastTosuperclass </i>. Loads the class and returns an
     * instance (constructed with the default constructor), or throws a
     * ParamClassLoadException if there is no such Class. If the parameter is
     * not found, the defaultParameter is used. The parameter chosen is marked
     * "used".
     */
    public Object getInstanceForParameter(Parameter parameter, Parameter defaultParameter, Class mustCastTosuperclass) throws ParamClassLoadException 
        {
        printGotten(parameter, defaultParameter, false);
        Parameter p;
        if (_exists(parameter))
            p = parameter;
        else if (_exists(defaultParameter))
            p = defaultParameter;
        else
            throw new ParamClassLoadException(
                "No class name provided.\nPARAMETER: "
                + parameter
                + (defaultParameter == null ? "" : "\n     ALSO: "
                    + defaultParameter));
        try 
            {
            Class c = Class.forName(getParam(p), true, Thread.currentThread().getContextClassLoader());
            if (!mustCastTosuperclass.isAssignableFrom(c))
                throw new ParamClassLoadException("The class "
                    + c.getName()
                    + "\ndoes not cast into the superclass "
                    + mustCastTosuperclass.getName()
                    + "\nPARAMETER: "
                    + parameter
                    + (defaultParameter == null ? "" : "\n     ALSO: "
                        + defaultParameter));
            if (mustCastTosuperclass == c)
                throw new ParamClassLoadException("The class "
                    + c.getName()
                    + "\nmust not be the same as the required superclass "
                    + mustCastTosuperclass.getName()
                    + "\nPARAMETER: "
                    + parameter
                    + (defaultParameter == null ? "" : "\n     ALSO: "
                        + defaultParameter));
            return c.newInstance();
            } 
        catch (ClassNotFoundException e) 
            {
            throw new ParamClassLoadException("Class not found: "
                + getParam(p)
                + "\nPARAMETER: "
                + parameter
                + (defaultParameter == null ? "" : "\n     ALSO: "
                    + defaultParameter) + "\nEXCEPTION: \n\n" + e);
            } 
        catch (IllegalArgumentException e) 
            {
            throw new ParamClassLoadException("Could not load class: "
                + getParam(p)
                + "\nPARAMETER: "
                + parameter
                + (defaultParameter == null ? "" : "\n     ALSO: "
                    + defaultParameter) + "\nEXCEPTION: \n\n" + e);
            } 
        catch (InstantiationException e) 
            {
            throw new ParamClassLoadException(
                "The requested class is an interface or an abstract class: "
                + getParam(p)
                + "\nPARAMETER: "
                + parameter
                + (defaultParameter == null ? "" : "\n     ALSO: "
                    + defaultParameter) + "\nEXCEPTION: \n\n"
                + e);
            } 
        catch (IllegalAccessException e) 
            {
            throw new ParamClassLoadException(
                "The requested class cannot be initialized with the default initializer: "
                + getParam(p)
                + "\nPARAMETER: "
                + parameter
                + (defaultParameter == null ? "" : "\n     ALSO: "
                    + defaultParameter) + "\nEXCEPTION: \n\n"
                + e);
            }
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a full Class name, and the class must be a descendent, or equal
     * to, <i>mustCastTosuperclass </i>. Loads the class and returns an instance
     * (constructed with the default constructor), or throws a
     * ParamClassLoadException if there is no such Class. The parameter chosen
     * is marked "used".
     */
    public Object getInstanceForParameterEq(Parameter parameter,
        Parameter defaultParameter, Class mustCastTosuperclass)
        throws ParamClassLoadException 
        {
        printGotten(parameter, defaultParameter, false);
        Parameter p;
        if (_exists(parameter))
            p = parameter;
        else if (_exists(defaultParameter))
            p = defaultParameter;
        else
            throw new ParamClassLoadException(
                "No class name provided.\nPARAMETER: "
                + parameter
                + "\n     ALSO: "
                + (defaultParameter == null ? "" : "\n     ALSO: "
                    + defaultParameter));
        try
            {
            Class c = Class.forName(getParam(p), true, Thread.currentThread().getContextClassLoader());
            if (!mustCastTosuperclass.isAssignableFrom(c))
                throw new ParamClassLoadException("The class "
                    + c.getName()
                    + "\ndoes not cast into the superclass "
                    + mustCastTosuperclass.getName()
                    + "\nPARAMETER: "
                    + parameter
                    + "\n     ALSO: "
                    + (defaultParameter == null ? "" : "\n     ALSO: "
                        + defaultParameter));
            return c.newInstance();
            } 
        catch (ClassNotFoundException e) 
            {
            throw new ParamClassLoadException("Class not found: "
                + getParam(p)
                + "\nPARAMETER: "
                + parameter
                + "\n     ALSO: "
                + (defaultParameter == null ? "" : "\n     ALSO: "
                    + defaultParameter) + "\nEXCEPTION: \n\n" + e);
            } 
        catch (IllegalArgumentException e) 
            {
            throw new ParamClassLoadException("Could not load class: "
                + getParam(p)
                + "\nPARAMETER: "
                + parameter
                + "\n     ALSO: "
                + (defaultParameter == null ? "" : "\n     ALSO: "
                    + defaultParameter) + "\nEXCEPTION: \n\n" + e);
            } 
        catch (InstantiationException e) 
            {
            throw new ParamClassLoadException(
                "The requested class is an interface or an abstract class: "
                + getParam(p)
                + "\nPARAMETER: "
                + parameter
                + "\n     ALSO: "
                + (defaultParameter == null ? "" : "\n     ALSO: "
                    + defaultParameter) + "\nEXCEPTION: \n\n"
                + e);
            } 
        catch (IllegalAccessException e) 
            {
            throw new ParamClassLoadException(
                "The requested class cannot be initialized with the default initializer: "
                + getParam(p)
                + "\nPARAMETER: "
                + parameter
                + "\n     ALSO: "
                + (defaultParameter == null ? "" : "\n     ALSO: "
                    + defaultParameter) + "\nEXCEPTION: \n\n"
                + e);
            }
        }

    /**
     * Searches down through databases to find a given parameter. The value
     * associated with this parameter must be a full Class name, and the class
     * must be a descendent of but not equal to <i>mustCastTosuperclass </i>.
     * Loads and returns the associated Class, or throws a
     * ParamClassLoadException if there is no such Class. If the parameter is
     * not found, the defaultParameter is used. The parameter chosen is marked
     * "used".
     */
    public Class getClassForParameter(Parameter parameter,
        Parameter defaultParameter, Class mustCastTosuperclass)
        throws ParamClassLoadException 
        {
        printGotten(parameter, defaultParameter, false);
        Parameter p;
        if (_exists(parameter))
            p = parameter;
        else if (_exists(defaultParameter))
            p = defaultParameter;
        else
            throw new ParamClassLoadException(
                "No class name provided.\nPARAMETER: "
                + parameter
                + "\n     ALSO: "
                + (defaultParameter == null ? "" : "\n     ALSO: "
                    + defaultParameter));
        try
            {
            Class c = Class.forName(getParam(p), true, Thread.currentThread().getContextClassLoader());
            if (!mustCastTosuperclass.isAssignableFrom(c))
                throw new ParamClassLoadException("The class "
                    + c.getName()
                    + "\ndoes not cast into the superclass "
                    + mustCastTosuperclass.getName()
                    + "\nPARAMETER: "
                    + parameter
                    + "\n     ALSO: "
                    + (defaultParameter == null ? "" : "\n     ALSO: "
                        + defaultParameter));
            return c;
            } 
        catch (ClassNotFoundException e) 
            {
            throw new ParamClassLoadException("Class not found: "
                + getParam(p)
                + "\nPARAMETER: "
                + parameter
                + "\n     ALSO: "
                + (defaultParameter == null ? "" : "\n     ALSO: "
                    + defaultParameter) + "\nEXCEPTION: \n\n" + e);
            } 
        catch (IllegalArgumentException e) 
            {
            throw new ParamClassLoadException("Could not load class: "
                + getParam(p)
                + "\nPARAMETER: "
                + parameter
                + "\n     ALSO: "
                + (defaultParameter == null ? "" : "\n     ALSO: "
                    + defaultParameter) + "\nEXCEPTION: \n\n" + e);
            }
        }

    /**
     * Searches down through databases to find a given parameter; If the
     * parameter does not exist, defaultValue is returned. If the parameter
     * exists, and it is set to "false" (case insensitive), false is returned.
     * Else true is returned. The parameter chosen is marked "used" if it
     * exists.
     */
    public boolean getBoolean(Parameter parameter,
        Parameter defaultParameter, boolean defaultValue) 
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getBoolean(parameter, defaultValue);
        else
            return getBoolean(defaultParameter, defaultValue);
        }

    /**
     * Searches down through databases to find a given parameter; If the
     * parameter does not exist, defaultValue is returned. If the parameter
     * exists, and it is set to "false" (case insensitive), false is returned.
     * Else true is returned. The parameter chosen is marked "used" if it
     * exists.
     */
    boolean getBoolean(Parameter parameter, boolean defaultValue) 
        {
        if (!_exists(parameter))
            return defaultValue;
        return (!getParam(parameter).equalsIgnoreCase("false"));
        }

    /**
     * Parses an integer from a string, either in decimal or (if starting with
     * an x) in hex
     */
    // we assume that the string has been trimmed already
    int parseInt(String string)
        throws NumberFormatException 
        {
        char c;
        if (string != null && string.length() > 0
            && ((string.charAt(0) == (c = 'x')) || c == 'X')) 
            {
            // it's a hex int, load it as hex
            return Integer.parseInt(string.substring(1), 16);
            } 
        else
            {
            try
                {
                // it's decimal
                return Integer.parseInt(string);
                }
            catch (NumberFormatException e)
                {
                // maybe it's a double ending in .0, which should be okay
                try 
                    {
                    double d = Double.parseDouble(string);
                    if (d == (int) d) return (int) d;  // looking fine
                    else throw e;
                    }
                catch (NumberFormatException e2)
                    {
                    throw e;
                    }
                }
            }
        }

    /**
     * Parses a long from a string, either in decimal or (if starting with an x)
     * in hex
     */
    // we assume that the string has been trimmed already
    /*protected*/ long parseLong(String string)
        throws NumberFormatException 
        {
        char c;
        if (string != null && string.length() > 0
            && ((string.charAt(0) == (c = 'x')) || c == 'X')) 
            {
            // it's a hex int, load it as hex
            return Long.parseLong(string.substring(1), 16);
            } 
        else
            { 
            try
                {
                // it's decimal
                return Long.parseLong(string);
                }
            catch (NumberFormatException e)
                {
                // maybe it's a double ending in .0, which should be okay
                try 
                    {
                    double d = Double.parseDouble(string);
                    if (d == (long) d) return (long) d;  // looking fine
                    else throw e;
                    }
                catch (NumberFormatException e2)
                    {
                    throw e;
                    }
                }
            }
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be an integer. It returns the value, else throws a
     * NumberFormatException exception if there is an error in parsing the
     * parameter. The parameter chosen is marked "used" if it exists. Integers
     * may be in decimal or (if preceded with an X or x) in hexadecimal.
     */
    /*protected*/ int getInt(Parameter parameter)
        throws NumberFormatException 
        {
        if (_exists(parameter)) 
            {
            try
                {
                return parseInt(getParam(parameter));
                } 
            catch (NumberFormatException e) 
                {
                throw new NumberFormatException("Bad integer ("
                    + getParam(parameter) + " ) for parameter " + parameter);
                }
            } 
        else
            throw new NumberFormatException(
                "Integer does not exist for parameter " + parameter);
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be an integer. It returns the value, else throws a
     * NumberFormatException exception if there is an error in parsing the
     * parameter. The parameter chosen is marked "used" if it exists. Integers
     * may be in decimal or (if preceded with an X or x) in hexadecimal.
     */
    public int getInt(Parameter parameter, Parameter defaultParameter)
        throws NumberFormatException 
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getInt(parameter);
        else if (_exists(defaultParameter))
            return getInt(defaultParameter);
        else
            throw new NumberFormatException(
                "Integer does not exist for either parameter " + parameter
                + "\nor\n" + defaultParameter);
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be an integer >= minValue. It returns the value, or minValue-1 if
     * the value is out of range or if there is an error in parsing the
     * parameter. The parameter chosen is marked "used" if it exists. Integers
     * may be in decimal or (if preceded with an X or x) in hexadecimal.
     */
    public int getInt(Parameter parameter, Parameter defaultParameter,
        int minValue) 
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getInt(parameter, minValue);
        else
            return getInt(defaultParameter, minValue);
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be an integer >= minValue. It returns the value, or minValue-1 if
     * the value is out of range or if there is an error in parsing the
     * parameter. The parameter chosen is marked "used" if it exists. Integers
     * may be in decimal or (if preceded with an X or x) in hexadecimal.
     */
    /*protected*/ int getInt(Parameter parameter, int minValue) 
        {
        if (_exists(parameter)) 
            {
            try
                {
                int i = parseInt(getParam(parameter));
                if (i < minValue)
                    return minValue - 1;
                return i;
                } 
            catch (NumberFormatException e) 
                {
                return minValue - 1;
                }
            } 
        else
            return minValue - 1;
        }

    /**
     * Searches down through databases to find a given parameter, which must be
     * an integer. If there is an error in parsing the parameter, then default
     * is returned. The parameter chosen is marked "used" if it exists. Integers
     * may be in decimal or (if preceded with an X or x) in hexadecimal.
     */
    public int getIntWithDefault(Parameter parameter,
        Parameter defaultParameter, int defaultValue) 
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getIntWithDefault(parameter, defaultValue);
        else
            return getIntWithDefault(defaultParameter, defaultValue);
        }

    /**
     * Searches down through databases to find a given parameter, which must be
     * an integer. If there is an error in parsing the parameter, then default
     * is returned. The parameter chosen is marked "used" if it exists. Integers
     * may be in decimal or (if preceded with an X or x) in hexadecimal.
     */
    int getIntWithDefault(Parameter parameter, int defaultValue) 
        {
        if (_exists(parameter)) 
            {
            try
                {
                return parseInt(getParam(parameter));
                } 
            catch (NumberFormatException e) 
                {
                return defaultValue;
                }
            } 
        else
            return defaultValue;
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be an integer >= minValue and <= maxValue. It returns the value, or
     * minValue-1 if the value is out of range or if there is an error in
     * parsing the parameter. The parameter chosen is marked "used" if it
     * exists. Integers may be in decimal or (if preceded with an X or x) in
     * hexadecimal.
     */
    public int getIntWithMax(Parameter parameter,
        Parameter defaultParameter, int minValue, int maxValue) 
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getIntWithMax(parameter, minValue, maxValue);
        else
            return getIntWithMax(defaultParameter, minValue, maxValue);
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be an integer >= minValue and <= maxValue. It returns the value, or
     * minValue-1 if the value is out of range or if there is an error in
     * parsing the parameter. The parameter chosen is marked "used" if it
     * exists. Integers may be in decimal or (if preceded with an X or x) in
     * hexadecimal.
     */
    int getIntWithMax(Parameter parameter, int minValue, int maxValue) 
        {
        if (_exists(parameter)) 
            {
            try
                {
                int i = parseInt(getParam(parameter));
                if (i < minValue)
                    return minValue - 1;
                if (i > maxValue)
                    return minValue - 1;
                return i;
                } 
            catch (NumberFormatException e) 
                {
                return minValue - 1;
                }
            } 
        else
            return minValue - 1;
        }


    float getFloat(Parameter parameter) throws NumberFormatException
        {
        if (_exists(parameter)) 
            {
            try
                {
                // For JDK 1.2 and later, this is more efficient...
                // float i = Float.parseFloat(getParam(parameter));
                // ...but we can't use it and still be compatible with JDK 1.1
                return Float.valueOf(getParam(parameter)).floatValue(); // what stupidity...
                } 
            catch (NumberFormatException e) 
                {
                throw new NumberFormatException("Bad float ("
                    + getParam(parameter) + " ) for parameter " + parameter);
                }
            } 
        else
            throw new NumberFormatException(
                "Float does not exist for parameter " + parameter);
        }

    /*
     * Searches down through databases to find a given parameter, whose value
     * must be a float. It returns the value, else throws a
     * NumberFormatException exception if there is an error in parsing the
     * parameter. The parameter chosen is marked "used" if it exists.
     */
    public float getFloat(Parameter parameter, Parameter defaultParameter)
        throws NumberFormatException 
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getFloat(parameter);
        else if (_exists(defaultParameter))
            return getFloat(defaultParameter);
        else
            throw new NumberFormatException(
                "Float does not exist for either parameter " + parameter
                + "\nor\n" + defaultParameter);
        }
        

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a float >= minValue. If not, this method returns minvalue-1, else
     * it returns the parameter value. The parameter chosen is marked "used" if
     * it exists.
     */

    public float getFloat(Parameter parameter,
        Parameter defaultParameter, double minValue) 
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getFloat(parameter, minValue);
        else
            return getFloat(defaultParameter, minValue);
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a float >= minValue. If not, this method returns minvalue-1, else
     * it returns the parameter value. The parameter chosen is marked "used" if
     * it exists.
     */

    float getFloat(Parameter parameter, double minValue) 
        {
        if (_exists(parameter)) 
            {
            try
                {
                float i = Float.valueOf(getParam(parameter)).floatValue(); // what stupidity...

                // For JDK 1.2 and later, this is more efficient...
                // float i = Float.parseFloat(getParam(parameter));
                // ...but we can't use it and still be compatible with JDK 1.1

                if (i < minValue)
                    return (float) (minValue - 1);
                return i;
                } 
            catch (NumberFormatException e) 
                {
                return (float) (minValue - 1);
                }
            } 
        else
            return (float) (minValue - 1);
        }

    /**
     * Searches down through databases to find a given parameter, which must be
     * a float. If there is an error in parsing the parameter, then default is
     * returned. The parameter chosen is marked "used" if it exists.
     */
    public float getFloatWithDefault(Parameter parameter,
        Parameter defaultParameter, double defaultValue) 
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getFloatWithDefault(parameter, defaultValue);
        else
            return getFloatWithDefault(defaultParameter, defaultValue);
        }

    /**
     * Searches down through databases to find a given parameter, which must be
     * a float. If there is an error in parsing the parameter, then default is
     * returned. The parameter chosen is marked "used" if it exists.
     */
    float getFloatWithDefault(Parameter parameter, double defaultValue) 
        {
        if (_exists(parameter)) 
            {
            try
                {
                // For JDK 1.2 and later, this is more efficient...
                // return Float.parseFloat(getParam(parameter));
                // ...but we can't use it and still be compatible with JDK 1.1
                return Float.valueOf(getParam(parameter)).floatValue(); // what stupidity...
                } 
            catch (NumberFormatException e) 
                {
                return (float) (defaultValue);
                }
            } 
        else
            return (float) (defaultValue);
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a float >= minValue and <= maxValue. If not, this method returns
     * minvalue-1, else it returns the parameter value. The parameter chosen is
     * marked "used" if it exists.
     */

    public float getFloatWithMax(Parameter parameter,
        Parameter defaultParameter, double minValue, double maxValue) 
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getFloat(parameter, minValue, maxValue);
        else
            return getFloat(defaultParameter, minValue, maxValue);
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a float >= minValue and <= maxValue. If not, this method returns
     * minvalue-1, else it returns the parameter value. The parameter chosen is
     * marked "used" if it exists.
     *
     * @deprecated Use getFloatWithMax instead
     */

    public float getFloat(Parameter parameter,
        Parameter defaultParameter, double minValue, double maxValue) 
        {
        return getFloatWithMax(parameter, defaultParameter, minValue, maxValue);
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a float >= minValue and <= maxValue. If not, this method returns
     * minvalue-1, else it returns the parameter value. The parameter chosen is
     * marked "used" if it exists.
     */

    float getFloat(Parameter parameter, double minValue, double maxValue) 
        {
        if (_exists(parameter)) 
            {
            try
                {
                float i = Float.valueOf(getParam(parameter)).floatValue(); // what stupidity...

                // For JDK 1.2 and later, this is more efficient...
                // float i = Float.parseFloat(getParam(parameter));
                // ...but we can't use it and still be compatible with JDK 1.1

                if (i < minValue)
                    return (float) (minValue - 1);
                if (i > maxValue)
                    return (float) (minValue - 1);
                return i;
                } 
            catch (NumberFormatException e) 
                {
                return (float) (minValue - 1);
                }
            } 
        else
            return (float) (minValue - 1);
        }

    double getDouble(Parameter parameter) throws NumberFormatException
        {
        if (_exists(parameter)) 
            {
            try
                {
                // For JDK 1.2 and later, this is more efficient...
                // double i = Double.parseDouble(getParam(parameter));
                // ...but we can't use it and still be compatible with JDK 1.1
                return Double.valueOf(getParam(parameter)).doubleValue(); // what stupidity...
                } 
            catch (NumberFormatException e) 
                {
                throw new NumberFormatException("Bad double ("
                    + getParam(parameter) + " ) for parameter " + parameter);
                }
            } 
        else
            throw new NumberFormatException(
                "Double does not exist for parameter " + parameter);
        }

    /*
     * Searches down through databases to find a given parameter, whose value
     * must be an double. It returns the value, else throws a
     * NumberFormatException exception if there is an error in parsing the
     * parameter. The parameter chosen is marked "used" if it exists. 
     */
    public double getDouble(Parameter parameter, Parameter defaultParameter)
        throws NumberFormatException 
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getDouble(parameter);
        else if (_exists(defaultParameter))
            return getDouble(defaultParameter);
        else
            throw new NumberFormatException(
                "Double does not exist for either parameter " + parameter
                + "\nor\n" + defaultParameter);
        }
        

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a double >= minValue. If not, this method returns minvalue-1,
     * else it returns the parameter value. The parameter chosen is marked
     * "used" if it exists.
     */

    public double getDouble(Parameter parameter,
        Parameter defaultParameter, double minValue) 
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getDouble(parameter, minValue);
        else
            return getDouble(defaultParameter, minValue);
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a double >= minValue. If not, this method returns minvalue-1,
     * else it returns the parameter value. The parameter chosen is marked
     * "used" if it exists.
     */

    double getDouble(Parameter parameter, double minValue) 
        {
        if (_exists(parameter)) 
            {
            try
                {
                double i = Double.valueOf(getParam(parameter)).doubleValue(); // what stupidity...

                // For JDK 1.2 and later, this is more efficient...
                // double i = Double.parseDouble(getParam(parameter));
                // ...but we can't use it and still be compatible with JDK 1.1

                if (i < minValue)
                    return (double) (minValue - 1);
                return i;
                } 
            catch (NumberFormatException e) 
                {
                return (double) (minValue - 1);
                }
            } 
        else
            return (double) (minValue - 1);
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a double >= minValue and <= maxValue. If not, this method returns
     * minvalue-1, else it returns the parameter value. The parameter chosen is
     * marked "used" if it exists.
     */

    public double getDoubleWithMax(Parameter parameter,
        Parameter defaultParameter, double minValue, double maxValue) 
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getDouble(parameter, minValue, maxValue);
        else
            return getDouble(defaultParameter, minValue, maxValue);
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a double >= minValue and <= maxValue. If not, this method returns
     * minvalue-1, else it returns the parameter value. The parameter chosen is
     * marked "used" if it exists.
     *
     * @deprecated use getDoubleWithMax instead
     */

    public double getDouble(Parameter parameter,
        Parameter defaultParameter, double minValue, double maxValue) 
        {
        return getDoubleWithMax(parameter, defaultParameter, minValue, maxValue);
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a double >= minValue and <= maxValue. If not, this method returns
     * minvalue-1, else it returns the parameter value. The parameter chosen is
     * marked "used" if it exists.
     */

    double getDouble(Parameter parameter, double minValue, double maxValue) 
        {
        if (_exists(parameter)) 
            {
            try
                {
                double i = Double.valueOf(getParam(parameter)).doubleValue(); // what stupidity...

                // For JDK 1.2 and later, this is more efficient...
                // double i = Double.parseDouble(getParam(parameter));
                // ...but we can't use it and still be compatible with JDK 1.1

                if (i < minValue)
                    return (double) (minValue - 1);
                if (i > maxValue)
                    return (double) (minValue - 1);
                return i;
                } 
            catch (NumberFormatException e) 
                {
                return (double) (minValue - 1);
                }
            } 
        else
            return (double) (minValue - 1);
        }

    /**
     * Searches down through databases to find a given parameter, which must be
     * a float. If there is an error in parsing the parameter, then default is
     * returned. The parameter chosen is marked "used" if it exists.
     */
    public double getDoubleWithDefault(Parameter parameter,
        Parameter defaultParameter, double defaultValue) 
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getDoubleWithDefault(parameter, defaultValue);
        else
            return getDoubleWithDefault(defaultParameter, defaultValue);
        }

    /**
     * Searches down through databases to find a given parameter, which must be
     * a float. If there is an error in parsing the parameter, then default is
     * returned. The parameter chosen is marked "used" if it exists.
     */
    double getDoubleWithDefault(Parameter parameter, double defaultValue) 
        {
        if (_exists(parameter)) 
            {
            try
                {
                // For JDK 1.2 and later, this is more efficient...
                // return Double.parseDouble(getParam(parameter));
                // ...but we can't use it and still be compatible with JDK 1.1
                return Double.valueOf(getParam(parameter)).doubleValue(); // what stupidity...
                } 
            catch (NumberFormatException e) 
                {
                return defaultValue;
                }
            } 
        else
            return defaultValue;
        }


    double[] extendBag(double[] bag)
        {
        double[] newbag = new double[bag.length * 2 + 1];
        System.arraycopy(bag, 0, newbag, 0, bag.length);
        return newbag;
        }
                
    double[] collapseBag(double[] bag, int size)
        {
        double[] newbag = new double[size];
        System.arraycopy(bag, 0, newbag, 0, size);
        return newbag;
        }

    static final int ARRAY_NO_EXPECTED_LENGTH = (-1);
    double[] getDoublesWithMax(Parameter parameter, double minValue, double maxValue, int expectedLength)
        {
        if (_exists(parameter)) 
            {
            double[] bag = new double[256];
            int bagSize = 0;
            
            Scanner scanner = new Scanner(getParam(parameter));
            while(scanner.hasNextDouble())
                {
                if (expectedLength != ARRAY_NO_EXPECTED_LENGTH && bagSize >= expectedLength)
                    return null;  // too big
                                
                double val = scanner.nextDouble();
                if (val != val || val > maxValue || val < minValue)
                    return null;
                else
                    {
                    if (bagSize == bag.length) bag = extendBag(bag);
                    bag[bagSize] = val;
                    bagSize++; 
                    }
                }
            if (scanner.hasNext())
                return null;  // too long, or garbage afterwards
            if (expectedLength != ARRAY_NO_EXPECTED_LENGTH && bagSize != expectedLength)
                return null;
            if (bagSize == 0)
                return null;            // 0 lengths not permitted
            return collapseBag(bag, bagSize);
            } 
        else
            {
            return null;
            }
        }

    double[] getDoublesWithMax(Parameter parameter, double minValue, double maxValue)
        {
        return getDoublesWithMax(parameter, minValue, maxValue, ARRAY_NO_EXPECTED_LENGTH);
        }
        
    double[] getDoubles(Parameter parameter, double minValue, int expectedLength)
        {
        return getDoublesWithMax(parameter, minValue, Double.POSITIVE_INFINITY, expectedLength);
        }

    double[] getDoubles(Parameter parameter, double minValue)
        {
        return getDoublesWithMax(parameter, minValue, Double.POSITIVE_INFINITY, ARRAY_NO_EXPECTED_LENGTH);
        }

    double[] getDoublesUnconstrained(Parameter parameter, int expectedLength)
        {
        return getDoublesWithMax(parameter, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, expectedLength);
        }

    double[] getDoublesUnconstrained(Parameter parameter)
        {
        return getDoublesWithMax(parameter, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, ARRAY_NO_EXPECTED_LENGTH);
        }



    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a space- or tab-delimited list of doubles, each of which is >= minValue and <= maxValue,
     * and which must be exactly expectedLength (> 0) long.  If the parameter does not exist,
     * or any of its doubles are out of bounds, or the list is not long enough or is  
     * too long or has garbage at the end of it, then this method returns null.
     * Otherwise the method returns the doubles in question.  The doubles may not
     * be NaN, +Infinity, or -Infinity. The parameter chosen is
     * marked "used" if it exists.
     */

    public double[] getDoublesWithMax(Parameter parameter, Parameter defaultParameter, double minValue, double maxValue, int expectedLength)
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getDoublesWithMax(parameter, minValue, maxValue, expectedLength);
        else
            return getDoublesWithMax(defaultParameter, minValue, maxValue, expectedLength);
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a space- or tab-delimited list of doubles, each of which is >= minValue and <= maxValue,
     * and which must be at least 1 number long.  If the parameter does not exist,
     * or any of its doubles are out of bounds, or the list is not long enough or is  
     * too long or has garbage at the end of it, then this method returns null.
     * Otherwise the method returns the doubles in question.  The doubles may not
     * be NaN, +Infinity, or -Infinity. The parameter chosen is
     * marked "used" if it exists.
     */

    public double[] getDoublesWithMax(Parameter parameter, Parameter defaultParameter, double minValue, double maxValue)
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getDoublesWithMax(parameter, minValue, maxValue);
        else
            return getDoublesWithMax(defaultParameter, minValue, maxValue);
        }
        
    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a space- or tab-delimited list of doubles, each of which is >= minValue,
     * and which must be exactly expectedLength (> 0) long.  If the parameter does not exist,
     * or any of its doubles are out of bounds, or the list is not long enough or is  
     * too long or has garbage at the end of it, then this method returns null.
     * Otherwise the method returns the doubles in question.  The doubles may not
     * be NaN, +Infinity, or -Infinity. The parameter chosen is
     * marked "used" if it exists.
     */

    public double[] getDoubles(Parameter parameter, Parameter defaultParameter, double minValue, int expectedLength)
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getDoubles(parameter, minValue, expectedLength);
        else
            return getDoubles(defaultParameter, minValue, expectedLength);
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a space- or tab-delimited list of doubles, each of which is >= minValue,
     * and which must be at least 1 number long.  If the parameter does not exist,
     * or any of its doubles are out of bounds, or the list is not long enough or is  
     * too long or has garbage at the end of it, then this method returns null.
     * Otherwise the method returns the doubles in question.  The doubles may not
     * be NaN, +Infinity, or -Infinity. The parameter chosen is
     * marked "used" if it exists.
     */

    public double[] getDoubles(Parameter parameter, Parameter defaultParameter, double minValue)
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getDoubles(parameter, minValue);
        else
            return getDoubles(defaultParameter, minValue);
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a space- or tab-delimited list of doubles,
     * and which must be exactly expectedLength (> 0) long.  If the parameter does not exist,
     * or the list is not long enough or is  
     * too long or has garbage at the end of it, then this method returns null.
     * Otherwise the method returns the doubles in question.  The doubles may not
     * be NaN, +Infinity, or -Infinity. The parameter chosen is
     * marked "used" if it exists.
     */

    public double[] getDoublesUnconstrained(Parameter parameter, Parameter defaultParameter, int expectedLength)
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getDoublesUnconstrained(parameter, expectedLength);
        else
            return getDoublesUnconstrained(defaultParameter, expectedLength);
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a space- or tab-delimited list of doubles,
     * and which must be at least 1 number long.  If the parameter does not exist,
     * or the list is not long enough or is  
     * too long or has garbage at the end of it, then this method returns null.
     * Otherwise the method returns the doubles in question.  The doubles may not
     * be NaN, +Infinity, or -Infinity. The parameter chosen is
     * marked "used" if it exists.
     */

    public double[] getDoublesUnconstrained(Parameter parameter, Parameter defaultParameter)
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getDoublesUnconstrained(parameter);
        else
            return getDoublesUnconstrained(defaultParameter);
        }



















    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a long. It returns the value, else throws a NumberFormatException
     * exception if there is an error in parsing the parameter. The parameter
     * chosen is marked "used" if it exists. Longs may be in decimal or (if
     * preceded with an X or x) in hexadecimal.
     */
    /*protected*/ long getLong(Parameter parameter)
        throws NumberFormatException 
        {
        if (_exists(parameter)) 
            {
            try
                {
                return parseLong(getParam(parameter));
                } 
            catch (NumberFormatException e) 
                {
                throw new NumberFormatException("Bad long (" + getParam(parameter)
                    + " ) for parameter " + parameter);
                }
            } 
        else
            throw new NumberFormatException(
                "Long does not exist for parameter " + parameter);
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a long. It returns the value, else throws a NumberFormatException
     * exception if there is an error in parsing the parameter. The parameter
     * chosen is marked "used" if it exists. Longs may be in decimal or (if
     * preceded with an X or x) in hexadecimal.
     */
    public long getLong(Parameter parameter, Parameter defaultParameter)
        throws NumberFormatException 
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getLong(parameter);
        else if (_exists(defaultParameter))
            return getLong(defaultParameter);
        else
            throw new NumberFormatException(
                "Long does not exist for either parameter " + parameter
                + "\nor\n" + defaultParameter);
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a long >= minValue. If not, this method returns errValue, else it
     * returns the parameter value. The parameter chosen is marked "used" if it
     * exists. Longs may be in decimal or (if preceded with an X or x) in
     * hexadecimal.
     */

    public long getLong(Parameter parameter, Parameter defaultParameter,
        long minValue) 
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getLong(parameter, minValue);
        else
            return getLong(defaultParameter, minValue);
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a long >= minValue. If not, this method returns errValue, else it
     * returns the parameter value. The parameter chosen is marked "used" if it
     * exists. Longs may be in decimal or (if preceded with an X or x) in
     * hexadecimal.
     */
    long getLong(Parameter parameter, long minValue) 
        {
        if (_exists(parameter)) 
            {
            try
                {
                long i = parseLong(getParam(parameter));
                if (i < minValue)
                    return minValue - 1;
                return i;
                } 
            catch (NumberFormatException e) 
                {
                return minValue - 1;
                }
            } 
        else
            return (minValue - 1);
        }

    /**
     * Searches down through databases to find a given parameter, which must be
     * a long. If there is an error in parsing the parameter, then default is
     * returned. The parameter chosen is marked "used" if it exists. Longs may
     * be in decimal or (if preceded with an X or x) in hexadecimal.
     */
    public long getLongWithDefault(Parameter parameter,
        Parameter defaultParameter, long defaultValue) 
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getLongWithDefault(parameter, defaultValue);
        else
            return getLongWithDefault(defaultParameter, defaultValue);
        }

    /**
     * Searches down through databases to find a given parameter, which must be
     * a long. If there is an error in parsing the parameter, then default is
     * returned. The parameter chosen is marked "used" if it exists. Longs may
     * be in decimal or (if preceded with an X or x) in hexadecimal.
     */
    long getLongWithDefault(Parameter parameter, long defaultValue) 
        {
        if (_exists(parameter)) 
            {
            try
                {
                return parseLong(getParam(parameter));
                } 
            catch (NumberFormatException e) 
                {
                return defaultValue;
                }
            } 
        else
            return defaultValue;
        }

    /**
     * Searches down through databases to find a given parameter, whose value
     * must be a long >= minValue and = < maxValue. If not, this method returns
     * errValue, else it returns the parameter value. The parameter chosen is
     * marked "used" if it exists. Longs may be in decimal or (if preceded with
     * an X or x) in hexadecimal.
     */
    public long getLongWithMax(Parameter parameter,
        Parameter defaultParameter, long minValue, long maxValue) 
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getLong(parameter, minValue, maxValue);
        else
            return getLong(defaultParameter, minValue, maxValue);
        }

    /**
     * Use getLongWithMax(...) instead. Searches down through databases to find
     * a given parameter, whose value must be a long >= minValue and = <
     * maxValue. If not, this method returns errValue, else it returns the
     * parameter value. The parameter chosen is marked "used" if it exists.
     * Longs may be in decimal or (if preceded with an X or x) in hexadecimal.
     */
    long getLongWithMax(Parameter parameter, long minValue, long maxValue) 
        {
        if (_exists(parameter)) 
            {
            try
                {
                long i = parseLong(getParam(parameter));
                if (i < minValue)
                    return minValue - 1;
                if (i > maxValue)
                    return minValue - 1;
                return i;
                } 
            catch (NumberFormatException e) 
                {
                return minValue - 1;
                }
            } 
        else
            return (minValue - 1);
        }

    /**
     * Use getLongWithMax(...) instead. Searches down through databases to find
     * a given parameter, whose value must be a long >= minValue and = <
     * maxValue. If not, this method returns errValue, else it returns the
     * parameter value. The parameter chosen is marked "used" if it exists.
     * Longs may be in decimal or (if preceded with an X or x) in hexadecimal.
     * 
     * @deprecated
     */
    public long getLong(Parameter parameter, Parameter defaultParameter,
        long minValue, long maxValue) 
        {
        printGotten(parameter, defaultParameter, false);
        return getLongWithMax(parameter, defaultParameter, minValue, maxValue);
        }

    /**
     * Use getLongWithMax(...) instead. Searches down through databases to find
     * a given parameter, whose value must be a long >= minValue and = <
     * maxValue. If not, this method returns errValue, else it returns the
     * parameter value. The parameter chosen is marked "used" if it exists.
     * 
     * @deprecated
     */
    long getLong(Parameter parameter, long minValue, long maxValue) 
        {
        return getLongWithMax(parameter, minValue, maxValue);
        }

    /**
     * Searches down through the databases to find a given parameter, whose
     * value must be an absolute or relative path name. If it is absolute, a
     * File is made based on the path name. If it is relative, a file is made by
     * resolving the path name with respect to the directory in which the file
     * was which defined this ParameterDatabase in the ParameterDatabase
     * hierarchy. If the parameter is not found, this returns null. The File is
     * not checked for validity. The parameter chosen is marked "used" if it
     * exists.
     */

    public File getFile(Parameter parameter, Parameter defaultParameter) 
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getFile(parameter);
        else
            return getFile(defaultParameter);
        }

    /**
     * Searches down through the databases to find a given parameter, whose
     * value must be an absolute or relative path name. If the parameter begins
     * with a "$", a file is made based on the relative path name and returned
     * directly. Otherwise, if it is absolute, a File is made based on the path
     * name, or if it is relative, a file is made by resolving the path name
     * with respect to the directory in which the file was which defined this
     * ParameterDatabase in the ParameterDatabase hierarchy. If the parameter is
     * not found, this returns null. The File is not checked for validity. The
     * parameter chosen is marked "used" if it exists.
     */

    File getFile(Parameter parameter) 
        {
        if (_exists(parameter)) 
            {
            String p = getParam(parameter);
            if (p == null)
                return null;
            if (p.startsWith(C_HERE))
                return new File(p.substring(C_HERE.length()));
            else if (p.startsWith(C_CLASS))
                return null;  // can't start with that.
            else 
                {
                File f = new File(p);
                if (f.isAbsolute())
                    return f;
                else
                    return new File(directoryFor(parameter), p);
                }
            } 
        else
            return null;
        }

    /**
     * Searches down through the databases to find a given parameter, whose
     * value must be an absolute or relative path name. If it is absolute, a
     * file is made based on the path name, and an InputStream is opened on 
     * the file and returned.  If the path name begins with "$", then an
     * InputStream is opened on a file relative to the directory where the
     * system was started.  Otherwise if the path name is relative, an InputStream is made by
     * resolving the path name with respect to the directory in which the file
     * was which defined this ParameterDatabase in the ParameterDatabase
     * hierarchy, be it in the file system or in a jar file.  If the parameter is not found, 
     * this returns null.  If no such file exists, null is also returned.
     * The parameter chosen is marked "used" if it exists.
     */

    public InputStream getResource(Parameter parameter, Parameter defaultParameter)
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getResource(parameter);
        else
            return getResource(defaultParameter);
        }

    int indexOfFirstWhitespace(String s)
        {
        int len = s.length();
        for(int i =0; i < len; i++)
            if (Character.isWhitespace(s.charAt(i)))
                return i;
        return -1;
        }

    InputStream getResource(Parameter parameter) 
        {
        try
            {
            if (_exists(parameter)) 
                {
                String p = getParam(parameter);
                if (p == null)
                    return null;
                if (p.startsWith(C_HERE))
                    return new FileInputStream(getFile(parameter));
                else if (p.startsWith(C_CLASS))
                    {
                    int i = indexOfFirstWhitespace(p);
                    if (i == -1)
                        return null;
                    String classname = p.substring(C_CLASS.length(),i);
                    String filename = p.substring(i).trim();
                    return Class.forName(classname, true, Thread.currentThread().getContextClassLoader()).getResourceAsStream(filename);
                    }
                else 
                    {
                    File f = new File(p);
                    if (f.isAbsolute())
                        return new FileInputStream(f);
                    Class c = getLocation(parameter.param).relativeClass;
                    String rp = getLocation(parameter.param).relativePath;
                    if (c != null)
                        {
                        return c.getResourceAsStream(new File(new File(rp).getParent(), p).getPath());
                        }
                    else
                        return new FileInputStream(new File(directoryFor(parameter), p));
                    }
                } 
            else
                return null;
            }
        catch (FileNotFoundException ex1) 
            { return null; }
        catch (ClassNotFoundException ex2) 
            { return null; } 
        }

    /**
     * Searches down through databases to find a given parameter. Returns the
     * parameter's value (trimmed) or null if not found or if the trimmed result
     * is empty. The parameter chosen is marked "used" if it exists.
     */

    public synchronized String getString(Parameter parameter,
        Parameter defaultParameter) 
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getString(parameter);
        else
            return getString(defaultParameter);
        }

    /**
     * Searches down through databases to find a given parameter. Returns the
     * parameter's value (trimmed) or null if not found or if the trimmed result
     * is empty. The parameter chosen is marked "used" if it exists.
     */

    /*protected*/ synchronized String getString(Parameter parameter) 
        {
        if (_exists(parameter))
            return getParam(parameter);
        else
            return null;
        }

    /**
     * Searches down through databases to find a given parameter. Returns the
     * parameter's value trimmed of whitespace, or defaultValue.trim() if the
     * result is not found or the trimmed result is empty.
     */
    public String getStringWithDefault(Parameter parameter,
        Parameter defaultParameter, String defaultValue) 
        {
        printGotten(parameter, defaultParameter, false);
        if (_exists(parameter))
            return getStringWithDefault(parameter, defaultValue);
        else
            return getStringWithDefault(defaultParameter, defaultValue);
        }

    /**
     * Searches down through databases to find a given parameter. Returns the
     * parameter's value trimmed of whitespace, or defaultValue.trim() if the
     * result is not found or the trimmed result is empty.
     */
    /*protected*/ String getStringWithDefault(Parameter parameter,
        String defaultValue) 
        {
        if (_exists(parameter)) 
            {
            String result = getParam(parameter);
            if (result == null) 
                {
                if (defaultValue == null)
                    return null;
                else
                    result = defaultValue.trim();
                } 
            else 
                {
                result = result.trim();
                if (result.length() == 0) 
                    {
                    if (defaultValue == null)
                        return null;
                    else
                        result = defaultValue.trim();
                    }
                }
            return result;
            } 
        else 
            {
            if (defaultValue == null)
                return null;
            else
                return defaultValue.trim();
            }
        }

    /** Clears the checked flag */
    /*protected*/ synchronized void uncheck() 
        {
        if (!checked)
            return; // we already unchecked this path -- this is dangerous if
                    // parents are used without children
        checked = false;
        int size = parents.size();
        for (int x = 0; x < size; x++)
            ((ParameterDatabase) (parents.elementAt(x))).uncheck();
        }
    
    /**
     * Sets a parameter in the topmost database to a given value, trimmed of
     * whitespace.
     */
    public synchronized void set(Parameter parameter, String value) 
        {
        String tmp = value.trim();
        properties.put(parameter.param, tmp);
        }

    /**
     * Prints out all the parameters marked as used, plus their values. If a
     * parameter was listed as "used" but not's actually in the database, the
     * value printed is UNKNOWN_VALUE (set to "?????")
     */

    public synchronized void listGotten(PrintWriter p) 
        {
        Vector vec = new Vector();
        Enumeration e = gotten.keys();
        while (e.hasMoreElements())
            vec.addElement(e.nextElement());

        // sort the keys
        Object[] array = new Object[vec.size()];
        java.util.Collections.sort(vec);
        vec.copyInto(array);

        // Uncheck and print each item
        for (int x = 0; x < array.length; x++) 
            {
            String s = (String) (array[x]);
            String v = null;
            if (s != null) 
                {
                v = (String) (_getParam(s));
                uncheck();
                }
            if (v == null)
                v = UNKNOWN_VALUE;
            p.println(s + " = " + v);
            }
        p.flush();
        }

    /** Prints out all the parameters NOT marked as used, plus their values. */

    public synchronized void listNotGotten(PrintWriter p) 
        {
        Vector vec = new Vector();

        Hashtable all = new Hashtable();
        _list(null, false, null, all); // grab all the nonshadowed keys
        Enumeration e = gotten.keys();
        while (e.hasMoreElements())
            all.remove(e.nextElement());
        e = all.keys();
        while (e.hasMoreElements())
            vec.addElement(e.nextElement());

        // sort the keys
        Object[] array = new Object[vec.size()];
        vec.copyInto(array);

        java.util.Collections.sort(vec);

        // Uncheck and print each item
        for (int x = 0; x < array.length; x++) 
            {
            String s = (String) (array[x]);
            String v = null;
            if (s != null) 
                {
                v = (String) (_getParam(s));
                uncheck();
                }
            if (v == null)
                v = UNKNOWN_VALUE;
            p.println(s + " = " + v);
            }
        p.flush();
        }

    /** Prints out all the parameters NOT marked as used, plus their values. */

    public synchronized void listNotAccessed(PrintWriter p) 
        {
        Vector vec = new Vector();

        Hashtable all = new Hashtable();
        _list(null, false, null, all); // grab all the nonshadowed keys
        Enumeration e = accessed.keys();
        while (e.hasMoreElements())
            all.remove(e.nextElement());
        e = all.keys();
        while (e.hasMoreElements())
            vec.addElement(e.nextElement());

        // sort the keys
        Object[] array = new Object[vec.size()];
        vec.copyInto(array);

        java.util.Collections.sort(vec);

        // Uncheck and print each item
        for (int x = 0; x < array.length; x++) 
            {
            String s = (String) (array[x]);
            String v = null;
            if (s != null) 
                {
                v = (String) (_getParam(s));
                uncheck();
                }
            if (v == null)
                v = UNKNOWN_VALUE;
            p.println(s + " = " + v);
            }
        p.flush();
        }

    /**
     * Prints out all the parameters marked as accessed ("gotten" by some
     * getFoo(...) method), plus their values. If this method ever prints
     * UNKNOWN_VALUE ("?????"), that's a bug.
     */

    public synchronized void listAccessed(PrintWriter p) 
        {
        Vector vec = new Vector();
        Enumeration e = accessed.keys();
        while (e.hasMoreElements())
            vec.addElement(e.nextElement());

        // sort the keys
        Object[] array = new Object[vec.size()];
        vec.copyInto(array);

        java.util.Collections.sort(vec);

        // Uncheck and print each item
        for (int x = 0; x < array.length; x++) 
            {
            String s = (String) (array[x]);
            String v = null;
            if (s != null) 
                {
                v = (String) (_getParam(s));
                uncheck();
                }
            if (v == null)
                v = UNKNOWN_VALUE;
            p.println(s + " = " + v);
            }
        p.flush();
        }

    /** Returns true if parameter exist in the database
        @deprecated use exists(Parameter, null) 
    */
    public synchronized boolean exists(Parameter parameter) 
        {
        printGotten(parameter, null, true);
        return _exists(parameter);
        }


    /*protected*/ synchronized boolean _exists(Parameter parameter) 
        {
        if (parameter == null) return false;
        String result = _getParam(parameter.param);
        uncheck();
        
        accessed.put(parameter.param, Boolean.TRUE);
        return (result != null);
        }

    /**
     * Returns true if either parameter or defaultParameter exists in the
     * database
     */
    public synchronized boolean exists(Parameter parameter,
        Parameter defaultParameter) 
        {
        printGotten(parameter, defaultParameter, true);
        if (exists(parameter))
            return true;
        if (exists(defaultParameter))
            return true;
        return false;
        }


    /*
      P: Successfully retrieved parameter
      !P: Unsuccessfully retrieved parameter
      <P: Would have retrieved parameter
      E: Successfully tested for existence of parameter
      !E: Unsuccessfully tested for existence of parameter
      <E: Would have tested for exidstence of parameter
    */

    /*protected*/ void printGotten(Parameter parameter, Parameter defaultParameter, boolean exists)
        {
        if (printState == PS_UNKNOWN)
            {
            Parameter p = new Parameter(PRINT_PARAMS);
            String jp = getParam(p);
            if (jp == null || jp.equalsIgnoreCase("false"))
                printState = PS_NONE;
            else
                printState = PS_PRINT_PARAMS;
            uncheck();
            printGotten(p,null,false);
            }

        if (printState == PS_PRINT_PARAMS)
            {
            String p = "P: ";
            if (exists) p = "E: ";
            
            if (parameter==null && defaultParameter == null) 
                return;
                
            else if (parameter == null)
                {
                String result = _getParam(defaultParameter.param);
                uncheck();
                if (result == null)
                    // null parameter, didn't find defaultParameter
                    System.err.println("\t!" + p +defaultParameter.param);
                else 
                    // null parameter, found defaultParameter
                    System.err.println("\t " + p +defaultParameter.param + " = " + result);
                }
            
            else if (defaultParameter == null)
                {
                String result = _getParam(parameter.param);
                uncheck();
                if (result == null)
                    // null defaultParameter, didn't find parameter
                    System.err.println("\t!" + p +parameter.param);
                else 
                    // null defaultParameter, found parameter
                    System.err.println("\t " + p +parameter.param+ " = " + result);
                }
            
            else
                {
                String result = _getParam(parameter.param);
                uncheck();
                if (result == null)
                    {
                    // didn't find parameter
                    System.err.println("\t!" + p +parameter.param);
                    result = _getParam(defaultParameter.param);
                    uncheck();
                    if (result == null)
                        // didn't find defaultParameter
                        System.err.println("\t!" + p +defaultParameter.param);
                    else 
                        // found defaultParameter
                        System.err.println("\t " + p +defaultParameter.param+ " = " + result);
                    }
                else 
                    {
                    // found parameter
                    System.err.println("\t " + p +parameter.param+ " = " + result);
                    System.err.println("\t<" + p +defaultParameter.param);
                    }
                }
            }
        }

    /*protected*/ synchronized String getParam(Parameter parameter) 
        {
        String result = _getParam(parameter.param);
        uncheck();

        // set hashtable appropriately
        accessed.put(parameter.param, Boolean.TRUE);
        gotten.put(parameter.param, Boolean.TRUE);
        return result;
        }
    
    /** Private helper function */
    synchronized String _getRecursive(String parameter) 
        {
        if (parameter == null)
            {
            return null;
            }
        if (checked)
            return null; // we already searched this path
        checked = true;
        String result = properties.getProperty(parameter);
        if (result == null) 
            {
            int size = parents.size();
            for (int x = 0; x < size; x++) 
                {
                result = ((ParameterDatabase) (parents.elementAt(x)))._getRecursive(parameter);
                if (result != null)
                    {
                    return result;
                    }
                }
            } 
        else  // preprocess
            {
            result = result.trim();
            if (result.length() == 0)
                result = null;
            }
        return result;
        }


    synchronized String _getParam(String parameter)
        {
        try
            {
            return _getInner(parameter);
            }
        catch (RuntimeException ex)
            {
            System.err.println("Parameter Database Error: " + ex.getMessage());
            return null;
            }
        }
    
    /** Private helper function */
    synchronized String _getInner(String parameter) 
        {
        if (parameter == null) 
            {
            this.popped = "";
            return null;
            }
        
        String result = _getRecursive(parameter);
        uncheck();
        
        int lastDelim = parameter.lastIndexOf(Parameter.delimiter);
        String top = null;

        if (result == null) 
            {
            // if parameter not found and there are no more delimiters (can't search for alias or defaults)
            if (lastDelim == -1) 
                {
                aliases = new Hashtable();
                return null;
                }

            else 
                {
                top = parameter.substring(lastDelim + 1);
                parameter = parameter.substring(0, lastDelim);

                // if you didn't find a parameter look for a default
                if (!top.equals("default") && !top.equals("alias")) 
                    {
                    if (this.popped.equals("")) 
                        this.popped = top;
                    else this.popped = top + Parameter.delimiter + this.popped;
                    result = _getInner(parameter + Parameter.delimiter + "default");
                    }

                //if you just looked for a default and didn't find anything
                else if (top.equals("default")) 
                    {
                    // look for an alias
                    if (aliases.get(parameter + Parameter.delimiter + "alias") == null) 
                        {
                        result = _getInner(parameter + Parameter.delimiter + "alias");
                        } 
                    else 
                        {
                        aliases = new Hashtable();
                        return null;
                        }
                    }

                // if you just looked for an alias and didn't find anything
                else 
                    {
                    //go one level higher and look for a default
                    lastDelim = parameter.lastIndexOf(Parameter.delimiter);
                    if (lastDelim==-1) 
                        {
                        aliases = new Hashtable();
                        return null;
                        } 
                    else 
                        {
                        top = parameter.substring(lastDelim+1);
                        parameter = parameter.substring(0,lastDelim);
                        this.popped = top + Parameter.delimiter + this.popped;
                        result = _getInner(parameter + Parameter.delimiter + "default");
                        }
                    }
                }
            }
        else 
            { // parameter found

            top = parameter.substring(lastDelim + 1);
            if (top.equals("alias")) 
                {  
                // if alias is found replace original parameter with aliased parameter and look again
                aliases.put(parameter,result);
                result = _getInner(result + Parameter.delimiter + this.popped); 
                } 

            else 
                { // found an actual result
                this.popped = "";
                result = result.trim();
                if (result.length() == 0) 
                    {
                    aliases = new Hashtable();
                    result = null;
                    }
                }

            }

        aliases = new Hashtable();
        return result;
        }


    public ParameterDatabase getLocation(Parameter parameter)
        {
        return getLocation(parameter.param);
        }
        
    public synchronized ParameterDatabase getLocation(String parameter) 
        {
        ParameterDatabase loc = _getLocation(parameter);
        uncheck();
        return loc;
        }

    /** Private helper function */
    synchronized ParameterDatabase _getLocation(String parameter) 
        {
        if (parameter == null)
            return null;
        if (checked)
            return null; // we already searched this path
        checked = true;
        String result = properties.getProperty(parameter);
        if (result == null) 
            {
            int size = parents.size();
            ParameterDatabase loc = null;
            for (int x = 0; x < size; x++) 
                {
                loc = ((ParameterDatabase) (parents.elementAt(x)))._getLocation(parameter);
                if (loc != null)
                    {
                    return loc;
                    }
                }
            return null;
            } 
        else return this;
        }


    /*protected*/ synchronized Set _getShadowedValues(Parameter parameter, Set vals) 
        {
        if (parameter == null) 
            {
            return vals;
            }
        
        if (checked) 
            {
            return vals;
            }
        
        checked = true;
        String result = properties.getProperty(parameter.param);
        if (result != null) 
            {
            result = result.trim();
            if (result.length() != 0)
                vals.add(result);
            }
        
        int size = parents.size();
        for (int i = 0; i < size; ++i) 
            {
            ((ParameterDatabase)parents.elementAt(i))._getShadowedValues(parameter,vals);
            }

        return vals;
        }

    public Set getShadowedValues(Parameter parameter) 
        {
        Set vals = new HashSet();
        vals = _getShadowedValues(parameter, vals);
        uncheck();
        return vals;
        }
    
    /**
     * Searches down through databases to find the directory for the database
     * which holds a given parameter. Returns the directory name or null if not
     * found.
     */

    File directoryFor(Parameter parameter) 
        {
        File result = _directoryFor(parameter);
        uncheck();
        return result;
        }
    
    /** Private helper function */
    synchronized File _directoryFor(Parameter parameter) 
        {
        if (checked)
            return null; // we already searched this path
        checked = true;
        File result = null;
        String p = properties.getProperty(parameter.param);
        if (p == null) 
            {
            int size = parents.size();
            for (int x = 0; x < size; x++) 
                {
                result = ((ParameterDatabase) (parents.elementAt(x)))._directoryFor(parameter);
                if (result != null)
                    return result;
                }
            return result;
            } 
        else
            return directory;
        }
    
    /** Returns a String describing the location of the ParameterDatabase holding
        this parameter, or "" if there is none. */
    public String getLabel()
        {
        return label; 
        }
        
    /*
     * Searches down through databases to find the parameter file 
     * which holds a given parameter. Returns the filename or null if not
     * found.
     *
     * @deprecated You probably want to use getLocation
     */
    /*
      public File fileFor(Parameter parameter) 
      {
      File result = _fileFor(parameter);
      uncheck();
      return result;
      }
      synchronized File _fileFor(Parameter parameter) 
      {
      if (checked)
      return null;
        
      checked = true;
      File result = null;
      String p = getProperty(parameter.param);
      if (p==null) 
      {
      int size = parents.size();
      for(int i = 0; i < size; ++i) 
      {
      result = ((ParameterDatabase)parents.elementAt(i))._fileFor(parameter);
      if (result != null)
      return result;
      }
      return result;
      }
      else
      return new File(directory,filename);
      }
    */

    /** Removes a parameter from the topmost database. */
    public synchronized void remove(Parameter parameter) 
        {
        if (parameter.param.equals(PRINT_PARAMS)) printState = PS_UNKNOWN;
        properties.remove(parameter.param);
        }

    /*
      Removes a parameter from the database and all its parent databases. 
      @deprecated  You shouldn't modify parent databases
    */
    public synchronized void removeDeeply(Parameter parameter) 
        {
        _removeDeeply(parameter);
        uncheck();
        }

    /** Private helper function */
    synchronized void _removeDeeply(Parameter parameter) 
        {
        if (checked)
            return; // already removed from this path
        checked = true;
        remove(parameter);
        int size = parents.size();
        for (int x = 0; x < size; x++)
            ((ParameterDatabase) (parents.elementAt(x)))
                .removeDeeply(parameter);
        }

    public void addParent(ParameterDatabase database)
        {
        parents.addElement(database);
        }

    /** Creates an empty parameter database. */
    public ParameterDatabase() 
        {
        properties = new Properties();
        accessed = new Hashtable();
        gotten = new Hashtable();
        directory = new File(new File("").getAbsolutePath()); // uses the user
                                                              // path
        //filename = "";
        label = "Basic Database";
        parents = new Vector();
        checked = false; // unnecessary
        }
    
    /** Creates a new parameter database from the given Dictionary.  
        Both the keys and values will be run through toString() before adding to the dataase.   
        Keys are parameters.  Values are the values of the parameters.  
        Beware that a ParameterDatabase is itself a Dictionary; but if you pass one in here you 
        will only get the lowest-level elements.  If parent.n are defined, parents will 
        be attempted to be loaded -- that's the reason for the FileNotFoundException and IOException.  */
    public ParameterDatabase(java.util.Dictionary map) throws FileNotFoundException, IOException 
        {
        this();
        label = "Dictionary: " + System.identityHashCode(map);
        java.util.Enumeration keys = map.keys();
        while(keys.hasMoreElements())
            {
            Object obj = keys.nextElement();
            set(new Parameter(""+obj),""+map.get(obj));
            }

        // load parents
        for (int x = 0;; x++) 
            {
            String s = properties.getProperty("parent." + x);
            if (s == null)
                return; // we're done

            if (new File(s).isAbsolute()) // it's an absolute file definition
                parents.addElement(new ParameterDatabase(new File(s)));
            else throw new FileNotFoundException("Attempt to load a relative file, but there's no parent file: " + s);
            }
        }

    
    //// The following four functions are used to modify the paths inside URLs representing
    //// the internals of Jar files.  This is because getResource and getResourceAsStream are
    //// broken with regard to resources in Jar files where the internal path has 
    //// ../ or ./ in it -- they return null rather than  just normalizing the path.  This 
    //// *isn't* the case for file: URLs, which is irritatingly inconsistent.  So we have to
    //// modify Jar file URLs specially.  To do this we need to (1) build a default URL for 
    //// a class, which is hard because for some reason resource URLs in Java can't be
    //// pointing to directories, or even to nonexistent files, but have to point to real
    //// files, grrrr, (2) know if a URL is a jar file URL, (3) takes a default Jar file
    //// URL, plus a relative path, and figures out what the internal path should be if
    //// revised using the relative path, and finally (4) takes a default Jar file
    //// URL, plus a relative path, and builds a new URL using the revised internal path.
    /// Hence the four functions.
    
    // Builds a default resource URL for a given class.  For this URL we use the class file
    // itself.  For example, if the class Foo were stored as /ec/util/Foo.class inside the
    // Jar file ecj.jar, we might build a URL of the form
    // jar:file:ecj.jar!/ec/util/Foo.class
    // This might break for unusual class file names (like Foo$12.class)
    static URL defaultResourceURL(Class cls) 
        { return cls.getResource(cls.getSimpleName() + ".class"); }
    
    // Returns whether or not a URL refers to something inside a Jar file.  We do this by
    // just checking if the protocol is 'jar'.
    static boolean isJarFile(URL url) 
        { return url.getProtocol().equalsIgnoreCase("jar"); }

    // Given a URL referring to something in a Jar file, removes the final filename from
    // the end of the existing internal path inside the Jar file, then revises the
    // path to point to the provided path relative to that path.  Returns the resultant
    // path.  For example, if the URL was jar:file:/private/tmp/ecj.jar!/ec/app/ant/Ant.class
    // and the relative path was ../../gp/koza.params then the function would return
    // /ec/gp/koza.params
    static String concatenatedJarPath(URL original, String path)
        {
        // A Jar URL looks like this:  jar:URLtoJarFile!/path/to/resource/in/jar
        // For example: jar:file:/private/tmp/ecj.jar!/ec/app/ant/Ant.class

        // Given another path to tack on, say ../../gp/koza.params
        // The goal is to return the string  "/ec/gp/koza.params"

        // 1. Identify the path start and extract the path
        // /ec/app/ant/Ant.class
        String url = original.toString();
        int i;
        for(i = url.length() - 2; i >= 0; i--)
            if (url.charAt(i) == '!' &&
                url.charAt(i+1) == '/')  // PROBABLY it
                {
                break;
                }

        if (i < 0) // uh oh
            return null;

        String originalPath = url.substring(i+1);  // don't include the !

        if (path.startsWith("/"))  // it's absolute
            {
            // 2. If the replacement path is absolute, just use that.
            originalPath = path;  // just replace it
            }
        else
            {
            // 3. Else remove the file
            // /ec/app/ant/
            int j;
            for(j = originalPath.length() - 1; j >= 0; j--)
                if (originalPath.charAt(j) == '/')  // PROBABLY is it
                    {
                    break;
                    }
            if (j < 0) // uh oh
                return null;

            originalPath = originalPath.substring(0, j+1);  // include the slash

            // 4. Tack on the replacement path
            // /ec/app/ant/../../gp/koza.params
            originalPath += path;
            }

        // 5. Simplify
        // /ec/gp/koza.params
        return simplifyPath(originalPath);
        }


    // Given a URL referring to something in a Jar file, removes the final filename from
    // the end of the existing internal path inside the Jar file, then revises the
    // path to point to the provided path relative to that path.  Returns the resultant
    // URL.  For example, if the URL was jar:file:/private/tmp/ecj.jar!/ec/app/ant/Ant.class
    // and the relative path was ../../gp/koza.params then the function would return
    // the URL jar:file:/private/tmp/ecj.jar!/ec/gp/koza.params

    static URL concatenatedJarResource(URL original, String path)
        {
        // A Jar URL looks like this:  jar:URLtoJarFile!/path/to/resource/in/jar
        // For example: jar:file:/private/tmp/ecj.jar!/ec/app/ant/Ant.class

        // Given another path to tack on, say ../../gp/koza.params
        // We need to edit this as follows:

        // 0. Identify the path start and extract the path
        // /ec/app/ant/Ant.class
        String url = original.toString();
        int i;
        for(i = url.length() - 2; i >= 0; i--)
            if (url.charAt(i) == '!' &&
                url.charAt(i+1) == '/')  // PROBABLY it
                {
                break;
                }

        if (i < 0) // uh oh
            return null;

        // see concatenatedJarPath for further procedures...
        String revisedPath = concatenatedJarPath(original, path);

        // 6. Put back in URL
        // jar:file:/private/tmp/ecj.jar!/ec/gp/koza.params
        try
            {
            return new URL(url.substring(0, i + 1) + revisedPath);
            }
        catch (MalformedURLException e)
            {
            return null;
            }
        }
        


    // Eliminates .. and . from a relative path without converting it
    // according to the file system. For example,
    // "hello/there/../how/./are/you/yo/../../hey" becomes
    // "hello/how/are/hey".  This is useful for cleaning up path names for
    // URLs.
    static String simplifyPath(String pathname)
        {
        File path = new File(pathname);
        ArrayList a = new ArrayList();
        while(path != null && path.getName() != null)
            {
            String n = path.getName();
            a.add(n);
            path = path.getParentFile();
            }
        
        ArrayList b = new ArrayList();
        for(int i = a.size() - 1; i >= 0; i--)
            {
            String n = (String)(a.get(i));
            if (n.equals(".")) 
                { } // do nothing
            else if (n.equals("..") &&
                b.size() != 0 && !b.get(0).equals(".."))
                b.remove(b.size() - 1);  
            else b.add(n);
            }
        
        if (b.size() == 0) return "";
        
        path = new File((String)(b.get(0)));
        for(int i = 1; i < b.size(); i++)
            {
            path = new File(path, (String)(b.get(i)));
            }


        // Convert to "\" for windows
        String returnPath;
        if(File.separatorChar != '/')
            returnPath = path.getPath().replace(File.separatorChar, '/');
        else 
            returnPath = path.getPath();
        return returnPath;
        }




    /**
     * Creates a new parameter database from a given database file and argv
     * list. The top-level database is completely empty, pointing to a second
     * database which contains the parameter entries stored in args, which
     * points to a tree of databases constructed using
     * ParameterDatabase(filename).
     */

    public ParameterDatabase(String pathNameRelativeToClassFile, Class cls, String[] args) throws FileNotFoundException, IOException 
        {
        this();
        label = "" + cls + " : " + pathNameRelativeToClassFile;

        ParameterDatabase files = new ParameterDatabase(pathNameRelativeToClassFile, cls);

        // Create the Parameter Database for the arguments
        ParameterDatabase a = new ParameterDatabase();
        a.relativeClass = cls;
        a.relativePath = files.relativePath;

        a.parents.addElement(files);
        boolean hasArgs = false;
        for (int x = 0; x < args.length - 1; x++) 
            {
            if (args[x].equals("-p"))
                {
                String s = args[x+1].trim();
                if (s.length() == 0) continue;  // failure
                int eq = s.indexOf('=');  // look for the '='
                if (eq <= 0) continue; // '=' isn't there, or it's the first char: failure                      
                set(new Parameter(s.substring(0,eq)), s.substring(eq+1));  // add the parameter
                if (!hasArgs)
                    {
                    label = label + "    Args:  ";
                    hasArgs = true;
                    }
                label = label + s + "  ";
                }
            }

        // Set me up
        relativeClass = cls;
        relativePath = files.relativePath;

        parents.addElement(a);
        }


    /** Creates a new parameter database loaded from a parameter file located relative to a class file,
        wherever the class file may be (such as in a jar).
        This approach uses resourceLocation.getResourceAsStream() to load the parameter file.
        If parent.n are defined, parents will be attempted to be loaded -- that's 
        the reason for the FileNotFoundException and IOException. */

    public ParameterDatabase(String pathNameRelativeToClassFile, Class cls) throws FileNotFoundException, IOException 
        {
        this();
        label = "" + cls + " : " + pathNameRelativeToClassFile;
        
        URL def = defaultResourceURL(cls);
        relativeClass = cls;
        try
            {
            if (isJarFile(def))
                {
                // loading from jar file, handle it specially.  This is because
                // file URLs can handle ../ etc but jar urls CANNOT, stupid Java
                relativePath = concatenatedJarPath(def, pathNameRelativeToClassFile);
                properties.load(concatenatedJarResource(def, pathNameRelativeToClassFile).openStream());
                }
            else
                {
                relativePath = simplifyPath(pathNameRelativeToClassFile);
                InputStream f = cls.getResourceAsStream(relativePath);
                properties.load(f);
                try { f.close(); } catch (IOException e) 
                    { }
                }
            }
        catch (NullPointerException e)
            {
            throw new IOException("Could not load database from resource file " + relativePath +
                " relative to the class " + cls, e);
            }
        catch (IOException e)
            {
            throw new IOException("Could not load database from resource file " + relativePath +
                " relative to the class " + cls, e);
            }

        // load parents
        for (int x = 0 ; ; x++) 
            {
            String s = properties.getProperty("parent." + x);
            if (s == null)
                return; // we're done

            if (new File(s).isAbsolute()) // it's an absolute file definition
                parents.addElement(new ParameterDatabase(new File(s)));
            else if (s.startsWith(C_CLASS))
                {
                int i = indexOfFirstWhitespace(s);
                if (i == -1) throw new FileNotFoundException("Could not parse file into filename and classname:\n\tparent." + x + " = " + s);
                String classname = s.substring(C_CLASS.length(),i);
                String filename = s.substring(i).trim();
                try
                    {
                    parents.addElement(new ParameterDatabase(filename, Class.forName(classname, true, Thread.currentThread().getContextClassLoader())));
                    }
                catch (ClassNotFoundException ex)
                    {
                    throw new FileNotFoundException("Could not parse file into filename and classname:\n\tparent." + x + " = " + s);
                    }
                }
            else
                {
                String path = new File(new File(pathNameRelativeToClassFile).getParent(), s).toString();
                parents.addElement(new ParameterDatabase(path, cls));
                }
            }
        }


    /** Creates a new parameter database loaded from the given stream.  Non-relative parents are not permitted.
        If parent.n are defined, parents will be attempted to be loaded -- that's 
        the reason for the FileNotFoundException and IOException. */

    public ParameterDatabase(java.io.InputStream stream) throws FileNotFoundException, IOException 
        {
        this();
        label = "Stream: " + System.identityHashCode(stream);
        properties.load(stream);

        // load parents
        for (int x = 0;; x++) 
            {
            String s = properties.getProperty("parent." + x);
            if (s == null)
                return; // we're done

            if (new File(s).isAbsolute()) // it's an absolute file definition
                parents.addElement(new ParameterDatabase(new File(s)));
            else if (s.startsWith(C_CLASS))
                {
                int i = indexOfFirstWhitespace(s);
                if (i == -1) throw new FileNotFoundException("Could not parse file into filename and classname:\n\tparent." + x + " = " + s);
                String classname = s.substring(C_CLASS.length(),i);
                String filename = s.substring(i).trim();
                try
                    {
                    parents.addElement(new ParameterDatabase(filename, Class.forName(classname, true, Thread.currentThread().getContextClassLoader())));
                    }
                catch (ClassNotFoundException ex)
                    {
                    throw new FileNotFoundException("Could not parse file into filename and classname:\n\tparent." + x + " = " + s);
                    }
                }
            else throw new FileNotFoundException("Attempt to load a relative file, but there's no parent file: " + s);
            }
        }


    /**
     * Creates a new parameter database tree from a given database file and its
     * parent files.
     */
    public ParameterDatabase(File file) throws FileNotFoundException, IOException 
        {
        this();
        label = "File: " + file.getPath();
        //this.file = file.getName();
        directory = new File(file.getParent()); // get the directory
        // file is in
        FileInputStream f = new FileInputStream(file);
        properties.load(f);
        try { f.close(); } catch (IOException e) 
            { }
                
        // load parents
        for (int x = 0;; x++) 
            {
            String s = properties.getProperty("parent." + x);
            if (s == null)
                return; // we're done

            if (new File(s).isAbsolute()) // it's an absolute file definition
                parents.addElement(new ParameterDatabase(new File(s)));
            else if (s.startsWith(C_CLASS))
                {
                int i = indexOfFirstWhitespace(s);
                if (i == -1) throw new FileNotFoundException("Could not parse file into filename and classname:\n\tparent." + x + " = " + s);
                String classname = s.substring(C_CLASS.length(),i);
                String fname = s.substring(i).trim();
                try
                    {
                    parents.addElement(new ParameterDatabase(fname, Class.forName(classname, true, Thread.currentThread().getContextClassLoader())));
                    }
                catch (ClassNotFoundException ex)
                    {
                    throw new FileNotFoundException("Could not parse file into filename and classname:\n\tparent." + x + " = " + s);
                    }
                }
            else
                // it's relative to my path
                parents.addElement(new ParameterDatabase(new File(file.getParent(), s)));
            }
        }

    /**
     * Creates a new parameter database from a given database file and argv
     * list. The top-level database is completely empty, pointing to a second
     * database which contains the parameter entries stored in args, which
     * points to a tree of databases constructed using
     * ParameterDatabase(file).
     */

    public ParameterDatabase(File file, String[] args) throws FileNotFoundException, IOException 
        {
        this();
        label = "File: " + file.getPath();
        //this.file = file.getName();
        directory = new File(file.getParent()); // get the directory
        // file is in

        // Create the Parameter Database tree for the files
        ParameterDatabase files = new ParameterDatabase(file);

        // Create the Parameter Database for the arguments
        ParameterDatabase a = new ParameterDatabase();
        a.parents.addElement(files);
        boolean hasArgs = false;
        for (int x = 0; x < args.length - 1; x++) 
            {
            if (args[x].equals("-p"))
                {
                String s = args[x+1].trim();
                if (s.length() == 0) continue;  // failure
                int eq = s.indexOf('=');  // look for the '='
                if (eq <= 0) continue; // '=' isn't there, or it's the first char: failure                      
                set(new Parameter(s.substring(0,eq)), s.substring(eq+1));  // add the parameter
                if (!hasArgs)
                    {
                    label = label + "    Args:  ";
                    hasArgs = true;
                    }
                label = label + s + "  ";
                }
            }

        // Set me up
        parents.addElement(a);
        }

    /**
     * Prints out all the parameters in the database, but not shadowed
     * parameters.
     */
    public void list(PrintWriter p) 
        {
        list(p, false);
        }

    /**
     * Prints out all the parameters in the database. Useful for debugging. If
     * listShadowed is true, each parameter is printed with the parameter
     * database it's located in. If listShadowed is false, only active
     * parameters are listed, and they're all given in one big chunk.
     */
    public void list(PrintWriter p, boolean listShadowed) 
        {
        if (listShadowed)
            _list(p, listShadowed, "root", null);
        else 
            {
            Hashtable gather = new Hashtable();
            _list(null, listShadowed, "root", gather);

            Vector vec = new Vector();
            Enumeration e = gather.keys();
            while (e.hasMoreElements())
                vec.addElement(e.nextElement());

            java.util.Collections.sort(vec);

            // Uncheck and print each item
            for (int x = 0; x < vec.size(); x++) 
                {
                String s = (String) vec.get(x);
                String v = null;
                if (s != null)
                    v = (String) gather.get(s);
                if (v == null)
                    v = UNKNOWN_VALUE;
                if (p!=null) p.println(s + " = " + v);
                }
            }
        if (p!=null) p.flush();
        }

    /** Private helper function. */
    void _list(PrintWriter p, boolean listShadowed,
        String prefix, Hashtable gather) 
        {
        if (listShadowed) 
            {
            // Print out my header
            if (p!=null)
                {
                p.println("\n########" + prefix);
                properties.list(p);
                }
            int size = parents.size();
            for (int x = 0; x < size; x++)
                ((ParameterDatabase) (parents.elementAt(x)))._list(p,
                    listShadowed, prefix + "." + x, gather);
            } 
        else 
            {
            // load in reverse order so things get properly overwritten
            int size = parents.size();
            for (int x = size - 1; x >= 0; x--)
                ((ParameterDatabase) (parents.elementAt(x)))._list(p,
                    listShadowed, prefix, gather);
            Enumeration e = properties.keys();
            while (e.hasMoreElements()) 
                {
                String key = (String) (e.nextElement());
                gather.put(key, properties.get(key));
                }
            }
        if (p!=null) p.flush();
        }

    public String toString() 
        {
        String s = super.toString();
        if (parents.size() > 0) 
            {
            s += " : (";
            for (int x = 0; x < parents.size(); x++) 
                {
                if (x > 0)
                    s += ", ";
                s += parents.elementAt(x);
                }
            s += ")";
            }
        return s;
        }

    /**
     * Builds a TreeModel from the available property keys.   
     */
    public TreeModel buildTreeModel() 
        {
        //String sep = System.getProperty("file.separator");
        ParameterDatabaseTreeNode root = new ParameterDatabaseTreeNode(
            //this.directory.getAbsolutePath() + sep + this.filename);
            label);
        ParameterDatabaseTreeModel model = new ParameterDatabaseTreeModel(root);

        _buildTreeModel(model, root);

        model.sort(root, new Comparator() 
            {
            public int compare(Object o1, Object o2) 
                {
                ParameterDatabaseTreeNode t1 = (ParameterDatabaseTreeNode)o1;
                ParameterDatabaseTreeNode t2 = (ParameterDatabaseTreeNode)o2;
                
                return ((Comparable)t1.getUserObject()).compareTo(t2.getUserObject());
                }
            });

        // In order to add elements to the tree model, the leaves need to be
        // visible. This is because some properties have values *and* sub-
        // properties. Otherwise, if the nodes representing these properties did
        // not yet have children, then they would be invisible and the tree model
        // would be unable to add child nodes to them.
        model.setVisibleLeaves(false);
        
        return model;
        }

    void _buildTreeModel(DefaultTreeModel model,
        DefaultMutableTreeNode root) 
        {
        Enumeration e = properties.keys();
        while (e.hasMoreElements()) 
            {
            _addNodeForParameter(model, root, (String)e.nextElement());
            }

        int size = parents.size();
        for (int i = 0; i < size; ++i) 
            {
            ParameterDatabase parentDB = (ParameterDatabase) parents
                .elementAt(i);
            parentDB._buildTreeModel(model, root);
            }
        }

    /**
     * @param model
     * @param root
     * @param e
     */
    void _addNodeForParameter(DefaultTreeModel model, DefaultMutableTreeNode root, String key) 
        {
        if (key.indexOf("parent.") == -1) 
            {
            /* 
             * TODO split is new to 1.4.  To maintain 1.2 compatability we need
             * to use a different approach.  Just use a string tokenizer.
             */ 
            StringTokenizer tok = new StringTokenizer(key,".");
            String[] path = new String[tok.countTokens()];
            int t = 0;
            while(tok.hasMoreTokens()) 
                {
                path[t++] = tok.nextToken();
                }
            DefaultMutableTreeNode parent = root;

            for (int i = 0; i < path.length; ++i) 
                {
                int children = model.getChildCount(parent);
                if (children > 0) 
                    {
                    int c = 0;
                    for (; c < children; ++c) 
                        {
                        DefaultMutableTreeNode child = 
                            (DefaultMutableTreeNode) parent.getChildAt(c);
                        if (child.getUserObject().equals(path[i])) 
                            {
                            parent = child;
                            break;
                            }
                        }

                    if (c == children) 
                        {
                        DefaultMutableTreeNode child = 
                            new ParameterDatabaseTreeNode(path[i]);
                        model.insertNodeInto(child, parent, 
                            parent.getChildCount());
                        parent = child;
                        }
                    }
                // If the parent has no children, just add the node.
                else 
                    {
                    DefaultMutableTreeNode child = 
                        new ParameterDatabaseTreeNode(path[i]);
                    model.insertNodeInto(child, parent, 0);
                    parent = child;
                    }
                }
            }
        }

    /**
     * Test the ParameterDatabase
     */
    public static void main(String[] args)
        throws FileNotFoundException, IOException
        {
        ParameterDatabase pd = new ParameterDatabase(new File(args[0]), args);
        pd.set(new Parameter("Hi there"), "Whatever");
        pd.set(new Parameter(new String[]
            {
            "1", "2", "3"
            }), " Whatever ");
        pd.set(new Parameter(new String[]
            {
            "a", "b", "c"
            }).pop().push("d"),
            "Whatever");

        System.err.println("\n\n PRINTING ALL PARAMETERS \n\n");
        pd.list(new PrintWriter(System.err, true), true);
        System.err.println("\n\n PRINTING ONLY VALID PARAMETERS \n\n");
        pd.list(new PrintWriter(System.err, true), false);
        }
    }
