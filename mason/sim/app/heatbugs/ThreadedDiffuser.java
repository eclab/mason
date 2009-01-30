/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.heatbugs;
import sim.engine.*;
import sim.field.grid.*;

/** A dual-threaded version of Diffuser for use on machines with two processors.
    Holds a ParallelSequence which in turn holds two dummy Steppables which each
    call diffuse(...) with different start and end values.  That way basically we
    split the array diffusion up among two processors, one taking the first half
    of the array and one taking up the second half.  Because Diffuser takes up
    nearly all our time, this results in a dramatic increase in speed on a
    dual-processor machine. */

public /*strictfp*/ class ThreadedDiffuser implements Steppable
    {
    public ParallelSequence diffusers;
        
    public ThreadedDiffuser()
        {
        diffusers = new ParallelSequence(new Steppable[]
            {
            new Steppable ()
                { 
                public void step(SimState state) 
                    {
                    // diffuse top half of field
                    HeatBugs heatbugs = (HeatBugs)state;
                    int _gridWidth = heatbugs.valgrid.getWidth();  // read-only, so threadsafe with other one
                    diffuse(heatbugs, 0, _gridWidth/2);
                    }
                },
            new Steppable ()
                {
                public void step(SimState state) 
                    {
                    // diffuse bottom half of field
                    HeatBugs heatbugs = (HeatBugs)state;
                    int _gridWidth = heatbugs.valgrid.getWidth();  // read-only, so threadsafe with other one
                    diffuse(heatbugs, _gridWidth/2, _gridWidth);
                    }
                }
            });
        }
        
        
    public void step(SimState state)
        {
        diffusers.step(state);
                
        // copy HeatBugs.this.valgrid2 to HeatBugs.this.valgrid
        HeatBugs heatbugs = (HeatBugs)state;
        heatbugs.valgrid.setTo(heatbugs.valgrid2);
        }
        
        
    /** Diffuse heatbugs.valgrid.field[start...end] not including end */
        
    // this code is confusing.  See Diffuser.java for other less confusing examples
    // and for an explanation for why the code looks the way it does.
        
    void diffuse(HeatBugs heatbugs, int start, int end)
        {
        // locals are faster than instance variables
        final DoubleGrid2D _valgrid = heatbugs.valgrid;
        final double[][] _valgrid_field = heatbugs.valgrid.field;
        final double[][] _valgrid2_field = heatbugs.valgrid2.field;
        final int _gridHeight = _valgrid.getHeight();
        final double _evaporationRate = heatbugs.evaporationRate;
        final double _diffusionRate = heatbugs.diffusionRate;

        double average;
        
        double[] _past = _valgrid_field[_valgrid.stx(start-1)];
        double[] _current = _valgrid_field[start];
        double[] _next;
        double[] _put;
        
        int yminus1;
        int yplus1;
        
        // for each x and y position
        for(int x=start ; x< end ;x++)
            {
            _next = _valgrid_field[_valgrid.stx(x+1)];
            _put = _valgrid2_field[_valgrid.stx(x)];
            
            yminus1 = _valgrid.sty(-1);  // initialized
            for(int y=0;y< _gridHeight;y++)
                {
                // for each neighbor of that position
                // go across top
                yplus1 = _valgrid.sty(y+1);
                average = (_past[yminus1] + _past[y] + _past[yplus1] +
                    _current[yminus1] + _current[y] + _current[yplus1] +
                    _next[yminus1] + _next[y] + _next[yplus1]) / 9.0;

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

