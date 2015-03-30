/*
  Copyright 2008 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.display;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import sim.engine.*;
import java.awt.*;
import java.text.*;
import java.util.*;
import ec.util.*;
import java.io.*;
import sim.util.*;
import sim.util.gui.*;
import sim.portrayal.*;
import java.lang.ref.*;
import java.lang.reflect.*;

/*
  SimpleController is a Controller with no GUI.  It implements all of the Controller interface,
  plus it has the same pressPlay/pressStop/etc. methods as Console.  But it is not a JFrame and
  displays nothing.  No extra features are provided: no thread priorities, no step intervals,
  etc.  SimpleController is reasonable to use if you're creating (for example) a game 
  and want to control the entire GUI yourself.

  <p>Though you can register JFrames with SimpleController, there is no way for the user to
  show or hide them like he can in Console.  This means that you should override the JFrames'
  close box behaviors to handle their closing yourself (perhaps to disable it).

  <p>Similarly, SimpleController cannot store inspectors in a nice list like Console can.  Instead
  if inspectors are registered, they are either not displayed at all or are immediately displayed 
  as separate windows (the equivalent of "Detatched" inspectors in the Console).  SimpleController's
  behavior here is determined by a parameter in its constructor.

  <p>For the Console, the standard way to start up a model is to construct the Console and then make
  it visible and leave it at that.  For SimpleController instead the standard way to start up a model 
  is to construct the SimpleController and call the <b>pressPlay</b> method. 
*/


public class SimpleController implements Controller
    {
    /** Our simulation */
    GUIState simulation;
    public GUIState getSimulation() { return simulation; }
    
    static 
        {
        // Use Quaqua if it exists
        try
            {
            System.setProperty( "Quaqua.TabbedPane.design","auto" );  // UI Manager Properties docs differ
            System.setProperty( "Quaqua.visualMargin","1,1,1,1" );
            UIManager.put("Panel.opaque", Boolean.TRUE);
            UIManager.setLookAndFeel((String)(Class.forName("ch.randelshofer.quaqua.QuaquaManager", true, Thread.currentThread().getContextClassLoader()).
                    getMethod("getLookAndFeelClassName",(Class[])null).invoke(null,(Object[])null)));
            } 
        catch (Exception e) { /* e.printStackTrace(); */ } // just in case we throw a RuntimeException here

        try  // now we try to set certain properties if the security permits it
            {
            // macOS X 1.4.1 java doesn't show the grow box.  We try to force it here.
            System.setProperty("apple.awt.showGrowBox", "true");
            // if we're on a mac, we should move the menu bar to the top
            // System.setProperty("com.apple.macos.useScreenMenuBar", "true");  // nah, confuses people when switching windows
            // if we're on a mac, let's make the tabs smaller
            // System.setProperty("com.apple.macos.smallTabs", "true");  // nah, looks dorky...
            }
        catch (Exception e) { }  // just in case we throw a RuntimeException here
        }

    /** Random number generator seed */
    long randomSeed = 0;  // it'll change
        
    public SimpleController(final GUIState simulation)
        {
        this(simulation, true);
        randomSeed = simulation.state.seed();
        }

    boolean displayInspectors;
    public SimpleController(final GUIState simulation, boolean displayInspectors)
        {
        this.displayInspectors = displayInspectors;
        this.simulation = simulation;

        // Fire up the simulation displays
        invokeInSwing(new Runnable() { public void run() { simulation.init(SimpleController.this); } });
                
        // Add us to the Console's Controllers list
        Console.allControllers.put(this,this);
        }


    /** If I'm already in the Swing dispatch thread, just run this.  Otherwise call SwingUtilities.invokeAndWait on it. */
    void invokeInSwing(Runnable runnable)
        {
        if (SwingUtilities.isEventDispatchThread()) runnable.run();
        else try
                 {
                 SwingUtilities.invokeAndWait(runnable);
                 }
            catch (InterruptedException e) { }
            catch (InvocationTargetException e) { }
        }


    /** The thread that actually goes through the steps */
    Thread playThread;
    
    /** A general lock used by a number of short methods which need to "synchronize on the play thread"
        even if it's changing to another thread.  To do this, we use this official 'play thread lock' */
    final Object playThreadLock = new Object();
    
    /** Whether the thread should stop.  Don't play with this. */
    boolean threadShouldStop = false;
    
    /** Returns whether or not a flag has been raised to ask the underlying play thread to stop.  */
    boolean getThreadShouldStop()
        {
        synchronized (playThreadLock)
            {
            return threadShouldStop;
            }
        }
        
    /** Sets or clears the flag indicating whether or not the underlying play thread should stop. */
    void setThreadShouldStop(final boolean stop)
        {
        synchronized (playThreadLock)
            {
            threadShouldStop = stop;
            }
        }

    /** The play thread is presently stopped. */
    public static final int PS_STOPPED = 0;
    
    /** The play thread is presently playing. */
    public static final int PS_PLAYING = 1;
    
    /** The play thread is presently paused. */
    public static final int PS_PAUSED = 2;

    /** The current state of the simulation: playing, stopped, or paused.  Don't play with this.*/
    int playState = PS_STOPPED;

    /** Sets whether or not the current thread is playing, stopped, or paused.  An internal method only. */
    void setPlayState(int state)
        {
        synchronized (playThreadLock)
            {
            playState = state;
            }
        }

    /** Gets whether or not the current thread is PS_PLAYING, PS_STOPPED, or PS_PAUSED. */
    public int getPlayState()
        {
        synchronized (playThreadLock)
            {
            return playState;
            }
        }

    
    /** Starts the simulation.  Called internally by methods when a simulation is fired up.
        Basically various custodial methods.
        Removes the existing inspectors, sets the random number generator, calls start()
        on the GUIState (and thus the model underneath), and sets up the global model
        inspector. */
    void startSimulation()
        {
        removeAllInspectors(true);      // clear inspectors
        simulation.state.setSeed(randomSeed);   // reseed the generator.  Do this BEFORE calling start() so it gets properly primed
        simulation.start();
        }







    /////////////////////// UTILITY FUNCTIONS

    
    /** Private internal flag which indicates if the program is already in the process of quitting. */    
    boolean isClosing = false;
    /** Private lock to avoid synchronizing on myself. */
    final Object isClosingLock = new Object();

    /** Closes the Controller and shuts down the simulation.  Quits the program only if other simulations
        are not running in the same program.  Called when the user clicks on the close button of the Console,
        or during a program-wide doQuit() process.  Can also be called programmatically. */
    public void doClose()
        {
        synchronized(isClosingLock)  // closing can cause quitting, which in turn can cause closing...
            {
            if (isClosing) return;  // already in progress...
            else isClosing = true;
            }
        pressStop();  // stop threads
        simulation.quit();  // clean up simulation
        Console.allControllers.remove(this);  // remove us from the Console's controllers list
        if (Console.allControllers.size() == 0)  Console.doQuit();  // we run doQuit on the console to quit gracefully, as it maintains all the controller lists
        }
    
    /** @deprecated renamed to setIncrementSeedOnStop */
    public void setIncrementSeedOnPlay(boolean val)
        {
        setIncrementSeedOnStop(val);
        }
        
    /** @deprecated renamed to getIncrementSeedOnStop */
    public boolean getIncrementSeedOnPlay()
        {
        return getIncrementSeedOnStop();
        }

    boolean incrementSeedOnStop = true;
    public void setIncrementSeedOnStop(boolean val)
        {
        incrementSeedOnStop = val;
        }
        
    public boolean getIncrementSeedOnStop()
        {
        return incrementSeedOnStop;
        }


    /////////////////////// PLAY/STOP/PAUSE BUTTON FUNCTIONS


    /** Called when the user presses the stop button.  You can call this as well to simulate the same. */
    public synchronized void pressStop()
        {
        if (getPlayState() != PS_STOPPED)
            {
            killPlayThread();
            simulation.finish();
            stopAllInspectors(true);                // stop the inspectors, letting them flush themselves out
            setPlayState(PS_STOPPED);

            // increment the random number seed if the user had said to do so
            if (getIncrementSeedOnStop())
                {
                randomSeed = (int)(randomSeed + 1);  // 32 bits only
                }
            }
        }
        
        
    /** Called when the user presses the pause button.  You can call this as well to simulate the same.  Keep in mind that pause is a toggle. */
    public synchronized void pressPause()
        {
        pressPause(true);
        }
        
    // presses the pause button.  If the simulation is presently stopped, and
    // shouldStartSimulationIfStopped is true (the default), then the simulation
    // is started and put into a paused state.  The only situation where you'd not
    // want to do this is if you're loading a simulation from a stopped state (see
    // doOpen() ).
    //
    synchronized void pressPause(boolean shouldStartSimulationIfStopped)
        {
        if (getPlayState() == PS_PLAYING) // pause
            {
            killPlayThread();
            setPlayState(PS_PAUSED);
            refresh();  // update displays even if they're skipping
            } 
        else if (getPlayState() == PS_PAUSED) // unpause
            {
            spawnPlayThread();
            setPlayState(PS_PLAYING);
            } 
        else if (getPlayState() == PS_STOPPED) // start stepping
            {
            // Be careful adding to here -- we should just optionally start
            // the simulation and then set the various icons and change the
            // play state.  Additional stuff should be done only with consideration
            // and examination of how it's used in doOpen()...  -- Sean 
            if (shouldStartSimulationIfStopped) startSimulation();
            
            setPlayState(PS_PAUSED);
            refresh();  // update displays even if they're skipping
            }
        }

        
    /** Called when the user presses the play button.  You can call this as well to simulate the same.  Keep in mind that play will change to step if pause is down. */
    public synchronized void pressPlay()
        {
        if (getPlayState() == PS_STOPPED)
            {
            // set up states
            startSimulation();
            spawnPlayThread();
            setPlayState(PS_PLAYING);
            } 
        else if (getPlayState() == PS_PAUSED) // step N times
            {
            // at this point we KNOW the play thread doesn't exist
            if (!simulation.step())
                pressStop();
            refresh();  // update displays even if they're skipping
            }
        }




    /////////////////////// METHODS FOR MANIPULATING THE PLAY THREAD
    /////////////////////// These are the most complex to think about methods in Console.  They go through
    /////////////////////// the elaborate dance of spawning or killing the underlying play thread.
    /////////////////////// Handling an underlying thread which paints and updates lots of widgets despite
    /////////////////////// the fact that Swing on top prefers to handle everything through the even thread,
    /////////////////////// AND Java3D does its own weird thread handling underneath -- well, it can get
    /////////////////////// pretty complex.


    /** Interrupts the play thread and asks it to die.  Spin-waits until it dies, repeatedly interrupting it. */

    // synchronized so that if I do a doChangeCode(...) and it checks to see that the playThread is
    // null, then does its stuff, the playThread WILL be null even after the check.
    synchronized void killPlayThread()
        {
        // request that the play thread die
        setThreadShouldStop(true);

        // join the thread.  
        try
            {
            if (playThread != null)
                {
                // we need to do a spin-wait interrupt, then join; rather than
                // a single interrupt followed by a join, because it's possible
                // that the play thread could test if it's interrupted, see that
                // it's not, then we interrupt it, and THEN the play thread goes
                // into its blocking situation.  This is extremely unlikely but
                // theoretically possible.  So we repeatedly interrupt the thread
                // even in this situation until it gets a clue.
                do
                    {
                    try
                        {
                        // grab lock on schedule so interruption can't
                        // occur within movie-making (which causes JMF to freak out)
                        // I hope this doesn't mess up things.  Looks like it shouldn't
                        // (the function of interrupt() here is to release invokeAndWait,
                        // and at that point nothing's blocked on the schedule)
                        synchronized(simulation.state.schedule)
                            {
                            playThread.interrupt();
                            }
                        }
                    catch (SecurityException ex) { } // some stupid browsers -- *cough* IE -- don't like interrupts
                    playThread.join(50);
                    }
                while(playThread.isAlive());
                playThread = null;
                }
            } 
        catch (InterruptedException e)
            { System.err.println("WARNING: This should never happen: " + e); }
        }




    /** Used to block until a repaint is handled -- see spawnPlayThread() below */
    Runnable blocker = new Runnable()
        {
        public void run()
            {
            // intentionally do nothing
            }
        };



    /** Spawns a new play thread.   The code below actually contains the anonymous subclass that iterates
        the play thread itself.  That's why it's so long.*/

    // synchronized so that if I do a doChangeCode(...) and it checks to see that the playThread is
    // null, then does its stuff, the playThread WILL be null even after the check.
    synchronized void spawnPlayThread()
        {
        setThreadShouldStop(false);

        // start the playing thread
        Runnable run = new Runnable()
            {
            public void run()
                {
                try
                    {
                    // we begin by doing a blocker on the swing event loop.  This gives any
                    // existing repaints a chance to do their thing.  See comments below as to
                    // why such a thing is necessary
                    if (!Thread.currentThread().isInterrupted() && !getThreadShouldStop())
                        try  // it's possible we could be interrupted in-between here (see killPlayThread)
                            {
                            // important here that we're not synchronized on schedule -- because
                            // killPlayThread blocks on schedule before interrupting for JMF bug
                            SwingUtilities.invokeAndWait(blocker);
                            }                    
                        catch (InterruptedException e)
                            {
                            try
                                {
                                Thread.currentThread().interrupt();
                                }
                            catch (SecurityException ex) { } // some stupid browsers -- *cough* IE -- don't like interrupts
                            }                    
                        catch (java.lang.reflect.InvocationTargetException e)
                            {
                            System.err.println("This should never happen: " + e);
                            }                    
                        catch (Exception e)
                            {
                            e.printStackTrace();
                            }

                    // name the current thread
                    simulation.state.nameThread();

                    // start the main loop

                    boolean result = true;
                    while (true)
                        {
                        // check to see if we are being asked to quit
                        if (getThreadShouldStop())
                            break;

                        result = simulation.step();

                        // Some steps (notably 2D displays and the timer) call repaint()
                        // to update themselves.  We need to try to guarantee that this repaint()
                        // actually gets fulfilled and not bundled up with other repaints
                        // isssued by the same display.  We do that by blocking on the event
                        // loop here, giving them a chance to redraw themselves without our
                        // thread running.  Issuing an invokeAndWait also has the effect of flushing
                        // out and forcing all current repaints and events; so since we're blocked
                        // waiting, we want to make sure that no events get called which then try
                        // to call us!

                        if (!Thread.currentThread().isInterrupted() && !getThreadShouldStop())
                            try  // it's possible we could be interrupted in-between here (see killPlayThread)
                                {
                                // important here that we're not synchronized on schedule -- because
                                // killPlayThread blocks on schedule before interrupting for JMF bug
                                SwingUtilities.invokeAndWait(blocker);
                                }                        
                            catch (InterruptedException e)
                                {
                                try
                                    {
                                    Thread.currentThread().interrupt();
                                    }
                                catch (SecurityException ex) { } // some stupid browsers -- *cough* IE -- don't like interrupts
                                }                        
                            catch (java.lang.reflect.InvocationTargetException e)
                                {
                                System.err.println("This should never happen" + e);
                                }                        
                            catch (Exception e)
                                {
                                e.printStackTrace();
                                }

                        // let's check if we're supposed to quit BEFORE we do any sleeping...

                        if (!result || getThreadShouldStop())
                            break;

                        // We include this code in case the reason we dropped out was that we
                        // actually ran OUT of simulation time.  So we "press stop" to reset the
                        // buttons and call finish().  Note that this will only happen if the
                        // system actually ISN'T in a PS_STOPPED state yet -- so that should prevent
                        // us from calling finish() twice accidentally if the user just so happens
                        // to press top at exactly the right time.  I think!
                        if (!result)
                            SwingUtilities.invokeLater(new Runnable()
                                {
                                public void run()
                                    {
                                    try
                                        {
                                        pressStop();
                                        }                        
                                    catch (Exception e)
                                        {  
                                        System.err.println("This should never happen: " + e);
                                        } // On X Windows, if we close the window during an invokeLater, we get a spurious exception
                                    }
                                });
                        }
                    }
                catch(Exception e) {e.printStackTrace();}
                }
            };
        playThread = new Thread(run);
        playThread.start();
        }








    Vector frameList = new Vector();

    /////////////////////// METHODS FOR IMPLEMENTING THE CONTROLLER INTERFACE


    /** Simulations can call this to add a frame to be listed in the "Display list" of the console */
    public synchronized boolean registerFrame(JFrame frame)
        {
        frameList.add(frame);
        return true;
        }

    /** Simulations can call this to remove a frame from the "Display list" of the console */
    public synchronized boolean unregisterFrame(JFrame frame)
        {
        frameList.removeElement(frame);
        return true;
        }

    /** Simulations can call this to clear out the "Display list" of the console */
    public synchronized boolean unregisterAllFrames()
        {
        frameList.removeAllElements();
        return true;
        }

/**
   @deprecated
*/
    public synchronized void doChangeCode(Runnable r)
        {
        if (playThread != null)
            {
            killPlayThread();
            r.run();
            spawnPlayThread();
            } 
        else
            r.run();
        }

    // we presume this isn't being called from the model thread.
    public void refresh()
        {
        // updates the displays.
        final Enumeration e = frameList.elements();
        while(e.hasMoreElements())
            ((JFrame)(e.nextElement())).getContentPane().repaint();

        // updates the inspectors
        Iterator i = allInspectors.keySet().iterator();
        while(i.hasNext())
            {
            Inspector c = (Inspector)(i.next());
            if (c!=null && !c.isStopped())  // this is a WeakHashMap, so the keys can be null if garbage collected
                {
                if (c.isVolatile())
                    {
                    c.updateInspector();
                    c.repaint();
                    }
                }
            }
        }





    /////////////////////// METHODS FOR HANDLING INSPECTORS
        
    // Inspectors may be in one of two places: 
    // 1. Stored by the Console in its Inspectors panel
    // 2. Detatched, and stored in a JFrame that should be closed when the inspector is told to go away
    //
    // When the Console stores inspectors in the first case, it places them in the vectors
    // inspectorNames, inspectorStoppables, and inspectorToolbars below.  In both cases,
    // the inspectors are stored in the WeakHashMap allInspectors.  The map is weak so that if
    // the JFrame is closed, the inspector can go away and save some memory perhaps.  Note that
    // not only is the inspector stored weakly, but so is the Stoppable responsible for stopping
    // it.  Thus if no one else is holding onto the Stoppable, it might get GCed.  This is ordinarily
    // not an issue because the JFrame itself is typically holding onto the Stoppable (to call it when
    // the JFrame's close button is pressed).  But in unusual cases you want to make sure that
    // it's held onto.

    // I dislike Vectors, but JList uses them, so go figure...

    /** Holds the Stoppable objects for each inspector presently in the inspectorSwitcher. */
    Vector inspectorStoppables = new Vector();
        
    /** Weakly maps inspectors to their stoppables for all inspectors that might possibly be around.
        Cleaned out when the user presses play. 
        As inspectors are closed or eliminated, they may disappear from this WeakHashMap and be garbage collected. */
    WeakHashMap allInspectors = new WeakHashMap();

    /** Adds new inspectors to the Console's list, given the provided inspectors, their portrayals, and appropriate names for them.
        These bags must match in size, else an exception will be thrown. */
    public void setInspectors(final Bag inspectors, final Bag names)
        {
        if (!displayInspectors) return;
        
        // clear out old inspectors
        removeAllInspectors(false);
        
        // check for sizes
        if (inspectors.numObjs != names.numObjs)
            throw new RuntimeException("Number of inspectors and names do not match");

        // schedule the inspectors and add them
        for(int x=0;x<inspectors.numObjs;x++)
            {
            if (inspectors.objs[x]!=null)  // double-check
                {
                final int xx = x; // duh, Java's anonymous classes are awful compared to true closures...
                Steppable stepper = new Steppable()
                    {
                    public void step(final SimState state)
                        {
                        SwingUtilities.invokeLater(new Runnable()
                            {
                            Inspector inspector = (Inspector)(inspectors.objs[xx]);
                            public void run()
                                {
                                synchronized(state.schedule)
                                    {
                                    // this is called while we have a lock on state.schedule,
                                    // so we have control over the model.
                                    if (inspector.isVolatile()) 
                                        {
                                        inspector.updateInspector();
                                        inspector.repaint();
                                        }
                                    }
                                }
                            });
                        }
                    };
                
                Stoppable stopper = null;
                try
                    {
                    stopper = ((Inspector)(inspectors.objs[x])).reviseStopper(simulation.scheduleRepeatingImmediatelyAfter(stepper));
                    inspectorStoppables.addElement(stopper);
                    }
                catch (IllegalArgumentException ex) { /* do nothing -- it's thrown if the user tries to pop up an inspector when the time is over. */ }

                // add the inspector
                registerInspector((Inspector)(inspectors.objs[x]),stopper);
                
                JFrame frame = ((Inspector)(inspectors.objs[x])).createFrame(stopper);
                frame.setVisible(true);
                }
            }
        }

    /**
       Registers an inspector to be Stopped if necessary in the future.  This automatically happens
       if you call setInspectors(...).
    */
    public void registerInspector(Inspector inspector, Stoppable stopper)
        {
        if (!displayInspectors) return;
        allInspectors.put(inspector, new WeakReference(stopper));  // warning: if no one else refers to stopper, it gets GCed!
        }

    /** Stops all inspectors.  If killDraggedOutWindowsToo is true, then the detatched inspectors are stopped as well. */
    public void stopAllInspectors(boolean killDraggedOutWindowsToo)
        {
        // update all the inspectors before we delete some of them, so they get written out
        // if necessary.
        Iterator i = allInspectors.keySet().iterator();
        while(i.hasNext())
            {
            Inspector insp = (Inspector)(i.next());
            insp.updateInspector();  // one last time
            insp.repaint();
            }

        // kill all the inspectors in the inspector window for sure
        // inspectors may get stop() called on them multiple times
        for(int x=0;x<inspectorStoppables.size();x++)
            {
            Stoppable stopper = ((Stoppable)(inspectorStoppables.elementAt(x)));
            if (stopper!=null) stopper.stop();
            }

        // possibly kill all inspectors detached in their own windows.
        if (killDraggedOutWindowsToo)
            {
            i = allInspectors.keySet().iterator();
            while(i.hasNext())
                {
                Inspector insp = (Inspector)(i.next());
                Stoppable stopper = (Stoppable)(allInspectors.get(insp));
                if (stopper != null) stopper.stop();
                }
            }
        }

    /** Stops and removes all inspectors. If killDraggedOutWindowsToo is true, then all inspector windows will be closed; else only
        the inspectors presently embedded in the console will be stopped and removed. */
    public void removeAllInspectors(boolean killDraggedOutWindowsToo)
        {
        stopAllInspectors(killDraggedOutWindowsToo);
        if (killDraggedOutWindowsToo)
            {
            // this will probably result in the inspectors getting 'stop' called on them a second time
            Iterator i = allInspectors.keySet().iterator();
            while(i.hasNext())
                {
                Component inspector = (Component)(i.next());
                
                // run up to the top-level component and see if it's the Console or not...
                while(inspector != null && !(inspector instanceof JFrame))
                    inspector = inspector.getParent();
                if (inspector != null)  // not the console -- it's a dragged-out window
                    ((JFrame)(inspector)).dispose();
                }
            allInspectors = new WeakHashMap();
            }
        }

    /** Calls forth the "New Simulation" window. */
    public boolean doNew()
        {
        return Console.doNew(null, false);
        }
                

    /** Returns a list of all current inspectors.  Some of these inspectors may be stored in
        the SimpleController itself, and others may have been dragged out into their own JFrames.  You will
        need to distinguish between these two on your own.  Note that some of these inspectors are stored as
        weak keys in the SimpleController, so holding onto this list will prevent them from getting garbage
        collected.  As a result, you should only use this list for temporary scans. */
    public ArrayList getAllInspectors()
        {
        ArrayList list = new ArrayList();
        Iterator i = allInspectors.keySet().iterator();
        while(i.hasNext())
            list.add((Inspector)(i.next()));
        return list;
        }

    /** Returns a list of all displays.  You own the resulting list and can do what you like with it. */
    public synchronized ArrayList getAllFrames()
        {
        return new ArrayList(frameList);
        }

    }

