/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.cto;

import sim.util.Double2D;

public /*strictfp*/ class KMeansEngine
    {

    final static double ALFA = 0.25;

    Double2D[] clusterPoints;// = new Double2D[ CooperativeObservation.NUM_AGENTS ];
    Boolean[] usable; // = new boolean[ CooperativeObservation.NUM_AGENTS ];
    Double2D[] means;// = new Double2D[ CooperativeObservation.NUM_AGENTS ];
    int[] labels;// = new int[ CooperativeObservation.NUM_TARGETS ];
    int[] n;// = new int[ CooperativeObservation.NUM_AGENTS ];
    double[] weight;// = new double[ CooperativeObservation.NUM_AGENTS ];

    CooperativeObservation co;

    public KMeansEngine( CooperativeObservation co )
        {
        this.co = co;

        clusterPoints = new Double2D[ CooperativeObservation.NUM_AGENTS ];
        usable = new Boolean[ CooperativeObservation.NUM_AGENTS ];
        means = new Double2D[ CooperativeObservation.NUM_AGENTS ];
        for( int i = 0 ; i < CooperativeObservation.NUM_AGENTS ; i++ )
            {
            clusterPoints[i] = new Double2D();
            means[i] = new Double2D();
            usable[i] = Boolean.FALSE;
            }
        labels = new int[ CooperativeObservation.NUM_TARGETS ];
        n = new int[ CooperativeObservation.NUM_AGENTS ];
        weight = new double[ CooperativeObservation.NUM_AGENTS ];
    
        Thread thread = new Thread()
            {
            public void run()
                {
                doKMeans();
                }
            };
        thread.start();
        }

    public Double2D getGoalPosition( int id )
        {
        synchronized( usable[id] )
            {
            if( usable[id].booleanValue() )
                return clusterPoints[id];
            else
                return null; // for exploration purposes
            }
        }

    public final double distanceBetweenPointsSQR( double x1, double y1, double x2, double y2 )
        {
        return (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2);
        }

    public void doKMeans()
        {
        while( true )
            {
            for( int i = 0 ; i < CooperativeObservation.NUM_AGENTS ; i++ )
                {
                weight[i] = 0;
                if( means[i].x == 0 && means[i].y == 0 )
                    {
                    clusterPoints[i] = means[i];
                    }
                means[i] = co.agentPos[i];
                }
            for( int i = 0 ; i < CooperativeObservation.NUM_TARGETS ; i++ )
                {
                int min = -1;
                double distance = -1;
                for( int j = 0 ; j < CooperativeObservation.NUM_AGENTS ; j++ )
                    {
                    double currDist = distanceBetweenPointsSQR( co.targetPos[i].x, co.targetPos[i].y,
                        co.agentPos[j].x, co.agentPos[j].y );
                    if( distance == -1 || distance > currDist )
                        {
                        min = j;
                        distance = currDist;
                        }
                    }
                labels[i] = min;
                }
            for( int i = 0 ; i < CooperativeObservation.NUM_AGENTS ; i++ )
                {
                means[i] = new Double2D( 0.0, 0.0 );
                n[i] = 0;
                }
            for( int i = 0 ; i < CooperativeObservation.NUM_TARGETS ; i++ )
                {
                if( labels[i] != -1 )
                    {
                    means[labels[i]] = new Double2D( means[labels[i]].x + co.targetPos[i].x,
                        means[labels[i]].y + co.targetPos[i].y );
                    n[labels[i]]++;
                    }
                }
            for( int i = 0 ; i < CooperativeObservation.NUM_AGENTS ; i++ )
                {
                synchronized( usable[i] )
                    {
                    if( n[i] != 0 )
                        {
                        means[i] = new Double2D( means[i].x/n[i], means[i].y/n[i] );
                        clusterPoints[i] = new Double2D( (1-ALFA)*clusterPoints[i].x + ALFA*means[i].x,
                            (1-ALFA)*clusterPoints[i].y + ALFA*means[i].y );
                        usable[i] = Boolean.TRUE;
                        }
                    else
                        {
                        usable[i] = Boolean.FALSE;
                        }
                    }
                }
            try { Thread.sleep(100); } catch (InterruptedException e) { }
            } // end while(true)
        }

    }
