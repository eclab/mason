/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.display3d;

import sim.engine.*;
import sim.portrayal3d.*;
import sim.portrayal3d.simple.*;
import sim.util.*;
import sim.util.gui.*;
import sim.util.media.*;
import sim.display.Console;
import sim.display.*;
import sim.portrayal.*;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;
import javax.vecmath.*;
import javax.media.j3d.*;

import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.behaviors.vp.*;
import com.sun.j3d.utils.image.*;
import com.sun.j3d.utils.geometry.Sphere;

import java.util.prefs.*;


/**
   Display3D holds, displays, and manipulates 3D Portrayal objects, allowing the user to scale them,
   rotate them, translate them, magnify them, change how often they're updated, take snapshots, and generate Quicktime movies.
   Display3D is Steppable, and each time it is stepped it updates and redisplays itself.  Display3D also handles
   double-click events, routing them to the underlying portrayals as inspector requests.

   <p>Display3D's constructor takes a height and width, which is the height and width of the underlying
   display area (not including the button bar at-top).  Display3D can be placed
   in a JFrame; indeed it provides a convenience function to sprout its own JFrame with the method
   createFrame().  You can put Display3D in your own JFrame if you like, but you should try to call
   Display3D.quit() when the frame is disposed.

   <p>The viewing area is by default set to roughly encapsulate a 2 by 2 by 2 cube centered at the origin.  
   The user's eye is on the positive Z axis outside of the cube area and is looking down the negative Z axis.

   <p>Display3D can have both SimplePortrayal3Ds and FieldPortrayal3Ds attached.  Once the Portrayals are attached,
   the you can transform them (rotate, scale, translate, etc.) all together as one scene via a Global Model Transform.
   Various Display3D functions are provided to manipulate this Transform (such as rotate(...)).  Java3Ders
   in-the-know can also provide the actual Transform3D if they're feeling lucky.

   <p>Display3D provides the optional ability for the user to show axes and a backdrop.  Axes are in the
   X, Y, and Z directions for one unit in length, and are added to the scene <i>after</i> the 
   Global Model Transform has been applied.  Thus if you've used the Global Model Transform to scale down your
   simulation into the space of a 2 by 2 by 2 cube centered at the origin, for example, the axes will just touch
   the edges of the cube.  The backdrop is an optional feature that the programmer must add.  You can provide
   either a spherical backdrop which rotates with the world (like a star field) or a flat backdrop which is always
   behind the environment and which fills the window exactly.  Spherical backdrops can be wrapped with images
   (use a spherical projection) or -- for the daring Java3Ders -- Apperance objects.  Flat backdrops can have
   images or flat colors.
 
   <h3>Display3D and Java3D</h3>
   
   <p>Display3D uses Java3D to do its magic.  Java3D is a sophisticated scene graph library, but we have
   done our best to hide this from you if you don't wish to know anything about how Java3D works.  For those
   experienced in Java3D, we describe here the top-level scene graph used by the Display3D.
   
   <p>The Unverse is a SimpleUnverse called <b>universe</b>, the top BranchGraph is <b>root</b>, and the 
   Canvas3D is a special subclass (CapturingCanvas3D) called <b>canvas</b>.  A CapturingCanvas3D contains some
   additional machinery to make possible snapshots and movies.  If you wish to add some scene graph material
   outside of Portrayal3Ds, and you don't want it to be transformed by the Global Model Transform, nor do you
   want it to be auto-spinnable, hanging it off of <b>root</b> is a good location.  Note that even so, the user
   will be able to rotate, zoom, magnify, and translate the world with the mouse, and that includes this extra
   scene graph material.
   
   <p>Hanging off of root are two pairs of TransformGroups and RotationInterpolators responsible for auto-spinning the scene graph and a
   spherical backdrop (a Background object, if one exists), respectively.  Auto-spinning happens in concert in these two
   TransformGroups, so the backdrop will always appear to be auto-spinning with the scene graph.
   
   <p>One of these TransformGroups (<b>autoSpinTransformGroup</b>) is public and holds the scene graph.
   The other TransformGroup is private.  <b>autoSpinTransformGroup</b> is a good place to hang extra scene
   graph stuff you wish to add, if you don't want that stuff to be transformed along with the Portrayal3Ds,
   but you'd like it to be auto-spun along with the Portrayal3Ds.
   
   <p>Hanging off of <b>autoSpinTransformGroup</b> are two items: a private Switch which turns on and
   off the optional axes and backdrop; and <b>globalModelTransformGroup</b>, the TransformGroup responsible
   for transforming all of the Portrayals in the scene according to the global model transform.  Additionally,
   <b>globalModelTransformGroup</b> is a good place to hang extra scene graph stuff you wish to add that you want to
   be transformed by the global model transform along with the rest of the Portrayal3Ds.
   
   <p>The <b>globalModelTransformGroup</b> in turn holds a private Switch which in turn holds the scene graph material
   supplied by each of the
   Portrayal3Ds in the scene.  This Switch turns the Portrayals on and off according the layers menu choice
   the user makes on the button bar.
   
   <p>The <b>LightSwitch</b>, which resides in a BranchGrouop hanging of the ViewPlatform's TransformGroup node
   (where the camera is), holds two lights, a PointLight and an AmbientLight, which may be turned on or off by the
   user.  You can change these lights if you like.
   
   <p>The right place to add scene graph material, or make other changes to the Java3D environment, is in
   the sceneGraphCreated() hook.  This function is called for your benefit after the Display3D has created
   its scene graph material but just before the renderer has been started -- thus the graph is not yet
   online and is still modifiable.
   
   <p>If you add extra scene graph material to the Display3D outside of a Portrayal3D context, 
   you should set it to be unpickable, or otherwise you
   will get errors when the user tries to select it from the scene (Display3D uses geometry-based picking
   for its object selection).  The easiest way for you to do this to a given node is to call the utility function 
   <tt>sim.display3d.SimpleDisplay3D.clearPickableFlags(<i>node</i>)</tt>

   @author Gabriel Balan 
**/

public class Display3D extends JPanel implements Steppable
    {
    public String DEFAULT_PREFERENCES_KEY = "Display3D";
    String preferencesKey = DEFAULT_PREFERENCES_KEY;  // default 
    /** If you have more than one Display3D in your simulation and you want them to have
        different preferences, set each to a different key value.    The default value is DEFAULT_PREFERENCES_KEY.
        You may not have a key which ends in a forward slash (/) when trimmed  
        Key may be set to null (the default).   */
    public void setPreferencesKey(String s)
        {
        if (s.trim().endsWith("/"))
            throw new RuntimeException("Key ends with '/', which is not allowed");
        else preferencesKey = s;
        }
    public String getPreferencesKey() { return preferencesKey; }


    ArrayList portrayals = new ArrayList();
    Stoppable stopper;
    GUIState simulation;
        
    /** The component bar at the top of the Display3D. */
    public Box header;
    /** The button which starts or stops a movie */
    public JButton movieButton;
    /** The button which snaps a screenshot */
    public JButton snapshotButton;
    /** The button which pops up the option pane */
    public JButton optionButton;
    /** The field for scaling values */
    public JPopupMenu refreshPopup;
    /** The button which pops up the refresh menu */
    public JToggleButton refreshbutton;  // for popup
    /** The button which starts or stops a movie */
    public NumberTextField scaleField;
    /** The field for skipping frames */
    public NumberTextField skipField;
    /** The combo box for skipping frames */
    public JComboBox skipBox;
    /** The frame which holds the skip controls */
    public JFrame skipFrame;
        
    /** The Java3D canvas holding the universe. A good time to fool around with this is
        in the sceneGraphCreated() hook. */
    public CapturingCanvas3D canvas = null;
    /** The Java3D universe.  Created (and recreated) by createSceneGraph.  A good time to fool around with this is
        in the sceneGraphCreated() hook. */
    public SimpleUniverse universe = null;
    /** The root scene graph node in the Java3D universe.  Created (and recreated) by createSceneGraph.  
        This is a good place to hang things you don't want auto-rotated nor transformed by the Display3D.  Hang things off of here
        in the sceneGraphCreated() hook. */
    public BranchGroup root = null;
    /** An additional root scene graph node which is attached to the viewing transform of the universe, and thus
        stays in the same location regardless of the placement of the camera. */
    public BranchGroup viewRoot = null;

    // these really don't need to be public...

    // switch and mask for the portrayals themselves
    Switch portrayalSwitch = null;
    BitSet portrayalSwitchMask = null;
    
    // Auxillary elements in the scene.  By default, element 0 is the Axes, element 1 is the bounding sphere, and element 2 is the floor.
    Switch auxillarySwitch = null;
    // The auxillarySwitch's visibility mask
    BitSet auxillarySwitchMask = new BitSet(NUM_AUXILLARY_ELEMENTS);
    // Number of auxillary elements in the auxillarySwitchMask
    static final int NUM_AUXILLARY_ELEMENTS = 2;  // the axes, the bounding sphere, and the floor
    // the elements
    static final int AXES_AUX_INDEX = 0;
    static final int BACKGROUND_AUX_INDEX = 1;
    //  static final int FLOOR_AUX_INDEX = 2;
        
    // light elements

    /** Holds two lights located at the camera: in slot 0, a white PointLight, and in slot 1, a white AmbientLight.
        You may change these lights to different colored lights, but please keep them PointLights and AmbientLights
        respectively.  These lights are turned on and off by the Options pane. */
    Switch lightSwitch = null;
    BitSet lightSwitchMask = new BitSet(NUM_LIGHT_ELEMENTS);
    final static int NUM_LIGHT_ELEMENTS = 2;
    final static int SPOTLIGHT_INDEX = 0;
    final static int AMBIENT_LIGHT_INDEX = 1;

    /* The MovieMaker.  If null, we're not shooting a movie. */
    MovieMaker movieMaker = null;    

    /** The popup layers menu */
    public JPopupMenu popup;
    /** The button which pops up the layers menu */
    public JToggleButton layersbutton;  // for popup

    /* Sets various MacOS X features.  This text is repeated in Console.java, Display2D.java, and Display3D.java
       The reason for the repeat is that the UseQuartz property must be set a precise time -- for example, we can't
       just use this static to call a common static method -- it doesn't work :-(  Otherwise we'd have made one
       static method which did all this stuff, duh.  */
    static 
        {
        // use heavyweight tooltips -- otherwise they get obscured by the Canvas3D
        // [this appears to be ignored by MacOS X Java 1.4.1 and 1.4.2.  A bug? ]
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

        // Use Quaqua if it exists
        try
            {
            //Set includes = new HashSet();
            //includes.add("ColorChooser");
            //ch.randelshofer.quaqua.QuaquaManager.setIncludedUIs(includes);
            System.setProperty( "Quaqua.TabbedPane.design","auto" );  // UI Manager Properties docs differ
            System.setProperty( "Quaqua.visualMargin","1,1,1,1" );
            UIManager.put("Panel.opaque", Boolean.TRUE);
            UIManager.setLookAndFeel((String)(Class.forName("ch.randelshofer.quaqua.QuaquaManager", true, Thread.currentThread().getContextClassLoader()).
                    getMethod("getLookAndFeelClassName",(Class[])null).invoke(null,(Object[])null)));
            } 
        catch (Exception e) { /* e.printStackTrace(); */ }  // just in case there's a RuntimeException raised

        try  // now we try to set certain properties if the security permits it
            {
            // turn on hardware acceleration on MacOS X.  As of September 2003, 1.3.1
            // turns this off by default, which makes 1.3.1 half the speed (and draws
            // objects wrong to boot).
            System.setProperty("com.apple.hwaccel","true");  // probably settable as an applet.  D'oh! Looks like it's ignored.
            System.setProperty("apple.awt.graphics.UseQuartz","true");  // counter the awful effect in OS X's Sun Renderer (though it's a bit faster)
            // the following are likely not settable
            // macOS X 1.4.1 java doesn't show the grow box.  We force it here.
            System.setProperty("apple.awt.showGrowBox","true");
            // we set this so that macos x application packages appear as files
            // and not as directories in the file viewer.  Note that this is the 
            // 1.3.1 version -- Apple gives us an obnoxious warning in 1.4.1 when
            // we call forth an open/save panel saying we should now use
            // apple.awt.use-file-dialog-packages instead, as if 1.3.1 isn't also
            // in common use...
            System.setProperty("com.apple.macos.use-file-dialog-packages","true");
            }
        catch (Exception e) { }  // just in case there's a RuntimeException raised
        }

    /** 
     * Creates a frame holding the Display3D.  This is the best method to create the frame,
     * rather than making a frame and putting the Display3D in it.  Various bug fixes are
     * provided in the JFrame which is returned by createFrame().
     **/
    public JFrame createFrame()
        {
        JFrame frame = new JFrame()
            {
            boolean previouslyShown = false;
            public void dispose()
                {
                quit();       // shut down the movies
                super.dispose();
                }

            /** Java3D adds a window listener to the frame to determine when the window has been closed;
                it stops the system as a result (on Linux).  This code removes this listener so the system can go on
                unabated underneath. */ 
            public void addWindowListener(WindowListener l) 
                {
                if ("class javax.media.j3d.EventCatcher".compareTo(l.getClass().toString()) == 0)
                    l = new LocalWindowListener(); 
                                                
                super.addWindowListener(l); 
                }

                
            /** A bug on MacOS X causes Canvas3Ds to not redisplay if their window is hidden and then reshown.
                This code gets around it */
            public void setVisible(boolean val)
                {
                super.setVisible(val);
                // MacOS X Java prior to 1.4.2 update 1 isn't fixed by this, and indeed it just
                // messes up on the first load of the window.  But previouslyShown at least provides
                // the status quo for people with the older Java...
                if (canvas != null && val && previouslyShown && sim.display.Display2D.isMacOSX)
                    {
                    SwingUtilities.invokeLater(new Runnable()
                        {
                        public void run()
                            {
                            Display3D.this.remove(canvas);
                            Display3D.this.add(canvas, BorderLayout.CENTER);
                            }
                        });
                    }
                if (val == true)
                    previouslyShown = true;
                }
            };
            
        frame.setResizable(true);
        
        // these bugs are tickled by our constant redraw requests.
        frame.addComponentListener(new ComponentAdapter()
            {
            // Bug in MacOS X Java 1.3.1 requires that we force a repaint.
            public void componentResized (ComponentEvent e) 
                {
                Utilities.doEnsuredRepaint(header);
                }
            });

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(this,BorderLayout.CENTER);
        frame.getContentPane().setBackground(Color.yellow);
        frame.setTitle(GUIState.getName(simulation.getClass()) + " Display");
        frame.pack();
        return frame;
        }
        
    static class LocalWindowListener extends java.awt.event.WindowAdapter 
        {
        // empty class to replace the windowlistener spawned by Canvas3D        
        }


    // how many subgraphs (field portrayals) do we have?
    int subgraphCount = 0;
    
    class Portrayal3DHolder
        {
        Portrayal3D portrayal;
        String name;
        JCheckBoxMenuItem menuItem;
        int subgraphIndex;
        boolean visible = true;  // added -- Sean
        public String toString() { return name; }
        Portrayal3DHolder(Portrayal3D p, String n, boolean visible)
            {
            portrayal=p; 
            name=n;
            this.visible = visible;
            menuItem = new JCheckBoxMenuItem(name,visible);
            subgraphIndex = subgraphCount;
            subgraphCount++;
            // during createSceneGraph, visibility will be used to turn things on or off
            menuItem.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    // a little cleaner, and now we can see if it's selected -- Sean
                    if (menuItem.isSelected())
                        {
                        Portrayal3DHolder.this.visible = true;
                        portrayalSwitchMask.set(subgraphIndex);
                        }
                    else
                        {
                        Portrayal3DHolder.this.visible = false;
                        portrayalSwitchMask.clear(subgraphIndex);
                        }
                    portrayalSwitch.setChildMask(portrayalSwitchMask);
                    }
                });

            }
        }


    /** Returns the frame holding this Component.  If there is NO such frame, an error will
        be generated (probably a ClassCastException). */
    public Frame getFrame()
        {
        Component c = this;
        while(c.getParent() != null)
            c = c.getParent();
        return (Frame)c;
        }

    /** Resets the Display3D so it reschedules itself.  This is
        useful when reusing the Display3D.  
    */
    public void reset()
        {
        synchronized(simulation.state.schedule)
            {
            // now reschedule myself
            if (stopper!=null) stopper.stop();
            //if (getInterval() < 1) setInterval(1);  // just in case...
            stopper = simulation.scheduleRepeatingImmediatelyAfter(this);
            }
            
        // deselect existing objects
        for(int x=0;x<selectedWrappers.size();x++)
            {
            LocationWrapper wrapper = ((LocationWrapper)(selectedWrappers.get(x)));
            wrapper.getFieldPortrayal().setSelected(wrapper,false);
            }
        selectedWrappers.clear();
        }


    // do I need to call createSceneGraph instead of updateSceneGraph because
    // new portrayals have been attached/detatched?
    boolean dirty = false;
    
    /** Attaches a portrayal to the Display3D, along with the provided human-readable name for the portrayal.
        The portrayal will be transformed, along with similar attached portrayals,
        according to the Display3D's internal transform.  */
    public void attach(Portrayal3D portrayal, String name)
        {
        attach(portrayal,name,true);
        }
        
    /** Attaches a portrayal to the Display3D, along with the provided human-readable name for the portrayal.
        The portrayal will be transformed, along with similar attached portrayals,
        according to the Display3D's internal transform.  The portrayal may be attached
        initially visible or not visible.  */
    public void attach(Portrayal3D portrayal, String name, boolean visible)
        {
        /* In case our attached portrayal was done AFTER the display is live, let's recreate */
        destroySceneGraph();
        
        Portrayal3DHolder p = new Portrayal3DHolder(portrayal,name,visible);
        portrayals.add(p);
        popup.add(p.menuItem);
        dirty = true;
        portrayal.setCurrentDisplay(this);

        createSceneGraph();
        }

    /** A convenience function: creates a popup menu item of the given name which, when selected, will display the
        given inspector in the Console.  Used rarely, typically for per-field Inspectors. */
    public void attach(final sim.portrayal.Inspector inspector, final String name)
        {
        JMenuItem consoleMenu = new JMenuItem("Show " + name);
        popup.add(consoleMenu);
        consoleMenu.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                Bag inspectors = new Bag();
                inspectors.add(inspector);
                Bag names = new Bag();
                names.add(name);
                simulation.controller.setInspectors(inspectors,names);
                }
            });
        }
    
    void createConsoleMenu()
        {
        if (simulation != null && simulation.controller != null &&
            simulation.controller instanceof Console)
            {
            final Console c = (Console)(simulation.controller);
            JMenuItem consoleMenu = new JMenuItem("Show Console");
            popup.add(consoleMenu);
            consoleMenu.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    c.setVisible(true);;
                    }
                });
            }
        popup.addSeparator();
        }
        
    /** Detatches all portrayals from the Display3D. */
    public ArrayList detatchAll()
        {    
        ArrayList old = portrayals;
        popup.removeAll();
        createConsoleMenu();
        portrayals = new ArrayList();
        portrayalSwitchMask = null;
        subgraphCount = 0;
        dirty = true;
        return old;
        }
    
    public GUIState getSimulation() { return simulation; }
        
    /**
       Creates a Display3D with the provided width and height for its portrayal region, 
       attached to the provided simulation.  The interval is ignored.
       @deprecated
    */
    // width and height are actually ints, but we're being consistent with Display2D
    public Display3D(final double width, final double height, GUIState simulation, long interval)
        {
        this(width, height, simulation);
        }

        
    /**
       Creates a Display3D with the provided width and height for its portrayal region, 
       attached to the provided simulation.
    */
    public Display3D(final double width, final double height, GUIState simulation)
        {
        // setInterval(interval);
        this.simulation = simulation;
        reset();  // must happen AFTER state is assigned
        
        final Color headerBackground = getBackground(); // will change later, see below
        header = new Box(BoxLayout.X_AXIS)
            {
            // bug in Java3D results in header not painting its background in Windows,
            // XWindows, so we force it here.
            public synchronized void paintComponent(final Graphics g)
                {
                g.setColor(headerBackground);
                g.fillRect(0,0,header.getWidth(),header.getHeight());
                }

            public Dimension getPreferredSize()  // we want to be as compressible as necessary
                {
                Dimension d = super.getPreferredSize();
                d.width = 0;
                return d;
                }
            };

        // maybe this will cut down on flashing in Windows, XWindows.
        // But it will mess up the headers' background, so we hard-code that above.
        setBackground(Color.black);  


        layersbutton = new JToggleButton(Display2D.LAYERS_ICON);
        layersbutton.setPressedIcon(Display2D.LAYERS_ICON_P);
        layersbutton.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        layersbutton.setBorderPainted(false);
        layersbutton.setContentAreaFilled(false);
        layersbutton.setToolTipText("Show and hide different layers");
        header.add(layersbutton);
        
        //Create the popup menu.
        popup = new JPopupMenu();
        popup.setLightWeightPopupEnabled(false);

        //Add listener to components that can bring up popup menus.
        layersbutton.addMouseListener(new MouseAdapter()
            {
            public void mousePressed(MouseEvent e)
                {
                popup.show(e.getComponent(),
                    layersbutton.getLocation().x,
                    //layersbutton.getLocation().y+
                    layersbutton.getSize().height);
                }
            public void mouseReleased(MouseEvent e) 
                {
                layersbutton.setSelected(false);
                }
            });




        //Create the popup menu.
        refreshbutton = new JToggleButton(Display2D.REFRESH_ICON);
        refreshbutton.setPressedIcon(Display2D.REFRESH_ICON_P);
        refreshbutton.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        refreshbutton.setBorderPainted(false);
        refreshbutton.setContentAreaFilled(false);
        refreshbutton.setToolTipText("Change How and When the Display Redraws Itself");
        
        header.add(refreshbutton);
        refreshPopup = new JPopupMenu();
        refreshPopup.setLightWeightPopupEnabled(false);

        //Add listener to components that can bring up popup menus.
        refreshbutton.addMouseListener(new MouseAdapter()
            {
            public void mousePressed(MouseEvent e)
                {
                rebuildRefreshPopup();
                refreshPopup.show(e.getComponent(),
                    0,
                    //refreshbutton.getLocation().x,
                    refreshbutton.getSize().height);
                }
            public void mouseReleased(MouseEvent e)
                {
                refreshbutton.setSelected(false);
                rebuildRefreshPopup();
                }
            });





        
        movieButton = new JButton(Display2D.MOVIE_OFF_ICON);
        movieButton.setPressedIcon(Display2D.MOVIE_OFF_ICON_P);
        movieButton.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        movieButton.setBorderPainted(false);
        movieButton.setContentAreaFilled(false);
        movieButton.setToolTipText("Create a Quicktime movie");
        movieButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                if (movieMaker==null) startMovie();
                else stopMovie();
                }
            });
        header.add(movieButton);
                
        snapshotButton = new JButton(Display2D.CAMERA_ICON);
        snapshotButton.setPressedIcon(Display2D.CAMERA_ICON_P);
        snapshotButton.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        snapshotButton.setBorderPainted(false);
        snapshotButton.setContentAreaFilled(false);
        snapshotButton.setToolTipText("Create a snapshot (as a PNG file)");
        snapshotButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                SwingUtilities.invokeLater(new Runnable(){
                    public void run()
                        {
                        takeSnapshot();
                        }
                    });
                }
            });
        header.add(snapshotButton);

        optionButton = new JButton(Display2D.OPTIONS_ICON);
        optionButton.setPressedIcon(Display2D.OPTIONS_ICON_P);
        optionButton.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        optionButton.setBorderPainted(false);
        optionButton.setContentAreaFilled(false);
        optionButton.setToolTipText("Options");
        optionButton.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                optionPane.setVisible(true);
                }
            });
        header.add(optionButton);
        
        // add the scale field
        scaleField = new NumberTextField("  Scale: ", 1.0, true)
            {
            public double newValue(double newValue)
                {
                if (newValue <= 0.0) newValue = currentValue;
                setScale(newValue);
                return newValue;
                }
            };
        scaleField.setToolTipText("Magnifies the scene.  Not the same as zooming (see the options panel)");
        scaleField.setBorder(BorderFactory.createEmptyBorder(0,0,0,2));
        header.add(scaleField);


        setPreferredSize(new Dimension((int)width,(int)height));
        
        setLayout(new BorderLayout());
        add(header,BorderLayout.NORTH);
                
        // default mask for auxillary objects
        auxillarySwitchMask.clear(AXES_AUX_INDEX);  // turn axes off
        auxillarySwitchMask.clear(BACKGROUND_AUX_INDEX);    // turn background off by default
        showBackgroundCheckBox.setSelected(true);
        
        createSceneGraph();
        
        skipFrame = new JFrame();
        rebuildSkipFrame();
        skipFrame.pack();

        createConsoleMenu();
        }
    
    Appearance backdropAppearance = null;  // for spherical backgrounds
    Image backdropImage = null;  // for nonspherical backgrounds
    Color backdropColor = null;  // for nonspherical backgrounds
    
    /** Clears any backdrop presently being used in the scene, turns off the backdrop checkbox, and disables the backdrop checkbox. */
    public void clearBackdrop()
        {
        backdropAppearance = null;
        backdropImage = null;
        backdropColor = null;
        setShowsBackdrop(false);
        }
    
    /** Sets a general appearance for a spherical backdrop, turns on the backdrop checkbox,  and enables the backdrop checkbox. */
    public void setBackdrop(Appearance appearance)
        {
        clearBackdrop();
        backdropAppearance = appearance;
        setShowsBackdrop(true);
        }
        
    /** Sets the color for a flat backdrop, turns on the backdrop checkbox,  and enables the backdrop checkbox. */
    public void setBackdrop(java.awt.Color color)
        {
        clearBackdrop();
        backdropColor = color;
        setShowsBackdrop(true);
        }

    /** Sets the image for a backdrop (spherical or flat), turns on the backdrop checkbox,  and enables the backdrop checkbox */
    public void setBackdrop(Image image, boolean spherical)
        {
        clearBackdrop();
        if (spherical && image!=null )
            {
            Appearance appearance = new Appearance(); 
            appearance.setTexture(new TextureLoader(image,null).getTexture()); 
            setBackdrop(appearance);
            }
        else 
            {
            backdropImage = image;
            }
        setShowsBackdrop(true);
        }
        
                
    void rebuildAuxillarySwitch()
        {
        auxillarySwitch = new Switch(Switch.CHILD_MASK);
        auxillarySwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
        auxillarySwitch.setCapability(Switch.ALLOW_CHILDREN_READ);
        auxillarySwitch.setCapability(Switch.ALLOW_CHILDREN_EXTEND);
        auxillarySwitch.setChildMask(auxillarySwitchMask);

        // Add Axes to position 0 of switch
        AxesPortrayal3D x = new AxesPortrayal3D(0.01f, true);
        x.setCurrentDisplay(this);
        auxillarySwitch.insertChild(x.getModel(null, null), AXES_AUX_INDEX);
                
        
        // Add Backdrop Sphere to position 1 of switch
        if (backdropAppearance != null || backdropColor != null || backdropImage !=null)  // make a backdrop!
            {
            Background background = new Background();
            background.setApplicationBounds(new BoundingSphere(
                    new Point3d(0,0,0), Double.MAX_VALUE));
            
            if (backdropAppearance!=null)
                {
                BranchGroup backgroundBG = new BranchGroup();
                Sphere sphere = new Sphere(1.0f, 
                    Sphere.GENERATE_TEXTURE_COORDS | 
                    Sphere.GENERATE_NORMALS | 
                    Sphere.GENERATE_NORMALS_INWARD, 45, backdropAppearance);
                // sphere lies along y axis.  Move it to Z axis
                Transform3D strans = new Transform3D();
                strans.rotX(-Math.PI/2);
                TransformGroup tg = new TransformGroup(strans);
                tg.addChild(sphere);
                
                // We want to auto-spin the spherical background with our autospinner.
                // unfortunately it only spins elements in the scene.  Our trick
                // here is to put the sphere in a transform group and spin that
                // transform group when we spin the rest of the scene.  Ick.  But
                // it works!
                autoSpinBackgroundTransformGroup.addChild(tg);

                backgroundBG.addChild(autoSpinBackgroundTransformGroup);
                background.setGeometry(backgroundBG);
                }
            else if (backdropColor!=null) 
                background.setColor(new Color3f(backdropColor));
            else // flat background image
                { // ensure it's a buffered image
                BufferedImage img = getGraphicsConfiguration().createCompatibleImage(
                    backdropImage.getWidth(null),
                    backdropImage.getHeight(null));
                Graphics g = img.getGraphics();
                g.drawImage(backdropImage,0,0,null);
                background.setImage(new ImageComponent2D(ImageComponent2D.FORMAT_RGB,img));
                background.setImageScaleMode(Background.SCALE_FIT_MAX);
                img.flush();  // just in case -- bug in OS X
                }
                                        
            auxillarySwitch.addChild(background);
            }
        else auxillarySwitch.addChild(new Group());  // empty
                                  
        // Add Floor to position 2 of switch
        //        auxillarySwitch.addChild(new Group());  // empty stub
        //        auxillarySwitch.setChildMask(auxillarySwitchMask);


        bogusMover = new PointArray(1, PointArray.COORDINATES);
        bogusMover.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
        moveBogusMover();
                  
        auxillarySwitch.addChild(new Shape3D(bogusMover)); 
        }
    
    // The bogusMover changes his value each time step.  He's a single point so he's invisible.
    // This ensures that something in the scene changes, which is important because for bizarre
    // reasons Java3D won't redraw otherwise -- so we can't write out a movie frame etc. if it so
    // happened that nothing in the scene changed in-between time steps.
    static final float[] bogusPosition = new float[]{0,0,0};
    PointArray bogusMover;
    void moveBogusMover()
        {
        bogusMover.setCoordinate(0,bogusPosition);
        }
    
    void toggleAxes()
        {
        if (auxillarySwitch != null)
            {
            auxillarySwitchMask.set(AXES_AUX_INDEX, showAxesCheckBox.isSelected());
            auxillarySwitch.setChildMask(auxillarySwitchMask);
            }
        }
                
    public void setShowsAxes(boolean value)
        {
        showAxesCheckBox.setSelected(value);
        toggleAxes();
        }

    void toggleBackdrop()
        {
        if (auxillarySwitch != null)
            {
            auxillarySwitchMask.set(BACKGROUND_AUX_INDEX, showBackgroundCheckBox.isSelected());
            auxillarySwitch.setChildMask(auxillarySwitchMask);
            }
        }

    public void setShowsBackdrop(boolean value)
        {
        showBackgroundCheckBox.setSelected(value);
        toggleBackdrop();
        }

    void toggleSpotlight()
        {
        if (lightSwitch != null)
            {
            lightSwitchMask.set(SPOTLIGHT_INDEX, showSpotlightCheckBox.isSelected());
            lightSwitch.setChildMask(lightSwitchMask);
            }
        }
                
    public void setShowsSpotlight(boolean value)
        {
        showSpotlightCheckBox.setSelected(value);
        toggleSpotlight();
        }

    void toggleAmbientLight()
        {
        if (lightSwitch != null)
            {
            lightSwitchMask.set(AMBIENT_LIGHT_INDEX, showAmbientLightCheckBox.isSelected());
            lightSwitch.setChildMask(lightSwitchMask);
            }
        }
                
    public void setShowsAmbientLight(boolean value)
        {
        showAmbientLightCheckBox.setSelected(value);
        toggleAmbientLight();
        }

    /** The TransformGroup which holds the switch holding the portrayal's scene graph models.   
        A good time to fool around with this is in the sceneGraphCreated() hook. 
        This is a good place to hang stuff which you want to get rotated AND transformed along with the scene graph. */
    public TransformGroup globalModelTransformGroup;
    
    void rebuildGlobalModelTransformGroup()
        {
        TransformGroup newGroup = new TransformGroup();
        newGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        newGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

        if (globalModelTransformGroup != null)  // transfer the transform :-)
            {
            Transform3D trans = new Transform3D();
            globalModelTransformGroup.getTransform(trans);
            newGroup.setTransform(trans);
            }
        globalModelTransformGroup = newGroup;
        }
        
    
    /** Returns a copy of the current global model transform. */
    public Transform3D getTransform()
        {
        Transform3D trans = new Transform3D();
        globalModelTransformGroup.getTransform(trans);
        return trans;
        }
        
    /** Sets the Display3D's global model transform.  This is a user-modifiable
        transform which should be used primarily to adjust the location all of the models together.  */
    public void setTransform(Transform3D transform)
        {
        if (transform!=null) globalModelTransformGroup.setTransform(new Transform3D(transform));
        else globalModelTransformGroup.setTransform(new Transform3D());
        }
        
    /** Changes the global model transform of the FieldPortrayal3D by 
        appending to it the provided transform operation. */
    public void transform(Transform3D transform)
        {
        Transform3D current = getTransform();
        current.mul(transform, current);
        setTransform(current);
        }
    
    /** Resets the global model transform to its default value (identity). */
    public void resetTransform()
        {
        globalModelTransformGroup.setTransform(new Transform3D());
        }
    
    /** Modifies the global model transform by rotating along the current X axis the provided number of degrees. */
    public void rotateX(double degrees)
        {
        Transform3D other = new Transform3D();
        other.rotX(degrees * Math.PI / 180);
        transform(other);
        }
    
    /** Modifies the global model transform by rotating along the current Y axis the provided number of degrees. */
    public void rotateY(double degrees)
        {
        Transform3D other = new Transform3D();
        other.rotY(degrees * Math.PI / 180);
        transform(other);
        }
    
    /** Modifies the global model transform by rotating along the current Z axis the provided number of degrees. */    
    public void rotateZ(double degrees)
        {
        Transform3D other = new Transform3D();
        other.rotZ(degrees * Math.PI / 180);
        transform(other);
        }
        
    /** Modifies the global model transform by translating in the provided x, y, and z amounts. */    
    public void translate(double dx, double dy, double dz)
        {
        Transform3D other = new Transform3D();
        other.setTranslation(new Vector3d(dx,dy,dz));
        transform(other);
        }
        
    /** Modifies the global model transform by uniformly scaling it in all directions by the provided amount.   Has 
        <i>nothing to do</i> with the setScale(...) and getScale(...) methods. */    
    public void scale(double value)
        {
        Transform3D other = new Transform3D();
        other.setScale(value);
        transform(other);
        }

    /** Modifies the internal transform by scaling it in a nonuniform fashion. Note that this is less efficient than a uniform scale.
        Has <i>nothing to do</i> with the setScale(...) and getScale(...) methods. */    
    public void scale(double sx, double sy, double sz)
        {
        Transform3D other = new Transform3D();
        other.setScale(new Vector3d(sx,sy,sz));
        transform(other);
        }

    /** Eliminates the existing scene graph.  This ought to be called when starting a simulation prior to creating
        any new Portrayal3Ds.  The reason is simple: if the Portrayal3Ds use shared geometry objects underneath, then
        creating them may create new shared geometry objects, which isn't permitted if there are existing shared geometry
        objects in a live scene graph.  So destroy the scene graph, create the new objects, and recreate the scene graph, and you're set.
    */
    public void destroySceneGraph()
        {
        // unhook the root from the universe so we can reuse the universe (Hmmmm....)
            
        mSelectBehavior.detach();
        root.detach();
        universe.getLocale().removeBranchGraph(root);
        canvas.stopRenderer();
        }

    ToolTipBehavior toolTipBehavior;
    boolean usingToolTips;
        
    /** Recreates the entire scene graph, including the universe, root, and canvas3d.  This is an expensive procedure
        and generally should only be called when starting a simulation (in GUIState's start() or load() methods). 
        Just before the renderer is started and the scene goes online, the hook sceneGraphCreated() is called in case
        you'd like to add anything to the scene graph. */
    public void createSceneGraph()
        {
        dirty = false;  // reset dirty flag -- we're creating the scene graph
        
        // Recreate the graph -- destroy the existing one
        if (universe == null)   // make a canvas
            {
            //if (universe != null)
            //{ remove(canvas); revalidate(); }
            canvas = new CapturingCanvas3D(SimpleUniverse.getPreferredConfiguration());
            add(canvas, BorderLayout.CENTER);
            universe = new SimpleUniverse(canvas);
            universe.getViewingPlatform().setNominalViewingTransform();  //take the viewing point a step back
       
            // set up light switch elements
            lightSwitch = new Switch(Switch.CHILD_MASK);
            lightSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
            lightSwitch.setCapability(Switch.ALLOW_CHILDREN_READ);
            lightSwitch.setCapability(Switch.ALLOW_CHILDREN_EXTEND);
                        
            lightSwitchMask.set(SPOTLIGHT_INDEX);    // turn spotlight on
            lightSwitchMask.clear(AMBIENT_LIGHT_INDEX);    // turn ambient light off 
            lightSwitch.setChildMask(lightSwitchMask);
            PointLight pl = new PointLight(new Color3f(1f,1f,1f),
                new Point3f(0f,0f,0f),
                new Point3f(1f,0f,0f));
            pl.setInfluencingBounds(new BoundingSphere(new Point3d(0,0,0), Double.POSITIVE_INFINITY));
            lightSwitch.addChild(pl);
            AmbientLight al = new AmbientLight(new Color3f(1f,1f,1f));
            al.setInfluencingBounds(new BoundingSphere(new Point3d(0,0,0), Double.POSITIVE_INFINITY));
            lightSwitch.addChild(al);
                        
            viewRoot = new BranchGroup();
            viewRoot.addChild(lightSwitch);
            universe.getViewingPlatform().getViewPlatformTransform().addChild(viewRoot);
            }
        else // reset the canvas
            {
            // detatches the root and the selection behavior from the universe.
            // we'll need to reattach those.  Everything else: the canvas and lights etc.,
            // will stay connected.
            destroySceneGraph();
            }
        
        // The root in our universe will be a branchgroup
        BranchGroup oldRoot = root;
        root = new BranchGroup();
        // in order to add/remove spinBehavior, I need these:
        root.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
        root.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
        root.setCapability(BranchGroup.ALLOW_BOUNDS_READ);
        // I need this one to delete the root when we reset the canvas above
        root.setCapability(BranchGroup.ALLOW_DETACH);

        // the root's child is a transform group (autoSpinTransformGroup), which can be spun around by the auto-spinner
        autoSpinTransformGroup = new TransformGroup();
        autoSpinTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);  // for spin behavior
   
        // autoSpinTransformGroup contains a switch to turn the various field portrayals on and off
        portrayalSwitch = new Switch(Switch.CHILD_MASK);
        portrayalSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
        portrayalSwitch.setCapability(Switch.ALLOW_CHILDREN_READ);
        portrayalSwitch.setCapability(Switch.ALLOW_CHILDREN_EXTEND);

        // We sneakily include ANOTHER transform group to spin around with another spinner
        // (autoSpinBackground).  This lets us spin the background around with the elements in the universe
        autoSpinBackgroundTransformGroup = new TransformGroup();
        autoSpinBackgroundTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);  // for spin behavior

        // ADD THE MODEL
        // Add to the switch each subgraph: all the field portrayals plus the axes.
        portrayalSwitchMask = new BitSet(subgraphCount);
        int count = 0;
        Iterator iter = portrayals.iterator();
        while (iter.hasNext())
            {
            Portrayal3DHolder p3h = (Portrayal3DHolder)(iter.next());
            Portrayal3D p = p3h.portrayal;
            Object obj = (p instanceof FieldPortrayal3D)? ((FieldPortrayal3D)p).getField(): null;
            p.setCurrentDisplay(this);
            portrayalSwitch.addChild(p.getModel(obj,null));
            if (p3h.visible)
                portrayalSwitchMask.set(count);
            else
                portrayalSwitchMask.clear(count);
            count++;  // go to next position in visibility mask
            }
        portrayalSwitch.setChildMask(portrayalSwitchMask);

        // add inspection
        BoundingSphere bounds = new BoundingSphere(new Point3d(0.0,0.0,0.0), Double.POSITIVE_INFINITY);
        mSelectBehavior =  new SelectionBehavior(canvas, root, bounds, simulation);
        mSelectBehavior.setSelectsAll(selectionAll, inspectionAll);
        mSelectBehavior.setEnable(selectBehCheckBox.isSelected());

        toolTipBehavior = new ToolTipBehavior(canvas, root, bounds);
        toolTipBehavior.setEnable(true);
        toolTipBehavior.setCanShowToolTips(usingToolTips);
        
        // make autoSpinTransformGroup spinnable
        // note that Alpha's loop count is ZERO beacuse I want the spin behaivor turned off.
        // Don't forget to put a -1 instead if you want endless spinning. 
        if (autoSpin == null)  // haven't set it up yet
            {
            autoSpin = new RotationInterpolator(new Alpha(), autoSpinTransformGroup);
            autoSpin.getAlpha().setLoopCount(0); 
            autoSpin.setSchedulingBounds(bounds);

            // spin the background too
            autoSpinBackground = new RotationInterpolator(new Alpha(), autoSpinBackgroundTransformGroup);
            autoSpinBackground.getAlpha().setLoopCount(0); 
            autoSpinBackground.setSchedulingBounds(bounds);

            setSpinningEnabled(false);
            }
        else 
            {
            oldRoot.removeChild(autoSpin);  // so it can be added to the new root
            oldRoot.removeChild(autoSpinBackground);
            }

        // create the global model transform group
        rebuildGlobalModelTransformGroup();
        
        // set up auxillary elements
        rebuildAuxillarySwitch();
                
        // add the ability to rotate, translate, and zoom
        mOrbitBehavior = new OrbitBehavior(canvas, OrbitBehavior.REVERSE_ALL);
        mOrbitBehavior.setRotateEnable(true);
        mOrbitBehavior.setRotXFactor(orbitRotateXCheckBox.isSelected() ? 1.0 : 0.0);
        mOrbitBehavior.setRotYFactor(orbitRotateYCheckBox.isSelected() ? 1.0 : 0.0);
        mOrbitBehavior.setTranslateEnable(true);
        mOrbitBehavior.setTransXFactor(orbitTranslateXCheckBox.isSelected() ? 1.0 : 0.0);
        mOrbitBehavior.setTransYFactor(orbitTranslateYCheckBox.isSelected() ? 1.0 : 0.0);
        mOrbitBehavior.setZoomEnable(orbitZoomCheckBox.isSelected());
        mOrbitBehavior.setSchedulingBounds(bounds);
        universe.getViewingPlatform().setViewPlatformBehavior(mOrbitBehavior);
                
        // hook everything up
        globalModelTransformGroup.addChild(portrayalSwitch);
        autoSpinTransformGroup.addChild(globalModelTransformGroup);
        autoSpinTransformGroup.addChild(auxillarySwitch);

        root.addChild(autoSpin);
        root.addChild(autoSpinBackground);
        autoSpin.setTarget(autoSpinTransformGroup);  // reuse
        autoSpinBackground.setTarget(autoSpinBackgroundTransformGroup);  // reuse
        root.addChild(autoSpinTransformGroup);

        // define attributes -- at this point the optionsPanel has been created so it's okay
        setCullingMode(cullingMode);
        setRasterizationMode(rasterizationMode);

        // call our hook
        sceneGraphCreated();

        // add the universe
        universe.addBranchGraph(root);

        // fire it up
        canvas.startRenderer();
        
        //updateSceneGraph(movieMaker != null);  // force a paint into a movie frame if necessary
        }

    static final double DEFAULT_FIELD_OF_VIEW = Math.PI / 4.0;
    double scale;
    Object scaleLock = new Object();
    
    /** A hook for people who want to modify the scene graph just after it's been created (when the
        user pressed the start button usually; or if the user dynamicallly attaches or detaches
        portrayals) but before the root has been attached to the universe and before the
        canvas has started rendering. Override  this as you see fit.  The default does nothing at all. */
    protected void sceneGraphCreated()
        {
        }
    
    /**
       Changes the value of the scale (magnification).  Usually done by the user through
       changing the value of the Scale text field.  This has <i>nothing to do</i> with 
       the scale() method.
    */
    public void setScale(double val) 
        {
        synchronized(scaleLock)
            {
            if (val < 1.0)
                scale = DEFAULT_FIELD_OF_VIEW + 
                    (Math.PI - DEFAULT_FIELD_OF_VIEW) * (1.0 - val) * (1.0 - val);
            else scale = DEFAULT_FIELD_OF_VIEW / val;
            canvas.getView().setFieldOfView(scale);
            }
        }
    
    /**
       Returns the value of the scale (magnification). This has <i>nothing to do</i> with 
       the scale() method.
    */
    public double getScale() 
        {
        synchronized(scaleLock)
            {
            return scale;
            }
        }
    
    RotationInterpolator autoSpin = null;
    RotationInterpolator autoSpinBackground = null;

    /** The TransformGroup which used to spin the underlying model. 
        This is a good place to hang stuff which you want to get rotated along with the models in the scene graph,
        but DON'T want transformed along with the scene graph. */
    public TransformGroup autoSpinTransformGroup = new TransformGroup();
    // We sneakily include ANOTHER transform group to spin around with another spinner
    // (autoSpinBackground).  This lets us spin the background around with the elements in the universe
    TransformGroup autoSpinBackgroundTransformGroup = new TransformGroup();

    OrbitBehavior mOrbitBehavior = null;
    SelectionBehavior mSelectBehavior = null; 
        
    public SelectionBehavior getSelectionBehavior() { return mSelectBehavior; }
    public ToolTipBehavior getToolTipBehavior() { return toolTipBehavior; }

    boolean selectionAll = true;
    boolean inspectionAll = true;
    /** Sets whether mouse-clicking results in selecting all picked elements (true) or just the closest one (false).
        This can be done independently of selection and inspection.  By default these values are both TRUE. */
    public void setSelectsAll(boolean selection, boolean inspection)
        {
        selectionAll = selection;
        inspectionAll = inspection;
        mSelectBehavior.setSelectsAll(selectionAll, inspectionAll);
        }

    /** Updates the scene graph */
    public synchronized void paintComponent(final Graphics g)
        {
        SwingUtilities.invokeLater(new Runnable(){ public void run(){ updateSceneGraph(false);}});
        } 


    protected int updateRule = Display2D.UPDATE_RULE_ALWAYS;
    protected long stepInterval = 1;
    protected double timeInterval = 0;
    protected long wallInterval = 0;
    long lastStep = -1;
    double lastTime = Schedule.BEFORE_SIMULATION;
    long lastWall = -1;  // the current time is around 1266514720569 so this should be fine (knock on wood)
    Object[] updateLock = new Object[0];
    boolean updateOnce = false;

    /** Asks Display3D to update itself next iteration regardless of the current redrawing/updating rule. */
    public void requestUpdate()
        {
        synchronized(updateLock)
            {
            updateOnce = true;
            }
        }

    /** Returns whether it's time to update. */
    public boolean shouldUpdate()
        {
        boolean val = false;
        boolean up = false;
        synchronized(updateLock) { up = updateOnce; } 
        
        if (up)
            val = true;
        else if (updateRule == Display2D.UPDATE_RULE_ALWAYS)
            val = true;
        else if (updateRule == Display2D.UPDATE_RULE_STEPS)
            {
            long step = simulation.state.schedule.getSteps();
            val = (lastStep < 0 || stepInterval == 0 || step - lastStep >= stepInterval || // clearly need to update
                lastStep % stepInterval >= step % stepInterval);  // on opposite sides of a tick
            if (val) lastStep = step;
            }
        else if (updateRule == Display2D.UPDATE_RULE_WALLCLOCK_TIME)
            {
            long wall = System.currentTimeMillis();
            val = (lastWall == 0 || wallInterval == 0 || wall - lastWall >= wallInterval || // clearly need to update
                lastWall % wallInterval >= wall % wallInterval);  // on opposite sides of a tick
            if (val) lastWall = wall;
            }
        else if (updateRule == Display2D.UPDATE_RULE_INTERNAL_TIME)
            {
            double time = simulation.state.schedule.getTime();
            val = (lastTime == 0 || timeInterval == 0 || time - lastTime >= timeInterval || // clearly need to update
                lastTime % timeInterval >= time % timeInterval);  // on opposite sides of a tick
            if (val) lastTime = time;
            }
        // else val = false;
                
        // reset updateOnce
        synchronized(updateLock) { updateOnce = false; }
        
        return val;
        }

    /** Steps the Display3D in the GUIState schedule.  Every <i>interval</i> steps,
        this results in updating the scen graph. */
    public void step(final SimState state)
        {
        if (shouldUpdate() &&
                (canvas.isShowing()    // only draw if we can be seen
                || movieMaker != null ))      // OR draw to a movie even if we can't be seen
            {
            updateSceneGraph(true);
            }
        }
        
    /**
       Updates the scene graph to reflect changes in the simulation.  If <i>waitForRenderer</i> is
       true, we block until the renderer has finished producing the scene on-screen.
    */
    public void updateSceneGraph(boolean waitForRenderer)
        {
        /* 
         * So far, the Canvas3D is not rendering off-screen, so new frames are not
         * produced when the display is hidden. Therefore, no point in copying the
         * same old frame over and over into the movie.
         * 
         * Canvas3D could be used in off-screen mode to make a movie without rendering
         * the frames on the screen, but I really don't think this is the bottle neck.
         * 
         * Meanwhile the simulation goes faster when this is hidden, 
         * because the renderer is short-circuited
         */
    
        // need to do this first...
        if (canvas==null) return;  // hasn't been created yet
        
        // creating the scene graph requires waiting for the renderer.  So
        // we can only uncheck the dirty flag if we're waiting for the renderer
        // ourselves.  Additionally, if we call createSceneGraph() when we're
        // NOT waiting for the renderer (a repaint for example), the program
        // will hang sometimes, dunno why.  :-(
        if (dirty && waitForRenderer) { createSceneGraph(); return; }
    
        //canvas.stopRenderer();
                
        boolean changes = false;
        Iterator iter = portrayals.iterator();
        
        moveBogusMover();
        while(iter.hasNext())
            {
            Portrayal3DHolder ph = (Portrayal3DHolder)iter.next();
            if(portrayalSwitchMask.get(ph.subgraphIndex))
                {
                // update model ONLY on what is actually on screen. 
                ph.portrayal.setCurrentDisplay(this);
                ph.portrayal.getModel(
                    (ph.portrayal instanceof FieldPortrayal3D)? ((FieldPortrayal3D)ph.portrayal).getField(): null,
                    (TransformGroup)portrayalSwitch.getChild(ph.subgraphIndex));
                changes = true;
                /** TODO sometimes, this is not enough.
                 * the models are called update upon, but 
                 * nothing actully changes; as a result,
                 * the canvas does not paint a new frame.
                 * => some change must be artificially perfomed
                 * to force post draw notification. Otherwise
                 * the simulation gets stuck at the wait(0)
                 **/
                }
            }
        //canvas.startRenderer();
                
        waitForRenderer &= changes; 
        if(!waitForRenderer)
            return;
            
        synchronized(canvas)
            {
            try
                {
                if (!Thread.currentThread().isInterrupted())
                    // couldn't there be a race condition here?  -- Sean
                    canvas.wait(0);
                }
            catch(InterruptedException ex)
                {
                try
                    {
                    Thread.currentThread().interrupt();
                    }
                catch (SecurityException ex2) { } // some stupid browsers -- *cough* IE -- don't like interrupts
                }
            }

        // we synchronize here so stopMovie() and startMovie() can
        // prevent us from adding images if necessary.
        synchronized(simulation.state.schedule)
            {
            // if waitForRenderer is false, does the movie maker ever get updated?  -- Sean
            if(movieMaker!=null)
                movieMaker.add(canvas.getLastImage());
            }
        }
    

    /** Takes a snapshot of the Display3D's currently displayed simulation. */
    public void takeSnapshot(File file) throws IOException
        {
        canvas.beginCapturing(false);
        BufferedImage image = canvas.getLastImage();
        PNGEncoder tmpEncoder = new PNGEncoder(image, false,PNGEncoder.FILTER_NONE,9);
        OutputStream stream = new BufferedOutputStream(new FileOutputStream(file));
        stream.write(tmpEncoder.pngEncode());
        stream.close();
        image.flush();  // just in case -- OS X bug?
        }


    /** Takes a snapshot of the Display3D's currently displayed simulation.
        Ought only be done from the main event loop. */
    public void takeSnapshot()
        {
        if (SimApplet.isApplet())
            {
            Object[] options = {"Oops"};
            JOptionPane.showOptionDialog(
                this, "You cannot save snapshots from an applet.",
                "MASON Applet Restriction",
                JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE,
                null, options, options[0]);
            return;
            }

        // start the image
        canvas.beginCapturing(false);
        
        // NOW pop up the save window
        FileDialog fd = new FileDialog(getFrame(), 
            "Save Snapshot as 24-bit PNG...", 
            FileDialog.SAVE);
        fd.setFile("Untitled.png");
        fd.setVisible(true);;
        if (fd.getFile()!=null)
            try
                {
                File snapShotFile = new File(fd.getDirectory(), Utilities.ensureFileEndsWith(fd.getFile(),".png"));
//                PNGEncoder tmpEncoder = new PNGEncoder(image, false,PNGEncoder.FILTER_NONE,9);
                BufferedImage image = canvas.getLastImage();
                PNGEncoder tmpEncoder = new PNGEncoder(image, false,PNGEncoder.FILTER_NONE,9);
                OutputStream stream = new BufferedOutputStream(new FileOutputStream(snapShotFile));
                stream.write(tmpEncoder.pngEncode());
                stream.close();
                image.flush();  // just in case -- OS X bug?
                }
            catch (FileNotFoundException e) { } // fail
            catch (IOException e) { /* could happen on close? */} // fail
        }



    /** Starts a Quicktime movie on the given Display3D.  The size of the movie frame will be the size of
        the display at the time this method is called.  This method ought to be called from the main event loop.
        Most of the default movie formats provided will result in a giagantic movie, which you can
        re-encode using something smarter (like the Animation or Sorenson codecs) to put to a reasonable size.
        On the Mac, Quicktime Pro will do this quite elegantly. */
    public void startMovie()
        {
        // can't start a movie if we're in an applet
        if (SimApplet.isApplet())
            {
            Object[] options = {"Oops"};
            JOptionPane.showOptionDialog(
                this, "You cannot create movies from an applet.",
                "MASON Applet Restriction",
                JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE,
                null, options, options[0]);
            return;
            }

        if (movieMaker!=null) return;  // already running

        // we synchronize because movieMaker.add() could
        // get called, via updateSceneGraph(), from inside the model thread.
        // This allows us to guarantee,
        // everywhere where movieMaker is set (to null or to new), that paintToMovie
        // isn't doing anything.
        synchronized(simulation.state.schedule)
            {
            movieMaker = new MovieMaker(getFrame());
                    
            canvas.beginCapturing(false);  // emit a single picture to get the image sizes
            final BufferedImage typicalImage = canvas.getLastImage();
                    
            if (!movieMaker.start(typicalImage))
                movieMaker = null;  // fail
            else
                {
                canvas.beginCapturing(true);
                movieButton.setIcon(Display2D.MOVIE_ON_ICON); 
                movieButton.setPressedIcon(Display2D.MOVIE_ON_ICON_P);
                simulation.scheduleAtEnd(new Steppable()   // to stop movie when simulation is stopped
                    {
                    public void step(SimState state) { stopMovie(); }
                    });
                }
                                
            typicalImage.flush();  // just in case -- bug in OS X
            }
        }
        
    /** Stops a Quicktime movie and cleans up, flushing the remaining frames out to disk. 
        This method ought to be called from the main event loop. */
    public void stopMovie()
        {
        // we synchronize because movieMaker.add() could
        // get called, via updateSceneGraph(), from inside the model thread.
        // This allows us to guarantee,
        // everywhere where movieMaker is set (to null or to new), that paintToMovie
        // isn't doing anything.
        synchronized(simulation.state.schedule)
            {
            if (movieMaker == null) return;  // already stopped
            canvas.stopCapturing();
            if (!movieMaker.stop())
                {
                Object[] options = {"Drat"};
                JOptionPane.showOptionDialog(
                    this, "Your movie did not write to disk\ndue to a spurious JMF movie generation bug.",
                    "JMF Movie Generation Bug",
                    JOptionPane.OK_OPTION, JOptionPane.WARNING_MESSAGE,
                    null, options, options[0]);
                }
            movieMaker = null;
            if (movieButton!=null)  // hasn't been destroyed yet
                {
                movieButton.setIcon(Display2D.MOVIE_OFF_ICON);
                movieButton.setPressedIcon(Display2D.MOVIE_OFF_ICON_P);
                }
            }
        }

    /** Quits the Display3D.  Called by the Display3D's frame if the Display3D made the frame itself.
        Also called by finalize().  Otherwise you should call this method before destroying the Display3D. */
    public void quit()
        {
        stopMovie();
            
        //universe.removeAllLocales();
        universe.cleanup(); 
        }
        
    /** Quits the Display3D.  Okay, so finalize is evil and we're not supposed to rely on it.
        We're not.  But it's an additional cargo-cult programming measure just in case. */
    protected void finalize() throws Throwable
        {
        super.finalize();
        quit();
        }
        
    JCheckBox orbitRotateXCheckBox = new JCheckBox("Rotate Left/Right");
    JCheckBox orbitRotateYCheckBox = new JCheckBox("Up/Down");
    JCheckBox orbitTranslateXCheckBox = new JCheckBox("Translate Left/Right");
    JCheckBox orbitTranslateYCheckBox = new JCheckBox("Up/Down");
    JCheckBox orbitZoomCheckBox = new JCheckBox("Move Towards/Away");
    JCheckBox selectBehCheckBox = new JCheckBox("Select");
    JRadioButton polyPoint = new JRadioButton("Vertices", false);
    JRadioButton polyLine = new JRadioButton("Edges", false);
    JRadioButton polyFill = new JRadioButton("Fill", true);
    JRadioButton polyCullNone = new JRadioButton("Both Sides", true);
    JRadioButton polyCullFront = new JRadioButton("Back Side Only", false);
    JRadioButton polyCullBack = new JRadioButton("Front Side Only", false);
    
    JCheckBox showAxesCheckBox = new JCheckBox("Axes");
    JCheckBox showBackgroundCheckBox = new JCheckBox("Backdrop");
    JCheckBox tooltips = new JCheckBox("ToolTips");
    JCheckBox showSpotlightCheckBox = new JCheckBox("Spotlight");
    JCheckBox showAmbientLightCheckBox = new JCheckBox("Ambient Light");
        
    //JCheckBox antialiasCheckBox = new JCheckBox("Antialias Graphics");
    //JRadioButton viewPerspective = new JRadioButton("Perspective Projection", true);
    //JRadioButton viewParallel = new JRadioButton("Parallel Projection", false);
        
    void setSpinningEnabled(boolean value)
        {
        // when turning off spinning, you want to both DISABLE
        // the spin rotator (because it sucks lots of juice otherwise)
        // and you want to set the loop count to 0 (to keep it from,
        // just for a second, spinning in the default (Y) direction until
        // it clues in that it has to stop spinning).  -- Sean
        if (autoSpin!=null)
            if (value) 
                {
                autoSpin.setEnable(true);
                autoSpin.getAlpha().setLoopCount(-1); 
                // spin the background too
                autoSpinBackground.setEnable(true);
                autoSpinBackground.getAlpha().setLoopCount(-1); 
                }
            else
                {
                autoSpin.setEnable(false);
                autoSpin.getAlpha().setLoopCount(0); 
                // spin the background too
                autoSpinBackground.setEnable(false);
                autoSpinBackground.getAlpha().setLoopCount(0); 
                }
        }
        
    // used by numberTextFields below to compute correct transform
    Transform3D getTransformForAxis(double dx, double dy, double dz)
        {
        Transform3D t = new Transform3D();
        Transform3D t1 = new Transform3D();
        t.rotX(Math.atan2(dz,dy));
        t1.rotZ(-Math.atan2(dx,Math.sqrt(dy*dy+dx*dz)));
        t.mul(t1);
        return t;
        }

    NumberTextField rotAxis_X = new NumberTextField(null,0, false) //1, Math.PI/4)
        {       
        public double newValue(double newValue)
            {
            autoSpin.setTransformAxis(getTransformForAxis(newValue, rotAxis_Y.getValue(), rotAxis_Z.getValue()));
            // spin background too
            autoSpinBackground.setTransformAxis(getTransformForAxis(newValue, rotAxis_Y.getValue(), rotAxis_Z.getValue()));
            if (spinDuration.getValue() == 0 ||
                (newValue == 0 && rotAxis_Y.getValue() == 0 && rotAxis_Z.getValue()==0))
                setSpinningEnabled(false);
            else setSpinningEnabled(true);
            return newValue;
            }
        };
    NumberTextField rotAxis_Y = new NumberTextField(null,0, false) // 1, Math.PI/4)
        {       
        public double newValue(double newValue)
            {
            autoSpin.setTransformAxis(getTransformForAxis(rotAxis_X.getValue(),newValue, rotAxis_Z.getValue()));
            // spin background too
            autoSpinBackground.setTransformAxis(getTransformForAxis(rotAxis_X.getValue(),newValue, rotAxis_Z.getValue()));
            if (spinDuration.getValue() == 0 ||
                (rotAxis_X.getValue() == 0 && newValue == 0 && rotAxis_Z.getValue()==0))
                setSpinningEnabled(false);
            else setSpinningEnabled(true);
            return newValue;
            }
        };      
    NumberTextField rotAxis_Z = new NumberTextField(null,0, false) // 1, Math.PI/4)
        {       
        public double newValue(double newValue)
            {
            autoSpin.setTransformAxis(getTransformForAxis(rotAxis_X.getValue(),rotAxis_Y.getValue(),newValue));
            // spin background too
            autoSpinBackground.setTransformAxis(getTransformForAxis(rotAxis_X.getValue(),rotAxis_Y.getValue(),newValue));
            if (spinDuration.getValue() == 0 ||
                (rotAxis_X.getValue() == 0 && rotAxis_Y.getValue() == 0 && newValue==0))
                setSpinningEnabled(false);
            else setSpinningEnabled(true);
            return newValue;
            }
        };
        
    NumberTextField spinDuration = new NumberTextField(null, 0, 1, 0.02) // 0, true)
        {       
        public double newValue(double newValue)
            {
            long mSecsPerRot = (newValue == 0 ? 1 /* don't care */ : (long)(1000 / newValue));
            
            autoSpin.getAlpha().setIncreasingAlphaDuration(mSecsPerRot);
            // spin background too
            autoSpinBackground.getAlpha().setIncreasingAlphaDuration(mSecsPerRot);
            if (newValue == 0 ||
                (rotAxis_X.getValue() == 0 && rotAxis_Y.getValue() == 0 && rotAxis_Z.getValue()==0))
                setSpinningEnabled(false);
            else setSpinningEnabled(true);
            return newValue;  // rounding errors ignored...
            }
        };    

    /** Sets the rasterization mode for configurable polygon portrayals. 
        Mode can be PolygonAttributes.POLYGON_FILL, PolygonAttributes.POLYGON_LINE, 
        or PolygonAttributes.POLYGON_POINT. */
    int rasterizationMode = PolygonAttributes.POLYGON_FILL;
    public void setRasterizationMode(int mode)
        {
        rasterizationMode = mode;
        polyFill.setSelected(mode==PolygonAttributes.POLYGON_FILL);
        polyLine.setSelected(mode==PolygonAttributes.POLYGON_LINE);
        polyPoint.setSelected(mode==PolygonAttributes.POLYGON_POINT);
        
        Iterator iter = portrayals.iterator();
        while(iter.hasNext())
            {
            PolygonAttributes pa = ((Portrayal3DHolder)iter.next()).portrayal.polygonAttributes();
            try
                {
                if (pa != null) pa.setPolygonMode(mode);
                }
            catch (javax.media.j3d.CapabilityNotSetException e) { }
            }
        }
        

    /** Sets the rasterization mode for configurable polygon portrayals. 
        Mode can be PolygonAttributes.CULL_BACK, PolygonAttributes.CULL_FRONT, 
        or PolygonAttributes.CULL_NONE. */
    int cullingMode = PolygonAttributes.CULL_NONE;
    public void setCullingMode(int mode)
        {
        cullingMode = mode;
        polyCullNone.setSelected(mode==PolygonAttributes.CULL_NONE);
        polyCullBack.setSelected(mode==PolygonAttributes.CULL_BACK);
        polyCullFront.setSelected(mode==PolygonAttributes.CULL_FRONT);

        Iterator iter = portrayals.iterator();
        while(iter.hasNext())
            {
            PolygonAttributes pa = ((Portrayal3DHolder)iter.next()).portrayal.polygonAttributes();
            try 
                {
                if (pa != null) pa.setCullFace(mode);
                }
            catch (javax.media.j3d.CapabilityNotSetException e) { }
            }
        }
    
    
    /** */
    ArrayList selectedWrappers = new ArrayList();

    /** Returns as LocationWrappers all the currently selected objects in the
        display.  Do not modify these wrapper objects; they are used internally. 
        These LocationWrappers may be invalid at any time in the near future if
        the user deselects objects.
    */
    public LocationWrapper[] getSelectedWrappers()
        {
        return (LocationWrapper[]) selectedWrappers.toArray(new LocationWrapper[selectedWrappers.size()]);
        }
    
    
    public void performSelection( LocationWrapper wrapper)
        {
        Bag b = new Bag();
        b.add(wrapper);
        performSelection(b);
        }
    
    public void clearSelections()
        {
        for(int x=0;x<selectedWrappers.size();x++)
            {
            LocationWrapper wrapper = ((LocationWrapper)(selectedWrappers.get(x)));
            wrapper.getFieldPortrayal().setSelected(wrapper, false);
            }
        selectedWrappers.clear();
        }
        

    public void performSelection( Bag locationWrappers )
        {
        // deselect existing objects first before selecting new ones
        clearSelections();
        
        if (locationWrappers == null) return;  // deselect everything
        
        // add new wrappers
        for(int x=0;x < locationWrappers.size(); x++)
            {
            LocationWrapper wrapper = ((LocationWrapper)(locationWrappers.get(x)));
            wrapper.getFieldPortrayal().setSelected(wrapper, true);
            selectedWrappers.add(wrapper);
            }
            
        updateSceneGraph(false);

        // finally, update the model inspector and other stuff, since this may
        // be affected by the new selection
        simulation.controller.refresh();
        }



    JButton systemPreferences = new JButton("MASON");
    JButton appPreferences = new JButton("Simulation");
    public class OptionPane3D extends JFrame
        {
        OptionPane3D(String label)
            {
            super(label);
                        
            // set some tool tips
            orbitRotateXCheckBox.setToolTipText("Rotates the scene left or right. Drag the left mouse button.");
            orbitRotateYCheckBox.setToolTipText("Rotates the scene up or down. Drag the left mouse button.");
            orbitTranslateXCheckBox.setToolTipText("Move the scene left or right.  Drag the middle mouse button.");
            orbitTranslateYCheckBox.setToolTipText("Move the scene up or down.  Drag the middle mouse button.");
            orbitZoomCheckBox.setToolTipText("Moves the eye towards/away from scene.  Not the same as scaling.  Drag the right mouse button.");
            selectBehCheckBox.setToolTipText("Selects objects.  Double-click the left mouse button.");

            // Mouse Behaviors
            Box outerBehaviorsPanel = new Box(BoxLayout.X_AXIS);
            outerBehaviorsPanel.setBorder(new javax.swing.border.TitledBorder("Mouse Actions"));
                        
            // add rotateX, translateX, zoom, select to left panel
            Box leftBehaviors = new Box(BoxLayout.Y_AXIS);
            leftBehaviors.add(orbitRotateXCheckBox);
            orbitRotateXCheckBox.setSelected(true);
            leftBehaviors.add(orbitTranslateXCheckBox);
            orbitTranslateXCheckBox.setSelected(true);
            leftBehaviors.add(orbitZoomCheckBox);
            orbitZoomCheckBox.setSelected(true);
            leftBehaviors.add(Box.createGlue());

            // add rotateY, translateY, reset to right panel
            Box rightBehaviors = new Box(BoxLayout.Y_AXIS);
            rightBehaviors.add(orbitRotateYCheckBox);
            orbitRotateYCheckBox.setSelected(true);
            rightBehaviors.add(orbitTranslateYCheckBox);
            orbitTranslateYCheckBox.setSelected(true);
            rightBehaviors.add(selectBehCheckBox);
            selectBehCheckBox.setSelected(true);
            rightBehaviors.add(Box.createGlue());

            outerBehaviorsPanel.add(leftBehaviors);
            outerBehaviorsPanel.add(rightBehaviors);
            outerBehaviorsPanel.add(Box.createGlue());
                        
                        
            Box resetBox = new Box(BoxLayout.X_AXIS);
            resetBox.setBorder(new javax.swing.border.TitledBorder("Viewpoint"));
            JButton resetButton = new JButton("Reset Viewpoint");
            resetButton.setToolTipText("Resets display to original rotation, translation, and zoom.");
            resetBox.add(resetButton);
            resetBox.add(Box.createGlue());

            resetButton.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    canvas.stopRenderer();
                    // reset scale field
                    scaleField.setValue(1);
                    setScale(1);
                                        
                    universe.getViewingPlatform().setNominalViewingTransform(); // reset translations/rotations
                    autoSpinTransformGroup.setTransform(new Transform3D());
                    // reset background spin too
                    autoSpinBackgroundTransformGroup.setTransform(new Transform3D());
                    canvas.startRenderer();
                    } 
                });
                        
            orbitRotateXCheckBox.addItemListener(new ItemListener()
                {
                public void itemStateChanged(ItemEvent e)
                    {
                    if (mOrbitBehavior!=null) mOrbitBehavior.setRotXFactor(orbitRotateXCheckBox.isSelected() ? 1.0 : 0.0); }
                });
            orbitRotateYCheckBox.addItemListener(new ItemListener()
                {
                public void itemStateChanged(ItemEvent e)
                    {
                    if (mOrbitBehavior!=null) mOrbitBehavior.setRotYFactor(orbitRotateYCheckBox.isSelected() ? 1.0 : 0.0); }
                });
            orbitTranslateXCheckBox.addItemListener(new ItemListener()
                {
                public void itemStateChanged(ItemEvent e)
                    {
                    if (mOrbitBehavior!=null) mOrbitBehavior.setTransXFactor(orbitTranslateXCheckBox.isSelected() ? 1.0 : 0.0); }
                });
            orbitTranslateYCheckBox.addItemListener(new ItemListener()
                {
                public void itemStateChanged(ItemEvent e)
                    {
                    if (mOrbitBehavior!=null) mOrbitBehavior.setTransYFactor(orbitTranslateYCheckBox.isSelected() ? 1.0 : 0.0); }
                });
            orbitZoomCheckBox.addItemListener(new ItemListener()
                {
                public void itemStateChanged(ItemEvent e)
                    {       if (mOrbitBehavior!=null) mOrbitBehavior.setZoomEnable(orbitZoomCheckBox.isSelected()); }
                });         
            selectBehCheckBox.addItemListener(new ItemListener()
                {
                public void itemStateChanged(ItemEvent e)
                    {       if (mSelectBehavior!=null) mSelectBehavior.setEnable(selectBehCheckBox.isSelected()); }
                });         

            // Auto-Orbiting
            LabelledList rotatePanel = new LabelledList("Auto-Rotate About <X,Y,Z> Axis");
            rotatePanel.addLabelled("X", rotAxis_X);
            rotatePanel.addLabelled("Y", rotAxis_Y);
            rotatePanel.addLabelled("Z", rotAxis_Z);
            rotatePanel.addLabelled("Rotations/Sec", spinDuration);
                                        
            Box polyPanel = new Box(BoxLayout.X_AXIS);
            polyPanel.setBorder(new javax.swing.border.TitledBorder("Polygon Attributes"));
            ButtonGroup polyLineGroup = new ButtonGroup();
            polyLineGroup.add(polyPoint);
            polyLineGroup.add(polyLine);
            polyLineGroup.add(polyFill);
            ButtonGroup polyCullingGroup = new ButtonGroup();
            polyCullingGroup.add(polyCullNone);
            polyCullingGroup.add(polyCullFront);
            polyCullingGroup.add(polyCullBack);
                                        
            Box polyLinebox = Box.createVerticalBox();
            polyLinebox.add(Box.createGlue());
            polyLinebox.add (new JLabel ("Draw Polygons As..."));
            polyLinebox.add (polyPoint);
            polyPoint.addActionListener(new ActionListener()
                { 
                public void actionPerformed(ActionEvent e) {setRasterizationMode(PolygonAttributes.POLYGON_POINT);} 
                });
            polyLinebox.add (polyLine);
            polyLine.addActionListener(new ActionListener()
                { 
                public void actionPerformed(ActionEvent e) {setRasterizationMode(PolygonAttributes.POLYGON_LINE);} 
                });
            polyLinebox.add (polyFill);
            polyFill.addActionListener(new ActionListener()
                { 
                public void actionPerformed(ActionEvent e) {setRasterizationMode(PolygonAttributes.POLYGON_FILL);} 
                });
            polyLinebox.add(Box.createGlue());
            polyLinebox.setBorder(new javax.swing.border.EmptyBorder(0,0,0,20));
            polyPanel.add(polyLinebox);
            Box polyCullbox = Box.createVerticalBox();
            polyCullbox.add(Box.createGlue());
            polyCullbox.add (new JLabel ("Draw Faces As..."));
            polyCullbox.add (polyCullNone);
            polyCullNone.addActionListener(new ActionListener()
                { 
                public void actionPerformed(ActionEvent e) {setCullingMode(PolygonAttributes.CULL_NONE);} 
                });
            polyCullbox.add (polyCullBack);
            polyCullBack.addActionListener(new ActionListener()
                { 
                public void actionPerformed(ActionEvent e) {setCullingMode(PolygonAttributes.CULL_BACK);} 
                });
            polyCullbox.add (polyCullFront);
            polyCullFront.addActionListener(new ActionListener()
                { 
                public void actionPerformed(ActionEvent e) {setCullingMode(PolygonAttributes.CULL_FRONT);} 
                });
            polyCullbox.add(Box.createGlue());
            polyCullbox.setBorder(new javax.swing.border.EmptyBorder(0,0,0,20));
            polyPanel.add(polyCullbox);
            polyPanel.add(Box.createGlue());
                        
            Box auxillaryPanel = new Box(BoxLayout.Y_AXIS);
            Box box = new Box(BoxLayout.X_AXIS);
            auxillaryPanel.setBorder(new javax.swing.border.TitledBorder("Auxillary Elements"));
            box.add(showAxesCheckBox);
            showAxesCheckBox.addItemListener(new ItemListener()
                {
                public void itemStateChanged(ItemEvent e)
                    {       
                    toggleAxes();
                    }
                });
            box.add(showBackgroundCheckBox);
            showBackgroundCheckBox.addItemListener(new ItemListener()
                {
                public void itemStateChanged(ItemEvent e)
                    {       
                    toggleBackdrop();
                    }
                });
            box.add(tooltips);
            tooltips.addItemListener(new ItemListener()
                {
                public void itemStateChanged(ItemEvent e)
                    {
                    usingToolTips = tooltips.isSelected();
                    if (toolTipBehavior != null)
                        toolTipBehavior.setCanShowToolTips(usingToolTips);
                    }
                });
            box.add(Box.createGlue());
            auxillaryPanel.add(box);
                                        
            // next row
            box = new Box(BoxLayout.X_AXIS);
            box.add(showSpotlightCheckBox);
            showSpotlightCheckBox.setSelected(true);
            showSpotlightCheckBox.addItemListener(new ItemListener()
                {
                public void itemStateChanged(ItemEvent e)
                    {       
                    toggleSpotlight();
                    }
                });

            box.add(showAmbientLightCheckBox);
            showAmbientLightCheckBox.addItemListener(new ItemListener()
                {
                public void itemStateChanged(ItemEvent e)
                    {       
                    toggleAmbientLight();
                    }
                });
            box.add(Box.createGlue());
            auxillaryPanel.add(box);

            // set up initial design
                                
                        
            Box optionsPanel = new Box(BoxLayout.Y_AXIS);
            optionsPanel.add(outerBehaviorsPanel);
            optionsPanel.add(rotatePanel);
            optionsPanel.add(auxillaryPanel);
            optionsPanel.add(polyPanel);
            optionsPanel.add(resetBox);
            //optionsPanel.add(viewPanel);

            getContentPane().add(optionsPanel);
            setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                        
                        
            // add preferences
                                
            Box b = new Box(BoxLayout.X_AXIS);
            b.add(new JLabel(" Save as Defaults for "));
            b.add(appPreferences);
            b.add(systemPreferences);
            getContentPane().add(b, BorderLayout.SOUTH);

            systemPreferences.putClientProperty( "JComponent.sizeVariant", "mini" );
            systemPreferences.putClientProperty( "JButton.buttonType", "bevel" );
            systemPreferences.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    String key = getPreferencesKey();
                    savePreferences(Prefs.getGlobalPreferences(key));
                                        
                    // if we're setting the system preferences, remove the local preferences to avoid confusion
                    Prefs.removeAppPreferences(simulation, key);
                    }
                });
                        
            appPreferences.putClientProperty( "JComponent.sizeVariant", "mini" );
            appPreferences.putClientProperty( "JButton.buttonType", "bevel" );
            appPreferences.addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    String key = getPreferencesKey();
                    savePreferences(Prefs.getAppPreferences(simulation, key));
                    }
                });

            pack();
            setResizable(false);
            } 


        /** Saves the Option Pane Preferences to a given Preferences Node */
        public void savePreferences(Preferences prefs)
            {
            try
                {
                prefs.putBoolean(ROTATE_LEFT_RIGHT_KEY, orbitRotateXCheckBox.isSelected());
                prefs.putBoolean(ROTATE_UP_DOWN_KEY, orbitRotateYCheckBox.isSelected());
                prefs.putBoolean(TRANSLATE_LEFT_RIGHT_KEY, orbitTranslateXCheckBox.isSelected());
                prefs.putBoolean(TRANSLATE_UP_DOWN_KEY, orbitTranslateYCheckBox.isSelected());
                prefs.putBoolean(MOVE_TOWARDS_AWAY_KEY, orbitZoomCheckBox.isSelected());
                prefs.putBoolean(SELECT_KEY, selectBehCheckBox.isSelected());

                prefs.putDouble(AUTO_ROTATE_X_KEY, rotAxis_X.getValue());
                prefs.putDouble(AUTO_ROTATE_Y_KEY, rotAxis_Y.getValue());
                prefs.putDouble(AUTO_ROTATE_Z_KEY, rotAxis_Z.getValue());
                prefs.putDouble(AUTO_ROTATE_RATE_KEY, spinDuration.getValue());
                                                        
                prefs.putBoolean(AXES_KEY, showAxesCheckBox.isSelected());
                prefs.putBoolean(TOOLTIPS_KEY, tooltips.isSelected());
                prefs.putBoolean(SPOTLIGHT_KEY, showSpotlightCheckBox.isSelected());
                prefs.putBoolean(AMBIENT_LIGHT_KEY, showAmbientLightCheckBox.isSelected());
                prefs.putBoolean(BACKDROP_KEY, showBackgroundCheckBox.isSelected());
                                                        
                prefs.putInt(DRAW_POLYGONS_KEY,
                    polyPoint.isSelected() ? 0 : 
                    polyLine.isSelected() ? 1 : 2);
                prefs.putInt(DRAW_FACES_KEY,
                    polyCullNone.isSelected() ? 0 : 
                    polyCullBack.isSelected() ? 1 : 2);

                if (!Prefs.save(prefs))
                    Utilities.inform ("Preferences Cannot be Saved", "Your Java system can't save preferences.  Perhaps this is an applet?", this);
                }
            catch (java.security.AccessControlException e) { } // it must be an applet
            }
                        
                        
        static final String ROTATE_LEFT_RIGHT_KEY = "Rotate Left Right";
        static final String TRANSLATE_LEFT_RIGHT_KEY = "Translate Left Right";
        static final String MOVE_TOWARDS_AWAY_KEY = "Move Towards Away";
        static final String ROTATE_UP_DOWN_KEY = "Rotate Up Down";
        static final String TRANSLATE_UP_DOWN_KEY = "Translate Up Down";
        static final String SELECT_KEY = "Select";
        static final String AUTO_ROTATE_X_KEY = "Auto Rotate X";
        static final String AUTO_ROTATE_Y_KEY = "Auto Rotate Y";
        static final String AUTO_ROTATE_Z_KEY = "Auto Rotate Z";
        static final String AUTO_ROTATE_RATE_KEY = "Auto Rotate Rate";
        static final String AXES_KEY = "Axes";
        static final String TOOLTIPS_KEY = "Tooltips";
        static final String SPOTLIGHT_KEY = "Spotlight";
        static final String AMBIENT_LIGHT_KEY = "Ambient Light";
        static final String BACKDROP_KEY = "Backdrop";
        static final String DRAW_POLYGONS_KEY = "Draw Polygons";
        static final String DRAW_FACES_KEY = "Draw Faces";
                
        /** Resets the Option Pane Preferences by loading from the preference database */
        void resetToPreferences()
            {
            try
                {
                Preferences systemPrefs = Prefs.getGlobalPreferences(getPreferencesKey());
                Preferences appPrefs = Prefs.getAppPreferences(simulation, getPreferencesKey());
                                                        
                orbitRotateXCheckBox.setSelected(appPrefs.getBoolean(ROTATE_LEFT_RIGHT_KEY,
                        systemPrefs.getBoolean(ROTATE_LEFT_RIGHT_KEY, true)));
                orbitRotateYCheckBox.setSelected(appPrefs.getBoolean(ROTATE_UP_DOWN_KEY,
                        systemPrefs.getBoolean(ROTATE_UP_DOWN_KEY, true)));
                orbitTranslateXCheckBox.setSelected(appPrefs.getBoolean(TRANSLATE_LEFT_RIGHT_KEY,
                        systemPrefs.getBoolean(TRANSLATE_LEFT_RIGHT_KEY, true)));
                orbitTranslateYCheckBox.setSelected(appPrefs.getBoolean(TRANSLATE_UP_DOWN_KEY,
                        systemPrefs.getBoolean(TRANSLATE_UP_DOWN_KEY, true)));
                selectBehCheckBox.setSelected(appPrefs.getBoolean(SELECT_KEY,
                        systemPrefs.getBoolean(SELECT_KEY, true)));

                rotAxis_X.setValue(rotAxis_X.newValue(appPrefs.getDouble(AUTO_ROTATE_X_KEY,
                            systemPrefs.getDouble(AUTO_ROTATE_X_KEY, 0))));
                rotAxis_Y.setValue(rotAxis_Y.newValue(appPrefs.getDouble(AUTO_ROTATE_Y_KEY,
                            systemPrefs.getDouble(AUTO_ROTATE_Y_KEY, 0))));
                rotAxis_Z.setValue(rotAxis_Z.newValue(appPrefs.getDouble(AUTO_ROTATE_Z_KEY,
                            systemPrefs.getDouble(AUTO_ROTATE_Z_KEY, 0))));
                spinDuration.setValue(spinDuration.newValue(appPrefs.getDouble(AUTO_ROTATE_RATE_KEY,
                            systemPrefs.getDouble(AUTO_ROTATE_RATE_KEY, 0))));

                showAxesCheckBox.setSelected(appPrefs.getBoolean(AXES_KEY,
                        systemPrefs.getBoolean(AXES_KEY, false)));
                tooltips.setSelected(appPrefs.getBoolean(TOOLTIPS_KEY,
                        systemPrefs.getBoolean(TOOLTIPS_KEY, false)));
                showSpotlightCheckBox.setSelected(appPrefs.getBoolean(SPOTLIGHT_KEY,
                        systemPrefs.getBoolean(SPOTLIGHT_KEY, true)));
                showAmbientLightCheckBox.setSelected(appPrefs.getBoolean(AMBIENT_LIGHT_KEY,
                        systemPrefs.getBoolean(AMBIENT_LIGHT_KEY, false)));
                showBackgroundCheckBox.setSelected(appPrefs.getBoolean(BACKDROP_KEY,
                        systemPrefs.getBoolean(BACKDROP_KEY, true)));

                int val = appPrefs.getInt(DRAW_POLYGONS_KEY, 
                    systemPrefs.getInt(DRAW_POLYGONS_KEY,
                        polyPoint.isSelected() ? 0 : 
                        polyLine.isSelected() ? 1 : 2));
                if (val == 0) polyPoint.setSelected(true);
                else if (val == 1) polyLine.setSelected(true);
                else // (val == 0) 
                    polyFill.setSelected(true);
                                                                        
                val = appPrefs.getInt(DRAW_FACES_KEY, 
                    systemPrefs.getInt(DRAW_FACES_KEY,
                        polyCullNone.isSelected() ? 0 : 
                        polyCullBack.isSelected() ? 1 : 2));
                if (val == 0) polyCullNone.setSelected(true);
                else if (val == 1) polyCullBack.setSelected(true);
                else // (val == 0) 
                    polyCullFront.setSelected(true);
                }
            catch (java.security.AccessControlException e) { } // it must be an applet
            }
                        
        }

// must be after all other declared widgets because its constructor relies on them existing
    public OptionPane3D optionPane = new OptionPane3D("3D Options");    
        
        
        
        
    protected void rebuildSkipFrame()
        {
        skipFrame.getContentPane().removeAll();
        skipFrame.getContentPane().invalidate();
        skipFrame.getContentPane().repaint();
        skipFrame.getContentPane().setLayout(new BorderLayout());

        JPanel skipHeader = new JPanel();
        skipHeader.setLayout(new BorderLayout());
        skipFrame.add(skipHeader, BorderLayout.CENTER);
                
        // add the interval (skip) field
        skipBox = new JComboBox(Display2D.REDRAW_OPTIONS);
        skipBox.setSelectedIndex(updateRule);
        ActionListener skipListener = new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                updateRule = skipBox.getSelectedIndex();
                if (updateRule == Display2D.UPDATE_RULE_ALWAYS || updateRule == Display2D.UPDATE_RULE_NEVER)
                    {
                    skipField.getField().setText("");
                    skipField.setEnabled(false);
                    }
                else if (updateRule == Display2D.UPDATE_RULE_STEPS)
                    {
                    skipField.setValue(stepInterval);
                    skipField.setEnabled(true);
                    }
                else if (updateRule == Display2D.UPDATE_RULE_INTERNAL_TIME)
                    {
                    skipField.setValue(timeInterval);
                    skipField.setEnabled(true);
                    }
                else // Display2D.UPDATE_RULE_WALLCLOCK_TIME
                    {
                    skipField.setValue((long)(wallInterval / 1000));  // integer division
                    skipField.setEnabled(true);
                    }
                }
            };
        skipBox.addActionListener(skipListener);
                
        // I want right justified text.  This is an ugly way to do it
        skipBox.setRenderer(new DefaultListCellRenderer()
            {
            public Component getListCellRendererComponent(JList list, Object value, int index,  boolean isSelected,  boolean cellHasFocus)
                {
                // JLabel is the default
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setHorizontalAlignment(SwingConstants.RIGHT);
                return label;
                }
            });
                        
        skipHeader.add(skipBox, BorderLayout.WEST);


        skipField = new NumberTextField(null, 1, false)
            {
            public double newValue(double newValue)
                {
                double val;
                if (updateRule == Display2D.UPDATE_RULE_ALWAYS || updateRule == Display2D.UPDATE_RULE_NEVER)  // shouldn't have happened
                    {
                    val = 0;
                    }
                else if (updateRule == Display2D.UPDATE_RULE_STEPS)
                    {
                    val = (long) newValue;
                    if (val < 1) val = stepInterval;
                    stepInterval = (long) val;
                    }
                else if (updateRule == Display2D.UPDATE_RULE_WALLCLOCK_TIME)
                    {
                    val = newValue;
                    if (val < 0) val = wallInterval / 1000;
                    wallInterval = (long) (newValue * 1000);
                    }
                else // if (updateRule == Display2D.UPDATE_RULE_INTERNAL_TIME)
                    {
                    val = newValue;
                    if (val < 0) val = timeInterval;
                    timeInterval = val;
                    }
                        
                // reset with a new interval
                reset();
                        
                return val;
                }
            };
        skipField.setToolTipText("Specify the interval between screen updates");
        skipField.getField().setColumns(10);
        skipHeader.add(skipField,BorderLayout.CENTER);
        skipHeader.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        skipListener.actionPerformed(null);  // have it update the text field accordingly
        }

    protected void rebuildRefreshPopup()
        {
        refreshPopup.removeAll();
        String s = "";
        switch(updateRule)
            {
            case Display2D.UPDATE_RULE_STEPS:
                s = (stepInterval == 1 ? "Currently redrawing each model iteration" :
                    "Currently redrawing each " + stepInterval +  " model iterations");
                break;
            case Display2D.UPDATE_RULE_INTERNAL_TIME:
                s = (timeInterval == 1 ? "Currently redrawing each unit of model time" :
                    "Currently redrawing every " + (timeInterval) +  " units of model time");
                break;
            case Display2D.UPDATE_RULE_WALLCLOCK_TIME:
                s = (wallInterval == 1000 ? "Currently redrawing each second of real time" :
                    "Currently redrawing every " + (wallInterval / 1000.0) +  " seconds of real time");
                break;
            case Display2D.UPDATE_RULE_ALWAYS:
                s = "Currently redrawing every model iteration";
                break;
            case Display2D.UPDATE_RULE_NEVER:
                s = "Currently never redrawing except when the window is redrawn";
                break;
            default:
                throw new RuntimeException("default case should never occur");
            }
        JMenuItem m = new JMenuItem(s);
        m.setEnabled(false);
        refreshPopup.add(m);
                
        refreshPopup.addSeparator();

        m = new JMenuItem("Always Redraw");
        refreshPopup.add(m);
        m.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                updateRule = Display2D.UPDATE_RULE_ALWAYS;
                rebuildSkipFrame();
                }
            });

        m = new JMenuItem("Never Redraw");
        refreshPopup.add(m);
        m.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                updateRule = Display2D.UPDATE_RULE_NEVER;
                rebuildSkipFrame();
                }
            });

        m = new JMenuItem("Redraw once every 2 iterations");
        refreshPopup.add(m);
        m.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                updateRule = Display2D.UPDATE_RULE_STEPS;
                stepInterval = 2;
                rebuildSkipFrame();
                }
            });

        m = new JMenuItem("Redraw once every 4 iterations");
        refreshPopup.add(m);
        m.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                updateRule = Display2D.UPDATE_RULE_STEPS;
                stepInterval = 4;
                rebuildSkipFrame();
                }
            });

        m = new JMenuItem("Redraw once every 8 iterations");
        refreshPopup.add(m);
        m.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                updateRule = Display2D.UPDATE_RULE_STEPS;
                stepInterval = 8;
                rebuildSkipFrame();
                }
            });

        m = new JMenuItem("Redraw once every 16 iterations");
        refreshPopup.add(m);
        m.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                updateRule = Display2D.UPDATE_RULE_STEPS;
                stepInterval = 16;
                rebuildSkipFrame();
                }
            });
                        
        m = new JMenuItem("Redraw once every 32 iterations");
        refreshPopup.add(m);
        m.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                updateRule = Display2D.UPDATE_RULE_STEPS;
                stepInterval = 16;
                rebuildSkipFrame();
                }
            });
                        
        refreshPopup.addSeparator();

        m = new JMenuItem("Redraw once at the next step");
        refreshPopup.add(m);
        m.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                requestUpdate();
                }
            });

        // add other menu items
        m = new JMenuItem("More Options...");
        refreshPopup.add(m);
        m.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                skipFrame.setTitle(getFrame().getTitle() + " Options");
                skipFrame.setVisible(true);
                }
            });

        refreshPopup.revalidate();
        }

        
        
    }
    
