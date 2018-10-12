/*
 * Mediator.java
 * 
 * $Id: Mediator.java 1707 2013-03-20 21:43:50Z escott8 $
 */

package sim.app.geo.riftland.conflict;

import java.util.LinkedList;
import java.util.List;
import sim.app.geo.riftland.household.Activity;
import sim.app.geo.riftland.parcel.GrazableArea;
import sim.engine.Steppable;
import sim.field.grid.SparseGrid2D;


/**
 * @author mcoletti
 */
public abstract class Mediator implements Steppable
{
    protected boolean herderFarmerConflictActive = false;
    protected boolean herderHerderConflictActive = false;
    protected boolean withinCultConflictActive = false;
    protected boolean escalateConflictActive = false;
    protected int numStepsToEscalate = 1;  // num steps before a recurring combat escalates
    protected int escalationDistance = 10; // max distance agents will go to call for help
    protected double damageRatio = 0.05;
    
    // variables for different types of conflicts
    // splitting HH conflicts into original used when not same Cult and new for within Cult "non"conflicts

    private int HFconflicts = 0;
    private int HHTBDconflicts = 0;
    private int HHconflicts = 0;
    private int HHnonconflicts = 0;
    private int numEscalations = 0;

    /** container of all current conflicts */
    private List<Conflict> conflictsList;

    /** sparse field of all conflicts */
    private SparseGrid2D conflictsGrid;

    /** We save the conflicts from the previous step so that we can display them.
     * Otherwise conflicts get cleared in each step() before we've had a chance
     * to display them.
     * 
     * @see #clearConflicts()
     */
    private SparseGrid2D prevConflictsGrid;
    protected SparseGrid2D escalationsGrid;
    
    //<editor-fold defaultstate="collapsed" desc="Accessors">
    public List<Conflict> conflicts()
    {
        return conflictsList;
    }
    
    public int getNumConflicts()
    {
        return conflicts().size();
    }
    
    public int getHFconflicts()
    {
        return HFconflicts;
    }

    public int getHHTBDconflicts()
    {
        return HHTBDconflicts;
    }

    public int getHHconflicts()
    {
        return HHconflicts;
    }

    public int getHHnonconflicts()
    {
        return HHnonconflicts;
    }

    public void setHHconflicts(int num)
    {
        HHconflicts = num;
    }

    public void setHHnonconflicts(int num)
    {
        HHnonconflicts = num;
    }

    public int getNumEscalations()
    {
        return numEscalations;
    }

    public boolean getHerderFarmerConflictActive() {return herderFarmerConflictActive;}
    public boolean getHerderHerderConflictActive() {return herderHerderConflictActive;}
    public boolean getWithinCultConflictActive() {return withinCultConflictActive;}
    public boolean getEscalateConflictActive() {return escalateConflictActive;}
    public int getNumStepsToEscalate() {return numStepsToEscalate;}
    public int getEscalationDistance() {return escalationDistance;}
    public double getDamageRatio() {return damageRatio;}

    public void setHerderFarmerConflictActive(boolean val) {herderFarmerConflictActive = val;}
    public void setHerderHerderConflictActive(boolean val) {herderHerderConflictActive = val;}
    public void setWithinCultConflictActive(boolean val) {withinCultConflictActive = val;}
    public void setEscalateConflictActive(boolean val) {escalateConflictActive = val;}

    public void setNumStepsToEscalate(int val)
    {
        if (val >= 0)
        {
            numStepsToEscalate = val;
        }
    }

    public void setEscalationDistance(int val)
    {
        if (val > 0)
        {
            escalationDistance = val;
        }
    }

    public void setDamageRatio(double val)
    {
        if (val > 0.0 && val <= 1.0)
        {
            damageRatio = val;
        }
    }

    public SparseGrid2D getConflictsGrid( )
    {
        return conflictsGrid;
    }
    
    public SparseGrid2D getPrevConflictsGrid( )
    {
        return prevConflictsGrid;
    }

    public SparseGrid2D getEscalationsGrid( )
    {
        return escalationsGrid;
    }
    
    //</editor-fold>
    
    Mediator(int width, int height)
    {
        super();
        conflictsGrid = new SparseGrid2D(width, height);
        prevConflictsGrid = new SparseGrid2D(width, height);
        escalationsGrid = new SparseGrid2D(width, height);
        conflictsList = new LinkedList<Conflict>();
    }


    /** resolve the given conflict
     *
     * @param conflict to be resolved
     */
    public abstract void reconcile(Conflict conflict);

    public void addConflict(Conflict conflict)
    {
        conflictsList.add(conflict);

        conflictsGrid.setObjectLocation(conflict, 
                                          conflict.getDefending().getX(), 
                                          conflict.getDefending().getY());

        // keep counters of types of conflicts
        Activity attacker = conflict.getAttacker();
        GrazableArea defender = conflict.getDefending();

        switch(conflict.getConflictType())
        {
            case Conflict.CONFLICT_TYPE_HF:
                HFconflicts++;
                break;

            case Conflict.CONFLICT_TYPE_HH_TBD:
                HHconflicts++;
                break;

//            case Conflict.CONFLICT_TYPE_HH_WITHIN_CULT:
//                HHnonconflicts++;
//                break;

            default:
                System.out.println("Unrecognized conflict type: " + conflict.getConflictType());
        }
        
    }
    
    public void addEscalation(Conflict conflict)
    {
        escalationsGrid.setObjectLocation(conflict, 
                                            conflict.getDefending().getX(),  
                                            conflict.getDefending().getY());

        // keep counters of types of conflicts
        numEscalations++;
    }
    
    public void clearConflicts()
    {
        conflictsList.clear();

        // XXX Swapping these could mess with the GUI.  Need to test.
        SparseGrid2D tempCF = prevConflictsGrid;
        prevConflictsGrid = conflictsGrid;
        conflictsGrid = tempCF;
        conflictsGrid.clear();

        //escalationsField_.clear();
    }
    
    public void resetConflictCounters()
    {
        HFconflicts = 0;
        HHTBDconflicts = 0;
        HHconflicts = 0;
        HHnonconflicts = 0;
        numEscalations = 0;
    }
}
