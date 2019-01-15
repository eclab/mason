/*
 * Conflict.java
 * 
 * $Id: Conflict.java 1594 2013-02-06 19:35:23Z escott8 $
 * 
 */

package sim.app.geo.riftland.conflict;

import sim.app.geo.riftland.household.ActivityAtLocation;
import sim.app.geo.riftland.household.Activity;
import sim.app.geo.riftland.parcel.GrazableArea;
import sim.util.Valuable;

/** Encapsulates a specific conflict between farmer_ and herder_
 * 
 * Created for each instance of conflict between farmer_ and herder_.  Added to
 * Mediator container of conflicts.
 *
 * @author mcoletti
 * 
 * Modified to support herder-herder conflict as well
 * Considered creating a new form of conflict for herder-herder, but decided
 * it made more sense to abstract conflict to use an attacker and defender 
 * who are persons and a conflict type flag to specify type of conflict to allow
 * multiple types of conflict in the future.
 * 
 * For now, type 1 conflict is farmer attacking herder due to trepassing
 *          type 2 conflict is herder attacking another herder
 *
 * @author Bill Kennedy (wkennedy@gmu.edu)
 * 
 * RiftLand changes to HerderLand conflict concept:
 *   Change: There can be multiple farmers on a parcel.  
 *   Approach: Attacker will interact with each other occupier serially. This
 *             should be a Lancaster simulation of conflict, i.e., a war of
 *             attrition.
 *   Code changes: attacker is a single herder activity
 *                 battlefield is the parcel attacked.  This could have one or
 *                             more households or herders there to be decided 
 *                             with on execution (i.e., if herder, could have moved)
 * 
 */
public class Conflict implements Valuable, java.io.Serializable
{
    
    private Activity attacker_;   // herder part of household

    private GrazableArea defending_;    // parcel where conflict is
        
    private int conflictType_;  // indicates what types of agents are in conflict

    public final static int CONFLICT_TYPE_NON = 0;
    public final static int CONFLICT_TYPE_HF = 1;
    public final static int CONFLICT_TYPE_HH_TBD = 2;  // not yet evaluated
    public final static int CONFLICT_TYPE_HH_WITHIN_CULT = 3;
    public final static int CONFLICT_TYPE_HH_OTHER_CULT = 4;
    
    Conflict()
    {
        attacker_ = null;
        defending_ = null;
    }
    
    Conflict(Activity attacker, GrazableArea defending, int conflictType)
    {
        attacker_ = attacker;
        defending_ = defending;
        conflictType_ = conflictType;
    }

    public
    Activity getAttacker()
    {
        return attacker_;
    }

    public
    void setAttacker(ActivityAtLocation attacker)
    {
        this.attacker_ = attacker;
    }

    public
    GrazableArea getDefending()
    {
        return defending_;
    }

    public
    void setDefender(GrazableArea defence)
    {
        this.defending_ = defence;
    }

    public
    int getConflictType()
    {
        return conflictType_;
    }

    public
    void setConflictType(int conflictType)
    {
        this.conflictType_ = conflictType;
    }

    /** Returns conflict type to be used in visualization
     *
     * @return will return herdHunger
     */
    @Override
    public
    double doubleValue()
    {
        return conflictType_;
    }

}
