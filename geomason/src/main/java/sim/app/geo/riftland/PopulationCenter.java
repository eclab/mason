/*
 * PopulationCenter.java
 *
 * $Id: PopulationCenter.java 2006 2013-08-19 18:01:13Z jharri $
 *
 */

package sim.app.geo.riftland;

import sim.app.geo.riftland.household.Household;
import sim.app.geo.riftland.parcel.UrbanArea;
import sim.util.Double2D;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import ec.util.MersenneTwisterFast;

/**
 * Represents an urban area that can be comprised of one of more UrbanAreas.
 *
 */
public class PopulationCenter implements Cloneable
{
    /** All towns, cities, and villages have names */
    private String name = "";    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    /** Unique identifier for this population center. Might be used as an index elsewhere. */
    private int id;
    public int getID() { return id; }
    public void setID(int val) { id = val; }     

    /** total current number of refugees */
    private int numRefugees = 0;
    public int getNumRefugees() { return numRefugees; }
    public void setNumRefugees(int val) { numRefugees = val; }	// TODO this should go away, since numRefugees should be set only by adding households

    /** total current population of non-refugees */
    private int urbanites = 0;
    public int getUrbanites() { return urbanites; }
    public void setUrbanites(int val) { urbanites = val; }
    
    /** A reflection of the combined refugee capacity and neighbor's capacity. */
    private double aggregateCapacity = 0;
    public double getAggregateCapacity() { return aggregateCapacity; }
    public void setAggregateCapacity(double val) { aggregateCapacity = val; }

    /** the centroid of this population center */
    private Double2D centroid = null;    
    public Double2D getCentroid() { return centroid; }
    public void setCentroid(Double2D val) { centroid = val; }
    
    private ArrayList<Household> refugees = new ArrayList<Household>();
    private ArrayList<RefugeeGroup> refugeeGroups = new ArrayList<RefugeeGroup>();

    public PopulationCenter()
    {
    }   

    public PopulationCenter(String name, int id, int urbanites, Double2D centroid) {
		super();
		this.name = name;
		this.id = id;
		this.urbanites = urbanites;
		this.centroid = centroid;
	}


    /** @param ID unique ID # of population center
     *
     * This ID is converted to a string and assigned to the name.
     */
    public PopulationCenter(Integer ID)
    {
    	id = ID;
        name = ID.toString();
    }

	public Object clone() {
		try {
			return super.clone();
		}
		catch (CloneNotSupportedException e) {
			return null; 
		}
	}
    
    public void addRefugees(Household h) {
    	refugees.add(h);
    	numRefugees += h.getDisplacing().getPopulation();
    }
    
    /**
     * Add the given refugee group to this city. If this is the group's final destination,
     * absorb them into the local population of refugees and dissolve the group.
     */
    public void addRefugees(RefugeeGroup rg) {
    	// if this is the group's destination, absorb them
    	if (rg.destination == this)
    		refugees.addAll(rg.households);
    	else
    		refugeeGroups.add(rg);
    	numRefugees += rg.getPopulation();
    }
    
    public void removeRefugees(Household h) {
    	refugees.remove(h);
    	numRefugees -= h.getDisplacing().getPopulation();
    }
    
    public void removeRefugees(RefugeeGroup rg) {
    	refugeeGroups.remove(rg);
    	numRefugees -= rg.getPopulation();
    }
    
    public void clearRefugees() {
    	refugeeGroups.clear();
    	numRefugees = 0;
    }
    

    /** 
     * Create a refugee group filled with refugee households, removing them from
     * population center.
     */
    public RefugeeGroup spawnRefugeeGroup(int numHouseholds, MersenneTwisterFast random) {
    	if (refugees.isEmpty())
    		return null;
    	
    	RefugeeGroup group = new RefugeeGroup();
    	
    	for (int i = 0; (i < numHouseholds) && !refugees.isEmpty(); i++) {
    		// pick a random household
    		int index = random.nextInt(refugees.size());
    		group.addHousehold(refugees.get(index));
    		numRefugees -= refugees.get(index).getDisplacing().getPopulation();
    		refugees.remove(index);
    	}
    	
    	group.currentCity = this;
    	this.addRefugees(group);
    	
    	return group;
    }
    
    /**
     * If there are already refugee groups in this city, randomly pick one and return it.
     * @return
     */
//    public RefugeeGroup expellRefugeeGroup(int numHouseholds, World world) {
//    	if (refugeeGroups.size() == 0)
//    		return spawnRefugeeGroup(numHouseholds, world);
//    	
//    	RefugeeGroup rg = refugeeGroups.get(world.random.nextInt(refugeeGroups.size()));
//    	removeRefugees(rg);
//    	
//    	return rg;
//    }
    
    public int getRefugeeGroupPopulation() {
    	int total = 0;
    	for (RefugeeGroup rg : refugeeGroups)
    		total += rg.getPopulation();
    			
    	return total;
    }

    /** find work for current refugees
     *
     * Some lucky refugees are able to find work. This means moving some numbers
     * from this.refugees to this.urbanites.
     *
     * XXX However, would it also be possible to employ refugees from a nearby
     * RefugeeGroup?  Or would that group have to be absorbed first?
     *
     * TODO: implement
     */
    public void employRefugees() {
        // TODO
    }
    
    public double refugeeCapacity = 0;
    public double getRefugeeCapacity() { return refugeeCapacity; }
    public void setRefugeeCapacity(double val) { refugeeCapacity = val; }
    
    public void consumeCapacity(double rate) {
    	refugeeCapacity = Math.max(0, refugeeCapacity - numRefugees * rate);
    }
    
//    public double getRefugeeCapacity() { return 0.1 * urbanites; }
    
    public double getRefugeeLoad() {
    	return (double)numRefugees / (double)getRefugeeCapacity();
    }
    
    public double getSpareRefugeeCapacity() {
    	return getRefugeeCapacity() - numRefugees;
    }
    
    public int getNumRefugeeHouseholds() {
    	return refugees.size();
    }
    
    public int getNumRefugeeGroups() {
    	return refugeeGroups.size();
    }
    
    public String toString() {
    	return name;
    }

//    @Override
//    public boolean equals(Object obj) {
//        if (obj == null) {
//            return false;
//        }
//        if (getClass() != obj.getClass()) {
//            return false;
//        }
//        final PopulationCenter other = (PopulationCenter) obj;
//        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
//            return false;
//        }
//        if (this.centroid != other.centroid && (this.centroid == null || !this.centroid.equals(other.centroid))) {
//            return false;
//        }
//        return true;
//    }
//
//    @Override
//    public int hashCode() {
//        int hash = 3;
//        hash = 23 * hash + (this.name != null ? this.name.hashCode() : 0);
//        hash = 23 * hash + (this.centroid != null ? this.centroid.hashCode() : 0);
//        return hash;
//    }


}
