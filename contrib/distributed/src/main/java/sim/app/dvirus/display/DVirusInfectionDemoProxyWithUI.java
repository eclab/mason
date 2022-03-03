package sim.app.dvirus.display;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.JFrame;

import sim.app.dvirus.DAgent;
import sim.app.dvirus.DEvil;
import sim.app.dvirus.DGood;
import sim.app.dvirus.DVirusInfectionDemo;
import sim.app.virus.VirusInfectionDemo;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.Portrayal;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.simple.MovablePortrayal2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.util.Double2D;

public class DVirusInfectionDemoProxyWithUI extends GUIState
{
public Display2D display;
public JFrame displayFrame;

public static void main(String[] args)
    {
    new DVirusInfectionDemoProxyWithUI().createController();  // randomizes by currentTimeMillis
    }

public Object getSimulationInspectedObject()
	{
	return state;  // non-volatile
	}

ContinuousPortrayal2D vidPortrayal = new ContinuousPortrayal2D();
    
//uncomment this to try out trails  (also need to uncomment out some others in this file, look around)
//ContinuousPortrayal2D trailsPortrayal = new ContinuousPortrayal2D(); 

public DVirusInfectionDemoProxyWithUI()
    {
    super(new DVirusInfectionDemoProxy(System.currentTimeMillis()));
    }

public DVirusInfectionDemoProxyWithUI(SimState state) 
    {
    super(state);
    }

public static String getName()
	{
	return "DVirus Proxy";
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
	    DVirusInfectionDemoProxy dvirusinf = (DVirusInfectionDemoProxy)state;

	    vidPortrayal.setField(dvirusinf.envgrid);
	    

	    
	    
	    //agents show up here, but not if individual agents
	    //vidPortrayal.setPortrayalForAll(new MovablePortrayal2D(new sim.portrayal.simple.OvalPortrayal2D(Color.green, 10.0)));
	    

	    
        display.reset();
        display.setBackdrop(Color.white);
                
        // redraw the display
        display.repaint();
        
    }

public void init(Controller c)
    {
    super.init(c);

    // make the displayer
    display = new Display2D(800,600,this);

    displayFrame = display.createFrame();
    displayFrame.setTitle("Virus (Dis)Infection Demonstration Display");
    c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
    displayFrame.setVisible(true);
    display.attach( vidPortrayal, "Agents" );
    }

public boolean step() {
	
    DVirusInfectionDemoProxy dvirusinf = (DVirusInfectionDemoProxy)state;

    
 
    
    for (int i=0; i<dvirusinf.envgrid.getAllObjects().size(); i++) {
    	
    
    	Object o = dvirusinf.envgrid.getAllObjects().getValue(i);   	

    	if (this.vidPortrayal.portrayals == null || !this.vidPortrayal.portrayals.containsKey(o)){
    		
    		System.out.println(o.getClass());
    		//System.exit(-1);
    		
    		
    		
    	    vidPortrayal.setPortrayalForObject(o, new DAgentPortrayal((DAgent)o));

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
    displayFrame = null;
    display = null;
    }
}
