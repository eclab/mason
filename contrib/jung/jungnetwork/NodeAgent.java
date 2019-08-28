/* This is our agent class. It implements MASON's Steppable interface 
 * (which allow us to use MASON schedule to direct activations as well
 *  as extends JUNG's SparseVertex class, allowing for exploiting JUNG to 
 *  operate and display the connections network. */

package jungnetwork;

import java.util.Iterator;
import java.util.Set;

import sim.engine.SimState;
import sim.engine.Steppable;
import edu.uci.ics.jung.graph.impl.SparseGraph;
import edu.uci.ics.jung.graph.impl.SparseVertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;

public class NodeAgent extends SparseVertex implements Steppable {

    private static final long serialVersionUID = 1L;

    // Handle to simulation
    public PreferentialAttachment sim;

    // Number of activations left
    public int timer;

    // Handle to graph
    public SparseGraph graph;

    public NodeAgent(PreferentialAttachment attachment, int timer) {
        this.sim = attachment;
        this.timer = timer;
        this.graph = (SparseGraph) attachment.graph;
    }

    public void step(SimState state) {

        /*
         * Each time the agent is activated, it will either add or drop a single
         * connection to other agents. The probability of dropping is increasing
         * in number of connections agent already has, equals 1 when agent's
         * degree reaches maxDegree cap.
         */

        double rand = sim.random.nextDouble();

        if ((this.degree() / sim.maxDegree) < rand) {

            /*
             * Decision to establish a new connection has been made. Target node
             * will be selected with probability proportional to its degree
             * using Boltzmann transformation - implementing preferential
             * attachment logic.
             */

            double temperature = sim.temperature;

            SparseVertex gfrom = this;
            Set graphSet = graph.getVertices();
            Iterator i = graphSet.iterator();

            double verSum = 0;

            while (i.hasNext()) {

                verSum += Math.exp(temperature * ((SparseVertex) i.next()).degree());

                }

            double cumSum = 0;
            rand = state.random.nextDouble();
            i = graphSet.iterator();
            SparseVertex nextVertex;

            do {

                nextVertex = (SparseVertex) i.next();
                cumSum += Math.exp(temperature * nextVertex.degree());

                } while (i.hasNext() && (rand > (cumSum / verSum)));

            graph.addEdge(new UndirectedSparseEdge(gfrom, nextVertex));

            } else {

                // Cognitive capacity exhausted. A random edge will be removed.

                UndirectedSparseEdge toRemove = (UndirectedSparseEdge) this.getOutEdges().toArray()[state.random.nextInt(this.degree())];

                graph.removeEdge(toRemove);

                }

        // Connection operations have been finished. If it is still possible,
        // agent will register itself on the schedule for next activation.

        if (this.timer > 0) {
            this.timer--;
            sim.schedule.scheduleOnce(sim.schedule.time() - Math.log(sim.random.nextDouble()), this);
            }

    }

    }
