package jungnetwork;

/* This demo shows how to use JUNG and MASON together. 
 * A number of agents will create and drop connections
 * between themselves, with this network of interactions 
 * described with JUNG graph. The agents will be actived
 * using Poisson activation scheme. */

import sim.util.Interval;
import edu.uci.ics.jung.graph.impl.SparseGraph;

public class PreferentialAttachment extends GraphSimState {

    // How often the display is to be updated in number of schedule ticks
    public double updateInterval = 2;

    // Maximal degree of nodes
    public int maxDegree = 5;

    // Number of nodes
    public int N = 10;

    // Number of activations each agent will undergo
    public int T = 50;

    // Temperature used in Boltzman transformation
    public double temperature = 0.33;

    /*
     * Following are getters and setters for parameters T, N, updateInterval and
     * temperature as required to have them automatically coupled with MASON
     * interface.
     */

    /* Start of setters - getters section */

    public int getN() {
        return N;
    }

    public void setN(int temp) {
        if (temp >= 0 && temp <= 20) {
            N = temp;
            }
    }

    public Object domN() {
        return new Interval(0, 20);
    }

    public int getT() {
        return T;
    }

    public void setT(int temp) {
        if (temp >= 0 && temp <= 100) {
            T = temp;
            }
    }

    public Object domT() {
        return new Interval(0, 100);
    }

    public double getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(double temp) {
        if (temp >= 0 && temp <= 5) {
            updateInterval = temp;
            }
    }

    public Object domUpdateInterval() {
        return new Interval(1.0, 5.0);
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temp) {
        if (temp >= 0 && temp <= 1.0) {
            temperature = temp;
            }
    }

    public Object domTemperature() {
        return new Interval(0.0, 1.0);
    }

    /* End of setters - getters section */

    // Constructor
    public PreferentialAttachment(long seed) {
        super(seed);
        // Initilize an empty JUNG graph.
        graph = new SparseGraph();
    }

    public void start() {

        super.start();

        // Just in case, clear schedule.
        schedule.reset();

        // Make sure that the graph is empty.
        this.graph.removeAllEdges();
        this.graph.removeAllVertices();

        /*
         * Create N agents, which will be added to the graph as nodes. The
         * initial activations of out agents are scheduled here, all subsequent
         * will be scheduled by agents themselves.
         */
        for (int i = 0; i < N; i++) {
            NodeAgent tempAgent = new NodeAgent(this, T);
            schedule.scheduleOnce(-Math.log(random.nextDouble()), tempAgent);
            graph.addVertex(tempAgent);
            }

    }
        
    public static void main(String[] args) {
        doLoop(PreferentialAttachment.class, args);
        System.exit(0);
    }

    static final long serialVersionUID = -7164072518609011190L;
    }
