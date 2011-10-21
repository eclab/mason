//***************************************************************
//Copyright 2011 Center for Social Complexity, GMU
//
//Author: Andrew Crooks and Sarah Wise, GMU
//
//Contact: acrooks2@gmu.edu & swise5@gmu.edu
//
//
//sleuth is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//It is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//
//***************************************************************
package sim.app.geo.sleuth;

import java.awt.Color;

import javax.swing.JFrame;

import org.jfree.data.xy.XYSeries;

import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.portrayal.grid.ObjectGridPortrayal2D;
import sim.util.gui.ColorMap;
import sim.util.media.chart.TimeSeriesChartGenerator;



/**
 * the GUIState that visualizes the simulation defined in SleuthWorld.java
 *
 */
public class SleuthWorldWithUI extends GUIState
{

    SleuthWorld sleuthworld;
    private Display2D display;
    private JFrame displayFrame;
    
    // portrayal data
    ObjectGridPortrayal2D slope = new ObjectGridPortrayal2D();
    ObjectGridPortrayal2D landuse = new ObjectGridPortrayal2D();
    ObjectGridPortrayal2D excluded = new ObjectGridPortrayal2D();
    ObjectGridPortrayal2D urban = new ObjectGridPortrayal2D();
    ObjectGridPortrayal2D urbanOverTime = new ObjectGridPortrayal2D();
    ObjectGridPortrayal2D transport = new ObjectGridPortrayal2D();
    ObjectGridPortrayal2D hillshade = new ObjectGridPortrayal2D();
    
    // chart information
    TimeSeriesChartGenerator urbanChart;
    XYSeries numUrban;

    // This must be included to have model tab, which allows mid-simulation
    // modification of the coefficients


    public Object getSimulationInspectedObject()
    {
        return state;
    }  // non-volatile



    /**
     * Constructor
     * @param state
     */
    protected SleuthWorldWithUI(SimState state)
    {
        super(state);
        sleuthworld = (SleuthWorld) state;
    }



    /**
     * Main function
     * @param args
     */
    public static void main(String[] args)
    {
        SleuthWorldWithUI simple = new SleuthWorldWithUI(new SleuthWorld(System.currentTimeMillis()));
        Console c = new Console(simple);
        c.setVisible(true);
    }



    /**
     * @return name of the simulation
     */
    public static String getName()
    {
        return "SleuthWorld";
    }



    /**
     * Called when starting a new run of the simulation. Sets up the portrayals
     * and chart data.
     */
    public void start()
    {
        super.start();

        // set up the chart info
        numUrban = new XYSeries("Number of Urban Tiles");
        urbanChart.removeAllSeries();
        urbanChart.addSeries(numUrban, null);

        // schedule the chart to take data
        state.schedule.scheduleRepeating(new Steppable()
        {

            public void step(SimState state)
            {
                SleuthWorld sw = (SleuthWorld) state;
                numUrban.add(state.schedule.time(), sw.numUrban
                    / (double) (sw.numUrban + sw.numNonUrban));
            }

        });
        
        // set up the portrayals
        slope.setField(sleuthworld.landscape);
        slope.setPortrayalForAll(new SlopePortrayal());

        landuse.setField(sleuthworld.landscape);
        landuse.setPortrayalForAll(new LandusePortrayal());

        excluded.setField(sleuthworld.landscape);
        excluded.setPortrayalForAll(new ExcludedPortrayal());

        urban.setField(sleuthworld.landscape);
        urban.setPortrayalForAll(new OriginalUrbanPortrayal());

        urbanOverTime.setField(sleuthworld.landscape);
        urbanOverTime.setPortrayalForAll(new GrowingUrbanZonesPortrayal());

        transport.setField(sleuthworld.landscape);
        transport.setPortrayalForAll(new TransportPortrayal());

        hillshade.setField(sleuthworld.landscape);
        hillshade.setPortrayalForAll(new HillshadePortrayal());

        // reschedule the displayer
        display.reset();
        display.setBackdrop(Color.white);

        // redraw the display
        display.repaint();
    }



    /**
     * Called when first beginning a SleuthWorldWithUI. Sets up the display window,
     * the JFrames, and the chart structure.
     */
    @Override
    public void init(Controller c)
    {
        super.init(c);

        // make the displayer
        display = new Display2D(600, 600, this);
        // turn off clipping
        display.setClipping(false);

        displayFrame = display.createFrame();
        displayFrame.setTitle("SleuthWorld Display");
        c.registerFrame(displayFrame); // register the frame so it appears in
        // the "Display" list
        displayFrame.setVisible(true);

        display.attach(slope, "Slope");
        display.attach(landuse, "Landuse");
        display.attach(excluded, "Excluded");
        display.attach(urbanOverTime, "Current Urban");
        display.attach(urban, "Initial Urban");
        display.attach(transport, "Transport");
        display.attach(hillshade, "Hillshade");

        // chart!
        urbanChart = new TimeSeriesChartGenerator();
        urbanChart.setTitle("Percent of Urban Tiles in Simulation");
        urbanChart.setYAxisLabel("Percent of Urban Tiles");
        urbanChart.setXAxisLabel("Time");

        JFrame chartFrame = urbanChart.createFrame(this);
        chartFrame.setVisible(true);
        chartFrame.pack();
        c.registerFrame(chartFrame);

    }



    /** called when quitting a simulation. Does appropriate garbage collection. */
    public void quit()
    {
        super.quit();

        if (displayFrame != null)
        {
            displayFrame.dispose();
        }
        displayFrame = null; // let gc
        display = null; // let gc
    }

    // COLORMAPS FOR PORTRAYALS
    // colormap for slope, which is assumed to be between 0 and 255.
    private static ColorMap slopeColor = new sim.util.gui.SimpleColorMap(
        0, 100, new Color(250, 250, 250), new Color(0, 0, 0));
    private static ColorMap hillshadeColor = new sim.util.gui.SimpleColorMap(
        0, 255, new Color(250, 250, 250, 100), new Color(0, 0, 0, 100));



    public static ColorMap getHillshadeColor()
    {
        return hillshadeColor;
    }



    public static ColorMap getSlopeColor()
    {
        return slopeColor;
    }


}