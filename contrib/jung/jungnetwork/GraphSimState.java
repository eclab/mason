package jungnetwork;

/* Auxiliary class extending SimState and adding a field with JUNG Graph. */

import sim.engine.SimState;
import edu.uci.ics.jung.graph.Graph;

public class GraphSimState extends SimState {

    private static final long serialVersionUID = -1037386169623375455L;

    public Graph graph;

    public GraphSimState(long seed) {
        super(seed);
    }

    }
