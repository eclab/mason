/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.heatbugs;
import sim.field.grid.*;
import sim.util.*;
import sim.engine.*;

public /*strictfp*/ class HeatBug implements Steppable
    {
    public double idealTemp;
    public double getIdealTemperature() { return idealTemp; }
    public void setIdealTemperature( double t ) { idealTemp = t; }

    public double heatOutput;
    public double getHeatOutput() { return heatOutput; }
    public void setHeatOutput( double t ) { heatOutput = t; }

    public double randomMovementProbability;
    public double getRandomMovementProbability() { return randomMovementProbability; }
    public void setRandomMovementProbability( double t ) { if (t >= 0 && t <= 1) randomMovementProbability = t; }
    public Object domRandomMovementProbability() { return new Interval(0.0,1.0); }
    
    public HeatBug( double idealTemp, double heatOutput, double randomMovementProbability) 
        {
        this.heatOutput = heatOutput;
        this.idealTemp = idealTemp;
        this.randomMovementProbability = randomMovementProbability;
        }
        
    public void addHeat(final DoubleGrid2D grid, final int x, final int y, final double heat)
        {
        grid.field[x][y] += heat;
        if (grid.field[x][y] > HeatBugs.MAX_HEAT) grid.field[x][y] = HeatBugs.MAX_HEAT;
        }
        
    public void step( final SimState state )
        {
        HeatBugs hb = (HeatBugs)state;
        
        Int2D location = hb.buggrid.getObjectLocation(this);
        int myx = location.x;
        int myy = location.y;
        
        final int START=-1;
        int bestx = START;
        int besty = 0;
        
        if (state.random.nextBoolean(randomMovementProbability))  // go to random place
            {
            bestx = hb.buggrid.stx(state.random.nextInt(3) - 1 + myx);  // toroidal
            besty = hb.buggrid.sty(state.random.nextInt(3) - 1 + myy);  // toroidal
            }
        else if( hb.valgrid.field[myx][myy] > idealTemp )  // go to coldest place
            {
            for(int x=-1;x<2;x++)
                for (int y=-1;y<2;y++)
                    if (!(x==0 && y==0))
                        {
                        int xx = hb.buggrid.stx(x + myx);    // toroidal
                        int yy = hb.buggrid.sty(y + myy);       // toroidal
                        if (bestx==START ||
                            (hb.valgrid.field[xx][yy] < hb.valgrid.field[bestx][besty]) ||
                            (hb.valgrid.field[xx][yy] == hb.valgrid.field[bestx][besty] && state.random.nextBoolean()))  // not uniform, but enough to break up the go-up-and-to-the-left syndrome
                            { bestx = xx; besty = yy; }
                        }
            }
        else if ( hb.valgrid.field[myx][myy] < idealTemp )  // go to warmest place
            {
            for(int x=-1;x<2;x++)
                for (int y=-1;y<2;y++)
                    if (!(x==0 && y==0))
                        {
                        int xx = hb.buggrid.stx(x + myx);    // toroidal
                        int yy = hb.buggrid.sty(y + myy);       // toroidal
                        if (bestx==START || 
                            (hb.valgrid.field[xx][yy] > hb.valgrid.field[bestx][besty]) ||
                            (hb.valgrid.field[xx][yy] == hb.valgrid.field[bestx][besty] && state.random.nextBoolean()))  // not uniform, but enough to break up the go-up-and-to-the-left syndrome
                            { bestx = xx; besty = yy; }
                        }
            }
        else            // stay put
            {
            bestx = myx;
            besty = myy;
            }

        hb.buggrid.setObjectLocation(this,bestx,besty);
        addHeat(hb.valgrid,bestx,besty,heatOutput);
        }

    }
