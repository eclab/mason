//***************************************************************
//Copyright 2011 Center for Social Complexity, GMU
//
//Author: Andrew Crooks and Sarah Wise, GMU
//
//Contact: acrooks2@gmu.edu & swise5@gmu.edu
//
//
//Gridlock is free software: you can redistribute it and/or modify
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
package sim.app.geo.gridlock;

import java.awt.Color;

import javax.swing.JFrame;

import org.jfree.data.xy.XYSeries;


import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.util.media.chart.TimeSeriesChartGenerator;



public class GridlockWithUI extends GUIState
{

    public Display2D display;
    public JFrame displayFrame;
    private GeomVectorFieldPortrayal roadsPortrayal = new GeomVectorFieldPortrayal(true);
    private GeomVectorFieldPortrayal tractsPortrayal = new GeomVectorFieldPortrayal(true);
    private GeomVectorFieldPortrayal agentPortrayal = new GeomVectorFieldPortrayal();
    TimeSeriesChartGenerator trafficChart;
    XYSeries maxSpeed;
    XYSeries avgSpeed;
    XYSeries minSpeed;



    protected GridlockWithUI(SimState state)
    {
        super(state);
    }



    /**
     * Main function
     * @param args
     */
    public static void main(String[] args)
    {
        GridlockWithUI simple = new GridlockWithUI(new Gridlock(System.currentTimeMillis()));
        Console c = new Console(simple);
        c.setVisible(true);
    }



    /**
     * @return name of the simulation
     */
    public static String getName()
    {
        return "Gridlock";
    }



    /**
     *  This must be included to have model tab, which allows mid-simulation
     *  modification of the coefficients
     */
    public Object getSimulationInspectedObject()
    {
        return state;
    }  // non-volatile



    /**
     * Called when starting a new run of the simulation. Sets up the portrayals
     * and chart data.
     */
    public void start()
    {
        super.start();

        Gridlock world = (Gridlock) state;

        maxSpeed = new XYSeries("Max Speed");
        avgSpeed = new XYSeries("Average Speed");
        minSpeed = new XYSeries("Min Speed");
        trafficChart.removeAllSeries();
        trafficChart.addSeries(maxSpeed, null);
        trafficChart.addSeries(avgSpeed, null);
        trafficChart.addSeries(minSpeed, null);

        state.schedule.scheduleRepeating(new Steppable()
        {

            public void step(SimState state)
            {
                Gridlock world = (Gridlock) state;
                double maxS = 0, minS = 10000, avgS = 0, count = 0;
                for (Agent a : world.agentList)
                {
                    if (a.reachedDestination)
                    {
                        continue;
                    }
                    count++;
                    double speed = Math.abs(a.speed);
                    avgS += speed;
                    if (speed > maxS)
                    {
                        maxS = speed;
                    }
                    if (speed < minS)
                    {
                        minS = speed;
                    }
                }
                double time = state.schedule.time();
                avgS /= count;
                maxSpeed.add(time, maxS, true);
                minSpeed.add(time, minS, true);
                avgSpeed.add(time, avgS, true);
            }

        });

        roadsPortrayal.setField(world.roads);
//        roadsPortrayal.setPortrayalForAll(new RoadPortrayal());//GeomPortrayal(Color.DARK_GRAY,0.001,false));
        roadsPortrayal.setPortrayalForAll(new GeomPortrayal(Color.DARK_GRAY, 0.001, false));

        tractsPortrayal.setField(world.censusTracts);
//        tractsPortrayal.setPortrayalForAll(new PolyPortrayal());//(Color.GREEN,true));
        tractsPortrayal.setPortrayalForAll(new GeomPortrayal(Color.GREEN, true));

        agentPortrayal.setField(world.agents);
        agentPortrayal.setPortrayalForAll(new GeomPortrayal(Color.RED, 0.01, true));

        display.reset();
        display.setBackdrop(Color.WHITE);

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
        display = new Display2D(1300, 600, this);
        // turn off clipping
//        display.setClipping(false);

        displayFrame = display.createFrame();
        displayFrame.setTitle("Gridlock Display");
        c.registerFrame(displayFrame); // register the frame so it appears in
        // the "Display" list
        displayFrame.setVisible(true);

        display.attach(tractsPortrayal, "Census Tracts");
        display.attach(roadsPortrayal, "Roads");
        display.attach(agentPortrayal, "Agents");

        // CHART
        trafficChart = new TimeSeriesChartGenerator();
        trafficChart.setTitle("Traffic Statistics");
        trafficChart.setYAxisLabel("Speed");
        trafficChart.setXAxisLabel("Time");
        JFrame chartFrame = trafficChart.createFrame(this);
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

//    class PolyPortrayal extends GeomPortrayal {
//
//		private static final long serialVersionUID = 1L;
//
//        public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
//        {
//        	paint = Color.green;
//        	filled = true;
//            super.draw(object, graphics, info);
//        }
//    }
//
//    class RoadPortrayal extends GeomPortrayal {
//
//    	double scale = .001;
//
//    	private static final long serialVersionUID = 1L;
//
//		BasicStroke regularRoad = new BasicStroke((float).001);
//
//        public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
//        {
//            MasonGeometry gm = (MasonGeometry)object;
//            Geometry geometry = gm.getGeometry();
//            if (geometry.isEmpty()) return;
//
//            int size = 0;//((Gridlock) state).edgePops.get((LineString)geometry).size();
//            if( size > 0 )
//            	size *= 10;
//
//        	graphics.setStroke( new BasicStroke((float) scale * size) );
//        	graphics.setColor( Color.black );
//
//        	super.drawGeometry(geometry, graphics, false);
//            //super.draw(object, graphics, info);
//        }
//    }
}