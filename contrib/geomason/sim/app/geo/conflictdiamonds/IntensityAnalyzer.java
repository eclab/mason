package conflictdiamonds;

import java.util.ArrayList;
import java.util.Collection;

import sim.util.Bag;

import conflictdiamonds.ConflictDiamonds.Goal;
import conflictdiamonds.ConflictDiamonds.Motive;

/**
 * The Intensity Analyzer determines which motivation is strongest, and thus the action-guiding motive
 * 
 * Agents can have 1 of 3 motives: need for basic necessities, need for security (better life), maintain current lifestyle/household
 * Agents can perform 1 of 3 activities/goals: Remain employed, find employment as diamond miner, stay home
 * 
 * This is adapted from Schmidt's (2000) PECS framework
 * 
 * @author bpint
 *
 */
public class IntensityAnalyzer {

    /**
     * Run the intensity analyzer
     * 
     * @param me - the agent
     * @param conflict
     * 
     * @return the agent's action-guiding motive/goal
     * 
     */ 
    public static Goal runIntensityAnalyzer(Person me, ConflictDiamonds conflict) {
		
        ConflictDiamonds.Goal oldGoal = me.getCurrentGoal(); //store my current goal

        ConflictDiamonds.Motive currentMotive = Motive.Maintain_Current_Lifestyle;
        ConflictDiamonds.Goal currentGoal = Goal.Stay_Home;
        
        //if no in my household is employed, then my motive is to meet my basic needs
        if ( !me.isEmployedHousehold() ) { currentMotive = Motive.Basic_Needs; }
        //if someone in my household is employed but I'm not and income level is still 0, then my motive is to meet my basic needs
        else if ( me.isEmployedHousehold() && !me.isEmployed() && me.getIncomeLevel() == 0 ) { currentMotive = Motive.Basic_Needs; }
        //if I'm employed and income level is 0 or 1, then my motive is a better life
        else if ( me.isEmployedHousehold() && me.isEmployed() && ( me.getIncomeLevel() == 0 || me.getIncomeLevel() == 1 )) { currentMotive = Motive.Better_Life; }
        //otherwise I'm happy to maintain my current lifestyle
        else currentMotive = Motive.Maintain_Current_Lifestyle;

        //determine if the opportunity exists to mine (E = set of environmental influences)
        boolean opportunity = doesOpportunityExist(me.getResidingParcel(), conflict);	
        boolean opportunityRisk = doesOpportunityExistRemoteness(me.getResidingParcel(), conflict);
        //determine rebel density
        double rebelDensity = calculateRebelDensity( me.getResidingParcel(), conflict);
        //determine if other influences impact goal (O = set of other influences)
        boolean otherInfluencestoRebel = doOtherInfluencesExistToRebel(me, rebelDensity,conflict);
        boolean otherInfluencestoMine = doOtherInfluencesExistToMine(me, conflict);
        //determine if resident is forced to join the rebellion
        
        //If a resident is seeking basic needs, the resident will look to get employment in the informal market as a miner, or rebel
        //depending on whether the opportunity exists	
        if ( currentMotive == Motive.Basic_Needs ) {
			
            if ( opportunity && otherInfluencestoMine && !conflict.diamondMinerEmployer.isEmployedHere(me) ) { 
                currentGoal = Goal.Find_Employment_As_Miner; 
            }
            else if ( opportunity && otherInfluencestoRebel ) { 
                currentGoal = Goal.Rebel; 
            }
            else if (me.isEmployed()) {
                currentGoal = Goal.Remain_Employed;
            }
            else {
                currentGoal = Goal.Stay_Home;
            }
			
        }
	
        //if resident is seeking a better life (to meet its security needs), resident will seek employment as a diamond miner
        //if the opportunity and other influences exist
        else if ( currentMotive == Motive.Better_Life ) {

            if ( opportunity && otherInfluencestoMine && !conflict.diamondMinerEmployer.isEmployedHere(me)) { 
                currentGoal = Goal.Find_Employment_As_Miner; 
            }
            
            else if ( me.isEmployed() ) {
                currentGoal = Goal.Remain_Employed;
            }
            
            else {
                currentGoal = Goal.Stay_Home;
            }
        }
		
        //else motive is to maintain current lifestyle
        else {
            if ( me.isEmployed() ) { 
                    currentGoal = Goal.Remain_Employed; 
            }
            else { 
                    currentGoal = Goal.Stay_Home; 
            }
        }
	
        //otherwise, resident may be forced to rebel regardless of motive
        if ( otherInfluencestoRebel && opportunityRisk ) {
            currentGoal = Goal.Rebel;
        }
	
       //report new goal to region for reporting purposes               
       if (currentGoal != oldGoal) {
            
            if (oldGoal == Goal.Stay_Home) { me.getResidingRegion().removeGoalStayHome(me); }
            if (oldGoal == Goal.Find_Employment_As_Miner) { me.getResidingRegion().removeGoalFindInformalEmployment(me); }
            if (oldGoal == Goal.Remain_Employed) { me.getResidingRegion().removeGoalRemainEmployed(me); }
            if (oldGoal == Goal.Rebel) { me.getResidingRegion().removeGoalRebel(me); }

            if (currentGoal == Goal.Stay_Home) { me.getResidingRegion().addGoalStayHome(me); }
            if (currentGoal == Goal.Find_Employment_As_Miner) { me.getResidingRegion().addGoalFindInformalEmployment(me); }
            if (currentGoal == Goal.Remain_Employed) { me.getResidingRegion().addGoalRemainEmployed(me); }
            if (currentGoal == Goal.Rebel) { me.getResidingRegion().addGoalRebel(me); }
            
       }
		
	return currentGoal;
    }
		
    //This corresponds to the E (set of Environmental Influences) from the Intensity function
    
    public static boolean doesOpportunityExist(Parcel residingParcel, ConflictDiamonds conflict) {
        //first determine if there area within agent vision is below the risk level they are willing to take
        //the more remote (i.e further from cities) the area and the less control, the lower the risk to mine		
        Bag neighbors = new Bag(); //get a copy of all neighboring parcels
        conflict.allLand.getNeighborsMaxDistance(residingParcel.getX(), residingParcel.getY(), conflict.params.global.getAgentVision(), false, neighbors, null, null);

        // find the set of neighbors that is below the max risk level
        ArrayList <Parcel> minRisk = new ArrayList <Parcel> ();
        double risk;

        for(Object o: neighbors){
            Parcel p = (Parcel) o;
            
            risk = (( 1 - p.getRemoteness()) + conflict.params.global.getGovernmentControlOfMines() ) / 2;

            if ( p.getRegion().getRegionID() == 0 ) continue; // the parcel is outside the country
            else if( risk < conflict.params.global.getMaxRisk() )
                    minRisk.add(p); // add our new find to it
        }

        //second determine if their are diamond mines in the region where the person resides		
        ArrayList <Parcel> mines = new ArrayList <Parcel> ();

        for(Object o: neighbors){
            Parcel p = (Parcel) o;
            if ( p.getRegion().getRegionID() == 0 ) continue; //the area is outside of the country
            else if( p.getDiamondMineDistance() == 0 )
                mines.add(p); // add our new find to it
        }	
		
        //if area around agent vision is remote, mines have minimal government control, and diamond mines exist, 
        //then opportunity exists
        double rand = conflict.random.nextDouble();
        double chance = (double) minRisk.size() / (double) neighbors.size();

        if ( minRisk.size() > 0 && mines.size() > 0 && rand < chance ) { 
            return true;
        }

        else return false;
				
    }
    
    /**
     * Determine if the opportunity (i.e. environmental influences) exist to rebel or mine
     * 
     * @param residingParcel - the agent's current position
     * @param conflict
     * 
     * @return true if the opportunity exists, false otherwise
     * 
     */  
    public static boolean doesOpportunityExistRemoteness(Parcel residingParcel, ConflictDiamonds conflict) {
        //first determine if the area within agent vision is below the risk level they are willing to take
        //the more remote (i.e further from cities and highways) the area and the less control, the lower the risk to mine		
        Bag neighbors = new Bag(); //get a copy of all neighboring parcels
        conflict.allLand.getNeighborsMaxDistance(residingParcel.getX(), residingParcel.getY(), conflict.params.global.getAgentVision(), false, neighbors, null, null);

        // find the set of neighbors that is below the max risk level
        ArrayList <Parcel> minRisk = new ArrayList <Parcel> ();
        double risk;
        //risk here is for those cases where agent is forced to rebel, so parcel must be less risky as
        //these are cases where the agent does not volunteer to rebel (is successfully forced to rebel)
        double maxRisk = conflict.params.global.getMaxRisk()/10;

        for(Object o: neighbors){
            Parcel p = (Parcel) o;
            risk = (( 1 - p.getRemoteness()) + conflict.params.global.getGovernmentControlOfMines() ) / 2;

            if ( p.getRegion().getRegionID() == 0 ) continue; // the parcel is outside the country
            else if( risk < maxRisk )
                minRisk.add(p); // add our new find to it
        }
        //second determine if their are diamond mines in the region where the person resides		
        ArrayList <Parcel> mines = new ArrayList <Parcel> ();

        for(Object o: neighbors){
            Parcel p = (Parcel) o;
            if ( p.getRegion().getRegionID() == 0 ) continue; //the area is outside of the country
            else if( p.getDiamondMineDistance() == 0 )
                mines.add(p); // add our new find to it
        }	
             
        //if area around agent vision is remote, mines have minimal government control, and diamond mines exist, 
        //then opportunity exists
        double rand = conflict.random.nextDouble();
        double chance = (double) minRisk.size() / (double) neighbors.size();

        if ( minRisk.size() > 0 && mines.size() > 0 && rand < chance ) { 
                return true;
        }

        else return false;
		
    }
    
    /**
     * Determine if the other influences exist to  mine
     * 
     * @param me - the agent
     * @param conflict
     * 
     * @return true if other influences exist to mine, false otherwise
     * 
     */  
    public static boolean doOtherInfluencesExistToMine (Person me, ConflictDiamonds conflict) {
		
        //if already a miner, remain a miner
        if ( conflict.params.global.useMinerProportions() && me.isMiner() ) {
            return true;
        }
        //based on poverty levels and whether agent is active (as income may be better if mine)
        //the likelihood to mine is higher for those with lower income levels (or no income)
        else if ( me.isActiveLaborForce() ) {

            if ( me.getIncomeLevel() == 0 && conflict.random.nextDouble() < conflict.params.global.getMinMotivationVeryPoorLevel() ) {
                return true;
            }
            else if ( me.getIncomeLevel() == 1 && conflict.random.nextDouble() < conflict.params.global.getMinMotivationPoorLevel() ) {
                return true;
            }			
            else {
                return false;
            }
        }

        else {
                return false;
        }
    }

    /**
     * Determine if the other influences exist to rebel
     * 
     * @param me - the agent
     * @param rebelDensity - the density of current rebels within the agent's vision
     * @param conflict
     * 
     * @return true if other influences exist to rebel, false otherwise
     * 
     */ 	
    public static boolean doOtherInfluencesExistToRebel (Person me, double rebelDensity, ConflictDiamonds conflict) {

        //If resident is working as a miner, the rebel density needed for him/her
        //to rebel is lower than if the resident does not work in the mining industry, unless the resident is
        //a minor (between ages of 7 and 17)
        if ( me.isMinor() && rebelDensity >= conflict.params.global.getMinRebelDensityMinor() ) {
            return true;
        }
        else if ( conflict.diamondMinerEmployer.isEmployedHere(me) && rebelDensity >= conflict.params.global.getMinRebelDensityMiner() ) {			
            return true;
        }
        else if ( me.isEligible() && rebelDensity >= conflict.params.global.getMinRebelDensityNotMiner() ) {
            return true;
        }
        else {
            return false;
        }
    }
    
    /**
     * Determine if resident is forced to rebel
     * 
     * @param me - the agent
     * @param rebelDensity - the density of current rebels within the agent's vision
     * @param conflict
     * 
     * @return true if forced to rebel, false otherwise
     * 
     */    
    public static boolean isForcedtoRebel(Person me, double rebelDensity, ConflictDiamonds conflict) { 
        //this represents the presence of external stimuli, in this case its rebels who force residents (typically minors) to rebel
        if ( me.isMinor() && rebelDensity >= conflict.params.global.getMinRebelDensityMinor() ) {
            return true;
        }
        else if (me.isMiner() && rebelDensity >= conflict.params.global.getMinRebelDensityMiner()) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Calculate the density of rebel's within agent's vision
     * 
     * @param residingParcel - the agent's current position
     * @param conflict
     * 
     * @return rebel density
     * 
     */ 
    public static double calculateRebelDensity(Parcel residingParcel, ConflictDiamonds conflict) {
				
        //Determine density of rebels in agent vision
        Bag neighbors = new Bag(); //get a copy of all neighboring parcels
        conflict.allLand.getNeighborsMaxDistance(residingParcel.getX(), residingParcel.getY(), conflict.params.global.getAgentVision(), false, neighbors, null, null);

        long countRebel = 0;
        long countTotal = 0;
        double rebelDensity = 0;
        
        //loop through all parcels within vision and count the total number of agents and the number of rebels
        for(Object o: neighbors){
            Parcel p = (Parcel) o;			

            Collection people = new ArrayList <Person> ();
            people = p.getResidingPopulation();

            for(Object obj: people){
                Person per = (Person) obj;
                if ( per.isPersonType(per) ) countRebel++; //not sure if this is correct, double check!!
                countTotal++;
            }
        }
	
        //rebel density is the number of rebels within vision divided by the total number of agents within vision
        rebelDensity = (double) countRebel / (double) countTotal;	

        return rebelDensity;
    }
	
}
