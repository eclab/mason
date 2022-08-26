package sim.app.dkeepaway.display;




import java.awt.Color;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.JFrame;

import sim.app.dkeepaway.DBall;
import sim.app.dkeepaway.DBot;
import sim.app.dkeepaway.DKeepaway;
import sim.app.dvirus.DAgent;
import sim.app.dvirus.display.DAgentPortrayal;
import sim.app.dvirus.display.DVirusInfectionDemoProxy;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.Portrayal;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.simple.MovablePortrayal2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.util.Double2D;

public class DKeepawayProxyWithUI  extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;

    ContinuousPortrayal2D entityPortrayal = new ContinuousPortrayal2D();

    public static void main(String[] args)
        {
        new DKeepawayProxyWithUI().createController();
        }
            
    public Object getSimulationInspectedObject()
        {
        return state;  // non-volatile
        }
            
    public DKeepawayProxyWithUI() { super(new DKeepawayProxy(System.currentTimeMillis())); }
            
    public DKeepawayProxyWithUI(SimState state) { super(state); }
            
    public static String getName() { return "Keep-Away Soccer"; }
            
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
                
        DKeepawayProxy dkeep = (DKeepawayProxy)state;

                
        // tell the portrayals what to portray and how to portray them
        entityPortrayal.setField(dkeep.fieldEnvironmentGrid);
        //entityPortrayal.setPortrayalForClass(DBot.class, new sim.portrayal.simple.OvalPortrayal2D(Color.red));
        //entityPortrayal.setPortrayalForClass(DBall.class, new sim.portrayal.simple.OvalPortrayal2D(Color.white));
                            
        // reschedule the displayer
        display.reset();
                        
        // redraw the display
        display.repaint();
        }
            
    public void init(Controller c)
        {
        super.init(c);
                
        // Make the Display2D.  We'll have it display stuff later.
        display = new Display2D(400,400,this); // at 400x400, we've got 4x4 per array position
        displayFrame = display.createFrame();
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);

        // attach the portrayals
        display.attach(entityPortrayal,"Bots and Balls");

        // specify the backdrop color  -- what gets painted behind the displays
        display.setBackdrop(new Color(0,80,0));  // a dark green
        }
            
            
    public boolean step() {
                
        DKeepawayProxy dkeep = (DKeepawayProxy)state;

                
             
                
        for (int i=0; i<dkeep.fieldEnvironmentGrid.getAllObjects().size(); i++) {
                        
                
            Object o = dkeep.fieldEnvironmentGrid.getAllObjects().getValue(i);      

            if (this.entityPortrayal.portrayals == null || !this.entityPortrayal.portrayals.containsKey(o)){
                                
                System.out.println(o.getClass());
                //System.exit(-1);
                                
                if (o.getClass()  == DBall.class) {
                                        
                    this.entityPortrayal.setPortrayalForObject(o, new sim.portrayal.simple.OvalPortrayal2D(Color.white));

                    }
                                
                else {
                                
                    this.entityPortrayal.setPortrayalForObject(o, new DBotPortrayal((DBot)o));
                    }

                //MovablePortrayal2D temp = new MovablePortrayal2D(new sim.portrayal.simple.OvalPortrayal2D(Color.green, 10.0));            
                //vidPortrayal.setPortrayalForObject(o, temp);
                }
                        
            }
                
                
                
        return super.step();
        }
                
    public void quit()
        {
        super.quit();
                
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;  // let gc
        display = null;       // let gc
        }

        
    }
