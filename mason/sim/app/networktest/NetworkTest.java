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

    CustomNode makeNode(String name)
        {
        CustomNode node = new CustomNode(name);
        environment.setObjectLocation(node, new Double2D( 
                random.nextDouble()*(XMAX-XMIN-DIAMETER)+XMIN+DIAMETER/2,
                random.nextDouble()*(YMAX-YMIN-DIAMETER)+YMIN+DIAMETER/2 ) );
        network.addNode(node);
        schedule.scheduleRepeating(node);
        return node;
        }

    public void start()
        {
        super.start();  // clear out the schedule

        environment = new Continuous2D(16.0, (XMAX-XMIN), (YMAX-YMIN) );
        network = new Network();

        CustomNode nodes[] = new CustomNode[6];
        nodes[0] = makeNode( "node0");
        nodes[1] = makeNode( "node1");
        nodes[2] = makeNode( "node2");
        nodes[3] = makeNode( "node3");
        nodes[4] = makeNode( "node4");
        nodes[5] = makeNode( "node5");

        network.addEdge( nodes[0], nodes[1], "0-1" );
        network.addEdge( nodes[1], nodes[2], "1-2" );
        network.addEdge( nodes[2], nodes[3], "2-3" );
        network.addEdge( nodes[3], nodes[4], "3-4" );
        network.addEdge( nodes[4], nodes[0], "4-0" );
        network.addEdge( nodes[0], nodes[5], "0-5" );
        network.addEdge( nodes[1], nodes[5], "1-5" );
        network.addEdge( nodes[2], nodes[5], "2-5" );
        network.addEdge( nodes[3], nodes[5], "3-5" );
        network.addEdge( nodes[4], nodes[5], "4-5" );
        }

    public static void main(String[] args)
        {
        doLoop(NetworkTest.class, args);
        System.exit(0);
        }    
    }
