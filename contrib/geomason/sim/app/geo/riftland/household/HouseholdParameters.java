/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package riftland.household;

import riftland.Parameters;
import riftland.parcel.GrazableArea;
import riftland.parcel.Parcel;

/** Used to make it easier to initialize a new Household with specific state.
* @see HouseholdSplitter
* @author Eric 'Siggy' Scott
*/
class HouseholdParameters
{
   private final Parameters params;
   private final int culture;
   private final int citizenship;
   private GrazableArea location;
   private boolean canHerd = true;
   private double farmArea = 0.0;
   private int numFarmers = 0;
   private int herdSize = 0;
   private int numHerders = 0;
   private int numLaborers = 0;
   private double wealth = 0;
   
   //<editor-fold defaultstate="collapsed" desc="Accessors">

    public double getFarmArea()
    {
        return farmArea;
    }

    public void setFarmArea(double farmArea) {
        this.farmArea = farmArea;
    }

    public int getNumFarmers() {
        return numFarmers;
    }

    public void setNumFarmers(int numFarmers) {
        this.numFarmers = numFarmers;
    }

    public int getHerdSize() {
        return herdSize;
    }

    public void setHerdSize(int herdSize) {
        this.herdSize = herdSize;
    }

    public int getNumHerders() {
        return numHerders;
    }

    public void setNumHerders(int numHerders) {
        this.numHerders = numHerders;
    }

    public int getNumLaborers() {
        return numLaborers;
    }

    public void setNumLaborers(int numLaborers) {
        this.numLaborers = numLaborers;
    }

    public boolean canHerd()
    {
        return canHerd;
    }

    public void setCanHerd(boolean canHerd)
    {
        this.canHerd = canHerd;
    }

    public Parameters getParams()
    {
        return params;
    }

    public GrazableArea getLocation()
    {
        return location;
    }

    public int getCulture()
    {
        return culture;
    }

    public int getCitizenship()
    {
        return citizenship;
    }

    public void setLocation(GrazableArea location)
    {
        this.location = location;
    }

	public void setWealth(double val) {
		wealth = val;
	}

	public double getWealth() {
		return wealth;
	}

    //</editor-fold>

   /** Begin with the required parameters. */
   HouseholdParameters(Parameters params, GrazableArea location, int culture, int citizenship)
   {
       assert(location != null);
       assert(params != null);
       assert(culture >= 0);
       assert(citizenship >= 0);
       
       this.params = params;
       this.location = location;
       this.culture = culture;
       this.citizenship = citizenship;
   }

   /** Return false if some value has not yet been set. */
   boolean allValuesSet()
   {
       return params != null
               && culture >= 0
               && citizenship >= 0
               && location != null
               && farmArea >= 0.0
               && numFarmers >= 0
               && !(numFarmers > 0 && farmArea == 0.0)
               && herdSize >= 0
               && numHerders >= 0
               && !(!canHerd && numHerders > 0)
               && numLaborers >= 0;
   }
}
