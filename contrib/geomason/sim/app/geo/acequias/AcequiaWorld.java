package sim.app.geo.acequias;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import sim.app.geo.acequias.objects.Acequia;
import sim.app.geo.acequias.objects.Parciante;
import sim.app.geo.acequias.objects.Tile;
//import sim.app.geo.acequias.
//import sim.app.geo.acequias.acequiasData;
//import sim.app.geo.acequias.acequiasData.AcequiasData;
import sim.engine.SimState;
import sim.field.grid.ObjectGrid2D;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.util.Bag;

/**
 * @author Sarah Wise and Andrew Crooks
 *
 * 
 */
public class AcequiaWorld extends SimState {

	// --- PARAMETERS ---
	
	// Water parameters - 
	double acequiaCostPerUnit = 5;
	double acequiaAnnualDecayRate = .99;
	
	// Weather parameters
	int rainfallThisYear = 50;
	public int getRainfallThisYear(){ return rainfallThisYear; }
	public void setRainfallThisYear(int r){ rainfallThisYear = r; }
	public Object domRainfallThisYear() { return new sim.util.Interval(0L, 100L); }
	
	// Economics Parameters
	int [] cropPrices = { 10 };
	double budgetStdDev = 10;
	double budgetThisYear = 100;
	public double getBudgetThisYear(){ return budgetThisYear; }
	public void setBudgetThisYear(double b){ budgetThisYear = b; budgetStdDev = b / 10; };
	public Object domBudgetThisYear() { return new sim.util.Interval(0, 1000); }
	
	int numRealEstateAgents = 100;
	public int getNumRealEstateAgents(){ return numRealEstateAgents; }
	public void setNumRealEstateAgents( int n ){ numRealEstateAgents = n; }
	
	int numberOfYearsInSimulation = 5; // how long should the simulation run?
	int id = -1; // used to identify output files that belong together
	
	// --- OBJECTS ---
	
	// --- Land Objects ---
	ObjectGrid2D tiles = null;
	Tile cityCenter = null; // "downtown", the center of the city

	// --- Agent Objects ---
	ArrayList <Parciante> parciantes = new ArrayList <Parciante> (); // a list of Parciantes
	double [] parcianteStrategyProfile = {.333,.333,.333}; // the relative number of Parciantes with 
			// each of the possible attitudes, in the order: SellAtOnce, Traditionalist, Sheep
	
	// --- Water Objects ---
	Network waterflow = new Network(); // links between tiles indicate water flows from one to another
	ArrayList <Acequia> acequiaList = new ArrayList <Acequia> (); // a list of Acequias
	// all edges are weighted between 0-100, indicating percent
	ArrayList <Edge> acequiaEdges = new ArrayList <Edge> ();
	HashMap <Integer, Acequia> acequiaIDMap = new HashMap <Integer, Acequia> (); // used only in setup

	
	// --- STATISTICS ---
	
	// --- used to keep easy track of stats --- 
	int numUrban = 0;
	int initialNumAg = 0;
	int numAg = 0;
	int initialNumParciantes = 0;
	int formerParciantes = 0;

	
	
	//
	// --- THE BODY OF THE SIMULATION ---
	//
	
	/** Main function allows simulation to be run in stand-alone, non-GUI mode */
	public static void main(String [] args){
		doLoop(AcequiaWorld.class, args);
		System.exit(0);		
	}
	
	/** constructor function WITH DEFAULT PARAMETERS */
	public AcequiaWorld(long seed) { 
		super(seed); 
	}
	
	/** constructor function WITH USER-DEFINED PARAMETERS */
	public AcequiaWorld(long seed, int rainfallThisYear, double acequiaAnnualDecayRate, double acequiaCostPerUnit, 
			int [] cropPrices, double [] parcianteStrategyProfile, double budgetStdDev, 
			double budgetThisYear, int numRealEstateAgents) {
		
		super(seed);

		this.rainfallThisYear = rainfallThisYear;
		this.acequiaAnnualDecayRate = acequiaAnnualDecayRate;
		
		this.cropPrices = cropPrices;
		this.acequiaCostPerUnit = acequiaCostPerUnit;

		this.parcianteStrategyProfile = parcianteStrategyProfile;

		this.budgetThisYear = budgetThisYear;
		this.budgetStdDev = budgetStdDev;
		this.numRealEstateAgents = numRealEstateAgents;

	}
	
	/** Read in and process the data: get the simulation set up and ready to go */
	public void start(){
		
		super.start();

		id = (int) System.currentTimeMillis(); // get an ID

		// read in the data
		System.err.println("read in elevations");
		setupDoubleFromFile("/acequias/acequiaData/ned.txt", 1);

		System.err.println("read in land use");
		setupIntFromFile("/acequias/acequiaData/landuse.txt", 3);

		System.err.println("read in hydrology");
		setupIntFromFile("/acequias/acequiaData/hydrology.txt", 1);
		
		System.err.println("read in acequia tracts");
		setupIntFromFile("/acequias/acequiaData/acequiatracts.txt", 2);

		System.err.println("read in acequias");
		setupIntFromFile("/acequias/acequiaData/acequias.txt", 4);
		
		System.err.println("read in roads");
		setupIntFromFile("/acequias/acequiaData/roads.txt", 5);

		System.err.println("read in counties");
		setupIntFromFile("/acequias/acequiaData/counties.txt", 6);

		System.err.println("read in city center");
		setupIntFromFile("/acequias/acequiaData/citycenter.txt", 7);

		// finished reading in the data
		
		System.err.println("Finished reading in the data.\nBeginning data setup...");
		
		// set up the data

		System.err.println("building the acequia network...");
		setupAcequias();
		
		System.out.println("building the water network...");
		constructWaterNetwork();
		
		System.out.println("extracting the tracts of land and assigning parciantes...");
		extractLandTractsAndAssignParciantes();
		
		initialNumParciantes = parciantes.size();
		initialNumAg = numAg;
		
		// finish setting up the data
		
		System.out.println("Finished setting up the data");
		
		// schedule the appropriate systems
		
		WaterSystem waterSystem;
		waterSystem = new WaterSystem(this);
		
		schedule.scheduleRepeating(waterSystem, 0, 1);
		
		Ticker ticker = new Ticker(this, numberOfYearsInSimulation);
		schedule.scheduleRepeating(ticker, 1, 1);
		
		// finish scheduling
	}

	/**
	 * Called at the end of the simulation. Calculates statistics, writes out, and generally cleans up
	 * after everyone else.
	 */
    public void finish()
    {
    	// calculate the number of Acequias which have gone out of commission over the course of the simulation
    	int defunctAcequias = 0;
    	for(Acequia a: acequiaList){
    		if( a.memberSize() == 0 )
    			defunctAcequias++;
    	}
    		    	
    	// print out to some file, giving a unique name to avoid overwriting anything
		String report = "results" + id + ".txt";
		
		// attempt to write out a report to this file
		try {
			// Convert our stream to a BufferedWriter
			BufferedWriter w = new BufferedWriter(new FileWriter(report, true));
			
			// the header: what do all these terms mean?
			String header = "currentTime \t rainfallThisYear \t acequiaAnnualDecayRate \t acequiaCostPerUnit" +
				"\t cropPrices \t parcianteStrategyProfile0 \t  parcianteStrategyProfile1 \t" +
				"parcianteStrategyProfile2 \t budgetStdDev \t budgetThisYear \t numRealEstateAgents \t initialNumAg \t" + 
	    		"numAg \t numUrban \t initialNumParciantes \t parciantesSize \t formerParciantes \t acequiaListSize \t" + 
	    		"defunctAcequias";

			// the actual numbers
	    	String output = System.currentTimeMillis() + "\t" + rainfallThisYear + "\t" + acequiaAnnualDecayRate + 
	    		"\t" + acequiaCostPerUnit + "\t" + cropPrices[0] + "\t" + 
	    		parcianteStrategyProfile[0] + "\t" + parcianteStrategyProfile[1] + "\t" + parcianteStrategyProfile[2] + 
	    		"\t" + budgetStdDev + "\t" + budgetThisYear + "\t" + numRealEstateAgents + "\t" + initialNumAg + "\t" + 
	    		numAg + "\t" + numUrban + "\t" + initialNumParciantes + "\t" + parciantes.size() + "\t" + formerParciantes + 
	    		"\t" + acequiaList.size() + "\t" + defunctAcequias;

	    	w.write(header);
	    	w.newLine();
	    	w.write(output);
	    	w.newLine();
	    	w.flush();
			
			w.close();

		} catch (Exception e) {
			System.err.println("File input error");
		}
    	
    	kill();  // cleans up asynchroonous and resets the schedule, a good ending
    }


	//
	// --- UTILITIES ---
	//
	
	/**
	 * Construct the water network from the data
	 */
	void constructWaterNetwork(){

		// iterate over the landscape
		for(int i = 0; i < tiles.getWidth(); i++){
			for(int j = 0; j < tiles.getHeight(); j++){
				
				Tile t = (Tile) tiles.get(i, j);
				if(t == null) continue;
				
				// check for all hydrological features whether they connect to something 
				// into which they flow (and that it's not an acequia and already processed)
				else if(t.getHydrologicalFeature() == 1 && t.getAcequia() == -1){

					// investigate all neighboring hydrological tiles in case they're 
					// lower (an thus an outflow)
					Bag neighbors = new Bag();
					tiles.getNeighborsHamiltonianDistance(
							i, j, 1, false, neighbors, null, null);
					
					// consider neighbors one by one...
					for(Object o: neighbors){
						
						if( o == null || o == t ) continue;
						Tile n = (Tile) o;
						
						// if it's both a part of the hydrology and lower, flow into it!
						// (can flow into an acequia without being an acequia link)
						if( (n.getHydrologicalFeature() == 1 || n.getAcequia() != -1) 
								&& n.getElevation()< t.getElevation() ){
							Edge e = new Edge(t, n, 100);
							waterflow.addEdge( e );
						}
					}
				}
			}
		}
		
		System.out.println("Network successfully constructed");
	}
	
	/**
	 * Set up the acequias and their network of waterflow
	 */
	void setupAcequias(){
		
		HashMap <Integer, ArrayList <Tile>> acequias = new HashMap <Integer, ArrayList <Tile>>();
		
		// extract all member tiles into acequias
		for(int i = 0; i < tiles.getWidth(); i++){
			for(int j = 0; j < tiles.getHeight(); j++){
				
				Tile t = (Tile) tiles.get(i, j);
				if(t == null || t.getAcequia() == -1) continue;
	
				// add the acequia to the list of acequias
				if( acequias.containsKey( t.getAcequia() ))
					acequias.get( t.getAcequia() ).add( t );
				else{
					ArrayList <Tile> ts = new ArrayList <Tile> ();
					ts.add( t );
					acequias.put( t.getAcequia(), ts );
				}
				
				//
				// set up its network properties
				//
				
				// investigate all neighboring hydrological tiles in case they're 
				// lower (an thus an outflow)
				Bag neighbors = new Bag();
				tiles.getNeighborsHamiltonianDistance(
						i, j, 1, false, neighbors, null, null);
				
				// consider neighbors one by one...
				for(Object o: neighbors){
					
					if( o == null || o == t ) continue;
					Tile n = (Tile) o;
					
					// if it's both part of an acequia and lower, it's an acequia link!
					if(n.getAcequia() != -1 && n.getElevation() < t.getElevation() ){
						Edge e = new Edge(t, n, 100);
						waterflow.addEdge( e );
						acequiaEdges.add( e );
					}
				}

			}
		}
		
		// add all of the acequias we've found to a list for future use
		for(Object o: acequias.keySet()){
			Integer i = (Integer) o;
			Acequia newAcequia = new Acequia(i, acequias.get(i));
			acequiaList.add( newAcequia );
			acequiaIDMap.put( i, newAcequia);
		}
	}
	
	/**
	 * Set up the tracts and assign Parciantes to them
	 */
	void extractLandTractsAndAssignParciantes(){

		HashMap <Integer, ArrayList <Tile>> tractTiles = new HashMap <Integer, ArrayList <Tile>>();
		HashMap <Integer, ArrayList <Tile>> acequiaTractTiles = new HashMap <Integer, ArrayList <Tile>>(); 
		HashMap <Integer, Integer> tractToAcequiaMapping = new HashMap <Integer, Integer>();
		
		// assemble the member tiles into groups
		for(int i = 0; i < tiles.getWidth(); i++){
			for(int j = 0; j < tiles.getHeight(); j++){
				
				Tile t = (Tile) tiles.get(i, j);
				if(t == null) continue;
				
				int tract = t.getTract();
				
				// save tile as part of tract
				if( tractTiles.containsKey( tract ))
					tractTiles.get( tract ).add( t );
				else{
					ArrayList <Tile> ts = new ArrayList <Tile> ();
					ts.add( t );
					tractTiles.put( tract, ts );
				}

				// update the acequia info about this tract
				if( t.getAcequia() != -1 ){

					tractToAcequiaMapping.put( tract, t.getAcequia() );
					
					// save tile as part of tract's acequia access tiles
					if( acequiaTractTiles.containsKey( tract ))
						acequiaTractTiles.get( tract ).add( t );
					else{
						ArrayList <Tile> ts = new ArrayList <Tile> ();
						ts.add( t );
						acequiaTractTiles.put( tract, ts );
					}

				}
			}
		}
		
		int inaccessibleTracts = 0;
		
		// go through all of the tracts and attempt to assign each to an acequia
		for(Integer i: tractTiles.keySet()){
			
			// assemble a list of acequia edges associated with this tract of land
			ArrayList <Edge> myAcequiaEdges = new ArrayList <Edge> ();
			if( acequiaTractTiles.get(i) != null)
				for(Tile t: acequiaTractTiles.get(i)){
					Bag edges = waterflow.getEdgesOut( t );
					for( Object o: edges ){
						if( acequiaEdges.contains( o))
							myAcequiaEdges.add( (Edge) o );
					}
				}
			else{
				inaccessibleTracts++;
				continue;
			}
			
			// set the Parciante strategy based on the randomly selected profile!
			double strategy = this.random.nextDouble(), val = 0.;
			int selectedStrategy = -1;
			for(int j = 0; j < parcianteStrategyProfile.length; j++){
				val += parcianteStrategyProfile[j];
				if( strategy < val){
					selectedStrategy = j;
					break;
				}
			}

			// initialize the new Parciante with this information
			Parciante p = new Parciante(this, selectedStrategy, tractTiles.get(i),
					acequiaTractTiles.get(i), myAcequiaEdges);
			
			// save this Parciante
			parciantes.add( p );
			
			// add this Parciante as an Acequia member
			if( tractToAcequiaMapping.containsKey( i ) ){
				Acequia a = acequiaIDMap.get( tractToAcequiaMapping.get(i) );
				a.gainMember( p );
				p.setAcequia( a );
			}
		}
		
		System.out.println("Inaccessible tracts:" + inaccessibleTracts + " out of " + tractTiles.size());
	}

	
	/** Return the cost per unit of Acequia*/
	public double getAcequiaCostPerUnit(){ return acequiaCostPerUnit; }
	public ObjectGrid2D getTiles(){ return tiles; }
	public Tile getCityCenter(){ return cityCenter; }
	
	//
	// IO
	//
	
	/**
	 * Read in INTEGER data from a file
	 * @param filename - the name of the file that holds the data
	 * @param param - the phenomenon being read in
	 */
	void setupIntFromFile(String filename, int param){

		try { // to read in a file

			// Open the file
			//FileInputStream fstream = new FileInputStream(filename);

			// Convert our input stream to a BufferedReader


			BufferedReader d = new BufferedReader(new InputStreamReader(sim.app.geo.acequias.acequiasData.AcequiasData.class.getResourceAsStream
					(filename)));


			// get the parameters from the file
			String s;
			int width = 0, height = 0;
			double nodata = -1;
			for(int i = 0; i < 6; i++){

				s = d.readLine();

				// format the line appropriately
				String [] parts = s.split(" ", 2);
				String trimmed = parts[1].trim();

				// save the data in the appropriate place
				if(i == 1)
					height = Integer.parseInt( trimmed );
				else if(i == 0)
					width = Integer.parseInt( trimmed );
				else if( i == 5)
					nodata = Double.parseDouble(trimmed );
				else
					continue;
			}

			// set up the field to hold the data
			if ( tiles == null )
				tiles = new ObjectGrid2D( width, height);

			// read in the data from the file and store it in tiles
			int i = 0, j = 0;
			while((s = d.readLine()) != null){
				String [] parts = s.split(" ");

				for(String p: parts){

					int value = Integer.parseInt(p);
					if( value == nodata ){ // don't add this value
						j++;
						continue;
					}

					Tile t = (Tile) tiles.get(j, i);
					if( t == null ){
						t = new Tile(j, i);
						tiles.set(j, i, t);
					}
					
					// update the relevant field
					if( param == 1 ) // hydrology
						((Tile)tiles.get(j, i)).setHydrologicalFeature(value);
					else if(param == 2) // tract membership
						((Tile)tiles.get(j, i)).setTract( value );						
					else if(param == 3){ // landuse
						((Tile)tiles.get(j, i)).setLanduse( value );
						if( value == 21 || value == 22 || value == 23 || value == 24)
							numUrban++;
						else if( value == 82)
							numAg++;
					}
					else if(param == 4) // acequias
						((Tile)tiles.get(j, i)).setAcequia( value );
					else if(param == 5) // road
						((Tile)tiles.get(j, i)).setRoad( true );
					else if(param == 6) // counties
						((Tile)tiles.get(j, i)).setCounty( value );
					else if(param == 7) // city center
						cityCenter = ((Tile)tiles.get(j,i));
					else {
						System.out.println("ERROR: invalid parameter given");
						System.exit(0);
					}
					
					j++; // increase the column count
				}

				j = 0; // reset the column count
				i++; // increase the row count
			}
		}
		// if it messes up, print out the error
		catch (Exception e){ e.printStackTrace();}//System.out.println(e);}
	}

	/**
	 * Read in DOUBLE data from a file
	 * @param filename - the name of the file that holds the data
	 * @param param - the phenomenon being read in
	 */
	void setupDoubleFromFile(String filename, int param){

		try { // to read in a file

			// Open the file
			//FileInputStream fstream = new FileInputStream();

			// Convert our input stream to a

			BufferedReader d = new BufferedReader(new InputStreamReader(sim.app.geo.acequias.acequiasData.AcequiasData.class.getResourceAsStream
					(filename)));

			// get the parameters from the file
			String s;
			int width = 0, height = 0;
			double nodata = -1;
			for(int i = 0; i < 6; i++){

				s = d.readLine();

				// format the line appropriately
				String [] parts = s.split(" ", 2);
				String trimmed = parts[1].trim();

				// save the data in the appropriate place
				if(i == 1)
					height = Integer.parseInt( trimmed );
				else if(i == 0)
					width = Integer.parseInt( trimmed );
				else if( i == 5)
					nodata = Double.parseDouble(trimmed );
				else
					continue;
			}

			// set up the field to hold the data
			if ( tiles == null )
				tiles = new ObjectGrid2D( width, height);

			// read in the data from the file and store it in tiles
			int i = 0, j = 0;
			while((s = d.readLine()) != null){
				String [] parts = s.split(" ");

				for(String p: parts){

					double value = Double.parseDouble(p);
					if( value == nodata ){ // this is not of interest
						j++;
						continue;
					}

					Tile t = (Tile) tiles.get(j, i);
					if( t == null ){
						t = new Tile(j, i);
						tiles.set(j, i, t);
					}
					
					// update the field
					if( param == 1 ) // elevation
						((Tile)tiles.get(j, i)).setElevation( value );
					j++; // increase the column count
				}

				j = 0; // reset the column count
				i++; // increase the row count
			}
		}
		// if it messes up, print out the error
		catch (Exception e){ System.out.println(e);}
	}

	//
	// END IO
	//
	
	//
	// --- END UTILITIES ---
	//

}
