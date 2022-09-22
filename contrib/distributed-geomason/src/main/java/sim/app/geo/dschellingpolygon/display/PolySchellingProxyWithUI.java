package sim.app.geo.dschellingpolygon.display;

import java.awt.Color;
import java.awt.Graphics2D;

import javax.swing.JFrame;

import sim.app.geo.dschellingpolygon.DPolygon;
import sim.app.geo.schellingpolygon.Polygon;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.portrayal.simple.MovablePortrayal2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.util.media.chart.HistogramGenerator;
import sim.util.media.chart.TimeSeriesChartGenerator;

public class PolySchellingProxyWithUI extends GUIState{

    Display2D display;
    JFrame displayFrame;
    // portrayal info
    GeomVectorFieldPortrayal polyPortrayal = new GeomVectorFieldPortrayal();
    
    // chart info removed, not tested in distributed

    double[] peoplesMoves;



    /** constructor function */
    protected PolySchellingProxyWithUI(SimState state)
    {
        super(state);
    }



    /** constructor function */
    public PolySchellingProxyWithUI()
    {
        super(new PolySchellingProxy(System.currentTimeMillis()));
    }



    /** return the name of the simulation */
    public static String getName()
    {
        return "PolySchelling";
    }



    /** initialize the simulation */
    public void init(Controller controller)
    {
        super.init(controller);

        display = new Display2D(800, 600, this);

        display.attach(polyPortrayal, "Polys");

        displayFrame = display.createFrame();
        controller.registerFrame(displayFrame);
        displayFrame.setVisible(true);


    }



    /** quit the simulation, cleaning up after itself*/
    public void quit()
    {
        super.quit();

        if (displayFrame != null)
        {
            displayFrame.dispose();
        }
        displayFrame = null;
        display = null;
    }



    /** start the simulation, setting up the portrayals and charts for a new run */
    public void start()
    {
        super.start();
        setupPortrayals();
    }



    /**
     * Sets up the portrayals and charts for the simulation
     */
    private void setupPortrayals()
    {
        PolySchellingProxy world = (PolySchellingProxy) state;

        // reset the chart info
 
        // the polygon portrayal
        polyPortrayal.setField(world.poly);
        //polyPortrayal.setPortrayalForAll(new PolyPortrayal());
        //polyPortrayal.setPortrayalForAll(new GeomPortrayal(Color.GREEN,true));
        
        int count = 0;
        for (Object o : world.poly.getGeometries()) {
        	
        	System.out.println(count++);
        	
        	polyPortrayal.setPortrayalForObject(o, new GeomPortrayal(Color.BLUE,true));

        	
        //for (Object o : this.polyPortrayal.portrayals.keySet()) {	
            
        	 	

        	//if (this.polyPortrayal.portrayals == null || !this.polyPortrayal.portrayals.containsKey(o)){
        		
        		//System.out.println(o.getClass());
        		//System.exit(-1);
        		
        		
        		
        		//polyPortrayal.setPortrayalForObject(o, new PolyPortrayal((DPolygon)o));
        		//polyPortrayal.setPortrayalForObject(o, new GeomPortrayal(Color.BLUE,true));

        		
        	    //MovablePortrayal2D temp = new MovablePortrayal2D(new sim.portrayal.simple.OvalPortrayal2D(Color.green, 10.0));    	
        	    //vidPortrayal.setPortrayalForObject(o, temp);
        	//}
        	
        }
        
        

        display.reset();

        display.repaint();
    }


    public boolean step() {
    	
        PolySchellingProxy world = (PolySchellingProxy) state;
        

        
        for (Object o : world.poly.getGeometries()) {
        	
        	System.out.println(o);
        	
        	polyPortrayal.setPortrayalForObject(o, new GeomPortrayal(Color.BLUE,true));
        }


        
        /*

        System.out.println(this.polyPortrayal.portrayals);

        if (this.polyPortrayal.portrayals != null) {
        	
        System.out.println(this.polyPortrayal.portrayals.keySet().size());
        for (Object o : this.polyPortrayal.portrayals.keySet()) {	
            
    	 	

        	//if (this.polyPortrayal.portrayals == null || !this.polyPortrayal.portrayals.containsKey(o)){
        		
        		System.out.println(o.getClass());
        		//System.exit(-1);
        		
        		
        		
        		//polyPortrayal.setPortrayalForObject(o, new PolyPortrayal((DPolygon)o));
        		polyPortrayal.setPortrayalForObject(o, new GeomPortrayal(Color.BLUE,true));

        		
        	    //MovablePortrayal2D temp = new MovablePortrayal2D(new sim.portrayal.simple.OvalPortrayal2D(Color.green, 10.0));    	
        	    //vidPortrayal.setPortrayalForObject(o, temp);
        	//}
        	
        } 
        
        
          } */
        
  
        
    	
    	
    	return super.step();
    }



    public static void main(String[] args)
    {
        PolySchellingProxyWithUI worldGUI = new PolySchellingProxyWithUI();
        Console console = new Console(worldGUI);
        console.setVisible(true);
    }



    /** The portrayal used to display Polygons with the appropriate color */
    class PolyPortrayal extends GeomPortrayal
    {

        private static final long serialVersionUID = 1L;
        DPolygon dpoly;

        public PolyPortrayal(DPolygon d) {
        	        	
        	super();
        	dpoly = d;

        }

        //THIS ISN"T BEING CALLED I THINK
        @Override
        public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
            //Polygon poly = (Polygon) object;
        	System.out.println("catfish");
        	System.exit(-1);

            if (dpoly.residents.isEmpty())
            {
                paint = Color.green;
                graphics.setColor(Color.green);

                
            } else if (dpoly.getSoc().equals("RED"))
            {
                paint = Color.red;
                graphics.setColor(Color.red);

            } else if (dpoly.getSoc().equals("BLUE"))
            {
                paint = Color.blue;
                graphics.setColor(Color.blue);

            } else
            {
                paint = Color.yellow;
                graphics.setColor(Color.yellow);

            }
            

            //Object object2 = poly.getMasonGeometry();
            
            super.draw(object, graphics, info);
        }

    }
	
}
