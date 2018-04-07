package conflictdiamonds;

import java.util.ArrayList;
import java.util.Random;
import conflictdiamonds.ConflictDiamonds.Goal;
import conflictdiamonds.ConflictDiamonds.Action;
import sim.engine.Stoppable;
import sim.util.*;

/**
 * The person agent represents the individuals in the model. 
 * The person can either be a resident or a rebel.
 * 
 * 
 * @author bpint
 *
 */
public abstract class Person {
	
    ConflictDiamonds conflict;
    Parcel residingParcel; //the current position of the person
    Region residingRegion; //the person's residing region (impacts socioeconomic conditions)
    private boolean opposition; //identifies if the person is part of the initial group of rebels
    private int incomeLevel; //the person's income level (0, 1, 2)
    OtherEmployers otherEmployer; //the employer a person can be assigned to (if employed)
    DiamondMiner diamondMiner; //the employer a person is assigned to if working in diamond mines
    
    Stoppable stopper;	
    
    private boolean eligible = false; //person is eligible (age 5-64) to work in informal market
    private boolean activeLaborForce = false; //person is part of active labor force (employed or unemployed)

    private boolean minor = false; //this is someone over 6 but under 18
    private boolean isEmployedHousehold = false;
    private boolean isEmployed = false; //this is the employment status of an active resident
    private boolean isMiner = false; //this is someone that is employed and is a miner
    private boolean isInitialRebel;

    private double unemployed;	//probability person is unemployed
    private Goal currentGoal;	//the person's current goal
    private Action currentAction; //the action the person is currently taking

    private static final long serialVersionUID = 1L;		
    
    public Person() {
    	super();
    }
  
    public Person( ConflictDiamonds c, Parcel p, Region r ) {
    	conflict = c;
    	residingParcel = p;
        residingRegion = r;
    	stopper = null;   	  	
    }   
     
    //Each person is given an income level depending on poverty rates in the region which they reside
    //Income level = 0 (very poor), 1 (poor), 2 (not poor)
    public double determineIncomeLevel() {	
        double probFoodPoor = residingParcel.getRegion().getFoodPoorRate();
        double probTotalPoor = residingParcel.getRegion().getTotalPoorRate();

        double rpoor = conflict.random.nextDouble();
        
        //assign income based on empirical data
        if ( isEmployedHousehold == false ) this.incomeLevel = 0;
        else if ( rpoor < (probFoodPoor - unemployed) ) this.incomeLevel = 0;
        else if ( rpoor < probTotalPoor ) this.incomeLevel = 1;
        else this.incomeLevel = 2;

        return incomeLevel;
    }
	
    public void determineLaborStatistics() {
	//assign employment status and age based on empirical data
        double employed = residingParcel.getRegion().getPercentEmployed();
        double laborForce = residingParcel.getRegion().getPercentLaborForce();
        double miner = residingParcel.getRegion().getPercentMiners();
        
        double age0to4 = residingParcel.getRegion().getPercent0to4();
        double age5to6 = residingParcel.getRegion().getPercent5to6();
        double age7to14 = residingParcel.getRegion().getPercent7to14();
        double age15to17 = residingParcel.getRegion().getPercent15to17();
        double age18to64 = residingParcel.getRegion().getPercent18to64();
        
        double rage = conflict.random.nextDouble();
        double rlabor = conflict.random.nextDouble();
        double rminer = conflict.random.nextDouble();
        
        double minorMinRange = age0to4 + age5to6;
        double minorMaxRange = minorMinRange + age7to14 + age15to17;
        
        double minerMinRange = age0to4 + age5to6;
        double minerMaxRange = minerMinRange + age7to14 + age15to17 + age18to64;
        
        double activeLaborForceMinRange = age0to4 + age5to6 + age7to14;
        double activeLaborForceMaxRange = activeLaborForceMinRange + age15to17 + age18to64;
        
        //person is a minor if between the ages of 7 and 17 (eligible to be a child soldier)
        if (rage >= minorMinRange && rage <= minorMaxRange) {
            minor = true;
        }
        //person is eligible to mine
        if (rage >= minerMinRange && rage <= minerMaxRange) {
            eligible = true;
        }

        //determine if resident is in active labor force (i.e. eligible to work in formal market)
        if (rage >= activeLaborForceMinRange && rage <= activeLaborForceMaxRange) {
            laborForce = laborForce / (activeLaborForceMaxRange - activeLaborForceMinRange);
            
            if ( rlabor < laborForce ) {
                activeLaborForce = true;
            }
        }

        //determine if resident is employed
        if ( activeLaborForce == true ) {
            employed = employed / (activeLaborForceMaxRange - activeLaborForceMinRange);
            
            if ( rlabor < employed ) {
                this.setOtherEmployer(conflict.otherEmployer);
                conflict.otherEmployer.addEmployee(this);
                isEmployed = true; //the resident is employed
                isEmployedHousehold = true; //the household that the resident resides is employed

                if ( rminer < miner ) {
                    this.isMiner = true;
                }
            }
        }

        //determine if resident is part of an unemployed household (this is a rough estimate)
        //the average number of household members that are active is 2
        if ( isEmployedHousehold == false ) {
            unemployed = laborForce - employed;
            if ( unemployed < 0 ) { unemployed = 0; }
            
            unemployed = unemployed * unemployed; //the probability that a resident is part of an unemployed household, is the probability that both active household members are unemployed

            if ( conflict.random.nextDouble() > unemployed ) {
                isEmployedHousehold = true;
            }		
        }		
    }
	
    //behaviors -- based on PECS
    //run the intensity analyzer and execute the associated action
    public void determineBehavior() {
        currentGoal = IntensityAnalyzer.runIntensityAnalyzer(this, conflict);
        setCurrentAction(ActionSequence.runActionSequence(this, currentGoal, conflict));
    }	
    
    //if person becomes a rebel, initialize person as rebel
    public void rebel() {
	//assign rebel a parcel and region
        Parcel rebelparcel = residingParcel;
        Region region = residingParcel.getRegion();
        
        //resident is now a rebel and no longer works in the formal or informal market
        if ( conflict.otherEmployer.isEmployedHere(this) )  {
            conflict.otherEmployer.removeEmployee(this);
            residingRegion.removeFormalEmployee(this);
        }
        if ( conflict.diamondMinerEmployer.isEmployedHere(this) ) {
            conflict.diamondMinerEmployer.removeEmployee(this);
            residingRegion.removeInformalEmployee(this);
        }
        //initialize ner rebel
        Rebel rebel = new Rebel(conflict, residingParcel, region);  
   
        region.addRebels(rebel);
        region.removePerson(this);
        region.addPerson(rebel);
 
        conflict.allRebels.add(rebel);
        rebel.setResidingRegion(region);
        rebel.setResidingParcel(rebelparcel);
        rebelparcel.addPopulation(rebel);        
        conflict.allPopulation.setObjectLocation(rebel, rebelparcel.getX(), rebelparcel.getY());  
        
        residingParcel.removePerson(this);
        this.setResidingParcel(null);	

        conflict.allPopulation.remove(this);
        conflict.allResidents.remove(this);
        
        conflict.schedule.scheduleOnce(rebel);
    
    }
    
    //move to my goal
    public void move() {
        //get a copy of all neighboring parcels
        Bag neighbors = new Bag();

        conflict.allLand.getNeighborsMaxDistance(residingParcel.getX(), residingParcel.getY(), 1, false, neighbors, null, null);

        // find the set of neighbors that has max opportunity
        ArrayList <Parcel> maxOpp = new ArrayList <Parcel> ();
        double currentremote = residingParcel.getRemoteness();	
        
        //move to a parcel that reduces my risk (i.e. remoteness is high)
        for(Object o: neighbors){
            Parcel p = (Parcel) o;
            if(p.getRegion().getRegionID() == 0 ) continue; // the tile is outside of the modeling world, can't move there
            else if( p.getRemoteness() <= currentremote && p.getDiamondMineDistance() == 0 ) {
                maxOpp.add(p); // add our new find to it
            }
        }	
					
        if(maxOpp.size() > 0){ // somewhere is more desirable
            // select randomly from the eligible neighbors
            Random rand = new Random();
            Parcel newparcel = maxOpp.get( rand.nextInt( maxOpp.size()) );
            Region newregion = newparcel.getRegion();            
            
            //if person moved to a new region, remove from old region and add to new one
            if (residingRegion != newregion) {
                
                if (this instanceof Rebel) {
                    Rebel me = (Rebel) this;
                    residingRegion.removeRebels(me);
                    newregion.addRebels(me);
                }
                
                //remove me from all collections in old region
                if (residingRegion.getActiveLaborMarket().contains(this)) {
                    residingRegion.removeActiveLaborMarket(this);
                    newregion.addActiveLaborMarket(this);
                }
                if (residingRegion.getEligibleToMine().contains(this)) {
                    residingRegion.removeEligibleToMine(this);
                    newregion.addEligibleToMine(this);
                }
                if (residingRegion.getFoodPoor().contains(this)) {
                    residingRegion.removeFoodPoor(this);
                    newregion.addFoodPoor(this);
                }
                if (residingRegion.getFormalEmployees().contains(this)) {
                    residingRegion.removeFormalEmployee(this);
                    newregion.addFormalEmployee(this);
                }
                if (residingRegion.getGoalFindInformalEmployment().contains(this)) {
                    residingRegion.removeGoalFindInformalEmployment(this);
                    newregion.addGoalFindInformalEmployment(this);
                }
                if (residingRegion.getGoalRebel().contains(this)) {
                    residingRegion.removeGoalRebel(this);
                    newregion.addGoalRebel(this);
                }
                if (residingRegion.getGoalRemainEmployed().contains(this)) {
                    residingRegion.removeGoalRemainEmployed(this);
                    newregion.addGoalRemainEmployed(this);
                }
                if (residingRegion.getGoalStayHome().contains(this)) {
                    residingRegion.removeGoalStayHome(this);
                    newregion.addGoalStayHome(this);
                }
                if (residingRegion.getInformalEmployees().contains(this)) {
                    residingRegion.removeInformalEmployee(this);
                    newregion.addInformalEmployee(this);
                }
                if (residingRegion.getInitialRebel().contains(this)) {
                    residingRegion.removeInitialRebel(this);
                    newregion.addInitialRebel(this);
                }
                if (residingRegion.getMinors().contains(this)) {
                    residingRegion.removeMinors(this);
                    newregion.addMinors(this);
                }
                if (residingRegion.getNotPoor().contains(this)) {
                    residingRegion.removeNotPoor(this);
                    newregion.addNotPoor(this);
                }
                if (residingRegion.getTotalPoor().contains(this)) {
                    residingRegion.removeTotalPoor(this);
                    newregion.addTotalPoor(this);
                }
                
                residingRegion.removePerson(this);               
                this.setResidingRegion(newregion);
                newregion.addPerson(this);
 
                residingParcel.removePerson(this);              
                newparcel.addPopulation(this);
                this.setResidingParcel(newparcel);             
                
                conflict.allPopulation.setObjectLocation(this, newparcel.getX(), newparcel.getY());          
            }
            
            else {
                // move to this new spot
                residingParcel.removePerson(this);
                newparcel.addPopulation(this);
                this.setResidingParcel(newparcel);
                conflict.allPopulation.setObjectLocation(this, newparcel.getX(), newparcel.getY());
            }
            
        }			
    }
	
    //getters and setters
    public void setOpposition(boolean opp) { opposition = opp; }    
    public boolean getOpposition() { return opposition; }

    public void setIncomeLevel(int inc) { incomeLevel = inc; }	
    public int getIncomeLevel() { return incomeLevel; }

    public void setResidingParcel(Parcel p) { residingParcel = p; }	
    public Parcel getResidingParcel() { return residingParcel; }
    
    public void setResidingRegion(Region r) { residingRegion = r; }	
    public Region getResidingRegion() { return residingRegion; }

    public void setDiamondMiner(DiamondMiner inf) { diamondMiner = inf; }
    public DiamondMiner getDiamondMiner() { return diamondMiner; }

    public void setOtherEmployer(OtherEmployers formal) { otherEmployer = formal; }
    public OtherEmployers getOtherEmployer() { return otherEmployer; }	

    public boolean isEligible() { return eligible; }
    public void setEligible(boolean eligible) { this.eligible = eligible; }

    public boolean isMinor() { return minor; }
    public void setMinor(boolean minor) { this.minor = minor; }

    public boolean isEmployedHousehold() { return isEmployedHousehold; }
    public void setEmployedHousehold(boolean isEmployedHousehold) { this.isEmployedHousehold = isEmployedHousehold; }

    public boolean isEmployed() { return isEmployed; }
    public void setEmployed(boolean isEmployed) { this.isEmployed = isEmployed; }

    public double isUnemployed() { return unemployed; }
    public void setUnemployed(double unemployed) { this.unemployed = unemployed; }

    public boolean isActiveLaborForce() { return activeLaborForce; }
    public void setActiveLaborForce(boolean activeLaborForce) { this.activeLaborForce = activeLaborForce; }

    public Goal getCurrentGoal() { return currentGoal; }
    public void setCurrentGoal(Goal currentGoal) { this.currentGoal = currentGoal; }

    public Action getCurrentAction() { return currentAction; }
    public void setCurrentAction(Action currentAction) { this.currentAction = currentAction; }

    public boolean isMiner() { return isMiner; }
    public void setMiner(boolean isMiner) { this.isMiner = isMiner; }
    
    public boolean isInitialRebel() { return isInitialRebel; }
    public void setInitialRebel(boolean isInitialRebel) { this.isInitialRebel = isInitialRebel; }

    public abstract boolean isPersonType(Person obj);
	
}
