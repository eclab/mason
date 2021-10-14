package sim.app.dpso.display;

import java.awt.Color;

import javax.swing.JFrame;

import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.simple.MovablePortrayal2D;

public class PSOProxyWithUI extends GUIState{


	    public Display2D display;
	    public JFrame displayFrame;

	    public static void main(String[] args)
	        {
	        new PSOProxyWithUI().createController();  // randomizes by currentTimeMillis
	        }

	    public Object getSimulationInspectedObject()
	    	{
	    	return state;  // non-volatile
	    	}

	    ContinuousPortrayal2D swarmPortrayal = new ContinuousPortrayal2D();
	        
	// uncomment this to try out trails  (also need to uncomment out some others in this file, look around)
	    //ContinuousPortrayal2D trailsPortrayal = new ContinuousPortrayal2D(); 
	    
	    public PSOProxyWithUI()
	        {
	        super(new PSOProxy(System.currentTimeMillis()));
	        }
	    
	    public PSOProxyWithUI(SimState state) 
	        {
	        super(state);
	        }

	    public static String getName()
	    	{
	    	return "PSO Proxy";
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
	    	
	    	    /*
	            PSOProxy swarm = (PSOProxy)state;
	            final SimpleColorMap map = new SimpleColorMap(
	                swarm.fitnessFunctionLowerBound[swarm.fitnessFunction], 1000, Color.blue, Color.red);
	            
	            
	            ArrayList<DParticle> swarmObjectList = swarm.space.getAllAgentsInStorage();
	            
	            swarmPortrayal.setField(swarm.space);
	            for(int x=0;x<swarmObjectList.size();x++)
	                {
	                final DParticle p = swarmObjectList.get(x);
	                swarmPortrayal.setPortrayalForObject(p,
	                    new RectanglePortrayal2D(Color.green,0.05)
	                        {
	                        public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
	                            {
	                            paint = map.getColor(p.getFitness());
	                            super.draw(object,graphics,info);
	                            }
	                        });
	                }
	                */
	    	
	    	    PSOProxy psoProx = (PSOProxy)state;

	    	    swarmPortrayal.setField(psoProx.space);
	    	    
	    	    swarmPortrayal.setPortrayalForAll(new MovablePortrayal2D(new sim.portrayal.simple.OvalPortrayal2D(Color.green,0.05)));

	            

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
        displayFrame.setTitle("Particle Swarm Optimization");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
// uncomment this to try out trails  (also need to uncomment out some others in this file, look around)
       // display.attach( trailsPortrayal, "Trails" );
                
        display.attach( swarmPortrayal, "Particles" );
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


