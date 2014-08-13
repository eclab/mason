/*
  Copyright 2009  by Sean Luke and Vittorio Zipparo
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.pacman;
import sim.engine.*;
import sim.field.continuous.*;
import sim.util.*;
import ec.util.*;

/** A ghost is an agent.  There are four ghosts, of course: Blinky, Pinky, Inky, and Clyde.  Each has his
    own special behavior.  
        
    <p> 
    All PacMan Ghosts have a "target" grid position that they're trying
    to achieve.  For Blinky, for example, that target is where the Pac is himself.
    The way that each Ghost achieves its target is to determine what action, at the NEXT
    grid position, he should do.  Ghosts never reverse direction, so of the remaining options,
    they pick the option which would move them FROM the NEXT grid position to a spot that 
    is closest to the target.  It's a greedy strategy but works reasonably well -- the
    refusal to reverse direction affects an effective Bug strategy of sorts (a common
    robotics path-charting procedure).
        
    <p>We'll assume that the Ghosts don't use the NEXT grid position but
    use the CURRENT grid position, and only compute a new action to perform
    when they're able to move (other than go in reverse, which they never do).

    <p>
    See http://home.comcast.net/~jpittman2/pacman/pacmandossier.html
    for a very good description.
        
    <p>Unlike regular pacman ghosts, we don't have a scatter target (except Clyde, who needs it for his algorithm).
*/


public abstract class Ghost extends Agent implements Steppable, Valuable
    {
    private static final long serialVersionUID = 1;

    /** How long the ghost stays frightened. */
    public static final int FRIGHTENED_PERIOD = 360;
        
    /** The discretization for the ghost when he's frightened.  This causes him to be slower than normal.  */
    public static final int FRIGHTENED_DISCRETIZATION = 15;

    /** The discretization for the ghost when he's normal.  This causes him to be slightly slower than the Pac.  */
    public static final int REGULAR_DISCRETIZATION = 10;

    /** How long the ghost stays in the jail when he's been eaten.  In real Pac Man these periods are actually variable and much shorter. */
    public static final int WAITING_PERIOD = 360;

    /** How long the ghost stays in the jail at the beginning of the game (except for Blinky, who's not in the jail at all).  */
    public static final int INITIAL_WAITING_PERIOD = WAITING_PERIOD / 4;
        
    /** The location of the jail's exit.  This is a special location that the agents can go out of but not into. */
    public Double2D exitLocation = new Double2D(13.5, 16);
        
    /** Ghost fright countdown timer.  If <= 0, the ghost is not frightened.  */
    public int frightened = 0;

    /** Ghost waiting-in-box countdown timer.  If <= 0, the ghost is free to leave the box.  */
    public int waiting = INITIAL_WAITING_PERIOD;

    /** Indicates whether the ghost is presently exiting the box (a special condition). */
    public boolean exiting = false;


    /** Returns 1 if the ghost is frightened, else 0.  Used to change the color of the ghost in the Portrayal.  */
    public double doubleValue() 
        {
        if (frightened > 0)
            {
            if (frightened > FRIGHTENED_PERIOD / 3) return 4;
            else // cause blinking
                {
                int s = frightened / (FRIGHTENED_PERIOD / 18); // integer division
                if (s % 2 == 0) return 4;
                else return 5;
                }
            }
        switch (lastAction)
            {
            case N: return 0;
            case W: return 1;
            case S: return 2;
            case E: return 3;
            }
        return 3;
        }

        
    /** Moves the ghost to the jail and sets him waiting.  Resets his frightened counter.  */
    public void putInJail()
        {
        location = new MutableDouble2D(exitLocation);
        pacman.agents.setObjectLocation(this, exitLocation);
        lastAction = W;
        frightened = 0;
        waiting = WAITING_PERIOD;
        }
        

    /** Creates a ghost, facing west, and schedules him on the schedule.  */
    public Ghost(PacMan pacman) 
        {
        super(pacman);
        lastAction = W;
        pacman.schedule.scheduleRepeating(this, 1, 1);
        }
                        


    /** Returns the opposite direction action of the one provided.  The opposite of NOTHING is NOTHING.  */
    public static int reverseOf(int action)
        {
        switch(action)
            {
            case N: return S;
            case E: return W;
            case S: return N;
            case W: return E;
            }
        return NOTHING;
        }
        
    /** Returns the ghost's target. */
    public abstract Double2D getTarget();
        
        
    static final int MIN_DIST_FOR_TOROIDAL = 4;
        
    /** Steps the ghost.  First, if the ghost has just been frightened, he reverses direction and starts to go slower.
        Second, if the ghost is in the process of leaving the box, he moves specially, heading north until he's out of
        the box.  This is separate from the standard performAction() routine.  Third, if the ghost is NOT in the process
        of leaving the box, and is at an intersection, he gathers all the legal directions to move (not including reverse
        unless it's the only option), then picks the one closest to his target (or randomly if he's frightened).
        The notion of "closest to target" is computed nontoroidally, but with some slop -- if the target is
        within 4 away toroidally, the distance is toroidal, so ghosts near the tunnel may go in the tunnel to chase
        the Pac.  We then perform the action and decrease our frightened and waiting counts by 1. */
                 
    public void step(SimState state)
        {
        // first things first: if the Pac has just eaten an energizer, and
        // then the reducer has reduced the pill time by one, we should reverse
        // our direction because we are now scared.
        if (pacman.frightenGhosts && waiting <=0 && !exiting)
            {
            lastAction = reverseOf(lastAction);
            frightened = FRIGHTENED_PERIOD;
            }
                        
        if (frightened > 0)
            discretization = FRIGHTENED_DISCRETIZATION; // I go slower
        else
            discretization = REGULAR_DISCRETIZATION;  // standard speed

        // Next compute if I'm done waiting but still in the box and need to get out
        if (waiting <=0 && location.x == exitLocation.x && location.y <= exitLocation.y && location.y > exitLocation.y - 3)
            {
            exiting = true;
            double x = location.x;
            double y = location.y;
            y = pacman.agents.sty(y - speed());
            if (y <= exitLocation.y - 3) y = exitLocation.y - 3;  // don't hit the wall
            changeLocation(x,y);
            lastAction = pacman.random.nextBoolean() ? W : E;  // so we figure something else to do when we get out
            }
        // can I change direction?
        else 
            {
            exiting = false;

            if (location.x == (int) location.x && location.y == (int) location.y)
                {
                double x = location.x;
                double y = location.y;
                                
                int bestAction = NOTHING;
                double bestActionDistanceSquared = Double.POSITIVE_INFINITY;  // bad
                                
                Double2D target = getTarget();

                int reverseAction = reverseOf(lastAction);
                Continuous2D agents = pacman.agents;
                                
                // the NEXT grid cell from performing a given action
                double nx = 0;
                double ny = 0;
                                
                int tick = 1;
                                
                // compute the possible actions I can do
                for(int action = N; action <= W; action++)
                    if (action != reverseAction && isPossibleToDoAction(action))
                        {
                        switch(action)
                            {
                            case N: nx = x; ny = y - 1; break;
                            case E: nx = x + 1; ny = y; break;
                            case S: nx = x; ny = y + 1; break;
                            case W: nx = x - 1; ny = y; break;
                            default:
                                throw new RuntimeException("default case should never occur");
                            }
                                                
                        // Here's how I'm going to define it.  
                        // The Ghosts only use toroidal information to compute distance to the pac
                        // if they're quite close to him in X or Y toroidally; else they use normal distance.
                        // also never use toroidal distance if scattering.
                                                
                        double dist = 0;
                        if (frightened <= 0 && 
                            Math.abs(agents.stx(target.x - nx)) <= MIN_DIST_FOR_TOROIDAL ||
                            Math.abs(agents.sty(target.y - ny)) <-MIN_DIST_FOR_TOROIDAL)
                            dist = agents.tds(target, new Double2D(nx, ny));
                        else dist = target.distanceSq(new Double2D(nx, ny));
                                                
                        if ((frightened <= 0 && (bestAction == NOTHING || (dist < bestActionDistanceSquared))) ||  // pick the best when I'm not afraid
                            (frightened > 0 && pacman.random.nextBoolean(1.0 / (tick++))))          // pick a random value when I'm afraid
                            { bestAction = action; bestActionDistanceSquared = dist; }
                        }
                                                
                // maybe there's no choice but to reverse
                if (bestAction == NOTHING)
                    bestAction = reverseAction;  // always possible to do
                                
                performAction(bestAction);
                }
            else performAction(lastAction);
                        
            // decrease counts
                        
            if (--frightened < 0) frightened = 0;
            if (--waiting < 0) waiting = 0;
            }
        }
    }
