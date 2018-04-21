
        /*
         * ConflictMediatorRural.java
         *
         * $Id$
         */
        package riftland.conflict;

import riftland.household.Herding;
import riftland.household.Activity;
import riftland.parcel.GrazableArea;
import java.util.Iterator;
import riftland.World;
import riftland.World;
import sim.engine.SimState;
import sim.util.Bag;


/** Mediator that hurts herders who trespass on farmer
 *  AND herders who trespass with another herder.
 *
 * Planned order of development:
 *   original: herder-farmer conflict
 *   this ver: person-person conflict (either herder-farmer or herder-herder)
 *   next ver: bag of persons v. bag of persons conflict to support esclation
 *
 *
 * @author jbassett & BillK
 *
 */
public class ConflictMediatorRural extends Mediator {


    // A list of all the herders participaring in an escalated conflict,
    // including the one that initiated the escalation.  The initiator is
    // always the first in the list.
    // This is very transient data.
    Bag escalationAllies = new Bag();

    public ConflictMediatorRural(int width, int height)
    {
        super(width, height);
    }

    /**
     * @param conflict multiple agents moved into the same space, one attacker, all others defender
     * @author Bill
     * Revised October 2012 to deal with higher density populations, i.e., multiple farmers per parcel
     * Changed conflict to be between a herder and grazable area with farmer(s) and/or herder(s)
     */
    @Override
    public
    void reconcile(Conflict conflict)
    {
        Herding attacker  = (Herding) conflict.getAttacker();
        GrazableArea defending  = conflict.getDefending();

        // clear counters so that they have results of meidation
        this.resetConflictCounters();

        // Herder-Farmer conflict type
        if (herderFarmerConflictActive &&
                conflict.getConflictType() == Conflict.CONFLICT_TYPE_HF)
        {
            // test if at home
            if  (  (attacker.getHousehold().getLocation().getX() == defending.getX())
                    &&(attacker.getHousehold().getLocation().getY() == defending.getY()) )
            {
                // at home farm, so not a conflict
                conflict.setConflictType(Conflict.CONFLICT_TYPE_NON);
            }
            else
            {
                // not home, process conflict with all farmers here
                Bag defenders = new Bag();
                defenders.addAll(defending.getFarms());
                int conflicts = defenders.size();

//                System.out.println("ConflictMediatorRural>herder vs. " + conflicts + " farmers");

                int attackerCulture;
                attackerCulture = attacker.getHousehold().getCulture();
                int newHerdSize = attacker.getHerdSize();

                for(int i=0; i<conflicts; i++)
                {
                    Activity farmer;
                    farmer = (Activity) defenders.get(i);
                    if (attackerCulture != farmer.getHousehold().getCulture())
                    {
                        // process diff cultures
                        newHerdSize = (int) (newHerdSize * (1.0 - damageRatio));
                    }
                    else
                    {
                        // process same culture - do nothing (share?)
                    }
                }
                // save resulting herd size
                attacker.setHerdSize(newHerdSize);

            } // end not at home, so h-f conflict

        } // endif processing of herder-farmer conflict


        // Herder-Herder conflict type
        if ( herderHerderConflictActive &&
                conflict.getConflictType() == Conflict.CONFLICT_TYPE_HH_TBD)
        {
            // collect all other herders here to be processed
            Bag defenders = new Bag();
            defenders.addAll(defending.getHerds());

            // go through bag and process based on culture match or non-match
            int potentialConflicts = defenders.size();

//            System.out.println("ConflictMediatorRural>herder vs. " + potentialConflicts + " herders");

            int attackerCulture;
            attackerCulture = attacker.getHousehold().getCulture();
            // conflicts will affect attacker's herdsize, so get herdsize 
            int newHerdSize = attacker.getHerdSize();

            // consider all potential conflicts here
            for(int i=0; i<potentialConflicts; i++)
            {
                // get from defender to other herder
                Activity otherActivity;
                otherActivity = (Activity) defenders.get(i);
                Herding otherHerder;
                // test if other activity exists then compare cultures
                // (other may have been died between conflict and here
                otherHerder = otherActivity.getHousehold().getHerding();
                if (otherHerder != null)
                {
                    if (attackerCulture != otherHerder.getHousehold().getCulture())
                    {
                        // process diff cultures and note conflict type for display
                        newHerdSize = (int) (newHerdSize * (1.0 - damageRatio));
                        conflict.setConflictType(Conflict.CONFLICT_TYPE_HH_OTHER_CULT);
                        this.setHHnonconflicts(this.getHHnonconflicts() + 1);
                    }
                    else
                    {
                        // process same culture - share resources  (and note conflict type)
                        double newFood = (attacker.getHerdFood() + otherHerder.getHousehold().getHerding().getHerdFood())/2.0;
                        attacker.setHerdFood(newFood);
                        otherHerder.getHousehold().getHerding().setHerdFood(newFood);
                        double newWater = (attacker.getHerdWater() + otherHerder.getHousehold().getHerding().getHerdWater())/2.0;
                        attacker.setHerdWater(newWater);
                        otherHerder.getHousehold().getHerding().setHerdWater(newWater);

                        conflict.setConflictType(Conflict.CONFLICT_TYPE_HH_WITHIN_CULT);
                        this.setHHconflicts(this.getHHconflicts() + 1);
                    }
                } // endif test other exists
            }  // end loop through potential conflicts

//            System.out.print("@" + world_.schedule.getSteps() + " ");
//            System.out.println("ConflictMediatorRural>herder in " 
//                               + this.getHHconflicts() + " conflicts and " 
//                               + this.getHHnonconflicts() + " sharings");

        } // endif processing of h-h conflict

        // remove herder if herd size goes to zero - note kills off herders...
        if (attacker.getHerdSize() <= 0)  // Delete herder if herd is all dead
        {
            attacker.remove();
            System.out.print("removed a herder ");
            System.out.print(attacker);
            System.out.println(" due to h-f conflict");
        }

    }


    @Override
    public
    void step(SimState state)
    {
        //System.err.println("# conflicts: " + this.getNumConflicts());

//        escalationsGrid.clear();  // If I put this in clearConflicts, nothing gets drawn

        for ( Conflict conflict : this.conflicts() )
        {
            reconcile(conflict);
        }

        this.clearConflicts();
    }


}