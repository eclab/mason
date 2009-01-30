/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.antsforage;

import sim.engine.*;
import sim.field.grid.*;

public /*strictfp*/ class Diffuser implements Steppable
    {

    DoubleGrid2D updateGrid;
    DoubleGrid2D tempGrid;
    double evaporationRate;
    double diffusionRate;

    public Diffuser( final DoubleGrid2D updateGrid,
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
        // stolen from HeatBugs and modified for our own purposes
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
            _next = _valgrid_field[_valgrid.stx(x+1)];
            _put = _valgrid2_field[_valgrid.stx(x)];
            
            yminus1 = _valgrid.sty(-1);     // initialized
            for(int y=0;y< _gridHeight;y++)
                {
                // for each neighbor of that position
                // go across top
                yplus1 = _valgrid.sty(y+1);
                average = (_past[yminus1] + _past[y] + _past[yplus1] +
                    _current[yminus1] + _current[y] + _current[yplus1] +
                    _next[yminus1] + _next[y] + _next[yplus1]) / 9.0;

                // load the new value into HeatBugs.this.valgrid2
                _put[y] = (1.0-_evaporationRate) * 
                    (_current[y] + _diffusionRate * 
                    (average - _current[y]));

                // set y-1 to what y was "last time around"
                yminus1 = y;
                }
                
            // swap elements
            _past = _current;
            _current = _next;
            }




        // ----------------------------------------------------------------------
        // If you have a multiprocessor machine, you can speed this up further by
        // dividing the work among two processors.  We do that over in ThreadedDiffuser.java
        //
        // You can also avoid some of the array bounds checks by using linearized
        // double arrays -- that is, using a single array but computing the double
        // array location yourself.  That way you only have one bounds check instead
        // of two.  This is how, for example, Repast does it.  This is certainly a
        // little faster than two checks.  We use a two-dimensional array because a
        // linearized array class is just too cumbersome to use in Java right now, 
        // what with all the get(x,y) and set(x,y,v) instead of just saying foo[x][y].  
        // Plus it turns out that for SMALL (say 100x100) arrays, the double array is 
        // actually *faster* because of cache advantages.
        //
        // At some point in the future Java's going to have to fix the lack of true
        // multidimensional arrays.  It's a significant speed loss.  IBM has some proposals
        // in the works but it's taking time.  However their proposals are for array classes.
        // So allow me to suggest how we can do a little syntactic sugar to make that prettier.
        // The array syntax for multidimensional arrays should be foo[x,y,z] and for
        // standard Java arrays it should be foo[x][y][z].  This allows us to mix the two:
        // a multidimensional array of Java arrays for example:  foo[x,y][z].  Further we
        // should be allowed to linearize a multidimensional array, accessing all the elements
        // in row-major order.  The syntax for a linearized array simply has empty commas:
        // foo[x,,]
        
        
        // oh yeah, we have one last step.
        
        // now finally copy HeatBugs.this.valgrid2 to HeatBugs.this.valgrid, and we're done
        _valgrid.setTo(tempGrid);
        }
    }

