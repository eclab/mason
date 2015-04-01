/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal;
import sim.engine.*;
import sim.display.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import sim.portrayal.inspector.*;
import sim.display.*;

/** An Inspector is a JPanel containing information about some object,
    and updates its displayed information when updateInspector() is called.
    In-between calls to updateInspector(), the Inspector should show the same
    information despite repeated repaints() etc.  
    Inspectors commonly also allow the user to change the information,
    but this is not required: they can be "read-only" if desired.

    <p><b>Volatility.</b> 
    Inspectors are either volatile (they change each frame) or non-volatile (they're generally static).
    If your inspector is not volatile, you should call setInspectorVolatile(false).  It's more efficient.
        
    <p>Some non-volatile inspectors never have their underlying data change except when the user tweaks
    the inspector itself.  Other non-volatile inspectors have the underlying data change but don't 
    reflect it automatically each frame because it's expensive to update the inspector.  
    In this second case (and for good measure the first case), you should provide a button 
    which manually updates the inspector via updateInspector().  The easiest way to do get such
    a button is to call makeUpdateButton(), which will do it for you.  You can then stick the
    button in your inspector.
        
    <p><b>The Stopper.</b>  Most inspectors, particularly volatile ones, are scheduled repeating and so need
    to have a way to be stopped if the user closes the inspector's window or otherwise clears it out.  Normally
    the system gets this Stoppable after scheduling the inspector repeating.  Before it uses it, it first calls
    reviseStopper(Stoppable).  This gives you a chance to "wrap" the original Stoppable in a new one which calls
    the original.  Beware that the original Stoppable could in theory be null.
        
    <p>Why would you want to do this?  So you can be informed of when the Stoppable has been called --
    to flush a stream perhaps -- or to call the underlying Stoppable yourself for some reason.  If you override
    this method, be sure to call super.reviseStopper(Stoppable) first, and wrap what <i>it</i> returns.  For example:
        
    <pre><tt>public Stoppable reviseStopper(Stoppable stopper)
    {
    final Stoppable newStopper = super.reviseStopper(stopper);
    return new Stoppable() { public void stop()
    {
    if (newStopper!=null) newStopper.stop();  // wraps the stopper
    System.out.println("Hey, I stopped!");  // do my thing
    }};
    }</tt></pre>
                
    <p>Beware that your stopper may and probably will have its stop() method called multiple times.
        
    <p><b>The Frame.</b>  Some inspectors are placed in separate JFrames either immediately or later on when
    the user requests that the inspector be "detatched".  When this happens, the system will call createFrame(Stoppable)
    to get a JFrame with the inspector in it.  The Stoppable passed in is the same one that the system received from reviseStopper(Stoppable).
    In most cases you probably don't need to change anything about the JFrame.  But occasionally you might want to 
    override the createFrame(Stoppable) method to revise the JFrame in some way: perhaps to change its title, say.  If you do,
    you probably should call super.createFrame(Stoppable), and use that existing JFrame, as the super method does a few
    other nice things as well (such as attaching the Stoppable to the close box on the JFrame).  
        
    <p>For example, the default version of the createFrame(Stoppable) places the Inspector in a JFrame with a scroll panel.
    You may not want this behavior.  If this is the case, you might do something like this:
        
    <pre><tt>public JFrame createFrame(Stoppable stopper)
    {
    JFrame frame = super.createFrame(stopper);
    frame.removeAll();  // get rid of scroll pane
    frame.setLayout(new BorderLayout());
    frame.add(this, BorderLayout.CENTER);  // I fill the whole frame
    frame.setTitle("I prefer this title");  // here's my new title
    frame.pack();
    }</tt></pre>
        
*/

public abstract class Inspector extends JPanel
    {
    boolean _volatile = true;
    
    /** Returns an inspector for the provided object.  If the object responds to the ProvidesInspector
        interface, then it provides its own inspector.  Else if the object responds to the Tabbable 
        interface, than a TabbedInspector is provided.  Else a SimpleInspector is provided. */
    public static Inspector getInspector(Object obj, GUIState state, String name)
        {
        if (obj == null)
            return new SimpleInspector(obj, state, name);
        else if (obj instanceof ProvidesInspector)
            return ((ProvidesInspector)obj).provideInspector(state, name);
        else if (obj instanceof Tabbable)
            return new TabbedInspector((Tabbable)obj, state, name);
        else
            return new SimpleInspector(obj, state, name);
        }
        
    /** Set to true (default) if the inspector should be updated every time step.  Else set to false. */
    public void setVolatile(boolean val) {_volatile = val;}
        
    /** Returns true (default) if the inspector should be updated every time step.  Else returns false. */
    public boolean isVolatile() { return _volatile; }
        
    /** Called by the system to inform the Inspector that it needs to update itself to reflect any
        changed in the underlying data. */
    public abstract void updateInspector();
        
    /** Called by the system to come up with an appropriate title for a free-floating inspector window.
        Often this is toString() on the underlying object.  Some inspectors never become free-floating
        and so don't need to override this method.  By default this method returns an empty String. */
    String title = "";
    public String getTitle() { return title; } 
    public void setTitle(String title) { this.title = title; } 
    
    /**
       Called whenever the system needs to get a Steppable which, when stepped, will update the inspector and
       repaint it. 
    */
    public Steppable getUpdateSteppable()
        {
        return new Steppable()
            {
            public void step(final SimState state)
                {
                SwingUtilities.invokeLater(new Runnable()
                    {
                    public void run()
                        {
                        synchronized(state.schedule)
                            {
                            Inspector.this.updateInspector();
                            Inspector.this.repaint();
                            }
                        }
                    });
                }
            };
        }
    
    /* If you've added an UpdateButton with makeUpdateButton(), it will call updateButtonPressed
       when it is pressed, which by default will call updateInspector().  Override this
       method if that's not the behavior you want.
    */
    final protected void updateButtonPressed()
        {
        updateInspector();
        }
    
    /** A convenient function to create UpdateButton which you might add to the bottom of the JPanel
        (assuming it still is using BorderLayout).
        This is helpful for the user if your inspector isn't volatile. */
    public Component makeUpdateButton()
        {
        JButton jb = new JButton(UPDATE_ICON);
        jb.setText("Refresh");
        // quaquaify
        jb.putClientProperty("Quaqua.Button.style","square");

        //jb.setPressedIcon(UPDATE_ICON_P);
        //jb.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
                
        jb.setToolTipText("Refreshes this inspector to reflect the current underlying values in the model.");

        jb.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                updateInspector();
                }
            });
        return jb;
        }

    public static final ImageIcon INSPECT_ICON = iconFor("Inspect.png");
    public static final ImageIcon INSPECT_ICON_P = iconFor("InspectPressed.png");
    public static final ImageIcon UPDATE_ICON = iconFor("Update.png");
    public static final ImageIcon UPDATE_ICON_P = iconFor("UpdatePressed.png");

    /** Returns icons for a given filename, such as "Layers.png". A utility function. */
    static ImageIcon iconFor(String name)
        {
        return new ImageIcon(Inspector.class.getResource(name));
        }

    /** Gives the user a chance to wrap the Inspector's stopper in a larger stopper,
        which will then get registered with the Inspector; this larger stopper is
        also what is passed into Inspector.createFrame(...).  If you 
        override this method, be sure to call super.getRevisedStopper(stopper) and
        wrap *that*.  The default version of this method creates a wrapped stopper
        which guarantees that when the stopper's stop() method is called, the 
        Inspector knows about it so that it can properly set isStopped(). 
    */
    public Stoppable reviseStopper(final Stoppable stopper)
        {
        return new Stoppable()
            {
            public void stop()
                {
                stopper.stop();
                stopped = true;
                }
            };
        }

    /** Disposes the Inspector's frame if it's not a Controller.  Override this
        to change the disposal behavior of the Inspector. */
    public void disposeFrame()
        {
        Component c = this;
        while(c != null && !(c instanceof JFrame))
            c = c.getParent();
                
        // At this point c is the JFrame.  By default we dispose it if it's not
        // a Controller.
                
        if (c != null && !(c instanceof Controller))
            ((JFrame)c).dispose();
        }

    boolean stopped;
    /** Returns true if this Inspector's Stoppable has been stopped. */
    public boolean isStopped() { return stopped; }

    /** Creates a scrollable frame surrounding the inspector which calls stop()
        on the underlying stopper when closed.  stopper may be null, in which
        case stop() is not called.  */
    public JFrame createFrame(Stoppable stopper)
        {
        JScrollPane scroller = new JScrollPane(this);
        scroller.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

        // put in new frame which stops when closed.
        // The stopperHolder trick allows us to null out the stopper even though
        // it's final, which might help in letting the WeakHashMap of inspectors
        // in the Console clear itself.  Maybe.
        final Stoppable[] stopperHolder = new Stoppable[] { stopper };
        JFrame frame = new JFrame()
            {
            public void dispose()
                {
                super.dispose();
                if (stopperHolder[0]!=null) stopperHolder[0].stop();
                stopperHolder[0] = null;
                }
            };
        frame.getRootPane().putClientProperty("Window.style", "small");  // on the Mac

        frame.setTitle(getTitle());
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(scroller, BorderLayout.CENTER);
        frame.setResizable(true);
        frame.pack();
        
        if (Display2D.isMacOSX)
            {
            // fix a bug in MacOS X 1.4.2, which has a minimum possible width and height (128x37)
            Dimension d = frame.getSize();
            if (d.width < 128) d.width = 128;
            if (d.height< 37) d.height = 37;
            frame.setSize(d);
            }
        return frame;
        }
    }
