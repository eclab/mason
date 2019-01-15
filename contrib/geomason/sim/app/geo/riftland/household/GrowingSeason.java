/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sim.app.geo.riftland.household;

import sim.app.geo.riftland.Parameters;

/**
 *
 * Single growing season that updates its plant date and season length.
 * This should make it easier to have two seasons which are mostly independent
 * but are constrained to not overlap.
 * 
 * @author Joey Harrison
 * @author Tim Gulden
 *
 */

public class GrowingSeason {
	/** Date on which crops are to be planted. */ 
	private int plantDate = 0;
	
	/** Length of the growing season */ 
	private int seasonLength = 90;
	
//	/** Level of rain two weeks before the start of the growing season. 
//	 * This will be compared against the rain during the two weeks before
//	 * harvest to determine a new plant date.
//	 */
//	private double rainBeforeStart = 0;
//	
//	/**
//	 * Amount of crop measured 7 days before harvest. This will be compared against
//	 * the amount of crop actually harvested to determine the new season length
//	 */
//	private double cropDensityBeforeEnd = 0;
//        
       Farming farm;
       
    
    public GrowingSeason(Farming myFarm, int targetHarvestDate, Parameters params){
        farm = myFarm;
        plantDate = targetHarvestDate - params.farming.getInitialPlantingSeasonLength();
        seasonLength = params.farming.getInitialPlantingSeasonLength();
    }
    
    public GrowingSeason(GrowingSeason other) {
        plantDate = other.plantDate;
        seasonLength = other.seasonLength;
    }

    public int getPlantDate(){
        return plantDate;
    }
    
    public int getHarvestDate(){
        return plantDate + seasonLength;
    }

    public int getSeasonLength(){
        return seasonLength;
    }

    /**
     * Update the planting date of this growing season. This will be based on
     * the amount of rain before the season compared to the rain at the end.
     * This function will also prevent this season from overlapping with another
     * season.
     * @param harvestDensity
     * @param rainBeforeStart
     * @param rainAtEnd
     * @param otherSeasonHarvestDate 
     */
    public void updatePlantDate(double harvestDensity, double rainBeforeStart, double rainAtEnd, GrowingSeason other) {
//        System.out.println("DB:" + farm.getRainBeforeGrowingSeason() + " DE: " + farm.getRainAtEndofGrowingSeason());
        // if the harvest was a complete bust, move forward 90 days in hopes of catching a better season.
        if (harvestDensity <= 0){
            //System.out.println("Harvest Density:" + harvestDensity + "........................................................");
            plantDate += 90;
        } else
        
            if (rainBeforeStart > rainAtEnd){
                plantDate -= 14;
//              System.out.println("earlier");
            } else {
                plantDate += 14;
//              System.out.println("later");
            }
        
        // advance updated plant date to next year
        plantDate += 365;
        
        if (plantDate < other.getHarvestDate()) {
            plantDate = other.getHarvestDate() + 1;
        }

    }

    public void restartPlantDate(int today){
            while (plantDate < today) {
                plantDate += 365;
            }
    }
    /**
     * Update the length of the growing season by comparing the actual harvest 
     * to an hypothetical (alt) harvest.
     * @param harvestPFD size of the harvest in person-food-days
     * @param altHarvestPFD size the alternate hypothetical harvest in person-food-days
     * @param farmPopulation number of farmers
     */
    public void updateSeasonLength(double harvestPFD, double altHarvestPFD, int farmPopulation) {
//        System.out.println("LB:" + farm.getVegetationBeforeHarvesting() + " LA: " + farm.getCropVegetationDensity());
        
        // if the last week of the growing season didn't grow enough to feed the farmers
        // for a week, it would have been better to harvest a week earlier.
        if (altHarvestPFD > (harvestPFD - farmPopulation*7)) {
            seasonLength -= 7;
//            System.out.println("shorter");
        } else {
            seasonLength += 7;
//            System.out.println("longer");
        }
        if (seasonLength < 14) seasonLength = 14;
        if (seasonLength > 120) seasonLength = 120;
    }    
    
}
