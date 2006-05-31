/*
  Copyright 2006 by Daniel Kuebrich
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.lightcycles;
import sim.engine.*;
import sim.field.grid.*;

public class LightCycles extends SimState
    {
    public int gridHeight;
    public int gridWidth;
    public int cycleCount;
    
    // The intGrid holds the walls drawn behind the cycles...
    public IntGrid2D grid;
    // while the sparsegrid holds the cycles themselves.
    public SparseGrid2D cycleGrid;

    /** Creates a LightCycles simulation with the given random number seed. */
    public LightCycles(long seed)
        {
        this(seed, 100, 100, 10);
        }
        
    public LightCycles(long seed, int width, int height, int count)
        {
        super(seed);
        gridWidth = width; gridHeight = height; cycleCount = count;
        createGrids();
        }

    protected void createGrids()
        {
        grid = new IntGrid2D(gridWidth, gridHeight,0);
        cycleGrid = new SparseGrid2D(gridWidth, gridHeight);
        }
    
    /** Resets and starts a simulation */
    public void start()
        {
        super.start();  // clear out the schedule
        
        // make new grids
        createGrids();

        // Create the cycles, add to both grid and schedule
        for(int x=0;x<cycleCount;x++)
            {
            Cycle c = new Cycle(x+1, random.nextInt(4)+1);
            cycleGrid.setObjectLocation(c, random.nextInt(gridWidth), random.nextInt(gridHeight));
            c.stopper = schedule.scheduleRepeating(c);
            }
        }
    
    public static void main(String[] args)
        {
        doLoop(LightCycles.class, args);
        System.exit(0);
        }    
    }
