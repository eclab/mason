package conflictdiamonds;

import java.util.ArrayList;
import java.util.Collection;
import sim.engine.*;

/**
 * The Parcel object are used to create the landscape. All other objects reside on parcels.
 * They store information such as population density, remoteness, and distance to diamond mines.
 * 
 * @author bpint
 *
 */
public class Parcel {
	
    private int density;  // density of population on parcel
    private double remoteness; //variable for assessing government control
    private double diamondMineDistance; //variable for assessing resource availability
		
    public static final int ORDERING = 1; // scheduling parcel first
    ConflictDiamonds c;
    private Region region;
    private Collection<Person> residingPopulation; //the set of agents residing in the region
    private int xLoc; //the parcels x location
    private int yLoc; //the parcels y location

    Parcel(){
       super();
    }
    
    Parcel(ConflictDiamonds conflictDiamonds, Region r){
        c = conflictDiamonds;
        region = r;
        residingPopulation = new ArrayList<Person>();
    }
		
    Parcel(int x, int y) {
        xLoc = x;
        yLoc = y;
        residingPopulation = new ArrayList<Person>();
    }
		
    Parcel(ConflictDiamonds conflictDiamonds){
        c = conflictDiamonds;
        residingPopulation = new ArrayList<Person>();

    }
    
    //the remoteness of the parcel in terms of distance from cities and highways
    public double getRemoteness() { return remoteness; }
    public void setRemoteness(double remote) { remoteness = remote; }
    
    //the distance from diamond mines
    public double getDiamondMineDistance() { return diamondMineDistance; }
    public void setDiamondMineDistance(double diamondDist) { diamondMineDistance = diamondDist; }
    
    //the region the parcel is located in
    public Region getRegion() { return region; }
    public void setRegion(Region r) { region = r; }	

    // hold population density. parcel can have different population densities
    public int getPopulationDensity(){ return density; }	    
    public void setPopulationDensity(int density){ this.density = density; }
    public void addPopulation(Person p) { residingPopulation.add(p); }
	    
     // remove a Person
    public void removePerson(Person p){ residingPopulation.remove(p); }	
    public Collection getResidingPopulation() { return residingPopulation; }
	    	    
    public void step(SimState state) {
        //anyPopulation();
    }
    // location X
    final public int getX() {return xLoc;}
    final public void setX(int x){this.xLoc = x;}

    // location Y
    final public int getY(){return yLoc;}
    final public void setY(int y){this.yLoc = y;}

}
