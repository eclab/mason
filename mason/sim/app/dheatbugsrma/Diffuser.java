/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.dheatbugsrma;
import sim.engine.*;
import sim.field.grid.*;

import mpi.*;

/** This agent decreases evaporates and diffuses all the heat at each time step.   See the comments in Diffuser.java for a tutorial on how to speed up Java many-fold in classes such as Diffuser. */

public class Diffuser implements Steppable {
    private static final long serialVersionUID = 1;

    int N = 1, S = 2, E = 4, W = 8;
    int NE = N | E, NW = N | W, SE = S | E, SW = S | W;

    // public void step(SimState state) {
    //     HeatBugs heatbugs = (HeatBugs)state;

    //     final double _evaporationRate = heatbugs.evaporationRate;
    //     final double _diffusionRate = heatbugs.diffusionRate;

    //     double average;

    //     final int xl = heatbugs.px * heatbugs.cell_width;
    //     final int xu = (heatbugs.px + 1) * heatbugs.cell_width;
    //     final int yl = heatbugs.py * heatbugs.cell_height;
    //     final int yu = (heatbugs.py + 1) * heatbugs.cell_height;

    //     for (int x = xl; x < xu; x++) {
    //         for (int y = yl; y < yu; y++) {
    //             average = 0.0;
    //             for (int xn = x - 1; xn <= x + 1; xn++)
    //                 for (int yn = y - 1; yn <= y + 1; yn++)
    //                     average += heatbugs.valgrid.get(xn, yn);
    //             average /= 9.0;
    //             heatbugs.valgrid2.set(x, y,  heatbugs.valgrid.get(x, y) * _evaporationRate + (average - heatbugs.valgrid.get(x, y)) * _diffusionRate);
    //         }
    //     }

    //     DDoubleGrid2D temp = heatbugs.valgrid;
    //     heatbugs.valgrid = heatbugs.valgrid2;
    //     heatbugs.valgrid2 = temp;

    //     return;
    // }

    public void step(SimState state) {
        DHeatBugsRMA heatbugs = (DHeatBugsRMA)state;

        // locals are faster than instance variables
        final DDoubleGrid2DRMA _valgrid = heatbugs.valgrid;
        final double[][] _valgrid_field = heatbugs.valgrid.field;
        final double[][] _valgrid2_field = heatbugs.valgrid2.field;

        final int _gridWidth = heatbugs.cell_width;
        final int _gridHeight = heatbugs.cell_height;
        final int xl = heatbugs.px * heatbugs.cell_width;
        final int xu = (heatbugs.px + 1) * heatbugs.cell_width;
        final int yl = heatbugs.py * heatbugs.cell_height;
        final int yu = (heatbugs.py + 1) * heatbugs.cell_height;

        final double _evaporationRate = heatbugs.evaporationRate;
        final double _diffusionRate = heatbugs.diffusionRate;

        double average;

        double avg_fc, avg_lc;

        double[] _past = _valgrid.m[N].getRow(0);
        double[] _current = _valgrid_field[0];
        double[] _next;
        double[] _put;

        int yminus1;
        int yplus1;

        // for each x and y position
        for (int x = 0; x < _gridWidth; x++) {
            _next = x + 1 < _gridWidth ? _valgrid_field[x + 1] : _valgrid.m[S].getRow(0);
            _put = _valgrid2_field[x];

            // first col and last col
            if (x == 0) {
                avg_fc = (_valgrid.m[NW].get(0, 0) + _past[0] + _past[1] + 
                            _valgrid.m[W].get(x, 0) + _current[0] + _current[1] +
                            _valgrid.m[W].get(x+1, 0) + _next[0] + _next[1] ) / 9.0;
                avg_lc = (_valgrid.m[NE].get(0, 0) + _past[0] + _past[_gridHeight - 1] + 
                            _valgrid.m[E].get(x, 0) + _current[0] + _current[_gridHeight - 1] +
                            _valgrid.m[E].get(x+1, 0) + _next[0] + _next[_gridHeight - 1] ) / 9.0;
            } else if (x == _gridWidth - 1) {
                avg_fc = (_valgrid.m[W].get(x-1, 0) + _past[0] + _past[1] + 
                            _valgrid.m[W].get(x, 0) + _current[0] + _current[1] +
                            _valgrid.m[SW].get(0, 0) + _next[0] + _next[1] ) / 9.0;
                avg_lc = (_valgrid.m[E].get(_gridWidth - 2, 0) + _past[0] + _past[_gridHeight - 1] + 
                            _valgrid.m[E].get(0, 0) + _current[0] + _current[_gridHeight - 1] +
                            _valgrid.m[SE].get(0, 0) + _next[0] + _next[_gridHeight - 1] ) / 9.0;
            } else {
                avg_fc = (_valgrid.m[W].get(x-1, 0) + _past[0] + _past[1] + 
                            _valgrid.m[W].get(x, 0) + _current[0] + _current[1] +
                            _valgrid.m[W].get(x+1, 0) + _next[0] + _next[1] ) / 9.0;
                avg_lc = (_valgrid.m[E].get(x-1, 0) + _past[0] + _past[_gridHeight - 1] + 
                            _valgrid.m[E].get(x, 0) + _current[0] + _current[_gridHeight - 1] +
                            _valgrid.m[E].get(x+1, 0) + _next[0] + _next[_gridHeight - 1] ) / 9.0;
            }
            _put[0] = _evaporationRate * (_current[0] + _diffusionRate * (avg_fc - _current[0]));
            _put[_gridHeight - 1] = _evaporationRate * (_current[_gridHeight - 1] + _diffusionRate * (avg_lc - _current[_gridHeight - 1]));

            // rest - pure local
            yminus1 = 0;
            for (int y = 1; y < _gridHeight-1; y++) {
                // for each neighbor of that position
                // go across top
                yplus1 = y + 1;
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

        DDoubleGrid2DRMA temp = heatbugs.valgrid;
        heatbugs.valgrid = heatbugs.valgrid2;
        heatbugs.valgrid2 = temp;

        return;
    }
}

