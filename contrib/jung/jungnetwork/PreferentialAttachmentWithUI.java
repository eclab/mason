package jungnetwork;

/* GUI for PreferentialAttachment simulation. In addition to 
 * console, JungDisplay with graph and JFreechart 
 * with degree histogram will be created.  */

import java.awt.BorderLayout;
import java.util.Iterator;

import javax.swing.JFrame;

import edu.uci.ics.jung.graph.impl.SparseVertex;

import sim.display.Console;
import sim.display.Controller;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.media.chart.HistogramGenerator;

public class PreferentialAttachmentWithUI extends GUIState {

    // Custom display for the graph from underlaying simulation.
    public JungDisplay jDisplay;

    // Vector with degrees of nodes in our network, filled with some fake
    // initial data for good start.
    double[] degrees = { 1, 2, 3 };

    // MASON's interface to JFreeChart.
    public HistogramGenerator degreesChart;

    // A Frame holding degree histogram
    public JFrame degreesFrame;

    public static void main(String[] args) {
        PreferentialAttachmentWithUI vid = new PreferentialAttachmentWithUI();
        Console c = new Console((GUIState) vid);
        c.setVisible(true);
    }

    public PreferentialAttachmentWithUI() {
        super(new PreferentialAttachment(System.currentTimeMillis()));
    }

    public PreferentialAttachmentWithUI(GraphSimState state) {
        super(state);
    }

    public static String getName() {
        return "Preferential Attachment";
    }

    public Object getSimulationInspectedObject() {
        return state;
    }

    public void start() {
        super.start();
        setupPortrayals();
    }

    public void load(GraphSimState state) {
        super.load(state);
        setupPortrayals();
    }

    public void setupPortrayals() {

        // Reset displays.
        jDisplay.reset();
        degreesChart.repaint();

        /*
         * A simple agent will be created and registered on schedule. It will
         * update information used for plotting the histogram. It is possible to
         * have underlying JFreeChart data structure in the SimState thread, but
         * it unnecessarily slows down simulation when run in the batch /
         * command line only mode.
         */
        Steppable histUpdater = new Steppable() {

            private static final long serialVersionUID = 6184761986120478954L;

            public void step(SimState state) {
                if (degreesFrame.isVisible()) {

                    degrees = new double[((PreferentialAttachment) state).graph.numVertices()];

                    Iterator i = ((PreferentialAttachment) state).graph.getVertices().iterator();
                    int j = 0;
                    while (i.hasNext()) {
                        degrees[j] = ((SparseVertex) i.next()).degree();
                        j++;
                        }

                    degreesChart.updateSeries(0, degrees, false);
                    }

            }
            };

        this.scheduleImmediateRepeat(true, histUpdater);
    }

    public void init(Controller c) {
        super.init(c);

        // Instantiate JungDisplay
        jDisplay = new JungDisplay(this);
        jDisplay.frame.setTitle("Preferential attachment graph");
        c.registerFrame(jDisplay.frame);
        jDisplay.frame.setVisible(true);

        // Instantiate histogram
        degreesChart = new HistogramGenerator();
        degreesChart.setTitle("Degree Histogram");
        degreesChart.addSeries(this.degrees, ((PreferentialAttachment) state).maxDegree, "Degree histogram", null);
        degreesChart.update();
        degreesFrame = degreesChart.createFrame(this);
        degreesFrame.getContentPane().setLayout(new BorderLayout());
        degreesFrame.getContentPane().add(degreesChart, BorderLayout.CENTER);
        degreesFrame.pack();
        c.registerFrame(degreesFrame);

    }

    public void quit() {
        super.quit();

        if (jDisplay.frame != null)
            jDisplay.frame.dispose();
        jDisplay.frame = null;
    }

    }
