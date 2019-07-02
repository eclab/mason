package sim.app.geo.ebola;


import sim.app.geo.ebola.Household;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.dial.*;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.FieldPortrayal2D;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.portrayal.grid.SparseGridPortrayal2D;
import sim.portrayal.network.NetworkPortrayal2D;
import sim.portrayal.network.SimpleEdgePortrayal2D;
import sim.portrayal.network.SpatialNetwork2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.portrayal.simple.RectanglePortrayal2D;
import sim.util.media.chart.TimeSeriesChartGenerator;

import javax.swing.*;
import java.awt.*;

/**
 * Created by rohansuri on 7/8/15.
 */
public class EbolaWithUI extends GUIState
{
    Display2D display; //displaying the model
    JFrame displayFrame; //frame containing all the displays


    public EbolaWithUI(EbolaABM sim)
    {
        super(sim);
    }

    @Override
    public void init(Controller c)
    {
        super.init(c);
        display = new Display2D(1000, 1000, this); //creates the display
        displayFrame = display.createFrame();
        c.registerFrame(displayFrame);
        displayFrame.setVisible(true);

        JFreeChart roadNetworkChart;
        roadNetworkChart = ChartFactory.createBarChart("Distribution Chart", "Distance",
                "Frequency", ((EbolaABM) this.state).distribution, PlotOrientation.VERTICAL, false, false,
                false);
        roadNetworkChart.setBackgroundPaint(Color.WHITE);
        roadNetworkChart.getTitle().setPaint(Color.BLACK);

        CategoryPlot p4 = roadNetworkChart.getCategoryPlot();
        p4.setBackgroundPaint(Color.WHITE);
        p4.setRangeGridlinePaint(Color.blue);

        // set the range axis to display integers only...
        NumberAxis rangeAxis4 = (NumberAxis) p4.getRangeAxis();
        rangeAxis4.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        //rangeAxis4.setRange(0, ((EbolaABM) this.state).max);

        ChartFrame frame4 = new ChartFrame("Road Network Distribution", roadNetworkChart);
        frame4.setVisible(false);
        frame4.setSize(700, 350);

        frame4.pack();
        c.registerFrame(frame4);

        //health status chart

        Dimension dm = new Dimension(30,30);
        Dimension dmn = new Dimension(30,30);

        TimeSeriesChartGenerator healthStatus;
        healthStatus = new TimeSeriesChartGenerator();
        healthStatus.createFrame();
        healthStatus.setSize(dm);
        healthStatus.setTitle("Health Status");
        healthStatus.setRangeAxisLabel("Number of People");
        healthStatus.setDomainAxisLabel("Hours");
        healthStatus.setMaximumSize(dm);
        healthStatus.setMinimumSize(dmn);
//        chartSeriesCholera.setMinimumChartDrawSize(400, 300); // makes it scale at small sizes
//        chartSeriesCholera.setPreferredChartSize(400, 300); // lets it be small

        healthStatus.addSeries(((EbolaABM) this.state).totalsusceptibleSeries, null);
        healthStatus.addSeries(((EbolaABM) this.state).totalExposedSeries, null);
        healthStatus.addSeries(((EbolaABM) this.state).totalInfectedSeries, null);
        healthStatus.addSeries(((EbolaABM) this.state).totalRecoveredSeries, null);
        healthStatus.addSeries(((EbolaABM) this.state).totalDeadSeries, null);

        //chart for cumulative cases
        TimeSeriesChartGenerator charSeriesCumalative;
        charSeriesCumalative = new TimeSeriesChartGenerator();
        charSeriesCumalative.createFrame();
        charSeriesCumalative.setSize(dm);
        charSeriesCumalative.setTitle("Cumulative Cases");
        charSeriesCumalative.setRangeAxisLabel("Number of People");
        charSeriesCumalative.setDomainAxisLabel("Days");
        charSeriesCumalative.setMaximumSize(dm);
        charSeriesCumalative.setMinimumSize(dmn);
//        chartSeriesCholera.setMinimumChartDrawSize(400, 300); // makes it scale at small sizes
//        chartSeriesCholera.setPreferredChartSize(400, 300); // lets it be small

        charSeriesCumalative.addSeries(((EbolaABM) this.state).totalLiberia , null);
        charSeriesCumalative.addSeries(((EbolaABM) this.state).totalGuinea , null);
        charSeriesCumalative.addSeries(((EbolaABM) this.state).totalSierra_Leone, null);
        charSeriesCumalative.addSeries(((EbolaABM) this.state).totalGuineaActual, null);
        charSeriesCumalative.addSeries(((EbolaABM) this.state).totalLiberiaActual, null);
        charSeriesCumalative.addSeries(((EbolaABM) this.state).totalSierraLeoneActual, null);

        JFrame frameSeries = healthStatus.createFrame(this);
        frameSeries.pack();
        c.registerFrame(frameSeries);

        JFrame cumalativeFrameSeries = charSeriesCumalative.createFrame(this);
        cumalativeFrameSeries.pack();
        c.registerFrame(cumalativeFrameSeries);

        //make a chart for total actual cases
        //chart for cumalative cases
        TimeSeriesChartGenerator charSeriesCumulativeActual;
        charSeriesCumulativeActual = new TimeSeriesChartGenerator();
        charSeriesCumulativeActual.createFrame();
        charSeriesCumulativeActual.setSize(dm);
        charSeriesCumulativeActual.setTitle("Cumulative Actual Cases");
        charSeriesCumulativeActual.setRangeAxisLabel("Number of Cases");
        charSeriesCumulativeActual.setDomainAxisLabel("Days since start");
        charSeriesCumulativeActual.setMaximumSize(dm);
        charSeriesCumulativeActual.setMinimumSize(dmn);
//        chartSeriesCholera.setMinimumChartDrawSize(400, 300); // makes it scale at small sizes
//        chartSeriesCholera.setPreferredChartSize(400, 300); // lets it be small

        charSeriesCumulativeActual.addSeries(((EbolaABM) this.state).totalGuineaActual, null);
        charSeriesCumulativeActual.addSeries(((EbolaABM) this.state).totalLiberiaActual, null);
        charSeriesCumulativeActual.addSeries(((EbolaABM) this.state).totalSierraLeoneActual, null);

        JFrame frameSeriesActual = charSeriesCumulativeActual.createFrame(this);
        frameSeriesActual.pack();
        c.registerFrame(frameSeriesActual);

        //time chart
        StandardDialFrame dialFrame = new StandardDialFrame();
        DialBackground ddb = new DialBackground(Color.white);
        dialFrame.setBackgroundPaint(Color.lightGray);
        dialFrame.setForegroundPaint(Color.darkGray);

        DialPlot plot = new DialPlot();
        plot.setView(0.0, 0.0, 1.0, 1.0);
        plot.setBackground(ddb);
        plot.setDialFrame(dialFrame);

        plot.setDataset(0, ((EbolaABM) this.state).hourDialer);
        plot.setDataset(1,((EbolaABM) this.state).dayDialer);


        DialTextAnnotation annotation1 = new DialTextAnnotation("Hour");
        annotation1.setFont(new Font("Dialog", Font.BOLD, 14));
        annotation1.setRadius(0.1);
        plot.addLayer(annotation1);


//        DialValueIndicator dvi = new DialValueIndicator(0);
//        dvi.setFont(new Font("Dialog", Font.PLAIN, 10));
//        dvi.setOutlinePaint(Color.black);
//        plot.addLayer(dvi);
//

        DialValueIndicator dvi2 = new DialValueIndicator(1);
        dvi2.setFont(new Font("Dialog", Font.PLAIN, 22));
        dvi2.setOutlinePaint(Color.red);
        dvi2.setRadius(0.3);
        plot.addLayer(dvi2);

        DialTextAnnotation annotation2 = new DialTextAnnotation("Day");
        annotation2.setFont(new Font("Dialog", Font.BOLD, 18));
        annotation2.setRadius(0.4);
        plot.addLayer(annotation2);

        StandardDialScale scale = new StandardDialScale(0.0, 23.99, 90, -360, 1.0,59);
        scale.setTickRadius(0.9);
        scale.setTickLabelOffset(0.15);
        scale.setTickLabelFont(new Font("Dialog", Font.PLAIN, 12));
        plot.addScale(0, scale);
        scale.setMajorTickPaint(Color.black);
        scale.setMinorTickPaint(Color.lightGray);




//        StandardDialScale scale2 = new StandardDialScale(1, 7, -150, -240, 1,1);
//        scale2.setTickRadius(0.50);
//        scale2.setTickLabelOffset(0.15);
//        scale2.setTickLabelPaint(Color.RED);
//        scale2.setTickLabelFont(new Font("Dialog", Font.PLAIN, 12));
//        plot.addScale(1, scale2);
//
//        DialPointer needle2 = new DialPointer.Pin(1);
//        plot.addPointer(needle2);
//        needle2.setRadius(0.40);
        // plot.mapDatasetToScale(1, 1);

        DialPointer needle = new DialPointer.Pointer(0);
        plot.addPointer(needle);


        DialCap cap = new DialCap();
        cap.setRadius(0.10);
        plot.setCap(cap);

        JFreeChart chart1 = new JFreeChart(plot);
        ChartFrame timeframe = new ChartFrame("Time Chart", chart1);
        timeframe.setVisible(false);
        timeframe.setSize(200, 100);
        timeframe.pack();
        c.registerFrame(timeframe);
    }

    @Override
    public void start()
    {
        super.start();
        setupPortrayals();
    }

    public void setupPortrayals()
    {
        FieldPortrayal2D householdortrayal = new SparseGridPortrayal2D();
        householdortrayal.setField(((EbolaABM)state).householdGrid);
        householdortrayal.setPortrayalForAll(new RectanglePortrayal2D(new Color(0, 128, 255), 1.0, false)
        {
            @Override
            public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
            {
                Household house = (Household)object;

                if(house.getCountry() == Parameters.GUINEA)
                    paint = new Color(216, 10, 255);
                else if(house.getCountry() == Parameters.LIBERIA)
                    paint = new Color(52, 222, 29);
                else if(house.getCountry() == Parameters.SL)
                    paint = new Color(255, 248, 98);
                else
                    paint = new Color(8, 20, 255);
                super.draw(object, graphics, info);
            }
        });
        display.attach(householdortrayal, "Household");

        FieldPortrayal2D urbanPortrayal = new SparseGridPortrayal2D();
        urbanPortrayal.setField(((EbolaABM)state).urbanAreasGrid);
        urbanPortrayal.setPortrayalForAll(new RectanglePortrayal2D(new Color(255, 21, 19), 1.0, false));
        display.attach(urbanPortrayal, "Urban Area");

        //---------------------Adding the road portrayal------------------------------
        GeomVectorFieldPortrayal roadLinkPortrayal = new GeomVectorFieldPortrayal();
        roadLinkPortrayal.setField(((EbolaABM) state).roadLinks);
        roadLinkPortrayal.setPortrayalForAll(new GeomPortrayal(new Color(0.42f, 0.42f, 0.42f, 0.5f), 2.0, true));
        display.attach(roadLinkPortrayal, "Roads");

//        FieldPortrayal2D roadPortrayal = new SparseGridPortrayal2D();
//        roadPortrayal.setField(((EbolaABM)state).allRoadNodes);
//        roadPortrayal.setPortrayalForAll(new OvalPortrayal2D(new Color(255, 64, 240), 1.0, true));
//        display.attach(roadPortrayal, "Road Node");

        FieldPortrayal2D schoolPortrayal = new SparseGridPortrayal2D();
        schoolPortrayal.setField(((EbolaABM)state).schoolGrid);
        schoolPortrayal.setPortrayalForAll(new RectanglePortrayal2D(new Color(255, 154, 146), 1.0, false));
        display.attach(schoolPortrayal, "Schools");

        //Farms
        FieldPortrayal2D farmPortrayal = new SparseGridPortrayal2D();
        farmPortrayal.setField(((EbolaABM) state).farmGrid);
        farmPortrayal.setPortrayalForAll(new RectanglePortrayal2D(new Color(17, 202, 255), 1.0, false)
        {
            @Override
            public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
            {
                WorkLocation wl = (WorkLocation)object;

                if(wl.getSector_id() == Constants.HEALTH)
                {
                    paint = new Color(193, 0, 255);
                    //super.scale = 4.0;
                }
                else
                {
                    paint = new Color(17, 202, 255);
                }

                super.draw(object, graphics, info);
            }
        });
        display.attach(farmPortrayal, "Work Locations");

//        NetworkPortrayal2D roadNetworkPortrayal = new NetworkPortrayal2D();
//        roadNetworkPortrayal.setField(new SpatialNetwork2D(((EbolaABM)state).allRoadNodes, ((EbolaABM)state).roadNetwork));
//        roadNetworkPortrayal.setPortrayalForAll(new SimpleEdgePortrayal2D());
//        display.attach(roadNetworkPortrayal, "Road Network");

        ContinuousPortrayal2D residentPortrayal = new ContinuousPortrayal2D();

        residentPortrayal.setField(((EbolaABM)this.state).world);
        residentPortrayal.setPortrayalForAll(new OvalPortrayal2D()
        {
            public void draw (Object object, Graphics2D graphics, DrawInfo2D info)
            {
                Resident resident = (Resident)object;
                if(resident.getHealthStatus() == Constants.DEAD)
                    paint = new Color(124, 115, 92);
                else if(resident.getHealthStatus() == Constants.SUSCEPTIBLE)
                    paint = new Color(20, 4, 255);
                else if(resident.getAge() == Constants.EXPOSED)
                    paint = new Color(255, 151, 71);
                else if(resident.getHealthStatus() == Constants.INFECTIOUS)
                    paint = new Color(255, 0, 48);
                else if(resident.getHealthStatus() == Constants.RECOVERED)
                    paint = new Color(255, 20, 215);

                if(resident.isMoving())
                {
                    //paint = new Color(255, 151, 71);
                    super.scale = 10.0;
                }
                else
                    super.scale = 1.0;
                super.draw(object, graphics, info);
            }
        });
        display.attach(residentPortrayal, "Residents");

        FieldPortrayal2D hotspotsPortrayal = new SparseGridPortrayal2D();
        hotspotsPortrayal.setField(((EbolaABM)state).hotSpotsGrid);
        hotspotsPortrayal.setPortrayalForAll(new RectanglePortrayal2D(new Color(255, 21, 19), 1.0, false));
        display.attach(hotspotsPortrayal, "Hotspot Area");
    }

    @Override
    public void quit()
    {
        super.quit();

        if (displayFrame != null)
            displayFrame.dispose();
        displayFrame = null;
        display = null;

    }

    public static void main(String[] args)
    {
        EbolaWithUI ebUI = new EbolaWithUI(new EbolaABM(System.currentTimeMillis()));
        Console c = new Console(ebUI);
        c.setVisible(true);
    }
}
