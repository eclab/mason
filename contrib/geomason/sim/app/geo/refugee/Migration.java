package refugee;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.data.xy.XYSeries;
import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.Continuous2D;
import sim.field.geo.GeomVectorField;
import sim.field.grid.SparseGrid2D;
import sim.field.network.Network;
import sim.util.Bag;
import java.io.*;
import java.util.*;

class Migration extends SimState {

	public Continuous2D world;
	public SparseGrid2D worldPopResolution;// all agents within each km grid
											// cell
	public SparseGrid2D cityGrid;
	public Network roadNetwork = new Network();
	public GeomVectorField regions;
	public GeomVectorField countries;
	public GeomVectorField roads;
	public GeomVectorField roadLinks; // roadLinks;
	public GeomVectorField cityPoints;

	public GeomVectorField adminBoundaries;// TODO may not be needed
	public GeomVectorField adminShape;// TODO may not be needed
	public SparseGrid2D allRoadNodes;// cities for now

	public int pop_width;
	public int pop_height;
	public int world_width;
	public int world_height;
	public int total_scaled_pop = 0; // number of agents in the model (scaled
										// from total_pop)
	public long total_pop = 0; // actual population (not scaled)
	public int total_dead = 0;
	public double avg_fin = 0;
	/*
	 * charts can include: number dead, specifics for each country, etc.
	 */

	public Bag refugees;
	public Bag refugeeFamilies;
	public Bag cities = new Bag();
	public Map<Integer, City> cityList = new HashMap<>();

	// public Map<Integer, List<MovementPattern>> movementPatternMap = new
	// HashMap<>();

	public XYSeries totalDeadSeries = new XYSeries(" Dead"); // shows number of
																// recovered
																// agents
	public XYSeries finSeries = new XYSeries("Finance"); // shows number of
															// recovered agents

	// timer graphics
	DefaultValueDataset hourDialer = new DefaultValueDataset(); // shows the
																// current hour
	DefaultValueDataset dayDialer = new DefaultValueDataset(); // counts

	public Migration(long seed) {
		super(seed);
	}

	@Override
	public void start() {
		super.start();
		refugees = new Bag();
		refugeeFamilies = new Bag();
		MigrationBuilder.initializeWorld(this);

		// charts

		Steppable chartUpdater = new Steppable() {
			@Override
			public void step(SimState simState) {
				// System.out.println("\n");
				total_dead = 0;
				double total_fin = 0;
				long cStep = simState.schedule.getSteps();
				if (cStep / 24 > 420) {
					simState.finish();
				}

				double day = cStep * Parameters.TEMPORAL_RESOLUTION / 24;
				double hour = cStep * Parameters.TEMPORAL_RESOLUTION % 24;
				hourDialer.setValue(hour);
				dayDialer.setValue(day);

				Bag allRefugees = world.getAllObjects();
				for (Object o : allRefugees) {
					// RefugeeFamily family = (RefugeeFamily) o;
					Refugee r = (Refugee) o;

					// for (Refugee r: family.getFamily()){
					if (r.getHealthStatus() == Constants.DEAD)
						total_dead++;
				}
				// }
				for (Object o : refugeeFamilies) {

					RefugeeFamily family = (RefugeeFamily) o;
					total_fin += family.getFinStatus();
				}
				// total_pop = allRefugees.size();
				// double display_deaths = total_dead/total_pop;
				// totalDeadSeries.add(cStep*Parameters.TEMPORAL_RESOLUTION,
				// display_deaths);
				double percentage_dead = total_dead * 1.0 / Parameters.TOTAL_POP;
				totalDeadSeries.add(cStep * Parameters.TEMPORAL_RESOLUTION, percentage_dead);

				double avg_fin = total_fin * 1.0 / Parameters.TOTAL_POP;
				finSeries.add(cStep * Parameters.TEMPORAL_RESOLUTION, avg_fin);
			}
		};
		this.schedule.scheduleRepeating(chartUpdater);

	}

	/*
	 * Steppable movementManager = new Steppable() { private long lastTime;
	 * 
	 * @Override public void step(SimState simState) { long cStep =
	 * simState.schedule.getSteps(); if(cStep %
	 * Math.round(24.0/Parameters.TEMPORAL_RESOLUTION) == 0)//only do this on
	 * the daily { long now = System.currentTimeMillis(); if(lastTime != 0)
	 * System.out.println("Day " + cStep/24 + "[" + (now-lastTime)/1000 +
	 * " secs ]"); moveResidents();
	 * System.out.println("Managing population flow [" +
	 * (System.currentTimeMillis()-now)/1000 + " sec]"); } }
	 * 
	 * private void moveResidents(){ for (Refugee r: refugees){
	 * 
	 * } } } }; this.schedule.scheduleRepeating(movementManager);
	 */

	@Override
	public void finish() {
		System.out.println("Finishing...");
		super.finish();
	}

	public static void main(String[] args) {

		String output_path = "parameter_sweeps/";
		String file_name = "output";

		if (args.length > 1) { // run parameter sweep
			Parameters.TRIALNO = Integer.parseInt(args[0]);
			Parameters.COST_WEIGHT = Double.parseDouble(args[1]);
			Parameters.RISK_WEIGHT = Double.parseDouble(args[2]);
			Parameters.DISTANCE_WEIGHT = Double.parseDouble(args[3]);
			Parameters.SPEED_WEIGHT = Double.parseDouble(args[4]);
			Parameters.POP_WEIGHT = Double.parseDouble(args[5]);
			Parameters.HEU_WEIGHT = Double.parseDouble(args[6]);
			Parameters.TOTAL_POP = Integer.parseInt(args[7]);
			// file_name = "output_all";
		}
		else{
			Parameters.TRIALNO = 1;//Integer.parseInt(args[0]);
			Parameters.COST_WEIGHT = 0.1;//Double.parseDouble(args[1]);
			Parameters.RISK_WEIGHT = 0.1;//D/ouble.parseDouble(args[2]);
			Parameters.DISTANCE_WEIGHT = 0.1;//Double.parseDouble(args[3]);
			Parameters.SPEED_WEIGHT = 0.1;//Double.parseDouble(args[4]);
			Parameters.POP_WEIGHT = 10.0;//Double.parseDouble(args[5]);
			Parameters.HEU_WEIGHT = 0.1;//Double.parseDouble(args[6]);
			Parameters.TOTAL_POP = 1000;//Integer.parseInt(args[7]);
		}

		long seed = System.currentTimeMillis();
		Migration simState = new Migration(seed);
		long io_start = System.currentTimeMillis();
		simState.start();
		long io_time = (System.currentTimeMillis() - io_start) / 1000;
		System.out.println("io_time = " + io_time);
		Schedule schedule = simState.schedule;
		while (true) {
			if (!schedule.step(simState)) {
				break;
			}
		}

		File output = new File(output_path + file_name);
		simState.writeToCSV("" + Parameters.TRIALNO, output_path, file_name);

		// create our run directory

		// write output data to output
		// create directory output if not exists
		// write effective reproductive rates

		// write run meta data to a json object and save it

		// doLoop(Migration.class, args);
		System.exit(0);

	}

	public void writeToCSV(String trialNo, String output_path, String file_name) {
		try {

			File passfile = new File(output_path + file_name + "_pass2.csv");
			File stayfile = new File(output_path + file_name + "_stay2.csv");
			File arrivefile = new File(output_path + file_name + "_arrive2.csv");
			File paramfile = new File(output_path + file_name + "_parameter2.csv");
			boolean newout = false;
			if (!passfile.exists()) {
				passfile.createNewFile();
				newout = true;
			}
			if (!stayfile.exists()) {
				stayfile.createNewFile();
				newout = true;
			}
			if (!arrivefile.exists()) {
				stayfile.createNewFile();
				newout = true;
			}
			boolean newparam = false;
			if (!paramfile.exists()) {
				paramfile.createNewFile();
				newparam = true;
			}
			FileWriter fw = new FileWriter(passfile, true);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter writer = new PrintWriter(bw);
			FileWriter fw1 = new FileWriter(stayfile, true);
			BufferedWriter bw1 = new BufferedWriter(fw1);
			PrintWriter writer1 = new PrintWriter(bw1);
			FileWriter fw3 = new FileWriter(arrivefile, true);
			BufferedWriter bw3 = new BufferedWriter(fw3);
			PrintWriter writer3 = new PrintWriter(bw3);
			if (newout) {
				writer.print("TRIALNO,");
				writer1.print("TRIALNO,");
				writer3.print("TRIALNO,");
				// edit to do columns
				for (Object c : cities) {
					City city = (City) c;
					writer.print(city.getName() + ",");
					writer1.print(city.getName() + ",");
					writer3.print(city.getName() + ",");
				}
				writer.println();
				writer1.println();
				writer3.println();
			}
			writer.print(trialNo + ",");
			writer1.print(trialNo + ",");
			writer3.print(trialNo + ",");
			for (Object c : cities) {
				City city = (City) c;
				writer.print(Math.min(city.getDepartures(), city.getArrivals()) + ",");
			}
			//writer.println();
			for (Object c : cities) {
				City city = (City) c;
				writer1.print(city.getArrivals() - city.getDepartures() + ",");
			}
			for (Object c : cities) {
				City city = (City) c;
				writer3.print(city.getArrivals() + ",");
			}
			writer.println();
			writer.close();
			writer1.println();
			writer1.close();
			writer3.println();
			writer3.close();

			FileWriter fw2 = new FileWriter(paramfile, true);
			BufferedWriter bw2 = new BufferedWriter(fw2);
			PrintWriter writer2 = new PrintWriter(bw2);

			if (newparam) {
				String[] param = { "TRIALNO", "COST_WEIGHT", "RISK_WEIGHT", "DISTANCE_WEIGHT", "SPEED_WEIGHT",
						"POP_WEIGHT", "HEU_WEIGHT", "TOTAL_POP" };
				for (int i = 0; i < param.length; i++)
					writer2.print(param[i] + ",");
			}
			writer2.println();
			writer2.printf("%d,", Parameters.TRIALNO);
			writer2.printf("%3.2f,", Parameters.COST_WEIGHT);
			writer2.printf("%3.2f,", Parameters.RISK_WEIGHT);
			writer2.printf("%3.2f,", Parameters.DISTANCE_WEIGHT);
			writer2.printf("%3.2f,", Parameters.SPEED_WEIGHT);
			writer2.printf("%3.2f,", Parameters.POP_WEIGHT);
			writer2.printf("%3.8f,", Parameters.HEU_WEIGHT);
			writer2.printf("%d,", Parameters.TOTAL_POP);
			writer2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
