/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.networktest;

import sim.field.continuous.*;
import sim.field.network.*;
import sim.engine.*;
import sim.util.*;

public /*strictfp*/ class NetworkTest extends SimState
    {
    public static final double XMIN = 0;
    public static final double XMAX = 800;
    public static final double YMIN = 0;
    public static final double YMAX = 600;

    public static final double DIAMETER = 8;

    public Continuous2D environment = null;
    public Network network = null;

    /** Creates a NetworkTest simulation with the given random number seed. */
    public NetworkTest(long seed)
        {
        super(seed);
        }

    boolean acceptablePosition( final CustomNode node, final Double2D location )
        {
        if( location.x < DIAMETER/2 || location.x > (XMAX-XMIN)-DIAMETER/2 ||
            location.y < DIAMETER/2 || location.y > (YMAX-YMIN)-DIAMETER/2 )
            return false;
        return true;
        }

    public void start()
        {
        super.start();  // clear out the schedule

        environment = new Continuous2D(16.0, (XMAX-XMIN), (YMAX-YMIN) );
        network = new Network();

        CustomNode[] nodes = new CustomNode[6];
        nodes[0] = new CustomNode( "node0",
                                   new Double2D( random.nextDouble()*(XMAX-XMIN-DIAMETER)+XMIN+DIAMETER/2,
                                                 random.nextDouble()*(YMAX-YMIN-DIAMETER)+YMIN+DIAMETER/2 ) );
        network.addNode(nodes[0]);
        nodes[1] = new CustomNode( "node1",
                                   new Double2D( random.nextDouble()*(XMAX-XMIN-DIAMETER)+XMIN+DIAMETER/2,
                                                 random.nextDouble()*(YMAX-YMIN-DIAMETER)+YMIN+DIAMETER/2 ) );
        network.addNode(nodes[1]);
        nodes[2] = new CustomNode( "node2",
                                   new Double2D( random.nextDouble()*(XMAX-XMIN-DIAMETER)+XMIN+DIAMETER/2,
                                                 random.nextDouble()*(YMAX-YMIN-DIAMETER)+YMIN+DIAMETER/2 ) );
        network.addNode(nodes[2]);
        nodes[3] = new CustomNode( "node3",
                                   new Double2D( random.nextDouble()*(XMAX-XMIN-DIAMETER)+XMIN+DIAMETER/2,
                                                 random.nextDouble()*(YMAX-YMIN-DIAMETER)+YMIN+DIAMETER/2 ) );
        network.addNode(nodes[3]);
        nodes[4] = new CustomNode( "node4",
                                   new Double2D( random.nextDouble()*(XMAX-XMIN-DIAMETER)+XMIN+DIAMETER/2,
                                                 random.nextDouble()*(YMAX-YMIN-DIAMETER)+YMIN+DIAMETER/2 ) );
        network.addNode(nodes[4]);
        nodes[5] = new CustomNode( "node5",
                                   new Double2D( random.nextDouble()*(XMAX-XMIN-DIAMETER)+XMIN+DIAMETER/2,
                                                 random.nextDouble()*(YMAX-YMIN-DIAMETER)+YMIN+DIAMETER/2 ) );
        network.addNode(nodes[5]);
        Edge e01 = new Edge( nodes[0], nodes[1], new EdgeInfo("0-1") );
        network.addEdge( e01 );
        Edge e12 = new Edge( nodes[1], nodes[2], new EdgeInfo("1-2") );
        network.addEdge( e12 );
        Edge e23 = new Edge( nodes[2], nodes[3], new EdgeInfo("2-3") );
        network.addEdge( e23 );
        Edge e34 = new Edge( nodes[3], nodes[4], new EdgeInfo("3-4") );
        network.addEdge( e34 );
        Edge e40 = new Edge( nodes[4], nodes[0], new EdgeInfo("4-0") );
        network.addEdge( e40 );
        Edge e05 = new Edge( nodes[0], nodes[5], new EdgeInfo("0-5") );
        network.addEdge( e05 );
        Edge e15 = new Edge( nodes[1], nodes[5], new EdgeInfo("1-5") );
        network.addEdge( e15 );
        Edge e25 = new Edge( nodes[2], nodes[5], new EdgeInfo("2-5") );
        network.addEdge( e25 );
        Edge e35 = new Edge( nodes[3], nodes[5], new EdgeInfo("3-5") );
        network.addEdge( e35 );
        Edge e45 = new Edge( nodes[4], nodes[5], new EdgeInfo("4-5") );
        network.addEdge( e45 );

        // Schedule the agents -- we could instead use a RandomSequence, which would be faster,
        // but this is a good test of the scheduler
        for(int x=0;x<6;x++)
            {
            environment.setObjectLocation(nodes[x],nodes[x].location);
            schedule.scheduleRepeating(nodes[x]);
            }
        }

    public static void main(String[] args)
        {
        doLoop(NetworkTest.class, args);
        System.exit(0);
        }    
    }
