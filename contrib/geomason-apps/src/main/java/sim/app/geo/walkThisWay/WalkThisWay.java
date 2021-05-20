package sim.app.geo.walkThisWay;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;

import sim.app.geo.walkThisWay.pedData.WalkThisWayData;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.IntGrid2D;
import sim.field.grid.ObjectGrid2D;
import sim.field.grid.SparseGrid2D;
import sim.util.Heap;
import sim.util.Int2D;
import sim.util.Int3D;

//
//*****************************************************************************

public class WalkThisWay extends SimState {

	// --- OPTIONS ---

	public boolean traceWriterOn = true; // output heatmaps?

	// --- DATA DIRECTORIES ---

	public String traceDataDir = "walkThisWayData/pedTraces/";

	// --- OBJECTS AND THE ENVIRONMENT ---

	public SparseGrid2D people; // Stores the pedestrians.
	public DoubleGrid2D obstacles; // Stores the combined cost surface.
	public RasterDoubleGrid2D traces; // Stores the traces of pedestrians.
	public ArrayList<Int2D> availableTiles; // Stores ped starting positions.

	public DoubleGrid2D allWalkableSurf;
	public DoubleGrid2D footPathsSurf;
	public DoubleGrid2D obstaclesSurf;
	public DoubleGrid2D baseFloor; // Use this for displaying.
	public IntGrid2D startSurf; // Use this to initialize pedestrians.

	// --- ENTRANCES AND EXITS ---

	// Entrance and Exit Location Data
	public ArrayList<ArrayList<Int2D>> entrances;
	public ArrayList<ArrayList<Int2D>> exits;
	public IntGrid2D entranceGrid, exitGrid;

	// Entrance and Exit Probability Data
	public double[] entranceProbability; // probability of any given entrance
	public double[][] conditionalExitProbability; // probability of any exit, conditional on the entrance

	// Gradient Data
	ObjectGrid2D entranceExitGradients;

	// --- SETUP ---

	ReadRasterData allWalkableData;
	ReadRasterData footPathsData;
	ReadRasterData obstaclesData;
	ReadRasterData basedDataForVisual;
	ArrayList<Pedestrian> pedsToAdd;

	// --- STATISTICS ---

	public double averageSpeed = 0.0; // ave over peds of their ave. speed.
	public double stdSpeed = 0.0; // std over peds of their ave. speed.
	public double maxSpeed = 0.0;
	public double minSpeed = Double.MAX_VALUE - 1.0;
	public double averageDensity = 7.25; // ave over peds of their ave. 1/density
	public double stdDensity = 0.0;
	public double maxDensity = 0.0;
	public double minDensity = Double.MAX_VALUE - 1.0;

	public double totalDistanceTraveled = 0;// used to record total distance traveled by all agents
	public long totalPedSteps = 0;// used to record all total number of steps

	public int grid_width = 100;
	public int grid_height = 100;
	public int noData = -9999;

	// Used to make sure that the XML gets scheduled after the pedestrians
	// Minus 2 just to be sure no issues with sign.
	public double maxGradient = -1 * (Double.MAX_VALUE - 2);
	public double minGradient = Double.MAX_VALUE;

	// --- PARAMETERS ---

	public int pedPlanningVision = 20; // How many meters can a pedestrian see for planning.
	public static double minProb = .01;
	public int neighborhoodType = 3; // Default is set to Circle.
	public static int frequency = 30;// 170;
	public static double frequencyCutoff = .6;

	public int startKeepingRecords = 10, // 72000,
			endKeepingRecords = Integer.MAX_VALUE; // 108000;

	boolean verbose = false;

	// --- OUTPUT ---

	ArrayList<ArrayList<Int3D>> recordOfPaths = new ArrayList<ArrayList<Int3D>>();

	private static final long serialVersionUID = 1L;

	///////////////////////////////////////////////////////
	///////////////////////////////////////////////////////
	// METHODS ////////////////////////////////////////////
	///////////////////////////////////////////////////////
	///////////////////////////////////////////////////////

	/**
	 * Constructors
	 */
	public WalkThisWay(final long seed) {
		super(seed);
	}

	public WalkThisWay(final long seed, final boolean writeTraces) {
		super(seed);
		traceWriterOn = writeTraces;
	}

	/** This method starts the simulation. It can be run without the UI. */
	public void start() {
		super.start();

		try {
			readInEntrancesAndExits();
			readInEntranceExitProbs();
			entranceExitGradients = readInAllGradients();

			readRasterHeaders(); // Read in the headers of the raster files.
			grid_width = allWalkableData.getGridWidth(); // Set the grid_width.
			grid_height = allWalkableData.getGridHeight(); // Set the grid_height.

			checkDims();
			setupContainers();

			if (verbose)
				System.out.println("Reading in raster values.");
			readRasterValues(); // Read in the environmental data about walkability, etc

			if (verbose)
				System.out.println("Creating combined cost surface.");
			createCombinedCostSurface(); // Create the combined cost surface.

			// Determine the availableTiles that can be used.
			if (verbose)
				System.out.println("Initializing available tiles.");
			initializeAvailableTiles();

			if (verbose)
				System.out.print("Adding pedestrians");

			// if the pedestrians are not going to be fed into the system from the data, set
			// up the
			// automatic pedestrian generator
			if (entExtSetup != 3) {

				final Steppable pedAdder = new Steppable() {
					@Override
					public void step(final SimState state) {
						generateRandomPed();
						double increment = random.nextDouble();
						while (increment < WalkThisWay.frequencyCutoff) {
							generateRandomPed();
							increment = random.nextDouble();
						}
						schedule.scheduleOnce(schedule.getTime() + 1, this);
						// schedule.scheduleOnce(schedule.getTime() + random.nextInt(frequency), this);
					}
				};

				schedule.scheduleOnce(0, pedAdder);
			} else
				uploadPeds();

			final Steppable stupidChecker = new Steppable() {

				@Override
				public void step(final SimState state) {
					System.out.println(((WalkThisWay) state).people.size());

				}

			};
			schedule.scheduleRepeating(stupidChecker);

			if (verbose)
				System.out.println("Setup complete.");

		} catch (final Exception e) {
			System.out.println(e);
		}
	} // End method.

	/** This method is called at the end of a simulation. */
	@Override
	public void finish() {

		super.finish();
		final long time = System.currentTimeMillis();
		final String baseFilename = traceDataDir + time + (heatMapsEnabled) + (entExtSetup);

		// -- output traces --

		if (traceWriterOn) {
			try {

				// first need to read in the file header info (ie no. of colums,
				// rows, cell size etc)
				final RasterDoubleGrid2D temp = RasterDoubleGrid2D
						.createFromFile("startonly.txt");

				traces.setHeader(temp);

				// build time stamp for the trace record with no spaces.
				String dateString = ""; // the current date as in: 11/22/2010.
				String timeString = ""; // the current time as in: 11:30:11 PM.
				String tFile = "", tNormFile = ""; // name of the trace record files
				final Calendar day = Calendar.getInstance();// access the system date & time.

				dateString = (day.get(Calendar.MONTH) + 1) + ""
						+ day.get(Calendar.DATE) + "" + day.get(Calendar.YEAR); // current date as: 11222010.
				timeString = day.get(Calendar.HOUR + 1) + "" + day.get(Calendar.MINUTE) + "" + day.get(Calendar.SECOND)
						+ ""
						+ ((day.get(Calendar.AM_PM) == 0) ? "AM" : "PM"); // current time as: 113011PM.

				tFile = baseFilename + "_traces.txt";
				// traces.writeToFile(tFile); // save the file.
				for (int x = 0; x < traces.getWidth(); x++) {
					for (int y = 0; y < traces.getHeight(); y++) {
						System.out.print(traces.field[x][y] + " ");
					}
					System.out.println();
				}

				System.out.println("\n\n\n NORMED \n\n\n");

				tNormFile = baseFilename + "_tracesNORM.txt";
				traces.multiply(1. / totalPedSteps); // normalize it
				for (int x = 0; x < traces.getWidth(); x++) {
					for (int y = 0; y < traces.getHeight(); y++) {
						System.out.print(traces.field[x][y] + "\t");
					}
					System.out.println();
				}
				traces.writeToFile(tNormFile); // save the file.

				// -- output pedestrian paths --

				// first create a new file
				final String oFile = baseFilename + "_paths.txt";

				// name of the output record file.
				BufferedWriter outputWriter;

				// CONSTANT true means file is opened and then a new line is
				// added.
				outputWriter = new BufferedWriter(new FileWriter(oFile));

				for (final ArrayList<Int3D> path : recordOfPaths) {
					String record = "";
					for (final Int3D point : path) {
						record += point.x + " " + point.y + " " + point.z + " ";
					}

					outputWriter.write(record);
					outputWriter.newLine();
				}
				outputWriter.close();

			} catch (final Exception e) {
				e.printStackTrace();
				System.out.println("ERROR writing paths to file!");
			}
		}

		// -- output key model parameters --

		try {

			// first create a new file
			final String oFile = traceDataDir + "Runs.txt"; // join path & time stamp.;

			// name of the output record file.
			FileWriter outputWriter;

			// CONSTANT true means file is opened and then a new line is added.
			outputWriter = new FileWriter(oFile, true);

			System.out.println("avgwalkingSpeed " + getAveWalkingSpeed());
			outputWriter.write(", avgwalkingSpeed " + getAveWalkingSpeed() + "\n");
			outputWriter.close();
		} catch (final Exception e) {
			System.out.println("ERROR:  Model parameter output error.");
			e.printStackTrace();
		}

	}

	// ***************************************************************************

	/////////////////////////////////////////////
	// BEGIN RANDOM /////////////////////////////
	/////////////////////////////////////////////

	int getRandomEntranceIndex() {

		final double rand = random.nextDouble();
		for (int i = 0; i < entranceProbability.length; i++) {
			if (rand <= entranceProbability[i])
				return i;
		}

		return entranceProbability.length - 1; // this should not happen, and indicates a problem with the probabilities
												// read into
		// the simulation
	}

	int getRandomExitIndex(final int entranceIndex) {

		final double rand = random.nextDouble();
		final int len = conditionalExitProbability[entranceIndex].length;
		for (int i = 0; i < len; i++) {
			if (rand <= conditionalExitProbability[entranceIndex][i])
				return i;
		}

		return conditionalExitProbability[entranceIndex].length - 1; // this should not happen, and indicates a problem
																		// with the probabilities read into
		// the simulation
	}

	public void generateRandomPed() {
		Pedestrian p;

		final int entrance = getRandomEntranceIndex(), exit = getRandomExitIndex(entrance);
		final ArrayList<Int2D> entranceTiles = entrances.get(entrance);
		Int2D startPos = entranceTiles
				.get(random.nextInt(entranceTiles.size()));

		int numTries = entranceTiles.size() * 2; // it is possible all entrance tiles are occupied: in this case
		// there is no place the pedestrian can enter, but it's not necessarily worth
		// our time to check
		// for every single pedestrian
		while (people.getObjectsAtLocation(startPos.x, startPos.y) != null && numTries > 0) {
			startPos = entranceTiles.get(random.nextInt(entranceTiles.size()));
			numTries--;
		}
		if (numTries == 0)
			return; // it's possible all of the tiles associated with the entrance may be occupied,
					// in which case just don't add anyone.

		p = new Pedestrian(startPos, (IntGrid2D) entranceExitGradients.get(entrance, exit), pedPlanningVision, 0);

		// set the random ID here so that it is tied to random seed.
		p.id = Long.toHexString(random.nextLong());
		people.setObjectLocation(p, startPos);

		p.pedColor = colorAgent(); // get color for the ped.

		schedule.scheduleOnce(p);
	}

	/** This method randomly assigns the pedestrian a color. */
	public Color colorAgent() {

		/*
		 * Color colorCarrier = null; // a dummy variable.
		 *
		 * switch (random.nextInt(9)) // pick a color { case 0: colorCarrier =
		 * Color.yellow; break; case 1: colorCarrier = Color.green; break; case 2:
		 * colorCarrier = Color.cyan; break; case 3: colorCarrier = Color.magenta;
		 * break; case 4: colorCarrier = Color.black; break; case 5: colorCarrier =
		 * Color.orange; break; case 6: colorCarrier = Color.pink; break; case 7:
		 * colorCarrier = Color.red; break; case 8: colorCarrier = Color.white; break;
		 * default: colorCarrier = Color.blue; break; }
		 *
		 * return colorCarrier;
		 */
		return Color.green;
	}

	/////////////////////////////////////////////
	// END RANDOM ///////////////////////////////
	/////////////////////////////////////////////

	// ***************************************************************************

	/////////////////////////////////////////////
	// BEGIN READING IN DATA ////////////////////
	/////////////////////////////////////////////

	/** Set up containers to hold the environmental data */
	private void setupContainers() {
		// ------------------------------------------------------------
		// Establish the appropriate fields with the correct dimensions
		// Initialize the trace to be zero for all grids.
		traces = new RasterDoubleGrid2D(grid_width, grid_height, 0);
		people = new SparseGrid2D(grid_width, grid_height);
		obstacles = new DoubleGrid2D(grid_width, grid_height);

		allWalkableSurf = new DoubleGrid2D(grid_width, grid_height);
		footPathsSurf = new DoubleGrid2D(grid_width, grid_height);
		obstaclesSurf = new DoubleGrid2D(grid_width, grid_height);
		baseFloor = new DoubleGrid2D(grid_width, grid_height);
		startSurf = new IntGrid2D(grid_width, grid_height);

		availableTiles = new ArrayList<Int2D>();
		// ------------------------------------------------------------
	}

	/** Combines the different cost surfaces into one that the pedestrians use. */
	private void createCombinedCostSurface() {

		double num = 0.0;

		for (int i = 0; i < obstacles.getWidth(); i++) {
			for (int j = 0; j < obstacles.getHeight(); j++) {

				// If the obstacle surface indicates that the tile is impassible, label it
				// with value 0.
				num = obstaclesSurf.field[i][j];
				if (num == -9999 || num >= Double.MAX_VALUE)
					num = 0.0;

				// if it is not impassible, save the value
				else {

					// for tiles which are not impassible, set the cost relative to the footpath
					// value
					if (footPathsSurf.field[i][j] != -9999 && num > 0) {
						num = footPathsSurf.field[i][j] * 50 / 628.4 + 1; // conversion between pixels of video for
																			// validation and simulated space
					} else // increment the value, because now the value 0 indicates an impassible tile
						num += 1;
				}
				obstacles.set(i, j, num);
			}
		}
	}

	/**
	 * Generate a list of tiles which are available at the beginning of the
	 * simulation
	 */
	private void initializeAvailableTiles() {

		for (int i = 0; i < startSurf.getWidth(); i++)
			for (int j = 0; j < startSurf.getHeight(); j++)

				// Add cell if startSurf has a value of 1.
				if (startSurf.field[i][j] == 1)
					availableTiles.add(new Int2D(i, j));
	}

	/**
	 * Read in the headers of the environmental data to ensure that they all are
	 * formatted correctly
	 */
	private void readRasterHeaders() {

		// Read in data from the file for allWalkable cost surface
		try {
			allWalkableData = new ReadRasterData("allwalkable.txt");
		} catch (final IOException e) {
			System.err.println(e);
			System.out.println("Exception caught while reading in allWalkable file");
		}

		// Read in data from the file for footPaths cost surface
		try {
			footPathsData = new ReadRasterData("footpaths.txt");
		} catch (final IOException e) {
			System.err.println(e);
			System.out.println("Exception caught while reading in footpaths file");
		}

		// Read in data from the file for obstacles cost surface
		try {
			obstaclesData = new ReadRasterData("obstacles.txt");
		} catch (final IOException e) {
			System.err.println(e);
			System.out.println("Exception caught while reading in obstacles file");
		}

		// Read in data from the file for baseFloor for visualization
		try {
			basedDataForVisual = new ReadRasterData("basedataforvisual.txt");
		} catch (final IOException e) {
			System.err.println(e);
			System.out.println("Exception caught while reading in basedataforvisual file");
		}

	} // End method.

	/**
	 * Read in the bodies of the environmental data to the provided data structures
	 */
	private void readRasterValues() {

		try {
			allWalkableData.readDoubleRaster(allWalkableSurf);
		} catch (final IOException e1) {
			e1.printStackTrace();
			System.out.println("Problems in readRasterValues() for allWalkableSurf");
		}

		try {
			footPathsData.readDoubleRaster(footPathsSurf);
		} catch (final IOException e1) {
			e1.printStackTrace();
			System.out.println("Problems in readRasterValues() for footPathsSurf");
		}

		try {
			obstaclesData.readDoubleRaster(obstaclesSurf);
		} catch (final IOException e1) {
			e1.printStackTrace();
			System.out.println("Problems in readRasterValues() for obstaclesSurf");
		}

		try {
			basedDataForVisual.readDoubleRaster(baseFloor);
		} catch (final IOException e1) {
			e1.printStackTrace();
			System.out.println("Problems in readRasterValues() for baseFloor");
		}

	}

	/**
	 * Check the environmental data to ensure that all of the files have the same
	 * dimensions.
	 */
	void checkDims() {
		// Data Integrity Check On Dimensions
		if (errorCheckGridDim(allWalkableData, footPathsData)) {
			System.out.println("ERROR:  allWalkableData & footPathsData have different dimensions");
			System.exit(0);
		}
		if (errorCheckGridDim(allWalkableData, obstaclesData)) {
			System.out.println("ERROR:  allWalkableData & obstaclesData have different dimensions");
			System.exit(0);
		}
		if (errorCheckGridDim(allWalkableData, basedDataForVisual)) {
			System.out.println("ERROR: allWalkableData & baseDataForVisual have different dimensions");
			System.exit(0);
		}
	}

	/** This method returns true if the dimensions disagree between inputs. */
	private boolean errorCheckGridDim(final ReadRasterData data1, final ReadRasterData data2) {

		if (data1.getGridHeight() == data2.getGridHeight()
				&& data1.getGridWidth() == data2.getGridWidth())
			return false;
		else
			return true;
	}

	/** Read integer values into an IntGrid2D */
	public IntGrid2D readIntoGrid(final String filename)
			throws NumberFormatException, IOException {

		IntGrid2D result = null;

		try {
			final BufferedReader d = new BufferedReader(
					new InputStreamReader(WalkThisWayData.class.getResourceAsStream(filename)));

			String s;

			final int width = Integer.parseInt(d.readLine());
			final int height = Integer.parseInt(d.readLine());

			result = new IntGrid2D(width, height);

			int j = 0;
			while ((s = d.readLine()) != null) {
				final String[] bits = s.split("\t");
				for (int i = 0; i < bits.length; i++) {
					final int num = Integer.parseInt(bits[i]);
					result.field[i][j] = num;
				}
				j++;
			}

			d.close();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * Reads in gradient information for each combination of entrances and exits.
	 *
	 * The gradient data is stored in file in the following format: <br>
	 * // Gradient File Format ///////<br>
	 * (Number of Entrances)<br>
	 * (Number of Exits)<br>
	 * (Width of environment)<br>
	 * (Height of environment)<br>
	 * <br>
	 * (Entrance Number)\t(Exit Number)<br>
	 * [0,0] \t [1,0] \t ...<br>
	 * [0,1] \t [1,1] \t ...<br>
	 * ...<br>
	 * (repeats for each combination of entrance and exit)<br>
	 * // END Gradient File Format ///////<br>
	 *
	 * @return a data structure holding the relevant gradient information for each
	 *         combination of entrances and exits
	 */
	public ObjectGrid2D readInAllGradients() throws NumberFormatException,
			IOException {

		ObjectGrid2D result = null;

		try {

			// Set up the file input stream depending on the user-selected day
			// and whether the user
			// has enabled the use of gradients
			InputStream instream;
			if (heatMapsEnabled) {
				final String filename = "gradients" + dayFiles[dayFile] + ".txt";
				instream = WalkThisWayData.class.getResourceAsStream(filename);
			} else
				instream = WalkThisWayData.class.getResourceAsStream("entranceExitGradientsNOHEAT.txt");

			// Convert our input stream to a BufferedReader
			final BufferedReader d = new BufferedReader(
					new InputStreamReader(instream));

			// Read in and set up basic parameters for the rest of the file
			String s;
			final int numEntrances = Integer.parseInt(d.readLine());
			final int numExits = Integer.parseInt(d.readLine());
			final int worldWidth = Integer.parseInt(d.readLine());
			final int worldHeight = Integer.parseInt(d.readLine());

			// Set up a container to hold the gradient information
			result = new ObjectGrid2D(numEntrances, numExits);

			//
			// READ IN DATA
			//
			while ((s = d.readLine()) != null) {

				// skip the empty line at the beginning of each section
				if (s.length() == 0)
					continue;

				// the section header: (entrance#) (exit#)
				String[] bits = s.split("\t");
				final int ent = Integer.parseInt(bits[0]), ext = Integer.parseInt(bits[1]);

				// the grid to hold the gradient between the entrance/exit pair
				final IntGrid2D grid = new IntGrid2D(worldWidth, worldHeight);

				// pan across the environment and read in each tile "height" or "heat"
				for (int i = 0; i < worldWidth; i++) {
					s = d.readLine();
					bits = s.split("\t");
					for (int j = 0; j < bits.length; j++) {

						// parse out the tile's value
						int num = Integer.parseInt(bits[j]);

						// increase all of the values so that the 0's can be colored to
						// show that they are exits, for easier user comprehension
						num += 1.0;

						// store the information
						grid.field[i][j] = num;

						// update the range of information about the gradient
						if (num < minGradient && num >= 0)
							minGradient = num;
						if (num > maxGradient)
							maxGradient = num;
					}
				}

				s = d.readLine(); // get rid of the empty line between sections

				// store the entrance/exit pair's gradient in the appropriate place
				result.field[ent][ext] = grid;
			}

			// clean up
			d.close();

		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * Pull in information about the location of entrances and exits in the
	 * environment.
	 */
	public void readInEntrancesAndExits() {
		try {

			// read in ENTRANCES

			entranceGrid = readIntoGrid("EntranceUNIQUE.txt");

			entrances = new ArrayList<ArrayList<Int2D>>();

			for (int i = 0; i < entranceGrid.getWidth(); i++)
				for (int j = 0; j < entranceGrid.getHeight(); j++) {
					final int num = entranceGrid.field[i][j];
					if (num > 0) {
						while (num > entrances.size())
							entrances.add(new ArrayList<Int2D>());
						entrances.get(num - 1).add(new Int2D(i, j));
					}
				}

			// read in EXITS

			exitGrid = readIntoGrid("ExitUNIQUE.txt");

			exits = new ArrayList<ArrayList<Int2D>>();

			for (int i = 0; i < exitGrid.getWidth(); i++)
				for (int j = 0; j < exitGrid.getHeight(); j++) {
					final int num = exitGrid.field[i][j];
					if (num > 0) {
						while (num > exits.size())
							exits.add(new ArrayList<Int2D>());
						exits.get(num - 1).add(new Int2D(i, j));
					}
				}

		} catch (final NumberFormatException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/** Add Pedestrians into the simulation at specified times */
	class PedAdder implements Steppable {

		Pedestrian p;

		public PedAdder(final Pedestrian p) {
			this.p = p;
		}

		public void step(final SimState state) {
			people.setObjectLocation(p, new Int2D(p.locX, p.locY));
			schedule.scheduleOnce(schedule.getTime(), p);
		}

	}

	/** Add Pedestrians from a file */
	public boolean uploadPeds() {

		pedsToAdd = new ArrayList<Pedestrian>();

		FileInputStream fstream;
		try {

			// determine which data file to read in
			final String filename = "testinput" + dayFiles[dayFile] + ".txt";
			fstream = new FileInputStream(filename);

			// Convert our input stream to a BufferedReader
			final BufferedReader d = new BufferedReader(new InputStreamReader(fstream));

			String s;

			// each line of the file represents a pedestrian
			while ((s = d.readLine()) != null) {

				// the first three tab-delimited numbers are the x-y coordinate point and time
				// of
				// entrance by the pedestrian into the simulation
				final String[] bits = s.split("\t");
				final Integer x = Integer.parseInt(bits[0]),
						y = Integer.parseInt(bits[1]),
						time = Integer.parseInt(bits[2]);

				// determine which entrance this entrance point entails
				final ArrayList<Int2D> entrancePoints = entrances.get(x);
				final Int2D startPos = entrancePoints.get(random.nextInt(entrancePoints.size()));

				// generate a Pedestrian with that same entrance
				final Pedestrian p = new Pedestrian(startPos,
						(IntGrid2D) entranceExitGradients.get(x, y), pedPlanningVision, 0);

				// set the random ID here so that it is tied to random seed.
				p.id = Long.toHexString(random.nextLong());
				p.pedColor = colorAgent(); // randomly color the agent

				// schedule the Pedestrian to enter the simulation at the appropriate time
				schedule.scheduleOnce(time, new PedAdder(p));

				// add the Pedestrian to a list for tracking purposes
				pedsToAdd.add(p);
			}

			return true;

		} catch (final Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Reads in probabilities of agent entering through a specific entrance,
	 * conditional probability based on that entrance of every given exit. Some
	 * minimum probability is expected, so that it is never impossible that an agent
	 * move between an entrance and an exit.
	 */
	void readInEntranceExitProbs() throws NumberFormatException, IOException {

		try {

			//
			// SET UP WITH UNIFORM PROBABILITY FOR EACH ENTRANCE AND EXIT
			//
			if (entExtSetup == 0) {

				// set up the containers for the information
				final int width = entrances.size();
				final int height = exits.size();
				entranceProbability = new double[width];
				conditionalExitProbability = new double[width][height];

				// the probability of using any entrance is a constant; likewise the probability
				// of
				// using any exit, regardless of entrance. The values are precalculated...
				final double entProb = 1. / width, extProb = 1. / height;

				// ...then the probability matrix is populated
				for (int i = 0; i < width; i++) {
					entranceProbability[i] = (i + 1) * entProb;
					for (int j = 0; j < height; j++)
						conditionalExitProbability[i][j] = (j + 1) * extProb;
				}
			}

			//
			// SET UP WITH DATA-DERIVED ENTRANCE PROBABILITIES
			//
			else if (entExtSetup == 1) {

				// Select a file based on the user-given settings
				final String filename = "entExtCombo" + dayFiles[dayFile] + ".txt"; // TODO change to make more
																					// agents!!!
				// Convert our input stream to a BufferedReader
				final BufferedReader d = new BufferedReader(
						new InputStreamReader(WalkThisWayData.class.getResourceAsStream(filename)));

				// Set up containers for the information
				String s;
				final int width = Integer.parseInt(d.readLine());
				final int height = Integer.parseInt(d.readLine());
				entranceProbability = new double[width];
				conditionalExitProbability = new double[width][height];

				// Pull in all of the entrance data from the file into the preconstructed
				// containers
				for (int i = 0; i < width; i++) {
					s = d.readLine();
					final double num = Math.max(WalkThisWay.minProb, Double.parseDouble(s));
					if (i > 0)
						entranceProbability[i] = num + entranceProbability[i - 1];
					else
						entranceProbability[i] = num;
				}

				// Pull in all of the conditional exit data from the file into the containers
				int j = 0;
				while ((s = d.readLine()) != null) {

					// each line of the file represents an entrance, with conditional probability
					// of every given exit separated by tabs
					final String[] bits = s.split("\t");

					// go through the exits for a given entrance
					for (int i = 0; i < bits.length; i++) {
						final double num = Math.max(WalkThisWay.minProb, Double.parseDouble(bits[i]));

						// we structure the distribution as a CDF, essentially
						if (i > 0)
							conditionalExitProbability[j][i] = num
									+ conditionalExitProbability[j][i - 1];
						else
							conditionalExitProbability[j][i] = num;
					}

					j++; // keep track of which entrance we're considering!
				}

				// Clean up
				d.close();
			}

		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}

	}

	/////////////////////////////////////////////
	// END READING IN DATA //////////////////////
	/////////////////////////////////////////////

	// ***************************************************************************

	/////////////////////////////////////////////
	// BEGIN STATISTICS /////////////////////////
	/////////////////////////////////////////////

	/**
	 * This method calculates the averages speed of pedestrians. It uses each
	 * pedestrian's average speed. The result is stored GLOBALLY.
	 */
	public void calcAverageSpeedAndDensity() {

		double speedResult = 0.0D;
		double speedResultSq = 0.0D;
		double densityResult = 0.0D;
		double densityResultSq = 0.0D;

		double maxPedSpeed = 0.0D;
		double minPedSpeed = Double.MAX_VALUE - 1.0;

		double maxPedDensity = 0.0D;
		double minPedDensity = Double.MAX_VALUE - 1.0;

		for (int i = 0; i < people.size(); i++) {

			final Pedestrian p = (Pedestrian) people.allObjects.objs[i];

			// test to make sure the peds used in this calculation are active.
			// all peds in a Sim w/o timedArrivals are active.
			// peds in Sims w/timedArrivals and entryTimer values LTE 0 are
			// active.
			speedResult += p.averageSpeed;
			speedResultSq += (p.averageSpeed * p.averageSpeed);
			densityResult += p.averageDensity;
			densityResultSq += (p.averageDensity * p.averageDensity);

			if (p.averageSpeed > maxPedSpeed)
				maxPedSpeed = p.averageSpeed;
			if (p.averageSpeed < minPedSpeed)
				minPedSpeed = p.averageSpeed;

			if (p.averageDensity > maxPedDensity)
				maxPedDensity = p.averageDensity;
			if (p.averageDensity < minPedDensity)
				minPedDensity = p.averageDensity;

		} // loop over all peds in Sim.

		// OK. We're going to need to know two things:
		// 1. how many peds (of any kind) are active in a timed arrival Sim
		// 2. how many ped leaders are active in a time arrival Sim.
		int countOfActivePeds = 0;
		for (int i = 0; i < people.size(); i++) {
			final Pedestrian p = (Pedestrian) people.allObjects.objs[i];
			if (p._getEntryTimer() <= 0)
				countOfActivePeds++;
		}

		countOfActivePeds = people.size(); // set all peds here active
		if (countOfActivePeds >= 1) {
			averageSpeed = speedResult / countOfActivePeds;
			final double averageSpeedSq = speedResultSq / countOfActivePeds;
			stdSpeed = Math.sqrt(averageSpeedSq - averageSpeed * averageSpeed);

			averageDensity = densityResult / countOfActivePeds;
			final double averageDensitySq = densityResultSq
					/ countOfActivePeds;
			stdDensity = Math.sqrt(averageDensitySq - averageDensity
					* averageDensity);

			maxSpeed = maxPedSpeed;
			minSpeed = minPedSpeed;
			maxDensity = maxPedDensity;
			minDensity = minPedDensity;
		}

	}

	/////////////////////////////////////////////
	// END STATISTICS ///////////////////////////
	/////////////////////////////////////////////

	// ***************************************************************************

	/////////////////////////////////////////////
	// BEGIN GET/SETTERS ////////////////////////
	/////////////////////////////////////////////

	int scenario = 0;
	String[] scenarios = new String[] { "User-Defined", "Scenario 1", "Scenario 2", "Scenario 3", "Scenario 4" };

	public int getScenario() {
		return scenario;
	}

	public void setScenario(final int i) {
		scenario = i;
		if (i == 1) { // Scenario 1: No Information
			heatMapsEnabled = false;
			entExtSetup = 0;
		} else if (i == 2) { // Scenario 2: Realistic Entrance and Exit Probabilities But Disabled Heat Maps
			heatMapsEnabled = false;
			entExtSetup = 1;
		} else if (i == 3) { // Scenario 3: Heat Maps Enabled But Disabled Entrance-exit Probabilities
			heatMapsEnabled = true;
			entExtSetup = 0;
		} else if (i == 4) { // Scenario 4: Both Realistic Entrance-exit Probabilities and Heat Maps Enabled
			heatMapsEnabled = true;
			entExtSetup = 1;
		}
	}

	public Object domScenario() {
		return scenarios;
	}

	int dayFile = 2;
	String[] dayFilesDisplayNames = new String[] { "August 24", "August 25", "August 26", "August 26 W/ GAP" };
	String[] dayFiles = new String[] { "Aug24", "Aug25", "Aug26all", "Aug26wo" };

	public int getDayFile() {
		return dayFile;
	}

	public void setDayFile(final int i) {
		dayFile = i;
	}

	public Object domDayFile() {
		return dayFilesDisplayNames;
	}

	int entExtSetup = 1;

	public int getEntExtSetup() {
		return entExtSetup;
	}

	public void setEntExtSetup(final int i) {
		entExtSetup = i;
	}

	public Object domEntExtSetup() {
		return new String[] { "Uniform", "Probabilistic", "True Values" };
	}

	boolean heatMapsEnabled = true;

	public boolean getHeatMapsEnabled() {
		return heatMapsEnabled;
	}

	public void setHeatMapsEnabled(final boolean enabled) {
		heatMapsEnabled = enabled;
	}

	/** These methods control the trace writer output control flag. */
	public boolean getTraceWriterOn() {
		return traceWriterOn;
	}

	public void setTraceWriterOn(final boolean on) {
		traceWriterOn = on;
	}

	/** These methods control the Pedestrian's planning vision distance */
	public int getPedPlanningVision() {
		return pedPlanningVision;
	}

	public void setPedPlanningVision(final int planningVision) {
		pedPlanningVision = planningVision;
	}

	public int getNeighborhoodType() {
		return neighborhoodType;
	}

	public void setNeighborhoodType(final int neighborhoodType) {
		this.neighborhoodType = neighborhoodType;
	}

	public Object domNeighborhoodType() {
		return new String[] { "Max", "Hamiltonian", "Hexagonal", "Circle" };
	}

	/** Returns average speed in distance units per step (second). */
	public double getAveWalkingSpeed() {
		if (totalPedSteps == 0)
			return 0.0D;
		// as cell size is 50cm and walking speed is in metres
		return (totalDistanceTraveled / totalPedSteps) * 0.5;
	}

	/////////////////////////////////////////////
	// END GET/SETTERS //////////////////////////
	/////////////////////////////////////////////

	/**
	 * This is the main method.
	 */
	public static void main(final String[] args) {
		doLoop(WalkThisWay.class, args);
		System.exit(0);

	}

}
