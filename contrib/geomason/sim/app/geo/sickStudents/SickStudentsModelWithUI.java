/**
 ** SickStudentsModelWithUI.java
 **
 ** Copyright 2011 by Joseph Harrison, Mark Coletti, Cristina Metgher, Andrew Crooks
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 ** $Id$
 * 
 **/
package sim.app.geo.sickStudents;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JFrame;
import org.jfree.data.xy.XYSeries;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.util.gui.SimpleColorMap;
import sim.util.media.chart.*;

public class SickStudentsModelWithUI extends GUIState {

    public SickStudentsModel model;
    public Display2D display;
    public JFrame displayFrame;
    public TimeSeriesChartGenerator diseaseTimeSeriesChart = new TimeSeriesChartGenerator() ;
    public JFrame diseaseTimeSeriesFrame;
    public XYSeries susceptibleSeries = new XYSeries("Susceptible");
    public XYSeries infectedSeries = new XYSeries("Infected");
    public XYSeries recoveredSeries = new XYSeries("Recovered");
    // GIS portrayals
    GeomVectorFieldPortrayal elementarySchoolCatchmentsPortrayal = new GeomVectorFieldPortrayal();
    GeomVectorFieldPortrayal middleSchoolCatchmentsPortrayal = new GeomVectorFieldPortrayal();
    GeomVectorFieldPortrayal highSchoolCatchmentsPortrayal = new GeomVectorFieldPortrayal();
    GeomVectorFieldPortrayal elementarySchoolsPortrayal = new GeomVectorFieldPortrayal();
    GeomVectorFieldPortrayal middleSchoolsPortrayal = new GeomVectorFieldPortrayal();
    GeomVectorFieldPortrayal highSchoolsPortrayal = new GeomVectorFieldPortrayal();
    GeomVectorFieldPortrayal householdsPortrayal = new GeomVectorFieldPortrayal();
    double lastTime = 0, time = 0;

    public SickStudentsModelWithUI() {
        super(new SickStudentsModel(System.currentTimeMillis()));
        model = (SickStudentsModel) state;
    }

    public SickStudentsModelWithUI(SimState state) {
        super(state);
        model = (SickStudentsModel) state;
    }

    public static String getName() {
        return "Sick Students";
    }

    public Object getSimulationInspectedObject() {
        return state;
    } // non-volatile

    public void start() {
        super.start();
        // set up our portrayals
        setupPortrayals();

        susceptibleSeries.clear();
        infectedSeries.clear();
        recoveredSeries.clear();
    }

    public void load(SimState state) {
        super.load(state);
        setupPortrayals();
    }

    public void init(final Controller c) {
        super.init(c);

        display = new Display2D(model.width, model.height, this);

        display.attach(elementarySchoolCatchmentsPortrayal, "Elementary School Catchments");
        display.attach(middleSchoolCatchmentsPortrayal, "Middle School Catchments");
        display.attach(highSchoolCatchmentsPortrayal, "High School Catchments");

        display.attach(elementarySchoolsPortrayal, "Elementary Schools");
        display.attach(middleSchoolsPortrayal, "Middle Schools");
        display.attach(highSchoolsPortrayal, "High Schools");
        display.attach(householdsPortrayal, "Households");

        displayFrame = display.createFrame();
        controller.registerFrame(displayFrame);
        displayFrame.setVisible(true);		// this makes the map window visible by default

        c.registerFrame(createDiseaseTimeSeriesFrame());
        diseaseTimeSeriesFrame.setVisible(true);		// this makes the SIR chart visible by default

        ((Console) controller).setSize(380, 540);
    }

    @SuppressWarnings("serial")
    public void setupPortrayals() {

		// setup GIS portrayals

        elementarySchoolCatchmentsPortrayal.setField(model.elementarySchoolZones);
        elementarySchoolCatchmentsPortrayal.setPortrayalForAll(new CatchmentPortrayal(
                new SimpleColorMap(0.0, 1.0, Color.LIGHT_GRAY, Color.RED), model));

        middleSchoolCatchmentsPortrayal.setField(model.middleSchoolZones);
        middleSchoolCatchmentsPortrayal.setPortrayalForAll(new CatchmentPortrayal(
                new SimpleColorMap(0.0, 1.0, Color.LIGHT_GRAY, Color.RED), model));

        highSchoolCatchmentsPortrayal.setField(model.highSchoolZones);
        highSchoolCatchmentsPortrayal.setPortrayalForAll(new CatchmentPortrayal(
                new SimpleColorMap(0.0, 1.0, Color.LIGHT_GRAY, Color.RED), model));

        elementarySchoolsPortrayal.setField(model.elementarySchools);
        elementarySchoolsPortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.BLUE, 3.0));

        middleSchoolsPortrayal.setField(model.middleSchools);
        middleSchoolsPortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.BLUE, 5.0));

        highSchoolsPortrayal.setField(model.highSchools);
        highSchoolsPortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.blue, 7.0));

        householdsPortrayal.setField(model.householdsField);
        householdsPortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.black, 1.0));

        display.reset();
        display.setBackdrop(Color.WHITE);
        display.repaint();

        this.scheduleRepeatingImmediatelyAfter(new Steppable() {

            @Override
            public void step(SimState state) {
                time = state.schedule.getTime();
                if (time == Schedule.AFTER_SIMULATION) {
                    return;
                }

                int sCount = 0, iCount = 0, rCount = 0;
                for (Student s : model.students) {
                    switch (s.status) {
                        case SUSCEPTIBLE:
                            sCount++;
                            break;
                        case INFECTED:
                            iCount++;
                            break;
                        case RECOVERED:
                            rCount++;
                            break;
                    }
                }

                susceptibleSeries.add(time, (sCount / (double) model.students.size()));
                infectedSeries.add(time, (iCount / (double) model.students.size()));
                recoveredSeries.add(time, (rCount / (double) model.students.size()));
                diseaseTimeSeriesChart.update(ChartGenerator.FORCE_KEY, true);
            }
        });

        diseaseTimeSeriesChart.repaint();
    }

    public JFrame createDiseaseTimeSeriesFrame() {

        diseaseTimeSeriesChart = new TimeSeriesChartGenerator();
        diseaseTimeSeriesChart.setTitle("Disease Statistics");
        diseaseTimeSeriesChart.setDomainAxisLabel("Days");
        diseaseTimeSeriesChart.setRangeAxisLabel("Proportion of Students");
        ((TimeSeriesAttributes)(diseaseTimeSeriesChart.addSeries(susceptibleSeries, null))).setStrokeColor(Color.blue);
        ((TimeSeriesAttributes)(diseaseTimeSeriesChart.addSeries(infectedSeries, null))).setStrokeColor(Color.red);
        diseaseTimeSeriesChart.addSeries(recoveredSeries, null);

        diseaseTimeSeriesChart.setForeground(Color.black);

        diseaseTimeSeriesFrame = diseaseTimeSeriesChart.createFrame(this);
        diseaseTimeSeriesFrame.getContentPane().setLayout(new BorderLayout());
        diseaseTimeSeriesFrame.getContentPane().add(diseaseTimeSeriesChart, BorderLayout.CENTER);
        diseaseTimeSeriesFrame.pack();

        return diseaseTimeSeriesFrame;
    }

    public void quit() {
        super.quit();

        if (displayFrame != null) {
            displayFrame.dispose();
        }
        displayFrame = null; // let gc
        display = null; // let gc

        if (diseaseTimeSeriesFrame != null) {
            diseaseTimeSeriesFrame.dispose();
        }
        diseaseTimeSeriesFrame = null;
    }

    public static void main(String[] args) {
        new SickStudentsModelWithUI().createController();
    }
}
