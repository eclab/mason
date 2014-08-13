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
import sim.portrayal.*;
import sim.field.grid.*;

/** An agent is either a Pac or a Ghost -- something which is capable of moving about.
    Agents have a previous action they've done which determines their "orientation".

    <p>Agents live in a continuous 2D world but associate that world with the grid environment
    of the maze.  The continuous location of the agent is 'location'.  Agents can't actually be
    *any* continuous location but rather are one of a set of finely-discretized locations.  The
    degree of discretization is 'discretization', and it in turn affects the speed of the agent
    (as the agent steps through the discretized locations to move from grid location to grid location,
    how fine the discretization is determines how many steps he must move to get from one grid
    location to another, and thus his speed going through the maze).
*/

public abstract class Agent implements Oriented2D
    {
    private static final long serialVersionUID = 1;

    /** The Action "Go North" */
    public static final int N = 0;
    /** The Action "Go East" */
    public static final int E = 1;
    /** The Action "Go South" */
    public static final int S = 2;
    /** The Action "Go West" */
    public static final int W = 3;
    /** The Action "Do Nothing" */
    public static final int NOTHING = -1;
    /** The last action performed by the agent.  Initially NOTHING. */
    public int lastAction = NOTHING;
    /** The location of the agent.  We store it here to avoid having to do multiple
        Continuous2D lookups, which are expensive.  */
    public MutableDouble2D location;
    // The PacMan simulation state
    PacMan pacman;
    /** The agent's discretization. */
    public int discretization = 10;

    /** The agent's maximum velocity.  Determined by discretization.  */
    public double speed()
        {
        return 1.0 / discretization;
        }

    /** Where the agent starts when the game is reset. */
    public abstract Double2D getStartLocation();

    /** Creates an agent, places it in its location in pacman.agents, and
        schedules it on pacman.schedule. */
    public Agent(PacMan pacman)
        {
        this.pacman = pacman;
        Double2D loc = getStartLocation();
        this.location = new MutableDouble2D(loc);
        pacman.agents.setObjectLocation(this, loc);
        }

    /** Returns the "orientation" of the agent. */
    public double orientation2D()
        {
        switch (lastAction)
            {
            case N:
                return (Math.PI * 3.0) / 2;
            case E:
                return 0;
            case S:
                return Math.PI / 2;
            case W:
                return Math.PI;
            }
        return 0;
        }

    /** Updates the location of the agent.  Has a little wiring to make sure that the x and y values
        are discretized to the discretization locations. */
    public void changeLocation(double x, double y)
        {
        // locations can get off a bit because of double floating point error.  This keeps them in check:
        location.x = (((int)(Math.round(x * discretization)))) / (double) discretization;
        location.y = (((int)(Math.round(y * discretization)))) / (double) discretization;
        pacman.agents.setObjectLocation(this, new Double2D(location));
        }

    protected MutableDouble2D nextCell(int nextAction)
        {
        double nx = 0, ny = 0;
        switch (nextAction)
            {
            case N:
                nx = location.x;
                ny = location.y - 1;
                break;
            case E:
                nx = location.x + 1;
                ny = location.y;
                break;
            case S:
                nx = location.x;
                ny = location.y + 1;
                break;
            case W:
                nx = location.x - 1;
                ny = location.y;
                break;
            default:
                throw new RuntimeException("default case should never occur");
            }
        return new MutableDouble2D(nx, ny);
        }

    /** Performs a given action (N/W/S/E/NOTHING), moving the agent appropriately. */
    public void performAction(int action)
        {
        double x = location.x;
        double y = location.y;

        switch (action)
            {
            // we allow toroidal actions
            case N:
                y = pacman.agents.sty(y - speed());
                break;
            case E:
                x = pacman.agents.stx(x + speed());
                break;
            case S:
                y = pacman.agents.sty(y + speed());
                break;
            case W:
                x = pacman.agents.stx(x - speed());
                break;
            default:
                throw new RuntimeException("default case should never occur");
            }
        changeLocation(x, y);
        lastAction = action;
        }

    /** Determines if the agent can move with the given action (N/W/S/E/NOTHING) without bumping into a wall.  */
    public boolean isPossibleToDoAction(int action)
        {
        if (action == NOTHING)
            {
            return false;  // no way
            }
        IntGrid2D maze = pacman.maze;
        int[][] field = maze.field;

        // the Agents grid is discretized exactly on 1x1 boundaries so we can use floor rather than divide

        // the agent can straddle two locations at a time.  The basic location is x0, y0, and the straddled location is x1, y1.
        // It may be that x0 == y0.
        int x0 = (int) location.x;
        int y0 = (int) location.y;
        int x1 = location.x == x0 ? x0 : x0 + 1;
        int y1 = location.y == y0 ? y0 : y0 + 1;

        // for some actions we can only do the action if we're not straddling, or if our previous action was NOTHING
        if ((x0 == x1 && y0 == y1) || lastAction == NOTHING)
            {
            switch (action)
                {
                // we allow toroidal actions
                case N:
                    return (field[maze.stx(x0)][maze.sty(y0 - 1)] == 0);
                case E:
                    return (field[maze.stx(x0 + 1)][maze.sty(y0)] == 0);
                case S:
                    return (field[maze.stx(x0)][maze.sty(y0 + 1)] == 0);
                case W:
                    return (field[maze.stx(x0 - 1)][maze.sty(y0)] == 0);
                }
            } // for other actions we're continuing to do what we did last time.
        // assuming we're straddling, this should always be allowed unless our way is blocked
        else if (action == lastAction)
            {
            switch (action)
                {
                // we allow toroidal actions
                case N:  // use y0
                    return (field[maze.stx(x0)][maze.sty(y0)] == 0);
                case E:  // use x1
                    return (field[maze.stx(x1)][maze.sty(y0)] == 0);
                case S:  // use y1
                    return (field[maze.stx(x0)][maze.sty(y1)] == 0);
                case W:  // use x0
                    return (field[maze.stx(x0)][maze.sty(y0)] == 0);
                }
            } // last there are reversal actions.  Generally these are always allowed as well.
        else if ((action == N && lastAction == S) ||
            (action == S && lastAction == N) ||
            (action == E && lastAction == W) ||
            (action == W && lastAction == E))
            {
            return true;
            }

        return false;
        }
    }
