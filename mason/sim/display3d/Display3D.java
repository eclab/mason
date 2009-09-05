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
    ArrayList portrayals = new ArrayList();
    Stoppable stopper;
    GUIState simulation;
    /** The component bar at the top of the Display3D. */
    public JComponent header;
    /** The button which starts or stops a movie */
    public JButton movieButton;
    /** The button which snaps a screenshot */
    public JButton snapshotButton;
    /** The button which pops up the option pane */
    public JButton optionButton;
    /** The field for scaling values */
    public NumberTextField scaleField;
    /** The field for skipping frames */
    public NumberTextField skipField;
        
    long interval = 1;
    Object intervalLock = new Object();
    /** Sets how many steps are skipped before the display updates itself. */
    public void setInterval(long i) { synchronized(intervalLock) { interval = i; } }
    /** Gets how many steps are skipped before the display updates itself. */
    public long getInterval() { synchronized(intervalLock) { return interval; } }
    
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
    public Switch lightSwitch = null;
    BitSet lightSwitchMask = new BitSet(NUM_LIGHT_ELEMENTS);
    final static int NUM_LIGHT_ELEMENTS = 2;
    final static int SPOTLIGHT_INDEX = 0;
    final static int AMBIENT_LIGHT_INDEX = 1;

    /** The MovieMaker.  If null, we're not shooting a movie. */
    public MovieMaker movieMaker = null;    

    /** The popup layers menu */
    public JPopupMenu popup;
    /** The button which pops up the layers menu */
    public JToggleButton togglebutton;  // for popup

    /** Sets various MacOS X features */
    static 
        {
        // use heavyweight tooltips -- otherwise they get obscured by the Canvas3D
        // [this appears to be ignored by MacOS X Java 1.4.1 and 1.4.2.  A bug? ]
        ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

        // Use Quaqua if it exists
        try
            {
            System.setProperty( "Quaqua.TabbedPane.design","auto" );  // UI Manager Properties docs differ
            System.setProperty( "Quaqua.visualMargin","1,1,1,1" );
            UIManager.put("Panel.opaque", Boolean.TRUE);
            UIManager.setLookAndFeel((String)(Class.forName("ch.randelshofer.quaqua.QuaquaManager").
                    getMethod("getLookAndFeelClassName",(Class[])null).invoke(null,(Object[])null)));
            } 
        catch (Exception e) { /* e.printStackTrace(); */ }

        try  // now we try to set certain properties if the security permits it
            {
            // turn on hardware acceleration on MacOS X.  As of September 2003, 1.3.1
            // turns this off by default, which makes 1.3.1 half the speed (and draws
            // objects wrong to boot).
            System.setProperty("com.apple.hwaccel","true");  // probably settable as an applet.  D'oh! Looks like it's ignored.
            System.setProperty("apple.awt.graphics.UseQuartz","true");  // counter the awful effect in OS X's Sun Renderer
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
        catch (Exception e) { }
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
                if ((new String("class javax.media.j3d.EventCatcher")).compareTo(l.getClass().toString()) == 0)
                    l = new localWindowListener(); 
                                                
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
        
    class localWindowListener extends java.awt.event.WindowAdapter 
        {
        // empty class to replace the windowlistener spawned by Canvas3D        
        }


    // how many subgraphs (field portrayals) do we have?
    int subgraphCount = 0;
    
    class Portrayal3DHolder
        {
        public Portrayal3D portrayal;
        public String name;
        public JCheckBoxMenuItem menuItem;
        public int subgraphIndex;
        public boolean visible = true;  // added -- Sean
        public String toString() { return name; }
        public Portrayal3DHolder(Portrayal3D p, String n, boolean visible)
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
    Frame getFrame()
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
            if (getInterval() < 1) setInterval(1);  // just in case...
            stopper = simulation.scheduleImmediateRepeat(true,this);
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
    
    /** Attaches a portrayal to the Display3D, along with the provided human-readable name for the portrayal.�
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
        /* In case our attached portrayal was done AFTER the display is live, let's recreate*/
        destroySceneGraph();
        
        Portrayal3DHolder p = new Portrayal3DHolder(portrayal,name,visible);
        portrayals.add(p);
        popup.add(p.menuItem);
        dirty = true;
        
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
    
    public void createConsoleMenu()
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
    
    /**
       Creates a Display3D with the provided width and height for its portrayal region, 
       attached to the provided simulation, and displaying itself with the given interval (which must be > 0).
    */
    // width and height are actually ints, but we're being consistent with Display2D
    public Display3D(final double width, final double height, GUIState simulation, long interval)
        {
        setInterval(interval);
        this.simulation = simulation;
        reset();  // must happen AFTER state is assigned
        
        final Color headerBackground = getBackground(); // will change later, see below
        header = new Box(BoxLayout.X_AXIS)
            // bug in Java3D results in header not painting its background in Windows,
            // XWindows, so we force it here.
            {
            public synchronized void paintComponent(final Graphics g)
                {
                g.setColor(headerBackground);
                g.fillRect(0,0,header.getWidth(),header.getHeight());
                }
            };

        // maybe this will cut down on flashing in Windows, XWindows.
        // But it will mess up the headers' background, so we hard-code that above.
        setBackground(Color.black);  


        togglebutton = new JToggleButton(Display2D.LAYERS_ICON);
        togglebutton.setPressedIcon(Display2D.LAYERS_ICON_P);
        togglebutton.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        togglebutton.setBorderPainted(false);
        togglebutton.setContentAreaFilled(false);
        togglebutton.setToolTipText("Show and hide different layers");
        header.add(togglebutton);
        
        //Create the popup menu.
        popup = new JPopupMenu();
        popup.setLightWeightPopupEnabled(false);

        //Add listener to components that can bring up popup menus.
        togglebutton.addMouseListener(new MouseAdapter()
            {
            public void mousePressed(MouseEvent e)
                {
                popup.show(e.getComponent(),
                    togglebutton.getLocation().x,
                    //togglebutton.getLocation().y+
                    togglebutton.getSize().height);
                }
            public void mouseReleased(MouseEvent e) 
                {
                togglebutton.setSelected(false);
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
                else                        stopMovie();
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
//                takeSnapshot();
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
                optionsFrame.setVisible(true);
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
        header.add(scaleField);

        skipField = new NumberTextField("  Skip: ", 1, false)
            {
            public double newValue(double newValue)
                {
                int val = (int) newValue;
                if (val < 1) val = 1;
                // reset with a new interval
                setInterval(val);
                reset();                        
                return val;
                }
            };
        header.add(skipField);
        
        setPreferredSize(new Dimension((int)width,(int)height));
        
        setLayout(new BorderLayout());
        add(header,BorderLayout.NORTH);
        
        createOptionsPanel();
        
        // default mask for auxillary objects
        auxillarySwitchMask.clear(AXES_AUX_INDEX);  // turn axes off
        auxillarySwitchMask.clear(BACKGROUND_AUX_INDEX);    // turn background off by default
        showBackgroundCheckBox.setSelected(true);
        showBackgroundCheckBox.setEnabled(false);  // disable backdrop checkbox until it's set by the simulation
        
        createSceneGraph();

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
        showBackgroundCheckBox.setEnabled(false);
        setShowsBackdrop(false);
        }
    
    /** Sets a general appearance for a spherical backdrop, turns on the backdrop checkbox,  and enables the backdrop checkbox. */
    public void setBackdrop(Appearance appearance)
        {
        clearBackdrop();
        backdropAppearance = appearance;
        showBackgroundCheckBox.setEnabled(appearance!=null);
        setShowsBackdrop(true);
        }
        
    /** Sets the color for a flat backdrop, turns on the backdrop checkbox,  and enables the backdrop checkbox. */
    public void setBackdrop(java.awt.Color color)
        {
        clearBackdrop();
        backdropColor = color;
        showBackgroundCheckBox.setEnabled(color != null);
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
        showBackgroundCheckBox.setEnabled(image != null);
        setShowsBackdrop(true);
        }
        
    /*
      Appearance floorAppearance = null;
      public void setFloor(Appearance appearance)
      { floorAppearance = appearance; }
    
      public void setFloor(java.awt.Color color)
      { floorAppearance = SimplePortrayal3D.appearanceForColor(color); }
    */


    void rebuildAuxillarySwitch()
        {
        auxillarySwitch = new Switch(Switch.CHILD_MASK);
        auxillarySwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
        auxillarySwitch.setCapability(Switch.ALLOW_CHILDREN_READ);
        auxillarySwitch.setCapability(Switch.ALLOW_CHILDREN_EXTEND);
        auxillarySwitch.setChildMask(auxillarySwitchMask);

        // Add Axes to position 0 of switch
        Axes x = new Axes(0.01f, true);
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
    private static final float[] bogusPosition = new float[]{0,0,0};
    PointArray bogusMover;
    void moveBogusMover()
        {
        bogusMover.setCoordinate(0,bogusPosition);
        }
    
    
    void toggleAxes()
        {
        auxillarySwitchMask.set(AXES_AUX_INDEX, showAxesCheckBox.isSelected());
        auxillarySwitch.setChildMask(auxillarySwitchMask);
        }
                
    public void setShowsAxes(boolean value)
        {
        showAxesCheckBox.setSelected(value);
        toggleAxes();
        }

    void toggleBackdrop()
        {
        auxillarySwitchMask.set(BACKGROUND_AUX_INDEX, showBackgroundCheckBox.isSelected());
        auxillarySwitch.setChildMask(auxillarySwitchMask);
        }

    public void setShowsBackdrop(boolean value)
        {
        showBackgroundCheckBox.setSelected(value);
        toggleBackdrop();
        }

    void toggleSpotlight()
        {
        lightSwitchMask.set(SPOTLIGHT_INDEX, showSpotlightCheckBox.isSelected());
        lightSwitch.setChildMask(lightSwitchMask);
        }
                
    public void setShowsSpotlight(boolean value)
        {
        showSpotlightCheckBox.setSelected(value);
        toggleSpotlight();
        }

    void toggleAmbientLight()
        {
        lightSwitchMask.set(AMBIENT_LIGHT_INDEX, showAmbientLightCheckBox.isSelected());
        lightSwitch.setChildMask(lightSwitchMask);
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
        
    
    /** Sets the Display3D's global model transform.  This is a user-modifiable
        transform which should be used primarily to adjust the location all of the models together.  */
    public Transform3D getTransform()
        {
        Transform3D trans = new Transform3D();
        globalModelTransformGroup.getTransform(trans);
        return trans;
        }
        
    /** Returns a copy of the current global model transform. */
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
                        
            BranchGroup bg = new BranchGroup();
            bg.addChild(lightSwitch);
            universe.getViewingPlatform().getViewPlatformTransform().addChild(bg);
            }
        else // reset the canvas
            {
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
        mSelectBehavior.setEnable(selectBehCheckBox.isSelected());

        toolTipBehavior = new ToolTipBehavior(canvas, root, bounds, simulation);
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
        universe.addBranchGraph(root);

        // define attributes -- at this point the optionsPanel has been created so it's okay
        setCullingMode(cullingMode);
        setRasterizationMode(rasterizationMode);

        // call our hook
        sceneGraphCreated();

        // fire it up
        canvas.startRenderer();
        
        //updateSceneGraph(movieMaker != null);  // force a paint into a movie frame if necessary
        }

    static final double DEFAULT_FIELD_OF_VIEW = Math.PI / 4.0;
    double scale;
    Object scaleLock = new Object();
    
    /** A hook for people who want to modify the scene graph just after it's been created (when the
        user pressed the start button usually; or if the user dynamicallly attaches or detaches
        portrayals) but before the canvas has started rendering. Override
        this as you see fit.  The default does nothing at all. */
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
    public SelectionBehavior mSelectBehavior = null; 

    /** Updates the scene graph */
    public synchronized void paintComponent(final Graphics g)
        {
        SwingUtilities.invokeLater(new Runnable(){ public void run(){ updateSceneGraph(false);}});
        } 

    /** Steps the Display3D in the GUIState schedule.  Every <i>interval</i> steps,
        this results in updating the scen graph. */
    public void step(final SimState state)
        {
        long steps = state.schedule.getSteps();
        if (steps % getInterval() == 0 &&   // time to update!
            state.schedule.time() < Schedule.AFTER_SIMULATION &&  // don't update if we're done
                (canvas.isShowing()    // only draw if we can be seen
                || movieMaker !=null ))      // OR draw to a movie even if we can't be seen
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
            Portrayal3DHolder ph =      (Portrayal3DHolder)iter.next();
            if(portrayalSwitchMask.get(ph.subgraphIndex))
                {
                // update model ONLY on what is actully on screen. 
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
                movieMaker.add(canvas.getFrameAsImage());
            }
        }
    
    Rectangle2D getImageSize()
        {
        Dimension s = canvas.getSize();
        Rectangle2D clip =  new Rectangle2D.Double(0,0,s.width,s.height);
        if(canvas == null)
            return null;
        Rectangle bounds = canvas.getGraphics().getClipBounds();
        if(bounds != null)
            clip = clip.createIntersection(bounds);
        return clip;
        }


    /** Takes a snapshot of the Display3D's currently displayed simulation.
        Ought only be done from the main event loop. */
    public void takeSnapshot()
        {
        if (SimApplet.isApplet)
            {
            Object[] options = {"Oops"};
            JOptionPane.showOptionDialog(
                this, "You cannot save snapshots from an applet.",
                "MASON Applet Restriction",
                JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE,
                null, options, options[0]);
            return;
            }

        canvas.setWritingParams(getImageSize(), false);
//              Image image = canvas.getFrameAsImage();
        
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
//                PngEncoder tmpEncoder = new PngEncoder(image, false,PngEncoder.FILTER_NONE,9);
                BufferedImage image = canvas.getFrameAsImage();
                PngEncoder tmpEncoder = new PngEncoder(image, false,PngEncoder.FILTER_NONE,9);
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
        if (SimApplet.isApplet)
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
                    
            Rectangle2D imgBounds = getImageSize();
            canvas.setWritingParams(imgBounds, false);
            final BufferedImage typicalImage = canvas.getFrameAsImage();
                    
            if (!movieMaker.start(typicalImage))
                movieMaker = null;  // fail
            else
                {
                canvas.setWritingParams(imgBounds, true);
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
            canvas.stopMovie();
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
    JFrame optionsFrame;
        
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
            if (spinDuration.currentValue == 0 ||
                (newValue == 0 && rotAxis_Y.currentValue == 0 && rotAxis_Z.currentValue==0))
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
            if (spinDuration.currentValue == 0 ||
                (rotAxis_X.currentValue == 0 && newValue == 0 && rotAxis_Z.currentValue==0))
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
            if (spinDuration.currentValue == 0 ||
                (rotAxis_X.currentValue == 0 && rotAxis_Y.currentValue == 0 && newValue==0))
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
                (rotAxis_X.currentValue == 0 && rotAxis_Y.currentValue == 0 && rotAxis_Z.currentValue==0))
                setSpinningEnabled(false);
            else setSpinningEnabled(true);
            return newValue;  // rounding errors ignored...
            }
        };    

    void createOptionsPanel()
        {
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
        leftBehaviors.add(selectBehCheckBox);
        selectBehCheckBox.setSelected(true);
        leftBehaviors.add(Box.createGlue());

        // add rotateY, translateY, reset to right panel
        Box rightBehaviors = new Box(BoxLayout.Y_AXIS);
        rightBehaviors.add(orbitRotateYCheckBox);
        orbitRotateYCheckBox.setSelected(true);
        rightBehaviors.add(orbitTranslateYCheckBox);
        orbitTranslateYCheckBox.setSelected(true);
        rightBehaviors.add(Box.createGlue());
        JButton resetButton = new JButton("Reset");
        resetButton.setToolTipText("Resets display to original rotation, translation, and zoom.");
        rightBehaviors.add(resetButton);

        outerBehaviorsPanel.add(leftBehaviors);
        outerBehaviorsPanel.add(rightBehaviors);
        outerBehaviorsPanel.add(Box.createGlue());
               
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
        /*
        // These aren't very helpful
        
        Box viewPanel = new Box(BoxLayout.Y_AXIS);
        viewPanel.setBorder(new javax.swing.border.TitledBorder("Viewing Attributes"));
        antialiasCheckBox.setSelected(false);
        antialiasCheckBox.addItemListener(new ItemListener()
        {
        public void itemStateChanged(ItemEvent e)
        {       canvas.getView().setSceneAntialiasingEnable(antialiasCheckBox.isSelected());    }
        });
        viewPanel.add(antialiasCheckBox);
        ButtonGroup viewProjectionGroup = new ButtonGroup();
        viewProjectionGroup.add(viewPerspective);
        viewProjectionGroup.add(viewParallel);
        viewPerspective.addActionListener(new ActionListener()
        { 
        public void actionPerformed(ActionEvent e)
        {       canvas.getView().setProjectionPolicy(canvas.getView().PERSPECTIVE_PROJECTION);}
        });
        viewParallel.addActionListener(new ActionListener()
        { 
        public void actionPerformed(ActionEvent e)
        {       canvas.getView().setProjectionPolicy(canvas.getView().PARALLEL_PROJECTION);}
        });
        viewPanel.add(viewPerspective);
        viewPanel.add(viewParallel);
        */
        
        Box auxillaryPanel = new Box(BoxLayout.Y_AXIS);
        Box box = new Box(BoxLayout.X_AXIS);
        auxillaryPanel.setBorder(new javax.swing.border.TitledBorder("Auxillary Elements"));
        box.add(showAxesCheckBox);
        showAxesCheckBox.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {       
                toggleAxes();
                }
            });
        box.add(showBackgroundCheckBox);
        showBackgroundCheckBox.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {       
                toggleBackdrop();
                }
            });
        box.add(tooltips);
        tooltips.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
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
        showSpotlightCheckBox.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {       
                toggleSpotlight();
                }
            });

        box.add(showAmbientLightCheckBox);
        showAmbientLightCheckBox.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
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
        //optionsPanel.add(viewPanel);

        optionsFrame = new JFrame("3D Options");
        optionsFrame.getContentPane().add(optionsPanel);
        optionsFrame.pack();
        optionsFrame.setResizable(false);
        optionsFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        } 


    /** Sets the rasterization mode for configurable polygon portrayals. 
        Mode can be PolygonAttributes.POLYGON_FILL, PolygonAttributes.POLYGON_LINE, 
        or PolygonAttributes.POLYGON_POINT. */
    int rasterizationMode = PolygonAttributes.POLYGON_FILL;
    void setRasterizationMode(int mode)
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
    void setCullingMode(int mode)
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
    
    public void performSelection( LocationWrapper wrapper)
        {
        Bag b = new Bag();
        b.add(wrapper);
        performSelection(b);
        }
    
    public void performSelection( final Bag locationWrappers )
        {
        // deselect existing objects first before selecting new ones
        for(int x=0;x<selectedWrappers.size();x++)
            {
            LocationWrapper wrapper = ((LocationWrapper)(selectedWrappers.get(x)));
            wrapper.getFieldPortrayal().setSelected(wrapper,false);
            }
        selectedWrappers.clear();
        
        if (locationWrappers == null) return;  // deselect everything
        
        // add new wrappers
        for(int x=0;x < locationWrappers.size(); x++)
            {
            LocationWrapper wrapper = ((LocationWrapper)(locationWrappers.get(x)));
            wrapper.getFieldPortrayal().setSelected(wrapper, true);
            selectedWrappers.add(wrapper);
            }
            
        updateSceneGraph(false);
        }
    }
    
