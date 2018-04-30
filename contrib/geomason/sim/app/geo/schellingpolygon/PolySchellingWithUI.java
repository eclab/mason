/**
 ** PolySchellingWithUI.java
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
package schellingpolygon;

import java.awt.Color;
import java.awt.Graphics2D;
import javax.swing.JFrame;
import org.jfree.data.xy.XYSeries;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.util.media.chart.HistogramGenerator;
import sim.util.media.chart.TimeSeriesChartGenerator;



public class PolySchellingWithUI extends GUIState
{

    Display2D display;
    JFrame displayFrame;
    // portrayal info
    GeomVectorFieldPortrayal polyPortrayal = new GeomVectorFieldPortrayal();
    // chart info
    TimeSeriesChartGenerator happinessChart;
    XYSeries happyReds;
    XYSeries happyBlues;
    // histogram info
    HistogramGenerator numMovesHisto;
    double[] peoplesMoves;



    /** constructor function */
    protected PolySchellingWithUI(SimState state)
    {
        super(state);
    }



    /** constructor function */
    public PolySchellingWithUI()
    {
        super(new PolySchelling(System.currentTimeMillis()));
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

        // the happiness chart setup
        happinessChart = new TimeSeriesChartGenerator();
        happinessChart.setTitle("Percent of Happy Persons in Simulation");
        happinessChart.setRangeAxisLabel("Percent Happy");
        happinessChart.setDomainAxisLabel("Opportunities to Move");
        JFrame chartFrame = happinessChart.createFrame(this);
        chartFrame.pack();
        controller.registerFrame(chartFrame);

        // the # moves histogram setup
        numMovesHisto = new HistogramGenerator();
        numMovesHisto.setTitle("Number of Moves People Have Made");
        numMovesHisto.setDomainAxisLabel("Number of Moves");
        numMovesHisto.setRangeAxisLabel("%");
        JFrame histoFrame = numMovesHisto.createFrame(this);
        histoFrame.pack();
        controller.registerFrame(histoFrame);
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
        PolySchelling world = (PolySchelling) state;

        // reset the chart info
        happyReds = new XYSeries("Happy Reds");
        happyBlues = new XYSeries("Happy Blues");
        happinessChart.removeAllSeries();
        happinessChart.addSeries(happyReds, null);
        happinessChart.addSeries(happyBlues, null);

        // schedule the chart to take data
        state.schedule.scheduleRepeating(new HappyTracker());

        // reset the histogram info
        peoplesMoves = new double[((PolySchelling) state).people.size()];
        numMovesHisto.removeAllSeries();
        numMovesHisto.addSeries(peoplesMoves, 10, "HistoMoves", null);

        // schedule the histogram to take data
        state.schedule.scheduleRepeating(new MoveTracker());

        // the polygon portrayal
        polyPortrayal.setField(world.world);
        polyPortrayal.setPortrayalForAll(new PolyPortrayal());


        display.reset();

        display.repaint();
    }



    /** Keeps track of the rates of happy Reds and happy Blues in the simulation */
    class HappyTracker implements Steppable
    {

        public void step(SimState state)
        {
            PolySchelling ps = (PolySchelling) state;
            double hReds = 0, hBlues = 0;

            // query all Persons whether their position is acceptable
            for (Person p : ps.people)
            {
                if (p.acceptable(p.region))
                {
                    if (p.color.equals("RED"))
                    {
                        hReds++;
                    } else
                    {
                        hBlues++;
                    }
                }
            }
            // add this data to the chart
            happyReds.add(state.schedule.getTime() / ps.people.size(),
                          hReds / ps.totalReds, true);
            happyBlues.add(state.schedule.getTime() / ps.people.size(),
                           hBlues / ps.totalBlues, true);
        }

    }



    /** Keeps track of the number of moves agents have made */
    class MoveTracker implements Steppable
    {

        public void step(SimState state)
        {
            PolySchelling ps = (PolySchelling) state;
            int numPeople = ps.people.size();
            peoplesMoves = new double[numPeople];
            for (int i = 0; i < numPeople; i++)
            {
                Person p = ps.people.get(i);
                peoplesMoves[i] = p.numMoves;
            }
            numMovesHisto.updateSeries(0, peoplesMoves);
        }

    }



    public static void main(String[] args)
    {
        PolySchellingWithUI worldGUI = new PolySchellingWithUI();
        Console console = new Console(worldGUI);
        console.setVisible(true);
    }



    /** The portrayal used to display Polygons with the appropriate color */
    class PolyPortrayal extends GeomPortrayal
    {

        private static final long serialVersionUID = 1L;



        @Override
        public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
            Polygon poly = (Polygon) object;

            if (poly.residents.isEmpty())
            {
                paint = Color.gray;
            } else if (poly.getSoc().equals("RED"))
            {
                paint = Color.red;
            } else if (poly.getSoc().equals("BLUE"))
            {
                paint = Color.blue;
            } else
            {
                paint = Color.gray;
            }

            super.draw(object, graphics, info);
        }

    }
}