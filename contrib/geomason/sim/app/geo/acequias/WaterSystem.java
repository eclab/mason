package acequias;

import java.util.HashMap;

import acequias.objects.Tile;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.network.Edge;
import sim.util.Bag;

/** <b>WaterSystem</b> the engine that controls water flow.
 * 
 * The WaterSystem takes rain and propagates it through the water system, through the rivers
 * and in the appropriate amounts through the acequias. It is responsible for decaying the 
 * acequias as they sediment over time.
 * 
 * @author Sarah Wise and Andrew Crooks
 */
public class WaterSystem implements Steppable {
	
	//
	// --- PARAMETERS ---
	//
	double acequiaAnnualDecayRate = .99; // that is, decays at 1% per year
	public static double hydrationCutoff = 20; // the minimum amount of hydration 
		// needed to be regarded as "irrigated": used by land tiles

	//
	// --- OBJECTS ---
	//	
	AcequiaWorld world;
	HashMap <Tile, Double> waterLevels;
	
	
	/** Constructor WITH DEFAULTS */
	public WaterSystem(SimState state){
		world = (AcequiaWorld) state;
		waterLevels = new HashMap <Tile, Double> ();
	}

	/** Constructor WITH USER-DEFINED VALUES */
	public WaterSystem(SimState state, double acequiaAnnualDecayRate){
		world = (AcequiaWorld) state;
		waterLevels = new HashMap <Tile, Double> ();
		this.acequiaAnnualDecayRate = acequiaAnnualDecayRate;
	}
	
	/** Reset all of the nodes to contain the amount of water that fell */
	void resetSystem(){
		Bag nodes = world.waterflow.allNodes;
		Double rainfall = new Double( world.rainfallThisYear );
		for( Object o: nodes){
			waterLevels.put( (Tile) o, rainfall );
		}
	}
	
	/** Push the water through the network in proportion to the quality of the linkages */
	boolean propagateWater(){
		
		boolean waterLevelsModified = false;
		
		Bag nodes = world.waterflow.allNodes;
		
		HashMap <Tile, Double> newWaterLevels = new HashMap <Tile, Double> ();
		
		// iterate over all nodes and move water from them to lower neighbors
		for( Object o: nodes){
			Tile t = (Tile) o;
			
			// find the current height of this node
			double height = t.getElevation() + waterLevels.get( t );
			
			// find the neighbors that have lower heights
			Bag neighbors = world.waterflow.getEdgesOut(t);
			Bag lower = new Bag();
			for( Object p: neighbors ){
				Edge e = (Edge) p;
				Tile n = (Tile) e.getTo();
				if( n.getElevation() + waterLevels.get(n) < height)
					lower.add( e );
			}
			
			// if any such neighbors exist, try to equalize their height 
			if( lower.size() > 0 ){
				
				waterLevelsModified = true; // the water levels shift
				double water = waterLevels.get( t ) / lower.size();
				double waterOut = 0;
				
				// equalize height for all neighbors
				for(Object p: lower){
					
					Edge e = (Edge) p;
					Tile l = (Tile) e.getTo();
					double lowerWater = waterLevels.get( l ) + l.getElevation();
					
					// determine the flow: equalize the levels of water while
					// water remains in the tile
					double waterFromTtoL = Math.min( water, 
							(water + t.getElevation() - lowerWater) / 2.);
					
					// that much water flows out
					waterOut += waterFromTtoL;
					
					// based on the maintenance of the system, some might bleed off
					waterFromTtoL *= ((Integer)e.info) / 100. ;
					
					if( newWaterLevels.containsKey(l))
						newWaterLevels.put( l, newWaterLevels.get(l) + waterFromTtoL );
					else
						newWaterLevels.put( l, waterFromTtoL);
				}
				
				// update this tile's water levels after flowing out
				if( newWaterLevels.containsKey(t) )
					newWaterLevels.put( t, newWaterLevels.get(t) + water - waterOut);
				else
					newWaterLevels.put( t, water - waterOut);
			}
			else if( ! newWaterLevels.containsKey(t) )
				newWaterLevels.put(t, waterLevels.get( t ));
		}
		
		// update the environment about the new water levels
		waterLevels = newWaterLevels;
		
		return waterLevelsModified;
	}
	
	/** Reset all of the nodes to contain the amount of water that fell */
	void saveHydrationLevels(){
		Bag nodes = world.waterflow.allNodes;
		for( Object o: nodes){
			Tile t = (Tile) o;
			t.setHydration( waterLevels.get( t ) );
		}
	}

	/** The acequias build up every year as a result of use */
	void updateAcequiaWeights(){
		for(Edge e: world.acequiaEdges){
			int weight = (Integer) e.info;
			e.info = (int) Math.floor(weight * acequiaAnnualDecayRate);
		}
	}
	
	@Override
	public void step(SimState state) {

		resetSystem();
		
		boolean settled = false;
		while( ! settled ){
			settled = propagateWater();
		}
		
		saveHydrationLevels();
		
		updateAcequiaWeights();
	}
	
	
}