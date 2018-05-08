/**
 ** SchellingSpaceWithUI.java
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
package schellingspace;

import javax.swing.JFrame;
import org.jfree.data.xy.XYSeries;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.util.media.chart.HistogramGenerator;
import sim.util.media.chart.TimeSeriesChartGenerator;



@SuppressWarnings("restriction")
public class SchellingSpaceWithUI extends GUIState
{

    Display2D display;
    JFrame displayFrame;
    // portrayal info
    GeomVectorFieldPortrayal polyPortrayal = new GeomVectorFieldPortrayal();
    GeomVectorFieldPortrayal peoplePortrayal = new GeomVectorFieldPortrayal();
    // chart info
    TimeSeriesChartGenerator happinessChart;
    XYSeries happyReds;
    XYSeries happyBlues;
    // histogram info
    HistogramGenerator numMovesHisto;
    double[] peoplesMoves;



    protected SchellingSpaceWithUI(SimState state)
    {
        super(state);
    }



    public SchellingSpaceWithUI()
    {
        super(new SchellingSpace(System.currentTimeMillis()));
    }



    /** return the name of the simulation */
    public static String getName()
    {
        return "SpaceSchelling";
    }



    /** initialize the simulation */
    public void init(Controller controller)
    {
        super.init(controller);

        display = new Display2D(600, 600, this);

        display.attach(polyPortrayal, "Polys");
        display.attach(peoplePortrayal, "People");

        displayFrame = display.createFrame();
        controller.registerFrame(displayFrame);
        displayFrame.setVisible(true);

        // the happiness chart setup
        happinessChart = new TimeSeriesChartGenerator();
        happinessChart.setTitle("Percent of Happy Persons in Simulation");
        happinessChart.setYAxisLabel("Percent Happy");
        happinessChart.setXAxisLabel("Opportunities to Move");
        JFrame chartFrame = happinessChart.createFrame(this);
        chartFrame.pack();
        controller.registerFrame(chartFrame);

        // the # moves histogram setup
        numMovesHisto = new HistogramGenerator();
        numMovesHisto.setTitle("Number of Moves People Have Made");
        numMovesHisto.setYAxisLabel("Number of Moves");
        numMovesHisto.setXAxisLabel("number");
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
        SchellingSpace world = (SchellingSpace) state;

        // reset the chart info
        happyReds = new XYSeries("Happy Reds");
        happyBlues = new XYSeries("Happy Blues");
        happinessChart.removeAllSeries();
        happinessChart.addSeries(happyReds, null);
        happinessChart.addSeries(happyBlues, null);

        // schedule the chart to take data
        state.schedule.scheduleRepeating(new HappyTracker());

        // reset the histogram info
        peoplesMoves = new double[((SchellingSpace) state).people.size()];
        numMovesHisto.removeAllSeries();
        numMovesHisto.addSeries(peoplesMoves, 10, "HistoMoves", null);

        // schedule the histogram to take data
        state.schedule.scheduleRepeating(new MoveTracker());

        // the polygon portrayal
        polyPortrayal.setField(world.world);
        polyPortrayal.setPortrayalForAll(new WardPortrayal());

        peoplePortrayal.setField(world.agents);
        peoplePortrayal.setPortrayalForAll(new PersonPortrayal());
//        peoplePortrayal.setPortrayalForAll(new GeomPortrayal(Color.RED));

        display.reset();

        display.repaint();
    }



    /** Keeps track of the rates of happy Reds and happy Blues in the simulation */
    class HappyTracker implements Steppable
    {

        public void step(SimState state)
        {
            SchellingSpace ps = (SchellingSpace) state;
            double hReds = 0, hBlues = 0;

            // query all Persons whether their position is acceptable
            for (Person p : ps.people)
            {
                if (p.acceptable(ps))
                {
                    if (p.getAffiliation().equals(Person.Affiliation.RED))
                    {
                        hReds++;
                    } else
                    {
                        hBlues++;
                    }
                }
            }
            // add this data to the chart
            happyReds.add(state.schedule.time(),
                          hReds / ps.totalReds, true);
            happyBlues.add(state.schedule.time(),
                           hBlues / ps.totalBlues, true);
        }

    }



    /** Keeps track of the number of moves agents have made */
    class MoveTracker implements Steppable
    {

        public void step(SimState state)
        {
            SchellingSpace ps = (SchellingSpace) state;
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
        SchellingSpaceWithUI worldGUI = new SchellingSpaceWithUI();
        Console console = new Console(worldGUI);
        console.setVisible(true);
    }

}