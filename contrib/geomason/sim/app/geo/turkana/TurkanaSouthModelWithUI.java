/**
 ** TurkanaSouthModelWithUI.java
 **
 ** Copyright 2011 by Andrew Crooks, Joey Harrison, Mark Coletti, and
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 ** $Id$
 **/
package turkana;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JFrame;
import org.jfree.data.xy.XYSeries;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.IntGrid2D;
import sim.portrayal.grid.FastValueGridPortrayal2D;
import sim.portrayal.grid.SparseGridPortrayal2D;
import sim.portrayal.simple.MovablePortrayal2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.util.gui.SimpleColorMap;
import sim.util.media.chart.*;

/*
 * TurkanaSouthModelWithUI
 * Main GUI class for the TurkanaSouth model.
 * 
 * Author: Joey Harrison
 * 
 */
public class TurkanaSouthModelWithUI extends GUIState
{
    public Display2D display;
    public JFrame displayFrame;
    FastValueGridPortrayal2D populationDensityPortrayal = new FastValueGridPortrayal2D("Population Density");
    FastValueGridPortrayal2D rainPortrayal = new FastValueGridPortrayal2D("Rain");
    FastValueGridPortrayal2D vegetationPortrayal = new FastValueGridPortrayal2D("Vegetation");
    SparseGridPortrayal2D agentPortrayal = new SparseGridPortrayal2D();
    JFrame populationStatsFrame;
    public TimeSeriesChartGenerator populationStatsChart = new TimeSeriesChartGenerator();
    private XYSeries populationSeries;
    TurkanaSouthModel model;


    public TurkanaSouthModelWithUI()
    {
        super(new TurkanaSouthModel(System.currentTimeMillis()));
        model = (TurkanaSouthModel) state;
    }

    public TurkanaSouthModelWithUI(SimState state)
    {
        super(state);
        model = (TurkanaSouthModel) state;
    }

    public static String getName()
    {
        return "Turkana South";
    }

    @Override
    public Object getSimulationInspectedObject()
    {
        return state;
    }  // non-volatile

    @Override
    public void load(SimState state)
    {
        super.load(state);
        setupPortrayals();
    }

    @Override
    public void start()
    {
        super.start();
        setupPortrayals();
        populationSeries.clear();	// clear the data for the chart
    }

    @SuppressWarnings("serial")
    public void setupPortrayals()
    {
        int maxValue = ((IntGrid2D)model.populationDensityGrid.getGrid()).max();
            
        populationDensityPortrayal.setField(model.populationDensityGrid.getGrid());
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

        rainPortrayal.setField(model.rainGrid);
        rainPortrayal.setMap(new SimpleColorMap(0, 1, Color.black, Color.white));

        vegetationPortrayal.setField(model.vegetationGrid);
        vegetationPortrayal.setMap(new SimpleColorMap(0, model.maxVegetationLevel, Color.black, Color.green));

        agentPortrayal.setField(model.agentGrid);
        agentPortrayal.setPortrayalForAll(new MovablePortrayal2D(new OvalPortrayal2D(Color.blue, 0.7)));

        this.scheduleRepeatingImmediatelyAfter(new Steppable() {
            @Override
            public void step(SimState state) {
                populationSeries.add(state.schedule.getTime() / model.ticksPerMonth, model.agents.size());
            }
        });

        populationStatsChart.repaint();
        display.reset();
        display.repaint();
    }

    @Override
    public void init(Controller c)
    {
        super.init(c);

        // since we're running the GUI, don't print stats
        model.printStats = false;

        display = new Display2D(model.windowWidth, model.windowHeight, this); // at 400x400, we've got 4x4 per array position
        displayFrame = display.createFrame();
        displayFrame.setTitle("Turkana South");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);

        display.attach(populationDensityPortrayal, "Pop. Density");
        display.attach(rainPortrayal, "Rain", false);
        display.attach(vegetationPortrayal, "Vegetation", true);
        display.attach(agentPortrayal, "Turkanians");
        display.setBackdrop(Color.black);

        c.registerFrame(createPopulationStatsFrame());
        //populationStatsFrame.setVisible(true);

    }

    public JFrame createPopulationStatsFrame()
    {
        populationSeries = new XYSeries("Population");
        populationStatsChart = new TimeSeriesChartGenerator();
        populationStatsChart.setTitle("Population Statistics");
        ((TimeSeriesAttributes)(populationStatsChart.addSeries(populationSeries, null))).setStrokeColor(Color.blue);
        populationStatsChart.setXAxisLabel("Time in months");
        populationStatsChart.setYAxisLabel("Population");

        populationStatsFrame = populationStatsChart.createFrame(this);
        populationStatsFrame.getContentPane().setLayout(new BorderLayout());
        populationStatsFrame.getContentPane().add(populationStatsChart, BorderLayout.CENTER);
        populationStatsFrame.pack();

        return populationStatsFrame;
    }

    public void quit()
    {
        super.quit();

        if (populationStatsFrame != null)
        {
            populationStatsFrame.dispose();
        }
        populationStatsFrame = null;
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        new TurkanaSouthModelWithUI().createController();
    }

}
