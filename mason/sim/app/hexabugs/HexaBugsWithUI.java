/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.hexabugs;
import sim.engine.*;
import sim.display.*;
import sim.portrayal.grid.*;
import java.awt.*;
import javax.swing.*;
import sim.util.gui.*;
import sim.portrayal.*;

public class HexaBugsWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;

    HexaSparseGridPortrayal2D bugPortrayal = new HexaSparseGridPortrayal2D();

    // we have heat as EITHER hexagons OR rectangles.  The current portrayal is always
    // currentHeatPortrayal, which can be changed in the model inspector (see
    // getInspector() below)
    FastHexaValueGridPortrayal2D heatPortrayal= new FastHexaValueGridPortrayal2D("Heat");
    HexaValueGridPortrayal2D heatPortrayal2 = new HexaValueGridPortrayal2D("Heat");
    HexaValueGridPortrayal2D currentHeatPortrayal = heatPortrayal;

    public static void main(String[] args)
        {
        HexaBugsWithUI hexaBugs = new HexaBugsWithUI();
        Console c = new Console(hexaBugs);
        c.setVisible(true);
        }
    
    public HexaBugsWithUI() { super(new HexaBugs(System.currentTimeMillis())); }
    public HexaBugsWithUI(SimState state) { super(state); }
    
    public static String getName() { return "HexaBugs"; }
    
    public Object getSimulationInspectedObject() { return state; }
    
    // One approach to creating the Hexagons/Rectangles popup menu is to hand-code the
    // menu ourselves in a wrapper inspector.  This requires some knowledge of Java's
    // event handling etc.

    // Another approach to doing the Hexagon/Rectangle code is to create a class which has a single
    // readable, writable integer property called DisplayGridCellsAs.  If the property is set to 0 or 1
    // the object changes the heat portrayal appropriately just like was done above.  We make the
    // popup menu appear by providing a domain in the form of an array (for the values 0 and 1).
    // Then we just make an inspector which encapsulates the previous inspector and a new SimpleInspector
    // on an instance of this object.
    public class HexagonChoice
        {
        int cells = 0;
        public Object domDisplayGridCellsAs() { return new Object[] { "Rectangles", "Hexagons"}; }
        public int getDisplayGridCellsAs() { return cells; }
        public void setDisplayGridCellsAs(int val)
            {
            if (val == 0)
                {
                cells = val;
                currentHeatPortrayal = heatPortrayal;
                }
            else if (val == 1)
                {
                cells = val;
                currentHeatPortrayal = heatPortrayal2;
                }
                
            // reattach the portrayals
            display.detatchAll();
            display.attach(currentHeatPortrayal,"Heat");
            display.attach(bugPortrayal,"Bugs");

            // redisplay
            if (display!=null) display.repaint();
            }
        }
        
    public Inspector getInspector()
        {
        // we'll make a fancy inspector which has a nicely arranged update button and
        // two subinspectors.  In fact the inspector doesn't need an update button --
        // there's nothing that ever changes on update.  But since the inspector
        // has been declared non-volatile, just to be consistent, we'll add an update
        // button up top to show how it's done.  First we get our two subinspectors,
        // one for the hexagon choice menu and one for the model inspector proper.
        
        final Inspector originalInspector = super.getInspector();
        final SimpleInspector hexInspector = new SimpleInspector(new HexagonChoice(),this);
        
        // The originalInspector is non-volatile.  It's a SimpleInspector, which shows
        // its update-button automagically when non-volatile.  We WANT it to be non-volatile,
        // but not show its update button because that just updates the inspector and nothing
        // else.  So we declare the inspector to be volatile.  It won't matter because it'll
        // NEVER receive updateInspector() calls except via the outer inspector we're 
        // constructing next (which will be NON-volatile).
        
        originalInspector.setVolatile(true);
        
        // our wrapper inspector
        Inspector newInspector = new Inspector()
            {
            public void updateInspector() { originalInspector.updateInspector(); }  // don't care about updating hexInspector
            };
        newInspector.setVolatile(false);
        
        // NOW we want our outer inspector to be NON-volatile, but show an update button.
        // While SimpleInspectors add their own buttons automagically, plain Inspectors
        // do not.  Instead we have to add it manually.  We grab an update-button from
        // the inspector, put it in a box so it doesn't stretch when the inspector does.
        // And we want to move it in a bit border-wise because that's what the SimpleInspector
        // does.
                
        Box b = new Box(BoxLayout.X_AXIS)
            {
            public Insets getInsets() { return new Insets(2,2,2,2); }  // in a bit
            };
        b.add(newInspector.makeUpdateButton());
        b.add(Box.createGlue());

        // okay, great.  But we want the button to be up top, followed by the hex inspector,
        // and then the originalInspector taking up the rest of the room.  Sadly, there's
        // no layout manager to do that.  So we do it thus:

        Box b2 = new Box(BoxLayout.Y_AXIS);
        b2.add(b);
        b2.add(hexInspector);
        b2.add(Box.createGlue());
        
        // all one blob now.  We can add it at NORTH.

        newInspector.setLayout(new BorderLayout());
        newInspector.add(b2,BorderLayout.NORTH);
        newInspector.add(originalInspector,BorderLayout.CENTER);

        return newInspector;
        }



    public void start()
        {
        super.start();
        // set up our portrayals
        setupPortrayals();
        }
    
    public void load(SimState state)
        {
        super.load(state);
        // we now have new grids.  Set up the portrayals to reflect that
        setupPortrayals();
        }
        
    // This is called by start() and by load() because they both had this code
    // so I didn't have to type it twice :-)
    public void setupPortrayals()
        {
        // tell the portrayals what to portray and how to portray them
        ColorMap map = new sim.util.gui.SimpleColorMap(0,HexaBugs.MAX_HEAT,Color.black,Color.red);
        heatPortrayal.setField(((HexaBugs)state).valgrid);
        heatPortrayal.setMap(map);
        heatPortrayal2.setField(((HexaBugs)state).valgrid);
        heatPortrayal2.setMap(map);
        
        bugPortrayal.setField(((HexaBugs)state).buggrid);
        bugPortrayal.setPortrayalForAll(
            new sim.portrayal.simple.OvalPortrayal2D(Color.white));   // all the HexaBugs will be white ovals
            
        // reschedule the displayer
        display.reset();
                
        // redraw the display
        display.repaint();
        }
    
    /** The ratio of the width of a hexagon to its height: 1 / Sin(60 degrees), otherwise known as 2 / Sqrt(3) */
    public static final double HEXAGONAL_RATIO = 2/Math.sqrt(3);

    public void init(Controller c)
        {
        super.init(c);
        
        // Make the Display2D.  We'll have it display stuff later.
        
        // Horizontal hexagons are staggered.  This complicates computations.  Thus
        // if  you have a M x N grid scaled to SCALE, then
        // your height is (N + 0.5) * SCALE
        // and your width is ((M - 1) * (3/4) + 1) * HEXAGONAL_RATIO * SCALE
        
        // You might need to adjust by 1 or 2 pixels in each direction to get circles 
        // which usually come out as circles and not as ovals.
        
        final double scale = 4;
        final double m = 100;
        final double n = 100;
        final int height = (int) ( (n + 0.5) * scale );
        final int width = (int) ( ((m - 1) * 3.0 / 4.0 + 1) * HEXAGONAL_RATIO * scale );
        
        display = new Display2D(width, height, this, 1);
        displayFrame = display.createFrame();
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);

        // attach the portrayals
        display.attach(currentHeatPortrayal,"Heat");
        display.attach(bugPortrayal,"Bugs");

        // specify the backdrop color  -- what gets painted behind the displays
        display.setBackdrop(Color.black);
        }
        
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;  // let gc
        display = null;       // let gc
        }
    }
    
    
    
    
    
