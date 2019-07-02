package sim.app.geo.acequias;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import sim.app.geo.acequias.objects.Parciante;
import sim.app.geo.acequias.objects.RealEstateAgent;
import sim.app.geo.acequias.objects.Tile;
import sim.engine.SimState;
import sim.engine.Steppable;

/** <b>Ticker</b> the scheduling mechanism for the simulation.
 * 
 * The Ticker arranges the hydration, planting, harvesting, and land offering subprocesses 
 * in the correct order. 
 * 
 * @author Sarah Wise and Andrew Crooks
 *
 */
public class Ticker implements Steppable {

	AcequiaWorld world;
	int limitYears = 100;
	int simulationAge;

	BufferedWriter report = null;
	
	/** Constructor */
	public Ticker( SimState state, int limitYears ){
		world = (AcequiaWorld) state;
		simulationAge = 0;
		this.limitYears = limitYears;
		
		// print out a running report of parameters as the simulation progresses
		// TODO
		String fout = "output" + world.id + ".txt";

		try {
			report = new BufferedWriter(new FileWriter(fout));
		} catch (Exception e) {
			System.err.println("File input error");
		}

	}
	
	@Override
	public void step(SimState state) {
		
		simulationAge++;
		if ( simulationAge > limitYears ){
			if(report != null)
				try {
					report.write(simulationAge + "\t" + world.parciantes.size() + "\t" + world.formerParciantes  + "\t"
							+ world.acequiaList.size() + "\t"
							+ world.numAg + "\t" + + world.numUrban + "\t" + world.numRealEstateAgents);
					report.newLine();
					report.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			world.finish();
		}
		
		
		// have all Parciantes harvest their crops
		for( Parciante p: world.parciantes ){
			p.plantCrops( world.cropPrices );
			p.harvestCrops( world.cropPrices );
			p.maintainAcequias();
		}
		
		
		// have all Real Estate Agents attempt to purchase land
		for( int i = 0; i < world.numRealEstateAgents; i++ ){

			if(world.parciantes.size() <= 0) // ain't no one to sell to
				return;

			RealEstateAgent re = new RealEstateAgent( 
					world.random.nextGaussian() * world.budgetStdDev + world.budgetThisYear);
			
			// pick a Parciante to offer money
			Parciante p = world.parciantes.get( 
					world.random.nextInt( world.parciantes.size() ) );
			int offer = re.formulateOffer( p );
			boolean accepted = p.considerOffer(offer);
			
			if( accepted ){
				// the offer was accepted! The parciante moves and his land is urbanized
				world.parciantes.remove( p );
				for(Tile t: p.getParcel()){
					t.urbanize();
					world.numAg--;
					world.numUrban++;
				}
				world.formerParciantes++;
				// p withdraws from the acequia
				p.getAcequia().loseMember( p );
			}
		}
		
		// try to write out the results
		if(report != null && simulationAge<=limitYears){
			try {
				report.write(simulationAge + "\t" + world.parciantes.size() + "\t" + world.formerParciantes  + "\t"
						+ world.acequiaList.size() + "\t"
						+ world.numAg + "\t" + + world.numUrban + "\t" + world.numRealEstateAgents);
				report.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
	}
	
	
}
