/*
  Copyright 2009  by Sean Luke and Vittorio Zipparo
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package sim.app.pacman;
import sim.engine.*;
import sim.field.continuous.*;
import sim.portrayal.Oriented2D;
import sim.util.*;
import ec.util.*;

/* The Pac is the Pac Man in the game.  Pac is an Agent and is also Steppable.  The Pac moves first, then the ghosts. */

public class Pac extends Agent implements Steppable
    {
    private static final long serialVersionUID = 1;

    /** How long we wait while the Pac dies (not spinning). */
    public static final int WAIT_TIME = 100;
        
    /** How long we wait while the Pac spins around while dying. */
    public static final int SPIN_TIME = 100;
        
    /** How often the Pac rotates 90 degrees while spinning. */
    public static final int SPIN_SPEED = 5;
        
    /** The Pac's discretization (9), which makes him faster than the ghosts, whose discretization is 10. */
    public static final int PAC_DISCRETIZATION = 9;
        
    /** The current score the Pac receives for eating a ghost. */
    public int eatGhostScore = 200;
        
    /** The Pac's index in the player array.  This will be used to allow multiple Pacs. */
    public int tag;
        
    /** The stoppable for this Pac so he can remove himself when he dies if it's multiplayer */
    public Stoppable stopper;
        
    /** Creates a Pac assigned to the given tag, puts him in pacman.agents at the start location, and schedules him on the schedule. */
    public Pac(PacMan pacman, int tag) 
        {
        super(pacman);
        this.tag = tag;
        discretization = PAC_DISCRETIZATION;  // I go a bit faster
        stopper = pacman.schedule.scheduleRepeating(this, 0, 1);  // schedule at time 0
        }

    // the pac's start location
    public Double2D getStartLocation() { return new Double2D(13.5, 25); }


   

    /* Default policy implementation: Pac is controlled through the joystick/keyboard
     * To changhe Pacs behavior derived classes should override this method
     */
    protected void doPolicyStep(SimState state)
        {
        int nextAction = pacman.getNextAction(tag);

        // pac man delays the next action until he can do it.  This requires a bit of special code
        if (isPossibleToDoAction(nextAction))
            {
            performAction(nextAction);
            }
        else if (isPossibleToDoAction(lastAction))
            {
            performAction(lastAction);
            }

        }
   
    /* Steps the Pac.  This does various things.  First, we look up the action from the user (getNextAction).
       Then we determine if it's possible to do the action.  If not, we determine if it's possible to do the
       previous action.  Then we do those actions.  As a result we may have eaten an energizer or a dot.  If so
       we remove the dot or energizer, update the score, and possibly frighten the ghosts.  If we've eaten all
       the dots, we schedule an event to reset the level.  Next we check to see if we've encountered a ghost.
       If the ghost is frightened, we eat it and put him in jail.  Otherwise we die.
    */
    public void step(SimState state)
        {
        doPolicyStep(state);
        // now maybe we eat a dot or energizer...

        Bag nearby = pacman.dots.getNeighborsWithinDistance(new Double2D(location), 0.3);  // 0.3 seems reasonable.  We gotta be right on top anyway
        for(int i=0; i < nearby.numObjs; i++)
            {
            Object obj = nearby.objs[i];
            if (obj instanceof Energizer && pacman.dots.getObjectLocation(obj).equals(location))  // uh oh
                {
                pacman.score+=40; // only 40 because there is a dot right below the energizer.  Total should appear to be 50
                pacman.dots.remove(obj);
                eatGhostScore = 200;  // reset
                pacman.frightenGhosts = true;

                // create a Steppable to turn off ghost frightening after the ghosts have had a chance to
                // be sufficiently frightened
                pacman.schedule.scheduleOnce(new Steppable()  // the pac goes first, then the ghosts, so they'll get frightened this timestep, so we turn it off first thing next time
                    {
                    public void step(SimState state)
                        {
                        pacman.frightenGhosts = false;
                        }
                    }, -1);
                }
            if (obj instanceof Dot && pacman.dots.getObjectLocation(obj).equals(location))
                {
                pacman.score+=10;
                pacman.dots.remove(obj);
                }
            }
        if (nearby.numObjs > 0)
            if (pacman.dots.size() == 0)  // empty!
                {
                pacman.schedule.scheduleOnceIn(0.25, new Steppable()            // so it happens next
                    {
                    public void step(SimState state)
                        { 
                        resetLevel();
                        }
                    });  // the Ghosts move a bit more
                }

        // a ghost perhaps?
                
        nearby = pacman.agents.getNeighborsWithinDistance(new Double2D(location), 0.3);  // 0.3 seems reasonable.  We gotta be right on top anyway
        for(int i=0; i < nearby.numObjs; i++)
            {
            Object obj = nearby.objs[i];
            if (obj instanceof Ghost && location.distanceSq(pacman.agents.getObjectLocation(obj)) <= 0.2) // within 0.4 roughly
                {
                Ghost m = (Ghost)obj;
                if (m.frightened > 0)  // yum
                    {
                    pacman.score += eatGhostScore;
                    eatGhostScore *= 2;  // each Ghost is 2x more
                    m.putInJail();
                    }
                else // ouch
                    {
                    pacman.schedule.scheduleOnceIn(0.5, new Steppable()             // so it happens next.  Should be after resetLEvel(), so we do 0.5 rather than 0.25
                        {
                        public void step(SimState state)
                            { 
                            die();
                            }
                        });  // the ghosts move a bit more
                    }
                }
            }
        }


    /** Resets the level as a result of eating all the dots.  To do this we first clear out the entire
        schedule; this will eliminate everything because resetLevel() was itself scheduled at a half-time
        timestep so it's the only thing going on right now.  Clever right?  I know!  So awesome.  Anyway,
        we then schedule a little pause to occur.  Then afterwards we reset the game.
    */
    public void resetLevel()
        {
        // clear out the schedule, we're done
        pacman.schedule.clear();

        // do a little pause
        pacman.schedule.scheduleOnce(
            new Steppable()
                {
                public int count = 0;
                public void step(SimState state) 
                    { 
                    if (++count < WAIT_TIME * 2) pacman.schedule.scheduleOnce(this); 
                    } 
                });
                                
        pacman.schedule.scheduleOnceIn(WAIT_TIME * 2,
            new Steppable()
                {
                public void step(SimState state) { pacman.level++; pacman.resetGame(); }
                });
        }
        
        
        
        
    /** Dies as a result of encountering a monster.  To do this we first clear out the entire
        schedule; this will eliminate everything because die() was itself scheduled at a half-time
        timestep so it's the only thing going on right now.  Clever right?  I know!  So awesome.  Anyway,
        we then schedule a little pause to occur.  Then afterwards we schedule a period where the pac
        spins around and around by changing his lastAction.  Then finally we wait a little bit more,
        then reset the agents so they're at their start locations again.
    */
        
    public void die()
        {
        pacman.deaths++;
        if (pacman.pacsLeft() > 1)
            {
            // there are other pacs playing.  We just delete ourselves.
            if (stopper != null) stopper.stop();
            stopper = null;
            pacman.agents.remove(this);
            pacman.pacs[tag] = null;
            return;
            }
                
        // okay so we're the last pac alive.  Let's do the little dance
                
        // clear out the schedule, we're done
        pacman.schedule.clear();

        // do a little pause
        pacman.schedule.scheduleOnce(
            new Steppable()
                {
                public int count = 0;
                public void step(SimState state) 
                    { 
                    if (++count < WAIT_TIME) pacman.schedule.scheduleOnce(this); 
                    } 
                });

        // wait a little more.
        pacman.schedule.scheduleOnceIn(WAIT_TIME,
            new Steppable()
                {
                public void step(SimState state)
                    {
                    // remove the Ghosts
                    Bag b = pacman.agents.getAllObjects();
                    for(int i = 0; i < b.numObjs; i++) {if (b.objs[i] != Pac.this) { b.remove(i); i--; } }
                    }
                });
                        
        // do a little spin
        pacman.schedule.scheduleOnceIn(WAIT_TIME + 1,
            new Steppable() 
                { 
                public int count = 0;
                public void step(SimState state) 
                    { 
                    if (count % SPIN_SPEED == 0) { lastAction = (lastAction + 1) % 4; }  // spin around
                    if (++count < SPIN_TIME) pacman.schedule.scheduleOnce(this); 
                    } 
                });

        // wait a little more, then reset the agents.
        pacman.schedule.scheduleOnceIn(WAIT_TIME * 2 + SPIN_TIME,
            new Steppable()
                {
                public void step(SimState state) { pacman.resetAgents(); }
                });
        }
    }
