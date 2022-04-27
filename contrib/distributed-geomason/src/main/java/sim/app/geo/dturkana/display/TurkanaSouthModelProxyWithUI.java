package sim.app.geo.dturkana.display;

import java.awt.Color;

import javax.swing.JFrame;

import org.jfree.data.xy.XYSeries;

import sim.app.geo.dturkana.DTurkanaSouthModel;
import sim.app.geo.turkana.TurkanaSouthModel;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.IntGrid2D;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.grid.DenseGridPortrayal2D;
import sim.portrayal.grid.FastValueGridPortrayal2D;
import sim.portrayal.grid.SparseGridPortrayal2D;
import sim.portrayal.simple.MovablePortrayal2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.util.gui.SimpleColorMap;
import sim.util.media.chart.TimeSeriesChartGenerator;

public class TurkanaSouthModelProxyWithUI extends GUIState{


    public Display2D display;
    public JFrame displayFrame;

    FastValueGridPortrayal2D populationDensityPortrayal = new FastValueGridPortrayal2D("Population Density");
    FastValueGridPortrayal2D rainPortrayal = new FastValueGridPortrayal2D("Rain");
    FastValueGridPortrayal2D vegetationPortrayal = new FastValueGridPortrayal2D("Vegetation");
    DenseGridPortrayal2D agentPortrayal = new DenseGridPortrayal2D();
    //JFrame populationStatsFrame;
    //public TimeSeriesChartGenerator populationStatsChart = new TimeSeriesChartGenerator();
    //private XYSeries populationSeries;
    //DTurkanaSouthModel model;

    public static void main(String[] args)
        {
        new TurkanaSouthModelProxyWithUI().createController();  // randomizes by currentTimeMillis
        }

    public Object getSimulationInspectedObject()
    	{
    	return state;  // non-volatile
    	}


    
    public TurkanaSouthModelProxyWithUI()
        {
        super(new TurkanaSouthModelProxy(System.currentTimeMillis()));
        //model = (DTurkanaSouthModel) state;

        }
    
    public TurkanaSouthModelProxyWithUI(SimState state) 
        {
        super(state);
        }

    public static String getName()
    	{
    	return "Turkana Proxy";
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
        
    @SuppressWarnings("serial")
    public void setupPortrayals()
    {   
    	//((TurkanaSouthModelProxy)state)
        //int maxValue = ((DIntGrid2D)((TurkanaSouthModelProxy)state).populationdensgrid.getGrid()).max();
    	int maxValue = 10;
    	
        //populationDensityPortrayal.setField(model.populationDensityGrid.getGrid());
        populationDensityPortrayal.setMap(
            new SimpleColorMap(0, maxValue, Color.black, Color.white) {
                @Override
                public double filterLevel(double level) {
                    // since the population grid values are all very small except
                    // a few verge large values, scale the color map nonlinearly
                    // so the low values don't just appear black
                    return Math.sqrt(level);
                }
            });

        rainPortrayal.setField(((TurkanaSouthModelProxy)state).raingrid);
        rainPortrayal.setMap(new SimpleColorMap(0, 1, Color.black, Color.white));

        vegetationPortrayal.setField(((TurkanaSouthModelProxy)state).veggrid);
        vegetationPortrayal.setMap(new SimpleColorMap(0, 1, Color.black, Color.green));

        agentPortrayal.setField(((TurkanaSouthModelProxy)state).turkanians);
        agentPortrayal.setPortrayalForAll(new MovablePortrayal2D(new OvalPortrayal2D(Color.blue, 0.7)));

        //don't worry about charting for now
        /*
        this.scheduleRepeatingImmediatelyAfter(new Steppable() {
            @Override
            public void step(SimState state) {
                populationSeries.add(state.schedule.getTime() / model.ticksPerMonth, model.agents.size());
            }
        });
        */

        //populationStatsChart.repaint();
        display.reset();
        display.repaint();
    }

    @Override
    public void init(Controller c)
    {
        super.init(c);

        // since we're running the GUI, don't print stats
        //model.printStats = false;

        display = new Display2D(400, 400, this); // at 400x400, we've got 4x4 per array position
        displayFrame = display.createFrame();
        displayFrame.setTitle("Turkana South");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);

        display.attach(populationDensityPortrayal, "Pop. Density");
        display.attach(rainPortrayal, "Rain", false);
        display.attach(vegetationPortrayal, "Vegetation", true);
        display.attach(agentPortrayal, "Turkanians");
        display.setBackdrop(Color.black);

        //c.registerFrame(createPopulationStatsFrame());
        //populationStatsFrame.setVisible(true);

    }
        
    public void quit()
        {
        super.quit();
        
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
        }	
	
}
