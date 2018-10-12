/**
 ** WaterWorldWithUI.java
 **
 ** Copyright 2011 by Sarah Wise, Mark Coletti, Andrew Crooks, and
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 ** $Id$
 **/
package sim.app.geo.waterworld;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JFrame;
import org.jfree.data.xy.XYSeries;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.grid.ObjectGridPortrayal2D;
import sim.portrayal.simple.RectanglePortrayal2D;
import sim.util.gui.ColorMap;
import sim.util.media.chart.TimeSeriesChartGenerator;



/**
 * the GUIState that visualizes the simulation defined in WaterWorld.java
 *
 */
public class WaterWorldWithUI extends GUIState
{

    WaterWorld waterworld;
    public Display2D display;
    public JFrame displayFrame;
    // portrayal data
    ObjectGridPortrayal2D ground = new ObjectGridPortrayal2D();
    ObjectGridPortrayal2D water = new ObjectGridPortrayal2D();
    // chart information
    TimeSeriesChartGenerator raindropChart;
    XYSeries numRaindrops;



    /**
     * Constructor
     * @param state
     */
    protected WaterWorldWithUI(SimState state)
    {
        super(state);
        waterworld = (WaterWorld) state;
    }



    /**
     * Main function
     * @param args
     */
    public static void main(String[] args)
    {
        WaterWorldWithUI simple = new WaterWorldWithUI(new WaterWorld(System.currentTimeMillis()));
        Console c = new Console(simple);
        c.setVisible(true);
    }



    /**
     * @return name of the simulation
     */
    public static String getName()
    {
        return "WaterWorld";
    }



    /**
     * Called when starting a new run of the simulation. Sets up the portrayals
     * and chart data.
     */
    public void start()
    {
        super.start();

        // set up the chart info
        numRaindrops = new XYSeries("Raindrops");
        raindropChart.removeAllSeries();
        raindropChart.addSeries(numRaindrops, null);

        // schedule the chart to take data
        state.schedule.scheduleRepeating(new Steppable()
        {

            public void step(SimState state)
            {
                numRaindrops.add(state.schedule.getTime(),
                                 ((WaterWorld) state).drops.size(),
                                 true);
            }

        });

        // set up the portrayals
        ground.setField(waterworld.landscape);
        ground.setPortrayalForAll(new GroundPortrayal());

        water.setField(waterworld.landscape);
        water.setPortrayalForAll(new WaterPortrayal());

        // reschedule the displayer
        display.reset();
        display.setBackdrop(new Color(250, 246, 237));

        // redraw the display
        display.repaint();
    }



    /**
     * Called when first beginning a WaterWorldWithUI. Sets up the display window,
     * the JFrames, and the chart structure.
     */
    public void init(Controller c)
    {
        super.init(c);

        // make the displayer
        display = new Display2D(600, 600, this);
        // turn off clipping
        display.setClipping(false);

        displayFrame = display.createFrame();
        displayFrame.setTitle("WaterWorld Display");
        c.registerFrame(displayFrame); // register the frame so it appears in
        // the "Display" list
        displayFrame.setVisible(true);

        display.attach(ground, "Ground");
        display.attach(water, "Water");

        // chart!
        raindropChart = new TimeSeriesChartGenerator();
        raindropChart.setTitle("Number of Raindrops in Simulation");
        raindropChart.setYAxisLabel("Number of Raindrops");
        raindropChart.setXAxisLabel("Time");
        JFrame chartFrame = raindropChart.createFrame(this);
        chartFrame.setVisible(true);
        chartFrame.pack();
        c.registerFrame(chartFrame);

    }



    /**
     * called when quitting a simulation. Does appropriate garbage collection.
     */
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

    /////////////////////
    // PORTRAYALS
    /////////////////////
    // colormap for ground height, which is assumed to be between 0 and 300.
    ColorMap elevation = new sim.util.gui.SimpleColorMap(
        0, 300, new Color(250, 246, 237), new Color(53, 44, 36));

    // elevation-based portrayal


    class GroundPortrayal extends RectanglePortrayal2D
    {

        public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
            Rectangle2D.Double draw = info.draw;
            final double width = draw.width * scale;
            final double height = draw.height * scale;

            final int x = (int) (draw.x - width / 2.0);
            final int y = (int) (draw.y - height / 2.0);
            final int w = (int) (width);
            final int h = (int) (height);

            Basin b = (Basin) object;
            graphics.setColor(elevation.getColor(b.baseheight));

            graphics.fillRect(x, y, w, h);
        }

    }
    // colormap for water depth
    ColorMap depth = new sim.util.gui.SimpleColorMap(
        0, 10, new Color(70, 100, 200, 0), new Color(70, 100, 200, 255));

    // water depth-based portrayal


    class WaterPortrayal extends RectanglePortrayal2D
    {

        public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
            Rectangle2D.Double draw = info.draw;
            final double width = draw.width * scale;
            final double height = draw.height * scale;

            final int x = (int) (draw.x - width / 2.0);
            final int y = (int) (draw.y - height / 2.0);
            final int w = (int) (width);
            final int h = (int) (height);

            Basin b = (Basin) object;
            graphics.setColor(depth.getColor(b.drops.size()));

            graphics.fillRect(x, y, w, h);
        }

    }
}
