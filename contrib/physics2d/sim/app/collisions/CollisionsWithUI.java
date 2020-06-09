package sim.app.collisions;
import sim.engine.*;
import sim.display.*;
import sim.portrayal.continuous.*;
import java.awt.*;
import javax.swing.*;

public class CollisionsWithUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;
        
    ContinuousPortrayal2D entityPortrayal = new ContinuousPortrayal2D();
        
    public static void main(String[] args)
        {
        CollisionsWithUI simCollisions = new CollisionsWithUI();
        Console c = new Console(simCollisions);
        c.setVisible(true);
        }
    
    public CollisionsWithUI() 
        { 
        super(new Collisions(System.currentTimeMillis())); 
        }
        
    public CollisionsWithUI(SimState state) 
        { 
        super(state); 
        }
        
    public static String getName() 
        { 
        return "Collisions"; 
        }
        
    // set it up so we an inspect the model
    public Object getSimulationInspectedObject()
        {
        return state;
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
        
    public void setupPortrayals()
        {
        // tell the portrayals what to portray and how to portray them
        entityPortrayal.setField(((Collisions)state).fieldEnvironment);
                
        // reschedule the displayer
        display.reset();
                
        // redraw the display
        display.repaint();
        }
        
    public void init(Controller c)
        {
        super.init(c);
                
        // Make the Display2D.  We'll have it display stuff later.
        display = new Display2D(800,800,this); // at 400x400, we've got 4x4 per array position
        displayFrame = display.createFrame();
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
                
        // attach the portrayals
        display.attach(entityPortrayal,"Collisions");
                
        // specify the backdrop color  -- what gets painted behind the displays
        display.setBackdrop(new Color(0,80,0));  // a dark green
        }
        
    public void quit()
        {
        super.quit();
                
        if (displayFrame!=null) 
            displayFrame.dispose();
                        
        displayFrame = null;  // let gc
        display = null;       // let gc
        }
    }
    
    
    
    
    
