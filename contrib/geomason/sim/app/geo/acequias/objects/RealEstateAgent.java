package objects;

/** <b>RealEstateAgent</b> the source of pressure 
 * 
 * The RealEstateAgent represents all of the people trying to convert agricultural land to residential
 * land. When activated, they pick a Parciante and make him an offer for his land, depending on the qualities
 * of the land and his own budget.
 * 
 * @author Sarah Wise and Andrew Crooks
 */
public class RealEstateAgent {
	
	double budget = 0; // SET OUTSIDE
	double weight_utility = .5;
	double weight_proximity = .5;
	double bsquared = 100;
	
	/**
	 * constructor
	 * @param b - the budget of the real estate agent
	 */
	public RealEstateAgent( double b){ budget = b; }
	
	/**
	 * Make an offer to the Parciante p for his land
	 * 
	 * @param p - the Parciante whose land is being considered
	 * @return - the asking price the RealEstateAgent is willing to pay of p's land
	 */
	public int formulateOffer( Parciante p ){
		
		// budget
		double budgetConstraint = budget - p.distanceToRoad;
		
		// utility is determined by the size of the parcel, here
		int size = p.getParcel().size();
		double utility = Math.pow( size, weight_utility) * 
			Math.pow( p.distanceToCityCenter, weight_proximity);
		double utilitySquared = Math.pow( utility, 2 );
		
		// so we get an asking price!
		int askingPrice = (int) (budgetConstraint * utilitySquared / 
			( bsquared + utilitySquared));
		return askingPrice;
	}
}