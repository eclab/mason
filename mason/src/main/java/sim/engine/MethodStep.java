/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.engine;
import java.lang.reflect.*;
import sim.util.*;

/** 
    A Steppable which calls an underlying method using Java's reflection system.  The underlying method can either have no arguments
    or have one argumen (SimState), and is specified by its name as a String.

    <p>You can use MethodStep to call methods on classes that aren't Steppables.  For example, if you have an object <b>myObject</b>
    and wish to submit both its <b>foo</b> and <b>bar</b> methods (no arguments) to be called at various times when the Schedule
    sees fit, you might do this:

    <pre><tt>
    MyClass myObject = ... ;
    schedule.scheduleRepeating( ... , new MethodStep(myObject, "foo"), ...);
    schedule.scheduleRepeating( ... , new MethodStep(myObject, "bar"), ...);
    </tt></pre>

    <p>This will call the <b>foo()</b> method and <b>bar()</b> method to be called at the appropriate times on myObject. 

    <p>You can also use MethodStep to pop up objects which aren't Steppables by their nature.  For example:

    <pre><tt>
    JFrame myJFrame = ... ;
    schedule.schedule( ... , new MethodStep(myJFrame, "show"), ...);
    </tt></pre>

    <p>MethodStep can also be called on methods which expect to be passed in the Steppable's <b>SimState</b> argument.  When
    the MethodStep is called, it passes its argument to them.  You do this by adding a <b>true</b> to the constructor:

    <pre><tt>
    MyClass myObject = ... ;
    schedule.scheduleRepeating( ... , new MethodStep(myObject, "baz", true), ...);
    schedule.scheduleRepeating( ... , new MethodStep(myObject, "quux", true), ...);
    </tt></pre>

    <p>This will call the <b>baz(SimState)</b> method and <b>quux(SimState)</b> method to be called at the appropriate times on myObject.

    <p>This method is mostly to help in ease of porting: but it's not good Java.  Reflection is slow and violates all sorts of design
    contracts -- generally speaking, you should use anonymous Steppables intead.  For example, the examples above could have instead been
    written this way:

    <pre><tt>
    final MyClass myObject = ... ;
    final JFrame myJFrame = ... ;
    schedule.scheduleRepeating( ... , new Steppable() { public void step(SimState state) { myObject.foo() } }, ...);
    schedule.scheduleRepeating( ... , new Steppable() { public void step(SimState state) { myObject.foo() } }, ...);
    schedule.schedule( ... , new Steppable() { public void step(SimState state) { myJFrame.show() } }, ...);
    schedule.scheduleRepeating( ... , new Steppable() { public void step(SimState state) { myObject.baz(state) } }, ...);
    schedule.scheduleRepeating( ... , new Steppable() { public void step(SimState state) { myObject.quux(state) } }, ...);
    </tt></pre>
*/

public class MethodStep implements Steppable
    {
    private static final long serialVersionUID = 1;

    Method method;
    Object object;
    boolean passInSimState;
        
    public MethodStep(Object object, String methodName)
        {
        this(object,methodName,false);
        }
                        
    public MethodStep(Object object, String methodName, boolean passInSimState)
        {
        this.object = object;
        this.passInSimState = passInSimState;
        if (object==null)
            {
            throw new NullPointerException("MethodStep asked to call the method " + methodName + (passInSimState ? "\"(SimState state)" : "\"") + " on a null object");
            }
        try
            {
            if (passInSimState)
                this.method = object.getClass().getMethod(methodName, new Class[]{SimState.class});
            else
                this.method = object.getClass().getMethod(methodName, new Class[]{});
            }
        catch (NoSuchMethodException ex)  // make runtime exception
            {
            throw new RuntimeException("Could not find a public method called \"" + methodName + (passInSimState ? "\"(SimState state)" : "\"") + " in the class " + object.getClass());
            }
        catch (SecurityException ex)
            {
            throw new RuntimeException("Could not find a public method called \"" + methodName + (passInSimState ? "\"(SimState state)" : "\"") + " in the class " + object.getClass());
            }
        }
                
    public void step(final SimState state)
        {
        try
            {
            if (passInSimState) method.invoke(object, new Object[] { state });
            else method.invoke(object, (Object[]) null);
            }
        catch (IllegalAccessException ex)  // make runtime exception
            {
            // generally should not happen -- the getMethod() method should only return public methods
            throw new RuntimeException("Could not find a public method called \"" + method + (passInSimState ? "\"(SimState state)" : "\"") + " in the class " + object.getClass());
            }
        catch (IllegalArgumentException ex)
            {
            throw new RuntimeException("Could not find a public method called \"" + method + (passInSimState ? "\"(SimState state)" : "\"") + " in the class " + object.getClass());
            }
        catch (InvocationTargetException ex)
            {
            /** At the moment, Java 1.3 cannot do causes, so we don't include this for compatability reasons.  Maybe we'll uncomment it later. */
            /*
              Throwable t = new RuntimeException("On calling \"" + methodName +"\" in the class " + object.getClass() + 
              ", an Exception was raised in the called method.");
              t.initCause(ex.getCause());
              throw t;
            */
            throw new RuntimeException("On calling \"" + method + (passInSimState ? "\"(SimState state)" : "\"") + " in the class " + object.getClass() + 
                ", an Exception was raised in the called method.", ex);
            }
        catch (NullPointerException ex)
            {
            // should not be able to occur -- we've already verified that object is not null.
            throw new NullPointerException("MethodStep asked to call the method " + method + (passInSimState ? "\"(SimState state)" : "\"") + " on a null object");
            }
        // dont' catch (ExceptionInInitializerError ex)  -- let it throw through like any error
        }
    }
