/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.hexabugs;
import sim.engine.*;
import sim.field.grid.*;

/** A dual-threaded version of Diffuser for use on machines with two processors.
    Holds a ParallelSequence which in turn holds two dummy Steppables which each
    call diffuse(...) with different start and end values.  That way basically we
    split the array diffusion up among two processors, one taking the first half
    of the array and one taking up the second half.  Because Diffuser takes up
    nearly all our time, this results in a dramatic increase in speed on a
    dual-processor machine. */

public /*strictfp*/ class ThreadedHexaDiffuser implements Steppable
    {
    public ParallelSequence diffusers;
        
    DoubleGrid2D updateGrid;
    DoubleGrid2D tempGrid;
    double evaporationRate;
    double diffusionRate;

    public ThreadedHexaDiffuser( final DoubleGrid2D updateGrid,
        final DoubleGrid2D tempGrid,
        final double evaporationRate,
        final double diffusionRate )
        {
        this.updateGrid = updateGrid;
        this.tempGrid = tempGrid;
        this.evaporationRate = evaporationRate;
        this.diffusionRate = diffusionRate;
        diffusers = new ParallelSequence(new Steppable[]
            {
            new Steppable ()
                { 
                public void step(SimState state) 
                    {
                    // diffuse top half of field
                    HexaBugs hexabugs = (HexaBugs)state;
                    int _gridWidth = hexabugs.valgrid.getWidth();  // read-only, so threadsafe with other one
                    diffuse(hexabugs, 0, _gridWidth/2);
                    }
                },
            new Steppable ()
                {
                public void step(SimState state) 
                    {
                    // diffuse bottom half of field
                    HexaBugs hexabugs = (HexaBugs)state;
                    int _gridWidth = hexabugs.valgrid.getWidth();  // read-only, so threadsafe with other one
                    diffuse(hexabugs, _gridWidth/2, _gridWidth);
                    }
                }
            });
        }
        
        
    public void step(SimState state)
        {
        diffusers.step(state);
                
        // copy HexaBugs.this.valgrid2 to HexaBugs.this.valgrid
        HexaBugs hexabugs = (HexaBugs)state;
        hexabugs.valgrid.setTo(hexabugs.valgrid2);
        }
        
        
    /** Diffuse hexabugs.valgrid.field[start...end] not including end */
        
    // this code is utterly incomprehenxible.  See HexaDiffuser.java for other less confusing examples
    // and for an explanation for why the code looks the way it does.
        
    void diffuse(HexaBugs hexabugs, int start, int end)
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
        double[] _past = _valgrid_field[_valgrid.stx(start-1)];
        double[] _current = _valgrid_field[start];
        double[] _next;
        double[] _put;
        
        int yminus1;
        int yplus1;
        
        // for each x and y position
        for(int x=start;x<end;x++)
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
        }

    }

