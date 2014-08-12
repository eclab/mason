/*
  Copyright 2009  by Sean Luke and Vittorio Zipparo
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.pacman;
import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;
import sim.field.grid.*;
import java.io.*;

/** PacMan is the model for the game.  The model contains three fields: a Continuous2D for the
    agents, a Continuous2D for the dots, and an IntGrid2D holding the maze (1 is wall, 0 is open space).
    The model holds an array of "actions", one per player, in case we want to make this a multiplayer game.
        
    <p>Note that you can easily modify this code to have different kinds of Pacs (internally we have invented
    AI Pacs.  :-).  Also if you just want one pacman, not two, change the Pacs array to be of size 1 (see the
    code below).
*/

public class PacMan extends SimState
    {
    private static final long serialVersionUID = 1;

    /** Holds the ghosts and the Pac. */
    public Continuous2D agents;
        
    /** Holds Energizers and Dots. */
    public Continuous2D dots;
        
    /** The maze proper. */
    public IntGrid2D maze;
        
    /** A signal to indicate to the ghosts that they should become frightened next step around. */
    boolean frightenGhosts;  // signal for the ghosts
        
    /** Desired actions from the user.  Presently only actions[0] used.  */
    public int[] actions;
        
    /** The number of deaths so far. */
    public int deaths = 0;
        
    /** The current level.  */
    public int level = 1;
        
    /** The current score. */
    public int score=0;

    /** The pacs.  Used by the ghosts to figure out where the closest Pac is. */
    public Pac[] pacs;      

    /** Creates a PacMan simulation with the given random number seed. */
    public PacMan(long seed)
        {
        super(seed);
        }
    
    /** Resets the scores, loads the maze, creates the fields, adds the dots and energizers, and resets the Pac and Ghosts. */
    public void start()
        {
        super.start();
                
        deaths = 0;
        level = 1;
        score = 0;
                
        // String mazefile = PacMan.class.getResource("images/maze0.pbm").getPath();
        // maze = new IntPBMGrid2D(mazefile);
        maze = new IntGrid2D(0,0);
        try { maze.setTo(TableLoader.loadPNMFile(PacMan.class.getResourceAsStream("images/maze0.pbm"))); }
        catch (Exception e) { e.printStackTrace(); }
        
        agents = new Continuous2D(1.0, maze.getWidth(), maze.getHeight());
        dots = new Continuous2D(1.0, maze.getWidth(), maze.getHeight());

        resetGame();
        }
        
    public int MAX_MAZES = 2;
        
    /** Resets the game board.  Doesn't change the score or deaths or level number */
    public void resetGame()
        {
        dots.clear();
                
        //String mazefile = PacMan.class.getResource("images/maze" + (level - 1) % MAX_MAZES + ".pbm").getPath();
        //maze.read(mazefile);
        //maze.read(PacMan.class.getResourceAsStream("images/maze" + (level - 1) % MAX_MAZES + ".pbm"));
        try { maze.setTo(TableLoader.loadPNMFile(PacMan.class.getResourceAsStream("images/maze" + (level - 1) % MAX_MAZES + ".pbm"))); }
        catch (Exception e) { e.printStackTrace(); }

        // add energizers
        dots.setObjectLocation(new Energizer(), new Double2D(1, 5));
        dots.setObjectLocation(new Energizer(), new Double2D(26, 5));
        dots.setObjectLocation(new Energizer(), new Double2D(1, 25));
        dots.setObjectLocation(new Energizer(), new Double2D(26, 25));

        // distribute dots.  We allow dots right on the energizers, no biggie
        for (int x= 0; x < maze.getWidth(); x++)
            for(int y =0; y < maze.getHeight(); y++)
                if (maze.field[x][y] == 0 && 
                    !(y==16 && x>= 12 && x <= 16))  // not in the jail
                    dots.setObjectLocation(new Dot(), new Double2D(x,y));

        resetAgents();
        }
        
    public int pacsLeft() { int count = 0; for(int i = 0; i < pacs.length;i++) if (pacs[i] != null) count++;  return count;}
        
    public Pac pacClosestTo(MutableDouble2D location)
        {
        if (pacs.length == 1) return pacs[0];
        Pac best = null;
        int count = 1;
        for(int i = 0; i < pacs.length; i++)
            {
            if (pacs[i] != null)
                {
                if (best == null ||
                        (best.location.distanceSq(location) > pacs[i].location.distanceSq(location) && ((count=1)==1) ||
                        best.location.distanceSq(location) == pacs[i].location.distanceSq(location) && random.nextBoolean( 1.0 / (++count))))
                    best = pacs[i];
                }
            }
        return best;
        }
        
        
    /** Puts the agents back to their regular locations, and clears the schedule.  */
    public void resetAgents()
        {
        agents.clear();
        schedule.clear();

        // make arrays
        actions = new int[] { Agent.NOTHING , Agent.NOTHING };
        pacs = new Pac[2];  // set this to Pac[1] to make this one-player

        // add the Pacs
        if (pacs.length > 1) pacs[1] = new Pac(this,  1);  // schedule pac 1 first so he appears on the bottom initially
        pacs[0] = new Pac(this,  0);

        // add Blinky
        // yes, dead store
        Blinky blinky = new Blinky(this);

        // add Pinky
        // yes, dead store
        Pinky pinky = new Pinky(this);

        // add Inky
        // yes, dead store
        Inky inky = new Inky(this, blinky);

        // add Clyde
        // yes, dead store
        Clyde clyde = new Clyde(this);
                
        // ghosts are no longer frightened
        frightenGhosts = false;
        }
        
        
        
    /** Returns the desired user action.  */
    public int getNextAction(int tag) { return actions[tag]; }

    public static void main(String[] args)
        {
        doLoop(PacMan.class, args);
        System.exit(0);
        }    
    }
