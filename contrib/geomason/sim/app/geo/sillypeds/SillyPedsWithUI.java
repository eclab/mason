/**
 ** SillyPedsWithUI.java
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
package sillypeds;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import javax.swing.JFrame;
import org.jfree.data.xy.XYSeries;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.portrayal.DrawInfo2D;
//import sim.portrayal.grid.ObjectGridPortrayal2D;
import sim.portrayal.grid.FastObjectGridPortrayal2D;
import sim.portrayal.simple.RectanglePortrayal2D;
import sim.util.gui.ColorMap;
import sim.util.media.chart.TimeSeriesChartGenerator;



/**
 * the GUIState that visualizes the simulation defined in WaterWorld.java
 *
 */
public class SillyPedsWithUI extends GUIState
{

    SillyPeds pedworld;
    public ArrayList<Display2D> displays = new ArrayList<Display2D>();
    ArrayList<JFrame> displayFrames = new ArrayList<JFrame>();
    int numFrames = 1; // set when SillyPeds instance is read in
    // portrayal data
    ArrayList<FastObjectGridPortrayal2D> traces = new ArrayList<FastObjectGridPortrayal2D>();
    ArrayList<FastObjectGridPortrayal2D> floors = new ArrayList<FastObjectGridPortrayal2D>();
    ArrayList<FastObjectGridPortrayal2D> people = new ArrayList<FastObjectGridPortrayal2D>();
    // chart information
    TimeSeriesChartGenerator pedestrianChart;
    XYSeries numPedestrians;



    /**
     * Constructor
     * @param state
     */
    protected SillyPedsWithUI(SimState state)
    {
        super(state);
        pedworld = (SillyPeds) state;
        numFrames = 1;
    }



    /**
     * Main function
     * @param args
     */
    public static void main(String[] args)
    {
        SillyPedsWithUI simple = new SillyPedsWithUI(new SillyPeds(System.currentTimeMillis()));
        Console c = new Console(simple);
        c.setVisible(true);
    }



    /**
     * @return name of the simulation
     */
    public static String getName()
    {
        return "SillyPedestrians";
    }



    /**
     * Called when starting a new run of the simulation. Sets up the portrayals
     * and chart data.
     */
    public void start()
    {
        super.start();

        // set up the chart info
        numPedestrians = new XYSeries("Pedestrians");
        pedestrianChart.removeAllSeries();
        pedestrianChart.addSeries(numPedestrians, null);

        // schedule the chart to take data
        state.schedule.scheduleRepeating(new Steppable()
        {

            public void step(SimState state)
            {
                numPedestrians.add(state.schedule.getTime(),
                                   ((SillyPeds) state).peds.size(),
                                   true);
            }

        });

        // set up the portrayals
        for (int i = 0; i < displayFrames.size(); i++)
        {
            FastObjectGridPortrayal2D floor = floors.get(i);
            floor.setField(pedworld.landscape.get(i).field);
            //floor.setPortrayalForAll(new FloorPortrayal());

            FastObjectGridPortrayal2D trace = traces.get(i);
            trace.setField(pedworld.landscape.get(i).field);
            //trace.setPortrayalForAll(new TracePortrayal());

            FastObjectGridPortrayal2D peeps = people.get(i);
            peeps.setField(pedworld.landscape.get(i).field);
            //peeps.setPortrayalForAll(new PedPortrayal());
        }

        // reschedule the displayers
        for (Display2D d : displays)
        {
            d.reset();
            d.setBackdrop(backdropColor);

            d.repaint();
        }
    }



    /**
     * Called when first beginning a SillyPedsWithUI. Sets up the display windows,
     * the JFrames, and the chart structure.
     */
    public void init(Controller c)
    {
        super.init(c);

        // dipslay windows and JFrames
        for (int i = 0; i < numFrames; i++)
        {

            // make a displayer
            Display2D display = new Display2D(600, 600, this);
            // turn off clipping
            display.setClipping(false);
            displays.add(display);

            JFrame frame = display.createFrame();
            frame.setTitle("SillyPeds Display " + (i + 1));
            c.registerFrame(frame);
            frame.setVisible(true);
            displayFrames.add(frame);

            // do the attaching here
            FastObjectGridPortrayal2D newfloor = new FastObjectGridPortrayal2D()
            	{
            	public double doubleValue(Object object)
            		{
            		return ((Tile)object).baseheight;
            		}
            	};
            newfloor.setMap(gradientColor);
            	
            floors.add(i, newfloor);
            display.attach(newfloor, "Floor " + (i + 1));

            FastObjectGridPortrayal2D newtrace = new FastObjectGridPortrayal2D()
            	{
            	public double doubleValue(Object object)
            		{
					// Seriously, who writes Math.pow(foo, 2.0) ?  See TracePortrayal below, gagh...
	            	//return Math.pow(((Tile)object).trace, 2.0);
	            	double trace = ((Tile)object).trace;
	            	if (trace < 0) return trace;
	            	else return trace * trace;
	            	}
            	};
            newtrace.setMap(traceColor);
            	
            traces.add(i, newtrace);
            display.attach(newtrace, "Trace " + (i + 1));

            FastObjectGridPortrayal2D newpeople = new FastObjectGridPortrayal2D()
            	{
            	public double doubleValue(Object object)
            		{
            		return (((Tile)object).peds.size() > 0) ? 1.0 : 0.0;
            		}
            	};
            newpeople.setMap(pedColor);
            
            people.add(i, newpeople);
            display.attach(newpeople, "People " + (i + 1));
        }

        // chart!
        pedestrianChart = new TimeSeriesChartGenerator();
        pedestrianChart.setTitle("Number of Pedestrians in Simulation");
        pedestrianChart.setYAxisLabel("Number of Pedestrians");
        pedestrianChart.setXAxisLabel("Time");
        JFrame chartFrame = pedestrianChart.createFrame(this);
        //	chartFrame.setVisible(true);
        chartFrame.pack();
        c.registerFrame(chartFrame);

    }



    /**
     * called when quitting a simulation. Does appropriate garbage collection.
     */
    public void quit()
    {
        super.quit();

        for (JFrame frame : displayFrames)
        {
            if (frame != null)
            {
                frame.dispose();
            }
            frame = null;
        }

        for (Display2D display : displays)
        {
            display = null; // let gc
        }
    }

    /////////////////////
    // PORTRAYALS
    /////////////////////
    // colormap for ground height, which is assumed to be between 0 and 300.
    ColorMap gradientColor = new sim.util.gui.SimpleColorMap(0, 2000, new Color(237, 246, 250), new Color(36, 44, 53))
    	{
    	public Color getColor(double level)
    		{
    		if (level == Double.MAX_VALUE)
    			{
    			return offwhite;
    			}
    		else return super.getColor(level);
    		}
    	
    	public int getRGB(double level)
    		{
    		if (level == Double.MAX_VALUE)
    			{
    			return offwhite.getRGB();
    			}
    		else return super.getRGB(level);
    		}
    	
    	public int getAlpha(double level)
    		{
    		if (level == Double.MAX_VALUE)
    			{
    			return offwhite.getAlpha();
    			}
    		else return super.getAlpha(level);
    		}
    	};
    
    static final Color EMPTY = new Color(0,0,0,0);
 //   ColorMap traceColor = new sim.util.gui.SimpleColorMap(0, 2000, new Color(250, 250, 0, 50), new Color(250, 250, 0, 255));
   ColorMap traceColor = new sim.util.gui.SimpleColorMap(0, 2000, new Color(250, 250, 0, 50), new Color(250, 250, 0, 255))
   	{
    	public Color getColor(double level)
    		{
    		if (level >= 0)
    			return super.getColor(level);
    		else return EMPTY;
    		}
    	
    	public int getRGB(double level)
    		{
    		if (level >= 0)
    			return super.getRGB(level);
    		else return EMPTY.getRGB();
    		}
    		
    	public int getAlpha(double level)
    		{
    		if (level >= 0)
    			return super.getAlpha(level);
    		else return EMPTY.getAlpha();
    		}
   	};
    
    ColorMap pedColor = new sim.util.gui.SimpleColorMap(new Color[]{EMPTY, Color.red});
    	
    Color offwhite = new Color(253, 253, 253);
    Color backdropColor = new Color(237, 246, 250);

    
    /*
    
    // elevation-based portrayal
    class FloorPortrayal extends RectanglePortrayal2D
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

            Tile b = (Tile) object;
            graphics.setColor(gradientColor.getColor(b.baseheight));
            if (b.baseheight == Double.MAX_VALUE)
            {
                graphics.setColor(offwhite);
            }
            graphics.fillRect(x, y, w, h);
        }

    }

    // water depth-based portrayal
    class PedPortrayal extends RectanglePortrayal2D
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

            Tile b = (Tile) object;
            if (b.peds.size() > 0)
            {
                graphics.setColor(Color.red);
                graphics.fillRect(x, y, w, h);
            }
        }

    }

    // portrayal of traffic over a given tile
    class TracePortrayal extends RectanglePortrayal2D
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

            Tile b = (Tile) object;
            if (b.trace >= 0)
            {
                graphics.setColor(traceColor.getColor(Math.pow(b.trace, 2)));
                graphics.fillRect(x, y, w, h);
            }
        }
    }
*/

}