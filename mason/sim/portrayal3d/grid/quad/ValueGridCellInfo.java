/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.portrayal3d.grid.quad;
import sim.field.grid.*;

/**
 * Used by ValueGrid2DPortrayal3D to send needed value information to
 * underlying QuadPortrayals.  Specifically, ValueGridCellInfo contains
 * the x,y,z dimensions of the current value point, plus a value() method
 * which returns the current value at that point.
 *
 * @author Catalin Gabriel Balan
 */
public class ValueGridCellInfo {
    /* This could be Grid2D or Grid3D */
    final Object grid;
    /* this is equivalent to grid instanceof Grid3D, but cached */
    //    final public boolean is3D;
    public int x = 0;
    public int y = 0;
    public int z = 0;
    
    public ValueGridCellInfo(Grid2D g)
        {
        grid = g;
        /*
          if(g instanceof IntGrid2D)
          grid = new IntGrid2DW((IntGrid2D)g);
          else
          grid = new DoubleGrid2DW((DoubleGrid2D)g);
          is3D = false;
        */
        } 
        
    public ValueGridCellInfo(Grid3D g)
        {
        grid = g;
        /*
          if(g instanceof IntGrid3D)
          grid = new IntGrid3DW((IntGrid3D)g);
          else
          grid = new DoubleGrid3DW((DoubleGrid3D)g);
          is3D = true;
        */
        } 

    public double value()
        {
        if (grid instanceof DoubleGrid2D)
            { return ((DoubleGrid2D)grid).field[x][y]; }
        else if (grid instanceof IntGrid2D)
            { return ((IntGrid2D)grid).field[x][y]; }
        else if (grid instanceof DoubleGrid3D)
            { return ((DoubleGrid3D)grid).field[x][y][z]; }
        else if (grid instanceof IntGrid3D)
            { return ((IntGrid3D)grid).field[x][y][z]; }
        else return 0;  // an error
        }
        
    /** 
     * Interface all grids should implement.
     * Until that happens, each grid get a 
     * wrapper that implements it
     */
    /*public interface ValueGrid {public double value(int x, int y, int z);}
    
      public class IntGrid2DW implements ValueGrid
      {
      IntGrid2D ig2;
      public IntGrid2DW(IntGrid2D g){ ig2 = g;}
      public double value(int x, int y, int z){return ig2.field[x][y];}
      }
      public class DoubleGrid2DW implements ValueGrid
      {
      DoubleGrid2D dg2;
      public DoubleGrid2DW(DoubleGrid2D g){ dg2 = g;}
      public double value(int x, int y, int z){return dg2.field[x][y];}
      }
      public class IntGrid3DW implements ValueGrid
      {
      IntGrid3D ig3;
      public IntGrid3DW(IntGrid3D g){ ig3 = g;}
      public double value(int x, int y, int z){return ig3.field[x][y][z];}
      }
      public class DoubleGrid3DW implements ValueGrid
      {
      DoubleGrid3D dg3;
      public DoubleGrid3DW(DoubleGrid3D g){ dg3 = g;}
      public double value(int x, int y, int z){return dg3.field[x][y][z];}
      }
    */
    }
