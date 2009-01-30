/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.heatbugs;
import sim.engine.*;
import sim.field.grid.*;

/** This agent decreases evaporates and diffuses all the heat at each time step.   See the comments in Diffuser.java for a tutorial on how to speed up Java many-fold in classes such as Diffuser. */

public /*strictfp*/ class Diffuser implements Steppable
    {
    public void step(SimState state)
        {
        HeatBugs heatbugs = (HeatBugs)state;
        
        
        // Donald Knuth said that premature optimization is the root of all evil.
        // Well, not so in HeatBugs, where a huge proportion of time is spent just
        // in one single method.
        //
        // This method is where HeatBugs spends 90% of its time, so it's worthwhile
        // optimizing the daylights out of it.
        
        // Let's go through some variations of the diffusion portion (dumping the
        // evaporated, diffused stuff into heatbugs.valgrid2), starting with the
        // simplest and slowest, and moving to the fastest variations.

        // We begin with the naive way to do it: double-loop through each of the
        // grid values.  For each grid value, gather the eight neighbor positions
        // around that grid value, and compute the average of them.  Note that the
        // getNeighborsMaxDistance function is set to include toroidal boundaries as well.
        // Then set (in the new grid 'valgrid2') the new value to include some evaporation,
        // plus some of this diffused average.
                
        //         double average;
        //         sim.util.IntBag xNeighbors = new sim.util.IntBag(9);
        //         sim.util.IntBag yNeighbors = new sim.util.IntBag(9);
                
        //         for(int x=0;x< heatbugs.valgrid.getWidth();x++)
        //             for(int y=0;y< heatbugs.valgrid.getHeight();y++)
        //                 {
        //                 average = 0.0;
        //                 // get all the neighbors
        //                 getNeighborsMaxDistance(x,y,1,true,xNeighbors,yNeighbors);
                
        //                 // for each neighbor...
        //                 for(int i = 0 ; i < xNeighbors.numObjs; i++)
        //                         {
        //                         // compute average
        //                         average += heatbugs.valgrid.get(xNeighbors.get(i), yNeighbors.get(i));
        //                         }
        //                 average /= 9.0;
                
        //                 // load the new value into HeatBugs.this.valgrid2
        //                 heatbugs.valgrid2.set(x,y, heatbugs.evaporationRate * 
        //                     (heatbugs.valgrid.get(x,y) + heatbugs.diffusionRate * 
        //                      (average - heatbugs.valgrid.get(x,y))));
        //                 }


        // ----------------------------------------------------------------------
        // It turns out that this is quite slow for a variety of reasons.  First, 
        // the getNeighborsMaxDistance loads and stores integers into a large array, 
        // then clears them out, all so we can just do some simple computation with 
        // them.  Since we already know exactly what locations we want to grab, why 
        // are we asking the system to do it for us?  We can do it much faster by 
        // doing a double for-loop over the nine locations directly.  To handle 
        // toroidal stuff, we use the tx() and ty() functions which perform 
        // wrap-around computations for us automagically.
                
        // Second, we're using get() and set() functions on grids.  This is fine -- 
        // it's not too slow -- but it's a bit faster to just access the underlying 
        // arrays directly, which is acceptable in MASON.  So combining these two, 
        // we'll get a somewhat faster, and slightly less naive, approach:
                
        //         double average;
                
        //         for(int x=0;x< heatbugs.valgrid.getWidth();x++)
        //             for(int y=0;y< heatbugs.valgrid.getHeight();y++)
        //                 {
        //                 average = 0.0;
        //                 // for each neighbor of that position
        //                 for(int dx=-1; dx< 2; dx++)
        //                     for(int dy=-1; dy<2; dy++)
        //                         {
        //                         // compute the toroidal <x,y> position of the neighbor
        //                         int xx = heatbugs.valgrid.tx(x+dx);
        //                         int yy = heatbugs.valgrid.ty(y+dy);
                                                                
        //                         // compute average
        //                         average += heatbugs.valgrid.field[xx][yy];
        //                         }
        //                 average /= 9.0;
                        
        //                 // load the new value into HeatBugs.this.valgrid2
        //                 heatbugs.valgrid2.field[x][y] = heatbugs.evaporationRate * 
        //                     (heatbugs.valgrid.field[x][y] + heatbugs.diffusionRate * 
        //                      (average - heatbugs.valgrid.field[x][y]));
        //                 }


        // ----------------------------------------------------------------------
        // The first thing to note is that tx and ty are slower than stx and sty.
        // You use tx and ty when you expect to have to wrap toroidal values that are
        // way out there.  stx and sty are fine if your toroidal values are never off
        // the board more than one width's worth in the x direction or one height's worth 
        // in the y direction.
        //
        // Also, you don't want to compute getWidth() every loop, and CERTAINLY don't want
        // to compute getHeight() width times every loop!  So now we can write:

        //         double average;
        //         final int _gridWidth = heatbugs.valgrid.getWidth();
        //         final int _gridHeight = heatbugs.valgrid.getHeight();
                
        //         for(int x=0;x< _gridWidth;x++)
        //             for(int y=0;y< _gridHeight;y++)
        //                 {
        //                 average = 0.0;
        //                 // for each neighbor of that position
        //                 for(int dx=-1; dx< 2; dx++)
        //                     for(int dy=-1; dy<2; dy++)
        //                         {
        //                         // compute the toroidal <x,y> position of the neighbor
        //                         int xx = heatbugs.valgrid.stx(x+dx);
        //                         int yy = heatbugs.valgrid.sty(y+dy);
                                                                
        //                         // compute average
        //                         average += heatbugs.valgrid.field[xx][yy];
        //                         }
        //                 average /= 9.0;
                        
        //                 // load the new value into HeatBugs.this.valgrid2
        //                 heatbugs.valgrid2.field[x][y] = heatbugs.evaporationRate * 
        //                     (heatbugs.valgrid.field[x][y] + heatbugs.diffusionRate * 
        //                      (average - heatbugs.valgrid.field[x][y]));
        //                 }



        // ----------------------------------------------------------------------
        // We set _gridWidth and _gridHeight to be final mostly to remind us that
        // they don't change.  Although Java COULD take advantage of
        // them being final to improve optimization, right now it doesn't.  The
        // final declaration is stripped out when compiling to bytecode.  Oh well!
        //
        // Okay, so what's wrong with the new incarnation?  Well, lots of variables
        // are being accessed via instances (like heatbugs.evaporationRate and
        // heatbugs.valgrid.field).  Instance data lookups, even for data in YOUR
        // instance, is always much slower than locals.  Let's make some locals.
                
        //         // locals are faster than instance variables
        //         final DoubleGrid2D _valgrid = heatbugs.valgrid;
        //         final double[][] _valgrid_field = heatbugs.valgrid.field;
        //         final double[][] _valgrid2_field = heatbugs.valgrid2.field;
        //         final int _gridWidth = heatbugs.valgrid.getWidth();
        //         final int _gridHeight = heatbugs.valgrid.getHeight();
        //         final double _evaporationRate = heatbugs.evaporationRate;
        //         final double _diffusionRate = heatbugs.diffusionRate;
                
        //         double average;
        //         for(int x=0;x< _gridWidth;x++)
        //             for(int y=0;y< _gridHeight;y++)
        //                 {
        //                 average = 0.0;
        //                 // for each neighbor of that position
        //                 for(int dx=-1; dx< 2; dx++)
        //                     for(int dy=-1; dy<2; dy++)
        //                         {
        //                         // compute the toroidal <x,y> position of the neighbor
        //                         int xx = _valgrid.stx(x+dx);
        //                         int yy = _valgrid.sty(y+dy);
                                                                
        //                         // compute average
        //                         average += _valgrid_field[xx][yy];
        //                         }
        //                 average /= 9.0;
                        
        //                 // load the new value into HeatBugs.this.valgrid2
        //                 _valgrid2_field[x][y] = _evaporationRate * 
        //                     (_valgrid_field[x][y] + _diffusionRate * 
        //                      (average - _valgrid_field[x][y]));
        //                 }
                        
                        
        // ----------------------------------------------------------------------
        // That was a BIG jump in speed!  Now we're getting somewhere!
        // Next, Java's array lookups work like this.  If you say foo[x][y], it
        // first looks up the array value foo[x] (which is a one-dimensional array
        // in of itself).  Then it looks up that array[y].  This is TWO array bounds
        // checks and you have to load the arrays into cache.  There's a significant
        // hit here.  We can get an almost 2x speedup if we keep the arrays around
        // that we know we need.
        //
        // We'll do it as follows.  We keep around _valgrid[x] and _valgrid2[x] and
        // just look up the [y] values in them when we need to.  Also since we're
        // diffusing, we need the _valgrid[x-1] and _valgrid[x+1] rows also.  We'll
        // call these:  _valgrid[x]     ->      _current
        //              _valgrid[x-1]   ->      _past
        //              _valgrid[x+1]   ->      _next
        //              _valgrid2[x]    ->      _put
        //
        // Note that next iteration around, _current becomes _past and _next becomes
        // _current.  So we only have to look up the new _next and the new _put.
        // We'll take advantage of that too.
            
        // Last, we'll get rid of the inner for-loop and just hand-code the nine
        // lookups for the diffuser.
                
                
                
        //         // locals are faster than instance variables
        //         final DoubleGrid2D _valgrid = heatbugs.valgrid;
        //         final double[][] _valgrid_field = heatbugs.valgrid.field;
        //         final double[][] _valgrid2_field = heatbugs.valgrid2.field;
        //         final int _gridWidth = _valgrid.getWidth();
        //         final int _gridHeight = _valgrid.getHeight();
        //         final double _evaporationRate = heatbugs.evaporationRate;
        //         final double _diffusionRate = heatbugs.diffusionRate;

        //         double average;
                
        //         double[] _past = _valgrid_field[_valgrid.stx(-1)];
        //         double[] _current = _valgrid_field[0];
        //         double[] _next;
        //         double[] _put;
                
        //         // for each x and y position
        //         for(int x=0;x< _gridWidth;x++)
        //             {
        //             _next = _valgrid_field[_valgrid.stx(x+1)];
        //             _put = _valgrid2_field[_valgrid.stx(x)];
                    
        //             for(int y=0;y< _gridHeight;y++)
        //                 {
        //                 // for each neighbor of that position
        //                 // go across top
        //                 average = (_past[_valgrid.sty(y-1)] + _past[_valgrid.sty(y)] + _past[_valgrid.sty(y+1)] +
        //                            _current[_valgrid.sty(y-1)] + _current[_valgrid.sty(y)] + _current[_valgrid.sty(y+1)] +
        //                            _next[_valgrid.sty(y-1)] + _next[_valgrid.sty(y)] + _next[_valgrid.sty(y+1)]) / 9.0;

        //                 // load the new value into HeatBugs.this.valgrid2
        //                 _put[y] = _evaporationRate * 
        //                     (_current[y] + _diffusionRate * 
        //                      (average - _current[y]));
        //                 }
                        
        //             // swap elements
        //             _past = _current;
        //             _current = _next;
        //             }


        // ----------------------------------------------------------------------
        // Well, that bumped us up a lot!  But we can double our speed yet again,
        // simply by cutting down on the number of times we call sty().  It's called
        // NINE TIMES for each stx().  Note that we even call _valgrid.sty(y) when
        // we _know_ that y is always within the toroidal range, that's an easy fix;
        // just replace _valgrid.sty(y) with y.  We can also replace _valgrid.sty(y-1)
        // and _valgrid.sty(y+1) with variables which we have precomputed so they're not
        // each recomputed three times (Java's optimizer isn't very smart).  Last we
        // can avoid computing _valgrid.sty(y-1) at all (except once) -- just set it
        // to whatever y was last time.
        //
        // The resultant code is below.  We could speed this up a bit more, avoiding
        // calls to sty(y+1) and reducing unnecessary calls to stx, but it won't buy
        // us the giant leaps we're used to by now.  We could also replace the stx and sty
        // with our own functions where we pass in the width and height as local variables,
        // and that's actually a fair bit faster also, but again, it's not a giant improvement.

        // locals are faster than instance variables
        final DoubleGrid2D _valgrid = heatbugs.valgrid;
        final double[][] _valgrid_field = heatbugs.valgrid.field;
        final double[][] _valgrid2_field = heatbugs.valgrid2.field;
        final int _gridWidth = _valgrid.getWidth();
        final int _gridHeight = _valgrid.getHeight();
        final double _evaporationRate = heatbugs.evaporationRate;
        final double _diffusionRate = heatbugs.diffusionRate;

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
        _valgrid.setTo(heatbugs.valgrid2);
        }
    }

