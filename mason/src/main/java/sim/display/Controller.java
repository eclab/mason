/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.display;
import javax.swing.*;
import sim.portrayal.*;
import sim.engine.*;
import sim.util.*;
import java.util.*;

/** A Controller is the top-level object responsible for setting up and running the GUIState.
    More or less, the Controller calls the init, start, step, finish, and quit methods on the GUIState.
    The Controller also provides a window management facility.  The controller allows the GUIState to register
    windows to appear in a list to the user and to be closed properly when the program is quit.  The controller
    also manages a repository of current inspectors.  Both of these facilities are optional.  Subclasses
    are not required to implement all these methods; they can stub some out except for doChangeCode(...).
*/
    
public interface Controller
    {
    /** This method will interrupt the simulation (pause it), call your runnable, then continue
        (uninterrupt) the simulation.  This allows you to guarantee a way to change the model from
        a separate thread -- for example, the Swing event thread -- in a synchronous, blocking
        fashion.
        
        <p>You have other options for updating the model from external threads.  One option is to add
        a Steppable to GUIState's scheduleImmediate(...) queue.  When the Steppable is stepped, it will
        be done so inside the model's thread.  This is asynchronous (non-blocking), however.
        
        <p>Alternatively, you can synchronize on state.schedule and run your code.  This is synchronous.
        @deprecated
    */
    public void doChangeCode(Runnable r);
    
    /** Simulations can call this to add a frame to be listed in the "Display list" of the Controller.
        If the Controller does not have such a list, FALSE is returned.  */
    public boolean registerFrame (JFrame frame);
        
    /** Simulations can call this to remove a frame from the "Display list" of the Controller.
        If the Controller does not have such a list, FALSE is returned.   */
    public boolean unregisterFrame (JFrame frame);
        
    /** Simulations can call this to clear out the "Display list" of the Controller.
        If the Controller does not have such a list, FALSE is returned.   */
    public boolean unregisterAllFrames();
    
    /** Returns all registered frames.  */
    public ArrayList getAllFrames();
    
    /** Lazily updates and redraws all the displays and inspectors.  Do not call this method from
        the model thread -- only from the Swing event thread.
        This is an expensive procedure and should not be done unless necessary.  Typically it's done
        in response to some event (a button press etc.) rather than in the model itself. */
    public void refresh();
    
    /** Replaces current inspectors with the ones provided.  The names bag should provide short,
        unique textual names appropriate for each inspector in the inspectors bag.  Automatically 
        registers the inspectors with registerInspector(...) as well.  */
    public void setInspectors(Bag inspectors, Bag names);
    
    /** Registers an inspector to be refreshed as appropriate and stopped when the model is restarted.
        Does not necessarily add the inspector to a list of inspectors like setInspectors(...) does. */
    public void registerInspector(Inspector inspector, Stoppable stopper);
        
    /** Returns a list of all current inspectors.  Some of these inspectors may be stored in
        the Controller itself, and others may have been dragged out into their own JFrames.  You will
        need to distinguish between these two on your own.  Note that some of these inspectors are stored as
        weak keys in the Controller, so holding onto this list will prevent them from getting garbage
        collected.  As a result, you should only use this list for temporary scans. */
    public ArrayList getAllInspectors();
    }
    
