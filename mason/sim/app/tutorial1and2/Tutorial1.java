/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.tutorial1and2;

import sim.engine.*;
import sim.field.grid.*;

public class Tutorial1 extends SimState
    {
    public Tutorial1(long seed)
        {
        super(seed);
        }
    
    // our own parameters for setting the grid size later on
    public IntGrid2D grid;
    
    public int gridWidth = 100;
    public int gridHeight = 100;
    
    // A b-heptomino looks like this:
    //  X
    // XXX
    // X XX
    public static final int[][] b_heptomino = new int[][]
    {{0, 1, 1},
         {1, 1, 0},
         {0, 1, 1},
         {0, 0, 1}};
    
    void seedGrid()
        {
        // we stick a b_heptomino in the center of the grid
        for(int x=0;x<b_heptomino.length;x++)
            for(int y=0;y<b_heptomino[x].length;y++)
                grid.field[x + grid.field.length/2 - b_heptomino.length/2]
                    [y + grid.field[x].length/2 - b_heptomino[x].length/2] =
                    b_heptomino[x][y];
        }
    
    public void start()
        {
        super.start();
        grid = new IntGrid2D(gridWidth, gridHeight);
        seedGrid();
        schedule.scheduleRepeating(new CA());
        }

    // THIS VERSION IS FOR TUTORIAL 1
    public static void main(String[] args)
        {
        Tutorial1 tutorial1 = new Tutorial1(System.currentTimeMillis());
        tutorial1.start();
        long steps = 0;
        while(steps < 5000)
            {
            if (!tutorial1.schedule.step(tutorial1))
                break;
            steps = tutorial1.schedule.getSteps();
            if (steps % 500 == 0)
                System.out.println("Steps: " + steps + " Time: " + tutorial1.schedule.time());
            }
        tutorial1.finish();
        System.exit(0);
        }
    
    
    // THIS VERSION IS FOR TUTORIAL 2
    // COMMENT OUT THE STATEMENT 'public static void main2(String[] args)'
    // AND UNCOMMENT THE STATEMENT 'public static void main(String[] args)'
    // THEN COMMENT OUT THE main() FUNCTION ABOVE IN THE CODE
    // (the purpose of the main2(...) is to maintain formatting -- if we
    // commented this out it would get flattened by my emacs autoformatter.  :-)
    // Sorry!  -- Sean
    
    //public static void main(String[] args)
    public static void main2(String[] args)
        {
        Tutorial1 tutorial1 = null;
        
        // should we load from checkpoint?  I wrote this little chunk of code to
        // check for this to give you the general idea.
        
        for(int x=0;x<args.length-1;x++)  // "-checkpoint" can't be the last string
            if (args[x].equals("-checkpoint"))
                {
                SimState state = SimState.readFromCheckpoint(new java.io.File(args[x+1]));
                if (state == null)   // there was an error -- it got printed out to the screen, so just quit
                    System.exit(1);
                else if (!(state instanceof Tutorial1))  // uh oh, wrong simulation stored in the file!
                    {
                    System.out.println("Checkpoint contains some other simulation: " + state);
                    System.exit(1);
                    }
                else // we're ready to lock and load!  
                    tutorial1 = (Tutorial1)state;
                }
        
        // ...or should we start fresh?
        if (tutorial1==null)  // no checkpoint file requested
            {
            tutorial1 = new Tutorial1(System.currentTimeMillis());
            tutorial1.start();
            }
        
        long steps = 0;
        while(steps < 5000)
            {
            if (!tutorial1.schedule.step(tutorial1))
                break;
            steps = tutorial1.schedule.getSteps();
            if (steps % 500 == 0)
                {
                System.out.println("Steps: " + steps + " Time: " + tutorial1.schedule.time());
                String s = steps + ".Tutorial1.checkpoint";
                System.out.println("Checkpointing to file: " + s);
                tutorial1.writeToCheckpoint(new java.io.File(s));
                }
            }
        tutorial1.finish();
        System.exit(0);  // make sure any threads finish up
        }
  
  
    // THIS VERSION IS *ALSO* FOR TUTORIAL 2
    // COMMENT OUT THE STATEMENT 'public static void main3(String[] args)'
    // AND UNCOMMENT THE STATEMENT 'public static void main(String[] args)'
    // THEN COMMENT OUT THE main() FUNCTION ABOVE IN THE CODE
    // (the purpose of the main3(...) is to maintain formatting -- if we
    // commented this out it would get flattened by my emacs autoformatter.  :-)
    // Sorry!  -- Sean
    
    //public static void main(String[] args)
    public static void main3(String[] args)
        {
        doLoop(Tutorial1.class, args);
        System.exit(0);
        }    

  
    }
