/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dflockers.display;
import java.awt.Color;

import javax.swing.JFrame;

import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.display.SimStateProxy;
import sim.engine.SimState;
import sim.portrayal.Inspector;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.simple.MovablePortrayal2D;

public class FlockersProxyWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;

    public static void main(String[] args)
        {
        new FlockersProxyWithUI().createController();  // randomizes by currentTimeMillis
        }

    public Object getSimulationInspectedObject()
    	{
    	return state;  // non-volatile
    	}

    ContinuousPortrayal2D flockersPortrayal = new ContinuousPortrayal2D();
        
// uncomment this to try out trails  (also need to uncomment out some others in this file, look around)
    //ContinuousPortrayal2D trailsPortrayal = new ContinuousPortrayal2D(); 
    
    public FlockersProxyWithUI()
        {
        super(new FlockersProxy(System.currentTimeMillis()));
        }
    
    public FlockersProxyWithUI(SimState state) 
        {
        super(state);
        }

    public static String getName()
    	{
    	return "Flockers Proxy";
    	}

    public void start()
        {
        super.start();
        setupPortrayals();
        }

    public void load(SimState state)
        {
        super.load(state);
        setupPortrayals();
        }
        
    public void setupPortrayals()
        {
        FlockersProxy flock = (FlockersProxy)state;

        flockersPortrayal.setField(flock.flockers);
        // uncomment this to try out trails  (also need to uncomment out some others in this file, look around)
        //trailsPortrayal.setField(flock.flockers);
        
        /*
            SimplePortrayal2D basic = new TrailedPortrayal2D(
                this,
                new OrientedPortrayal2D(
                    new SimplePortrayal2D(), 0, 4.0, Color.WHITE,
                    OrientedPortrayal2D.SHAPE_COMPASS)
                    	{
    					public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
                    		{
    						DObject dObject= (DObject)object;
    						int h = dObject.hashCode();
    						int hi = h >>> 24;
    						h = h & 0xFFFFFF;
    						h = h ^ (hi);
    						h = h ^ (hi << 8);
    						h = h ^ (hi << 16);
    						h = h | 128;
    						h = h | (128 << 8);
    						h = h | (128 << 16);
    						
                    		paint = new Color(h);
                    		super.draw(object, graphics, info);
                    		}
                    	},                    	
                trailsPortrayal, 100);
                */

            // note that the basic portrayal includes the TrailedPortrayal.  We'll add that to BOTH 
            // trails so it's sure to be selected even when moving.  The issue here is that MovablePortrayal2D
            // bypasses the selection mechanism, but then sends selection to just its own child portrayal.
            // but we need selection sent to both simple portrayals in in both field portrayals, even after
            // moving.  So we do this by simply having the TrailedPortrayal wrapped in both field portrayals.
            // It's okay because the TrailedPortrayal will only draw itself in the trailsPortrayal, which
            // we passed into its constructor.
                        
            flockersPortrayal.setPortrayalForAll(new MovablePortrayal2D(new sim.portrayal.simple.OvalPortrayal2D(Color.white)));
            //trailsPortrayal.setPortrayalForAll( basic );
            

            
        
         /*
        // update the size of the display appropriately.
        double w = flock.flockers.getWidth();
        double h = flock.flockers.getHeight();
        if (w == h)
            { display.insideDisplay.width = display.insideDisplay.height = 750; }
        else if (w > h)
            { display.insideDisplay.width = 750; display.insideDisplay.height = 750 * (h/w); }
        else if (w < h)
            { display.insideDisplay.height = 750; display.insideDisplay.width = 750 * (w/h); }
            */
        // reschedule the displayer
        display.reset();
                
        // redraw the display
        display.repaint();
        }

    public void init(Controller c)
        {
        super.init(c);
        
        

        // make the displayer
        display = new Display2D(750,750,this);
        display.setBackdrop(Color.black);


        displayFrame = display.createFrame();
        displayFrame.setTitle("Flockers");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
// uncomment this to try out trails  (also need to uncomment out some others in this file, look around)
       // display.attach( trailsPortrayal, "Trails" );
                
        display.attach( flockersPortrayal, "Behold the Flock!" );
        display.setClipping(false);
        }
        
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
        }
    }
