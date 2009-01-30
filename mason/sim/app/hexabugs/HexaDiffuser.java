/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.hexabugs;

import sim.engine.*;
import sim.field.grid.*;

/**
   Hexa Non-toroidal Diffuser
*/
public /*strictfp*/ class HexaDiffuser implements Steppable
    {

    DoubleGrid2D updateGrid;
    DoubleGrid2D tempGrid;
    double evaporationRate;
    double diffusionRate;

    public HexaDiffuser( final DoubleGrid2D updateGrid,
        final DoubleGrid2D tempGrid,
        final double evaporationRate,
        final double diffusionRate )
        {
        this.updateGrid = updateGrid;
        this.tempGrid = tempGrid;
        this.evaporationRate = evaporationRate;
        this.diffusionRate = diffusionRate;
        }

    public void step(SimState state)
        {
        // Let's start with the easy stuff.  We'll include some local variables because it's faster.
        // See heatbugs.Diffuser for more examples.

        //        // locals are faster than instance variables
        //        final DoubleGrid2D _valgrid = updateGrid;
        //        final DoubleGrid2D v = valgrid;  // shorter
        //        final double[][] _valgrid_field = updateGrid.field;
        //        final double[][] _valgrid2_field = tempGrid.field;
        //        final int _gridWidth = _valgrid.getWidth();
        //        final int _gridHeight = _valgrid.getHeight();
        //        final double _evaporationRate = evaporationRate;
        //        final double _diffusionRate = diffusionRate;
        //        
        //        double average;
        //        DoubleBag temp = new DoubleBag();
        //        // for each x and y position
        //        for(int x=0;x< _gridWidth;x++)
        //            for(int y=0;y< _gridHeight;y++)
        //                {
        //                // Get neighbors
        //                _valgrid.getNeighborsHexagonalDistance(x,y,1,true,temp,null,null);
        //       
        //                //Go through neighbors and compute average
        //                for( int i = 0 ; i < temp.numObjs ; i++ ) average += temp.objs[i];
        //                average /= (1+temp.numObjs);
        //                
        //                // load the new value into HexaBugs.this.valgrid2
        //                _valgrid2_field[x][y] = _evaporationRate * 
        //                    (_valgrid_field[x][y] + _diffusionRate * 
        //                     (average - _valgrid_field[x][y]));
        //                }
        
        
        
        // The problem with this approach is that getNeighborsHexagonalDistance is simple
        // and elegant but expensive.  Instead we can simply hard-code it as so:
        
        //        // locals are faster than instance variables
        //        final DoubleGrid2D _valgrid = updateGrid;
        //        final DoubleGrid2D v = valgrid;  // shorter
        //        final double[][] _valgrid_field = updateGrid.field;
        //        final double[][] _valgrid2_field = tempGrid.field;
        //        final int _gridWidth = _valgrid.getWidth();
        //        final int _gridHeight = _valgrid.getHeight();
        //        final double _evaporationRate = evaporationRate;
        //        final double _diffusionRate = diffusionRate;
        //        
        //        double average;
        //        // for each x and y position
        //        for(int x=0;x< _gridWidth;x++)
        //            for(int y=0;y< _gridHeight;y++)
        //                {
        //                average = 
        //                    (_valgrid_field[x][y] + 
        //                     _valgrid_field[v.stx(v.ulx(x,y))][v.sty(v.uly(x,y))] +
        //                     _valgrid_field[v.stx(v.urx(x,y))][v.sty(v.ury(x,y))] + 
        //                     _valgrid_field[v.stx(v.dlx(x,y))][v.sty(v.dly(x,y))] + 
        //                     _valgrid_field[v.stx(v.drx(x,y))][v.sty(v.dry(x,y))] + 
        //                     _valgrid_field[v.stx(v.upx(x,y))][v.sty(v.upy(x,y))] + 
        //                     _valgrid_field[v.stx(v.downx(x,y))][v.sty(v.downy(x,y))]) / 7.0;
        //                
        //                // load the new value into HexaBugs.this.valgrid2
        //                _valgrid2_field[x][y] = _evaporationRate * 
        //                    (_valgrid_field[x][y] + _diffusionRate * 
        //                     (average - _valgrid_field[x][y]));
        //                }
        
        
        
        
        // Now we're getting somewhere speed-wise.  But we can also eliminate the toroidal and hexagonal
        // cover functions with some work.  Boy, it's a chore though.  We include it here for fun, but
        // don't blame us if it's totally unreadable!

        
        // stolen from HeatBugs and modified for our own purposes
        // locals are faster than instance variables
        // locals are faster than instance variables
        final DoubleGrid2D _valgrid = updateGrid;
        final double[][] _valgrid_field = updateGrid.field;
        final double[][] _valgrid2_field = tempGrid.field;
        final int _gridWidth = _valgrid.getWidth();
        final int _gridHeight = _valgrid.getHeight();
        final double _evaporationRate = evaporationRate;
        final double _diffusionRate = diffusionRate;

        double average;
        
        double[] _past = _valgrid_field[_valgrid.stx(-1)];
        double[] _current = _valgrid_field[0];
        double[] _next;
        double[] _put;
        
        int yminus1;
        int yplus1;
        
        // for each x and y position
        for(int x=0;x< _gridWidth;x++)
            {
            int xplus1 = x+1;
            if( xplus1 == _gridWidth )
                xplus1 = 0;
            _next = _valgrid_field[xplus1];
            _put = _valgrid2_field[x];
            boolean xmodulo2equals0 = x%2==0;

            yminus1 = _gridHeight-1;     // initialized
            for(int y=0;y< _gridHeight;y++)
                {
                // for each neighbor of that position
                // go across top
                yplus1 = y+1;
                if( yplus1 == _gridHeight )
                    yplus1 = 0;
                if( xmodulo2equals0 )
                    {
                    average = (
                        _current[y] + // CURRENT
                        _past[yminus1] + //UL
                        _next[yminus1] + // UR
                        _past[y] + //DL
                        _next[y] + // DR
                        _current[yminus1] + // UP
                        _current[yplus1] // DOWN
                        ) / 7.0;
                    }
                else
                    {
                    average = (
                        _current[y] + // CURRENT
                        _past[y] + //UL
                        _next[y] + // UR
                        _past[yplus1] + //DL
                        _next[yplus1] + // DR
                        _current[yminus1] + // UP
                        _current[yplus1] // DOWN
                        ) / 7.0;
                    }

                // load the new value into HeatBugs.this.valgrid2
                _put[y] = _evaporationRate * 
                    (_current[y] + _diffusionRate * 
                    (average - _current[y]));

                // set y-1 to what y was "last time around"
                yminus1 = y;
                }
                
            // swap elements
            _past = _current;
            _current = _next;
            }

        updateGrid.setTo(tempGrid);
        }
    }

