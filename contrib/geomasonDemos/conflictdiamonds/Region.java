package conflictdiamonds;

import java.util.*;
import sim.util.Bag;

/**
 * Sierra Leone is divided into 14 districts/regions
 * Data for the regions is stored here
 * 
 * @author bpint
 *
 */
public class Region {
	
    private int regionID;
    private Collection<Parcel> regionParcels;
    private ArrayList<Person> population;
    ConflictDiamonds c;
    
    //variables used to assign socioeconoic variables to the agents of each region
    private double foodPoorRate; //the percent of residents with an income level 0
    private double totalPoorRate; //the percent of residents with an income level of 1
    private double percentEmployed; //the percent of residents that are employed
    private double percentLaborForce; //the percent of residents in the active labor force
    private double eligibleToWorkAsMiner; //the percent eligible to work in diamond mines
    private double percentMiners; //the percent of miners
    private double percent0to4; //the percent of residents under the age of 5
    private double percent5to6; //the percent of residents between 5 and 6
    private double percent7to14; //the percent of residents between 7 and 14
    private double percent15to17; //the percent of residents between 15 and 17
    private double percent18to64; //the percent of residents between 18 and 65
    private double percent65Over; //the percent over 64
    
    //store agents based on their attributes for tracking and reporting purposes
    private Bag rebels;
    private Bag formalEmployees;
    private Bag informalEmployees;
    private Bag minors;
    private Bag activeLaborMarket;
    private Bag foodPoor;
    private Bag totalPoor;
    private Bag notPoor;
    private Bag eligibleToMine;
    
    //store agent information based on their current goal for tracking and reporting purposes
    private Bag goalStayHome;
    private Bag goalFindInformalEmployment;
    private Bag goalRemainEmployed;
    private Bag goalRebel;
    
    //store agent's that are inital rebels for tracking and reporting purposes
    private Bag initialRebel;
    
    public Region(int id, double density) {
        //initialize variables
        setRegionID(id);
        regionParcels = new ArrayList<Parcel>();
        population = new ArrayList<Person>();
        rebels = new Bag();       
        formalEmployees = new Bag();
        informalEmployees = new Bag();
        minors = new Bag();
        activeLaborMarket = new Bag();
        foodPoor = new Bag();
        totalPoor = new Bag();
        notPoor = new Bag();
        eligibleToMine = new Bag();
        goalStayHome = new Bag();
        goalFindInformalEmployment = new Bag();
        goalRemainEmployed = new Bag();
        goalRebel = new Bag();
        initialRebel = new Bag();
    }
	
    public Region(ConflictDiamonds conflictDiamonds) {
        c = conflictDiamonds;
        regionParcels = new ArrayList<Parcel>();
        rebels = new Bag();
        population = new ArrayList<Person>();
        formalEmployees = new Bag();
        informalEmployees = new Bag();
        minors = new Bag();
        activeLaborMarket = new Bag();
        foodPoor = new Bag();
        totalPoor = new Bag();
        notPoor = new Bag();
        eligibleToMine = new Bag();
        goalStayHome = new Bag();
        goalFindInformalEmployment = new Bag();
        goalRemainEmployed = new Bag();
        goalRebel = new Bag();
        initialRebel = new Bag();
    }

    public Region(ConflictDiamonds conflictDiamonds, int id) {
        setRegionID(id);
        c = conflictDiamonds;
        regionParcels = new ArrayList<Parcel>();
        rebels = new Bag();
        population = new ArrayList<Person>();
        formalEmployees = new Bag();
        informalEmployees = new Bag();
        minors = new Bag();
        activeLaborMarket = new Bag();
        foodPoor = new Bag();
        totalPoor = new Bag();
        notPoor = new Bag();
        eligibleToMine = new Bag();
        goalStayHome = new Bag();
        goalFindInformalEmployment = new Bag();
        goalRemainEmployed = new Bag();
        goalRebel = new Bag();
        initialRebel = new Bag();
    }
    
    //getter and setters
    public void addRegionParcels(Parcel par) { regionParcels.add(par); }
    public Collection getRegionParcels() { return regionParcels; }
    
    public void addRebels(Rebel r) { rebels.add(r); }
    public void removeRebels(Rebel r) { rebels.remove(r); }
    public Bag getRebels() { return rebels; }
    
    public void addPerson(Person p) { population.add(p); }
    public void removePerson(Person p) { population.remove(p); }
    public ArrayList<Person> getResidingPopulation() { return population; }
	
    public void addFormalEmployee(Person r) { formalEmployees.add(r); }
    public void removeFormalEmployee(Person r) { formalEmployees.remove(r); }
    public Bag getFormalEmployees() { return formalEmployees; }
    
    public void addInformalEmployee(Person r) { informalEmployees.add(r); }
    public void removeInformalEmployee(Person r) { informalEmployees.remove(r); }
    public Bag getInformalEmployees() { return informalEmployees; }
    
    public void addMinors(Person r) { minors.add(r); }
    public void removeMinors(Person r) { minors.remove(r); }
    public Bag getMinors() { return minors; }
    
    public void addActiveLaborMarket(Person r) { activeLaborMarket.add(r); }
    public void removeActiveLaborMarket(Person r) { activeLaborMarket.remove(r); }
    public Bag getActiveLaborMarket() { return activeLaborMarket; }
    
    public void addFoodPoor(Person r) { foodPoor.add(r); }
    public void removeFoodPoor(Person r) { foodPoor.remove(r); }
    public Bag getFoodPoor() { return foodPoor; }
    
    public void addTotalPoor(Person r) { totalPoor.add(r); }
    public void removeTotalPoor(Person r) { totalPoor.remove(r); }
    public Bag getTotalPoor() { return totalPoor; }
    
    public void addNotPoor(Person r) { notPoor.add(r); }
    public void removeNotPoor(Person r) { notPoor.remove(r); }
    public Bag getNotPoor() { return notPoor; }
    
    public void addEligibleToMine(Person r) { eligibleToMine.add(r); }
    public void removeEligibleToMine(Person r) { eligibleToMine.remove(r); }
    public Bag getEligibleToMine() { return eligibleToMine; }
    
    public void addGoalStayHome(Person r) { goalStayHome.add(r); }
    public void removeGoalStayHome(Person r) { goalStayHome.remove(r); }
    public Bag getGoalStayHome() { return goalStayHome; }
    
    public void addGoalFindInformalEmployment(Person r) { goalFindInformalEmployment.add(r); }
    public void removeGoalFindInformalEmployment(Person r) { goalFindInformalEmployment.remove(r); }
    public Bag getGoalFindInformalEmployment() { return goalFindInformalEmployment; }
    
    public void addGoalRemainEmployed(Person r) { goalRemainEmployed.add(r); }
    public void removeGoalRemainEmployed(Person r) { goalRemainEmployed.remove(r); }
    public Bag getGoalRemainEmployed() { return goalRemainEmployed; }
    
    public void addGoalRebel(Person r) { goalRebel.add(r); }
    public void removeGoalRebel(Person r) { goalRebel.remove(r); }
    public Bag getGoalRebel() { return goalRebel; }
    
    public void addInitialRebel(Person r) { initialRebel.add(r); }
    public void removeInitialRebel(Person r) { initialRebel.remove(r); }
    public Bag getInitialRebel() { return initialRebel; }
    
    public void setRegionID(int id) { regionID = id; }
    public int getRegionID() { return regionID; }

    public void setPovertyRate(double frate, double trate) {
        foodPoorRate = frate;
        totalPoorRate = trate;
    }

    public double getFoodPoorRate() { return foodPoorRate; }
    public double getTotalPoorRate() { return totalPoorRate; }
	
    //public void setLaborStats(double employed, double laborForce, double under5, double under15, double under65, double miner) {
    public void setLaborStats(double employed, double laborForce, double age0to4, double age5to6, double age7to14, double age15to17, double age18to64, double age65Over, double miner) {
        percentEmployed = employed;
        percentLaborForce = laborForce;
        percentMiners = miner;
        
        percent0to4 = age0to4;
        percent5to6 = age5to6;
        percent7to14 = age7to14;
        percent15to17 = age15to17;
        percent18to64 = age18to64;
        percent65Over = age65Over;
        
    }
	
    public double getPercentEmployed() { return percentEmployed; }
    public double getPercentLaborForce() { return percentLaborForce; }
    public double getPercentEligibleInformal() { return eligibleToWorkAsMiner; }

    public double getPercent0to4() { return percent0to4; }
    public void setPercent0to4(double val) { percent0to4 = val; }
    
    public double getPercent5to6() { return percent5to6; }
    public void setPercent5to6(double val) { percent5to6 = val; }
    
    public double getPercent7to14() { return percent7to14; }
    public void setPercent7to14(double val) { percent7to14 = val; }
    
    public double getPercent15to17() { return percent15to17; }
    public void setPercent15to17(double val) { percent15to17 = val; }
    
    public double getPercent18to64() { return percent18to64; }
    public void setPercent18to64(double val) { percent18to64 = val; }
    
    public double getPercent65Over() { return percent65Over; }
    public void setPercent65Over(double val) { percent65Over = val; }
 
    public double getPercentMiners() { return percentMiners; }
    public void setPercentMiners(double percentMiners) { this.percentMiners = percentMiners; }
    
    
	
}
