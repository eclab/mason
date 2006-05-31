/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.tutorial1and2;

import sim.engine.*;
import sim.field.grid.*;

public class CA implements Steppable
    {
    // the width and height will change later
    public IntGrid2D tempGrid = new IntGrid2D(0,0);

    public void step(SimState state)
        {
        Tutorial1 tut = (Tutorial1)state;
        // first copy the grid into tempGrid
        tempGrid.setTo(tut.grid);
        
        // now apply the Game of Life!
        
        // for each cell...
        int count;
        int width = tempGrid.getWidth();
        int height = tempGrid.getHeight();
        for(int x=0;x<width;x++)
            for(int y=0;y<height;y++)
                {
                count = 0;
                // count the number of neighbors around the cell,
                // and for good measure include the cell itself
                for(int dx = -1; dx < 2; dx++)
                    for(int dy = -1; dy < 2; dy++)
                        count += tempGrid.field[tempGrid.stx(x+dx)][tempGrid.sty(y+dy)];

                // if the count is 2 or less, or 5 or higher, the cell dies
                // else if the count is 3 exactly, a dead cell becomes live again
                // else the cell stays as it is
                        
                if (count <= 2 || count >= 5)  // dead
                    tut.grid.field[x][y] = 0;
                else if (count == 3) // life
                    tut.grid.field[x][y] = 1;
                }
        }
    }
