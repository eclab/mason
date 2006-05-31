/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.hexabugs;
import sim.field.grid.*;
import sim.util.*;
import sim.engine.*;

public /*strictfp*/ class HexaBug implements Steppable
    {
    public double idealTemp;
    public double getIdealTemperature() { return idealTemp; }
    public void setIdealTemperature( double t ) { idealTemp = t; }

    public double heatOutput;
    public double getHeatOutput() { return heatOutput; }
    public void setHeatOutput( double t ) { heatOutput = t; }

    public double maxHeat;
    public double getMaximumHeat() { return maxHeat; }
    public void setMaximumHeat( double t ) { maxHeat = t; }

    public double randomMovementProbability;
    public double getRandomMovementProbability() { return randomMovementProbability; }
    public void setRandomMovementProbability( double t ) { randomMovementProbability = t; }
    
    public HexaBug( double idealTemp, double heatOutput, double maxHeat, double randomMovementProbability) 
        {
        this.heatOutput = heatOutput;
        this.idealTemp = idealTemp;
        this.maxHeat = maxHeat;
        this.randomMovementProbability = randomMovementProbability;
        }
        
    public void addHeat(final DoubleGrid2D grid, final int x, final int y, final double Heat)
        {
        grid.field[x][y] += Heat;
        if (grid.field[x][y] > maxHeat) grid.field[x][y] = maxHeat;
        }

    public void step( final SimState state )
        {
        HexaBugs hb = (HexaBugs)state;
        final DoubleBag neighVal = hb.neighVal;
        final IntBag neighX = hb.neighX;
        final IntBag neighY = hb.neighY;
        
        Int2D location = hb.buggrid.getObjectLocation(this);
        int myx = location.x;
        int myy = location.y;
        
        final int START=-1;
        int bestx = START;
        int besty = 0;

        hb.valgrid.getNeighborsHexagonalDistance(myx,myy,1,true,neighVal,neighX,neighY);

        if (state.random.nextBoolean(randomMovementProbability))  // go to random place
            {
            final int temp_random = state.random.nextInt(neighX.numObjs);
            bestx = neighX.objs[temp_random];
            besty = neighY.objs[temp_random];
            }
        else if( hb.valgrid.field[myx][myy] > idealTemp )  // go to coldest place
            {
            for( int i = 0 ; i < neighX.numObjs ; i++ )
                if( neighX.objs[i]!=myx || neighY.objs[i]!=myy )
                    {
                    
                    if (bestx==START ||
                        (neighVal.objs[i] < hb.valgrid.field[bestx][besty]) ||
                        (neighVal.objs[i] == hb.valgrid.field[bestx][besty] && state.random.nextBoolean()))  // not uniform, but enough to break up the go-up-and-to-the-left syndrome
                        { bestx = (int)(neighX.objs[i]); besty = (int)(neighY.objs[i]); }
                    }
            }
        else if ( hb.valgrid.field[myx][myy] < idealTemp )  // go to warmest place
            {
            for( int i = 0 ; i < neighX.numObjs ; i++ )
                if( neighX.objs[i]!=myx || neighY.objs[i]!=myy )
                    {
                    if (bestx==START || 
                        (neighVal.objs[i] > hb.valgrid.field[bestx][besty]) ||
                        (neighVal.objs[i] > hb.valgrid.field[bestx][besty] && state.random.nextBoolean()))  // not uniform, but enough to break up the go-up-and-to-the-left syndrome
                        { bestx = (int)(neighX.objs[i]); besty = (int)(neighY.objs[i]); }
                    }
            }
        else 
            {
            bestx = myx;
            besty = myy;
            }

        hb.buggrid.setObjectLocation(this,bestx,besty);
        addHeat(hb.valgrid,bestx,besty,heatOutput);
        }

    }
