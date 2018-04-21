package objects;

import java.util.ArrayList;

import AcequiaWorld;
import WaterSystem;

import sim.field.grid.ObjectGrid2D;
import sim.field.network.Edge;
import sim.util.Bag;


/** <b>Parciante</b> The critical actor in the system. 
 * 
 * Parciantes own land, farm, and maintain acequias. Depending on their own attitudes (called "strategies"
 * here) they may choose to sell their land when an offer is made. They are the focus of the simulation.
 *
 * @author Sarah Wise and Andrew Crooks
 */
public class Parciante {
	
	// --- PARAMETERS ---

	int strategy = 0;
	int distanceToRoad = -1; // set after tract of land is assigned
	int distanceToCityCenter = -1; // set after tract of land is assigned

	double money = 100; // the Parciante's current bank account. The agent starts with some buffer

	// Parciante strategies
	int STRATEGY_SELLATONCE = 0;
	int STRATEGY_TRADITIONALIST = 1;
	int STRATEGY_SHEEP = 2;

	// --- OBJECTS ---
	
	AcequiaWorld world;

	// Physical characteristics of the land - PHYSICAL
	Acequia acequia = null; // acequia with which this parciante is associated
	ArrayList <Tile> parcel = new ArrayList <Tile> ();
	ArrayList <Tile> acequiaTiles = new ArrayList <Tile> ();
	ArrayList <Edge> acequiaLinks = new ArrayList <Edge> ();

	
	/**
	 * Constructor of a Parciante, with the given traits
	 * 
	 * @param world - a copy of the AcequiaWorld
	 * @param selectedStrategy - the strategy this agent adotps
	 * @param land - the set of tiles the Parciante owns
	 * @param acequiaTiles - the set of tiles which border the acequia
	 * @param acequiaLinks - the set of links which are adjacent to the Parciante's land
	 */
	public Parciante(AcequiaWorld world, int selectedStrategy, ArrayList <Tile> land,
                     ArrayList <Tile> acequiaTiles, ArrayList <Edge> acequiaLinks){
		
		// --- save the objects ---
		
		this.world = world;
		this.strategy = selectedStrategy;
		
		
		// --- save the land and acequia info ---
		
		parcel = land;
		this.acequiaTiles = acequiaTiles;
		this.acequiaLinks = acequiaLinks;
		
		ObjectGrid2D tiles = world.getTiles();
		
		// calculate the distance to the city and check if road lies on property
		double bestDist = Double.MAX_VALUE;
		Tile cityCenter = world.getCityCenter();
		for(Tile t: land){
			
			// calculate the distance from this tile to the city center
			double dist = distance(t, cityCenter);
			
			// save the smallest distance
			if( dist < bestDist )
				bestDist = dist;
			
			if( t.road )
				distanceToRoad = 0;
		}
		// normalize the value!
		distanceToCityCenter = (int) ( bestDist / 
			Math.sqrt( Math.pow(tiles.getWidth(), 2) 
							+ Math.pow( tiles.getHeight(), 2) ) );
		
		// if road is not on property, search for nearest road
		if( distanceToRoad == -1 ){
			int radius = 5;
			double distance = Double.MAX_VALUE;
			
			// increase search radius until a nearby road is found
			while( distance == Double.MAX_VALUE){
				for(Tile t: land){
					
					Bag neighbors = new Bag();
					tiles.getNeighborsHamiltonianDistance(t.x, t.y, radius,
							false, neighbors, null, null);
					
					// look through all nearby neighbors of the tile for roads
					for(Object o: neighbors){
						Tile n = (Tile) o;
						if( ! n.road ) continue;
						
						// a road is found! Compare its distance
						double dist = distance( t, n );
						if( dist < distance )
							distance = dist;
					}
				}
				
				// if the road has not been found, double the search radius
				if( distance == Double.MAX_VALUE) radius *= 2;
			}
			
		}
	}
	
	// --- ACTIONS ---
	
	/**
	 * Check whether the Parciante's parcel of land has access to irrigation
	 * @return true if it does, false otherwise
	 */
	boolean checkIfIrrigated(){
		
		// if any tile has access to an acequia or stream, it's all irrigated
		for( Tile t: parcel ){
			if( t.hydrationLevel > WaterSystem.hydrationCutoff )
				return true;
		}
		
		return false;
	}
	
	/** 
	 * pick the crop the agent believes will maximize his profits and plant it
	 * @param cropPrices - an array giving the current price each kind of crop 
	 */
	public void plantCrops( int [] cropPrices ){
		// EXTENSION: make more sophisticated handling of crop choices
		// right now there is only one choice, so it doesn't make sense to complicate it
		for(Tile t: parcel){
			t.cropType = 0;
		}
	}
	
	/** the Parciante derives income from the crops he has grown on his land 
	 * @param cropPrices an array of prices indexed by crop type
	 */
	public void harvestCrops( int [] cropPrices ){
		
		boolean irrigated = checkIfIrrigated();
		
		if( ! irrigated) return; // nothing can grow, so nothing to harvest
		
		// otherwise, harvest and sell the crops
		int income = 0;
		for(Tile t: parcel)
			income += cropPrices[ t.cropType ];
		money += income;
	}


	/**
	 * The Parciante considers the offer being made for his land and either accepts
	 * or declines to sell at the given price.

	 * @param offeringPrice - the price the Real Estate Agent is offering for the land
	 * @return whether the Parciante accepts
	 */
	public boolean considerOffer(int offeringPrice){
		
		// if the Parciante has no money, he is forced to sell at whatever price
		if( money <= 0 ) return true;
		
		// if the Parciante wants to sell as soon as possible and the price is greater
		// than his current holdings, he will sell
		if( strategy == STRATEGY_SELLATONCE ){
			if( offeringPrice > money * 2 )
				return true;
			else return false;
		}

		// if the Parciante never wants to sell, he'll hold out for as long as he has the 
		// money to do so
		else if( strategy == STRATEGY_TRADITIONALIST ){
			if( money > 0 ) // if any money, don't sell
				return false;
			else 
				return true;
		}
		
		// if the Parciante wants to follow his neighbors' lead, he will take any price that
		// more than doubles his current holdings if half of his neighbors have moved away.
		else if( strategy == STRATEGY_SHEEP ){
			// if at least half of the other acequia members have sold, sell 
			if( acequia.proportionOfMaxMembers() <= .5 && offeringPrice > money)
				return true;
			else
				return false;
		}
		
		else { // something weird has happened
			System.out.println("ERROR: unknown strategy being played by " + this);
			return true;			
		}
	}

	/**
	 * The Parciante maintains his acequias and pays the cost of doing so.
	 */
	public void maintainAcequias(){
		
		if( acequia == null ) return; // this guy has no access to acequias

		// pay for the acequias, a function of the cost per unit, the number of tiles being fed by it, the length
		// of the acequia of which the Parciante is a member, and the number of other Parciantes helping out
		money -= world.getAcequiaCostPerUnit() * acequiaTiles.size() * acequia.length / acequia.memberSize();

		// update the weights on each of the acequias to reflect their brand new cleaned-out state
		for(Edge e: acequiaLinks)
			e.info = 100;
	}
	
	
	// --- ACCESSORS ---
	
	public ArrayList <Tile> getParcel(){ return parcel; }
	public void setAcequia( Acequia a ){ acequia = a; }
	public Acequia getAcequia(){ return acequia; }
	public void setStrategy(int i){ strategy = i; }
	double distance( Tile t1, Tile t2){ return Math.sqrt( Math.pow(t1.x - t2.x, 2) + Math.pow(t1.y - t2.y, 2)); }
}