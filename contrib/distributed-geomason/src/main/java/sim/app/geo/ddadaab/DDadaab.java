package sim.app.geo.ddadaab;
//import sim.app.geo.dadaab.dadaabData.DadaabData;


import org.jfree.data.category.DefaultCategoryDataset;



import org.jfree.data.general.DefaultValueDataset;
import org.jfree.data.xy.XYSeries;

import sim.app.geo.dcampusworld.DCampusWorld;
import sim.app.geo.ddadaab.ddadaabData.DDadaabData;

/*
import sim.app.geo.dadaab.CampBuilder;
import sim.app.geo.dadaab.DadaabObserver;
import sim.app.geo.dadaab.Facility;
import sim.app.geo.dadaab.Parameters;
import sim.app.geo.dadaab.Rainfall;
import sim.app.geo.dadaab.TimeManager;
import sim.app.geo.dadaab.dadaabData.DadaabData;
*/


import sim.engine.DSimState;
import sim.engine.DSteppable;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.DContinuous2D;
import sim.field.geo.DGeomVectorField;
import sim.field.geo.GeomGridField;  //replace ?
import sim.field.geo.GeomVectorField; //replace ?
import sim.field.grid.DDenseGrid2D;
import sim.field.grid.DDoubleGrid2D;
import sim.field.grid.DIntGrid2D;
import sim.field.grid.DObjectGrid2D;

import sim.field.network.Network;
import sim.util.Bag;

public class DDadaab extends DSimState{
	
	public DObjectGrid2D<DFieldUnit> allCamps; // The model environment - holds fields ( parcels)
	
	public GeomGridField allCampGeoGrid; //Do I reimpelement this   //DDoubleGrid2D
	//public DDoubleGrid2D allCampGeoGrid; //Try this first, then try DGeomGridField
	
	public DDoubleGrid2D rainfallGrid; // mainly for rainfall vizualization
	public DContinuous2D<DRefugee> allRefugees; // refugee agents
	
	//public SparseGrid2D facilityGrid;// facilities: schools, health center, borehol etc
	public DDenseGrid2D facilityGrid;
	
	public DIntGrid2D roadGrid; // road in grid- for navigation
		
	public DGeomVectorField roadLinks;
	public DGeomVectorField campShape; 
	
	//public SparseGrid2D nodes;
	public DDenseGrid2D nodes;
	
	public DObjectGrid2D closestNodes; // the road nodes closest to each of the // locations
	
	Network roadNetwork = new Network(); //this should be fine?

	public final DParameters params;
	private int totalSusciptible;
	private int totalExposed;
	private int totalInfected;
	private int totalRecovered;
	private int totalSusciptibleNewly;
	private int totalExposedNewly;
	private int totalInfectedNewly;
	private int totalRecoveredNewly;
	public int[] campSuscpitable;
	public int[] campExposed;
	public int[] campInfected;
	public int[] campRecovered;
	private int[] totalActivity;
	private double totalBacterialLoad = 0;
	/**
	 * charts and graphs
	 */
	// agent health status
	private static final long serialVersionUID = -5966446373681187141L;
	public XYSeries totalsusceptibleSeries = new XYSeries("Susceptible"); // shows number of Susceptible agents
	public XYSeries totalExposedSeries = new XYSeries("Exposed");
	public XYSeries totalInfectedSeries = new XYSeries(" Infected"); // shows number of infected agents
	public XYSeries totalRecoveredSeries = new XYSeries(" Recovered"); // shows number of recovered agents

	public XYSeries rainfallSeries = new XYSeries(" Rainfall"); //
	public XYSeries totalsusceptibleSeriesNewly = new XYSeries("Newly Susceptible"); // shows number of Newly
																						// Susceptible agents
	public XYSeries totalExposedSeriesNewly = new XYSeries("Newly Exposed");
	public XYSeries totalInfectedSeriesNewly = new XYSeries("Newly Infected"); // shows number of Newly infected agents
	public XYSeries totalRecoveredSeriesNewly = new XYSeries("Newly Recovered"); // shows number of Newly recovered
																					// agents
	public XYSeries totalTotalPopSeries = new XYSeries(" Total"); // shows number of dead agents
	public XYSeries totalDeathSeries = new XYSeries(" Death"); // shows number of dead agents

	public XYSeries totalBacteriaLoadSeries = new XYSeries(" Total vibrio Cholerae /million"); // shows number of
																								// recovered agents
	// private static final long serialVersionUID = -5966446373681187141L;
	DefaultCategoryDataset dataset = new DefaultCategoryDataset(); //
	DefaultCategoryDataset agedataset = new DefaultCategoryDataset();// shows age structure of agents
	DefaultCategoryDataset familydataset = new DefaultCategoryDataset(); // shows family size
	// timer graphics
	DefaultValueDataset hourDialer = new DefaultValueDataset(); // shows the current hour
	DefaultValueDataset dayDialer = new DefaultValueDataset(); // counts
	public int totalgridWidth = 10;
	public int totalgridHeight = 10;

	public Bag allFamilies; // holding all families
	public Bag campSites; // hold camp sites
	public Bag boreHoles; // holds borehols
	public Bag rainfallWater; // water points from rain
	public Bag allFacilities;
	public Bag schooles;
	public Bag healthCenters;
	public Bag mosques;
	public Bag market;
	public Bag foodCenter;
	public Bag other;
	// WaterContamination fm = new WaterContamination();
	DRainfall rainfall; // scheduling rainfall
	DFacility fac;// schduling borehole refill

	DTimeManager tm = new DTimeManager();

	public DDadaabObserver dObserver;
	int[] sumActivities = { 0, 0, 0, 0, 0, 0, 0, 0 }; //
	int[] dailyRain = new int[365];
	

	

	//public DDadaab(long seed, int width, int height, int aoi, final String[] args) {
	public DDadaab(long seed) {
		super(seed, 100, 100, 1, false);
		// TODO Auto-generated constructor stub
		String[] args = new String[0]; //do I need to pass this better?

		params = new DParameters(args);
		rainfall = new DRainfall();
		fac = new DFacility();//
		allFamilies = new Bag();
		campSites = new Bag();
		boreHoles = new Bag();
		rainfallWater = new Bag();
		allFacilities = new Bag();
		allCampGeoGrid = new GeomGridField();

		schooles = new Bag();
		healthCenters = new Bag();
		mosques = new Bag();
		market = new Bag();
		foodCenter = new Bag();
		other = new Bag();

		totalActivity = new int[10];
		campSuscpitable = new int[3];
		campExposed = new int[3];
		campInfected = new int[3];
		campRecovered = new int[3];
	}
	
	public void start() {
		super.start();
		// accessing inpt files
		try {
			DCampBuilder.create(DDadaabData.class.getResource("d_camp_a.txt"),
					DDadaabData.class.getResource("d_faci_a.txt"),
					DDadaabData.class.getResource("d_costp_a.txt"),
					this, random);

		} catch (final Exception e) {
			e.printStackTrace();
		}

		schedule.scheduleRepeating(rainfall, rainfall.ORDERING, 1);  //should have a rainfall object for each partition I believe
		schedule.scheduleRepeating(fac, fac.ORDERING, 1);  //same with fac?

		// if (getOutputStats ==true){
		dObserver = new DDadaabObserver(this);
		schedule.scheduleRepeating(dObserver, DDadaabObserver.ORDERING, 1.0);
		// }
		// updating chart information
		
		//Do I need to make this DSteppable?  Unsure? /////////////////
		final DSteppable chartUpdater = new DSteppable() {

			// all graphs and charts wll be updated in each steps
			public void step(final SimState state) {

				final Bag ref = new Bag(allRefugees.getAllAgentsInStorage()); // getting all refugees
				//
				final int[] sumAct = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }; // adding each activity and puting the value in
																		// array

				final int[] sumAge = { 0, 0, 0, 0, 0 }; // adding agent all agents whose age falls in a given age-class

				final int[] sumfamSiz = { 0, 0, 0, 0, 0, 0, 0 }; // adding all agent families based o their family size

				int totalSus = 0; // total suscibtible
				int totalExp = 0;
				int totalInf = 0; // total infected
				int totalRec = 0; // total recovered

				int totalSusNewly = 0; // total suscibtible
				int totalExpNewly = 0;
				int totalInfNewly = 0; // total infected
				int totalRecNewly = 0; // total recovered

				// by camp
				int totSusDag = 0;
				int totExpDag = 0;
				int totInfDag = 0;
				int totRecDag = 0;

				int totSusInfo = 0;
				int totExpInfo = 0;
				int totInfInfo = 0;
				int totRecInfo = 0;

				int totSusHag = 0;
				int totExpHag = 0;
				int totInfHag = 0;
				int totRecHag = 0;

				// accessing all families and chatagorize them based on their size
				for (int i = 0; i < allFamilies.numObjs; i++) {
					final DFamily f = (DFamily) allFamilies.objs[i];
					// killrefugee(f);

					int siz = 0;
					if (f.getMembers().numObjs > 6) { // aggregate all families of >6 family size
						siz = 6;
					} else {
						siz = f.getMembers().numObjs - 1;

					}
					sumfamSiz[siz] += 1;
				}
				int none = 0;
				// accessing each agent
				for (int i = 0; i < ref.numObjs; i++) {
					final DRefugee r = (DRefugee) ref.objs[i];

					sumAct[r.getCurrentActivity()] += 1; // current activity

					final int age = ageClass(r.getAge()); // age class of agent i
					// int siz = 0;
					sumAge[age] += 1;

					if (r.getHome().getCampID() == 1) {

						if (r.getHealthStatus() == 1) {
							totSusDag = totSusDag + 1;
						} else if (r.getHealthStatus() == 2) {
							totExpDag = totExpDag + 1;
						} else if (r.getHealthStatus() == 3) {
							totInfDag = totInfDag + 1;

						} else if (r.getHealthStatus() == 4) {
							totRecDag = totRecDag + 1;

						} else {
							none = 0;
						}

					}

					if (r.getHome().getCampID() == 2) {

						if (r.getHealthStatus() == 1) {
							totSusInfo = totSusInfo + 1;
						} else if (r.getHealthStatus() == 2) {
							totExpInfo = totExpInfo + 1;
						} else if (r.getHealthStatus() == 3) {
							totInfInfo = totInfInfo + 1;
						} else if (r.getHealthStatus() == 4) {
							totRecInfo = totRecInfo + 1;
						} else {
							none = 0;
						}

					}

					if (r.getHome().getCampID() == 3) {

						if (r.getHealthStatus() == 1) {
							totSusHag = totSusHag + 1;
						} else if (r.getHealthStatus() == 2) {
							totExpHag = totExpHag + 1;

						} else if (r.getHealthStatus() == 3) {
							totInfHag = totInfHag + 1;
						} else if (r.getHealthStatus() == 4) {
							totRecHag = totRecHag + 1;
						} else {
							none = 0;
						}
					}

					// total health status

					if (r.getHealthStatus() == 1) {

						totalSus = totalSus + 1;

					} else if (r.getHealthStatus() == 2) {
						totalExp = totalExp + 1;

					} else if (r.getHealthStatus() == 3) {

						totalInf = totalInf + 1;

					} else if (r.getHealthStatus() == 4) {

						totalRec = totalRec + 1;

					} else {

						none = 0;

					}

					if (r.getHealthStatus() != r.getPrevHealthStatus()) {
						if (r.getHealthStatus() == 1) {

							totalSusNewly = totalSusNewly + 1;

						} else if (r.getHealthStatus() == 2) {
							totalExpNewly = totalExpNewly + 1;

						} else if (r.getHealthStatus() == 3) {

							totalInfNewly = totalInfNewly + 1;

						} else if (r.getHealthStatus() == 4) {

							totalRecNewly = totalRecNewly + 1;

						} else {

							none = 0;

						}
					}

				}

				setNumberOfSuscipitableNewly(totalSusNewly);
				setNumberOfExposedNewly(totalExpNewly);
				setNumberOfInfectedNewly(totalInfNewly);
				setNumberOfRecoveredNewly(totalRecNewly);

				setNumberOfSuscipitable(totalSus);
				setNumberOfExposed(totalExp);
				setNumberOfInfected(totalInf);
				setNumberOfRecovered(totalRec);

				campSuscpitable[0] = totSusDag;
				campSuscpitable[1] = totSusInfo;
				campSuscpitable[2] = totSusHag;

				campExposed[0] = totExpDag;
				campExposed[1] = totExpInfo;
				campExposed[2] = totExpHag;

				campInfected[0] = totInfDag;
				campInfected[1] = totInfInfo;
				campInfected[2] = totInfHag;

				campRecovered[0] = totRecDag;
				campRecovered[1] = totRecInfo;
				campRecovered[2] = totRecHag;

				setTotalActivity(sumAct); // set activity array output

				final String actTitle = "Activity"; // row key - activity
				final String[] activities = new String[] { "At Home", "School", "Water", "Mosque", "Market", "Food C.",
						"Health C.", "Visit R.", "Social", "Hygiene" };

				// percentage - agent activity by type
				for (int i = 0; i < sumAct.length; i++) {
					dataset.setValue(sumAct[i] * 100 / new Bag(allRefugees.getAllAgentsInStorage()).numObjs, actTitle, activities[i]);
				}

				final String ageTitle = "Age Group";
				final String[] ageC = new String[] { "1-4", "5-11", "12-17", "18-60", "60 +" };

				// ageset
				for (int i = 0; i < sumAge.length; i++) {
					agedataset.setValue(sumAge[i] * 100 / new Bag(allRefugees.getAllAgentsInStorage()).numObjs, ageTitle, ageC[i]);
				}

				final String famTitle = "Household Size";
				final String[] famC = new String[] { "1", "2", "3", "4", "5", "6", "6+" };

				// family size
				for (int i = 0; i < sumAge.length; i++) {
					familydataset.setValue(sumfamSiz[i], famTitle, famC[i]);
				}

				final int totDead = countDeath();

				totalTotalPopSeries.add((state.schedule.time()), new Bag(allRefugees.getAllAgentsInStorage()).numObjs);
				totalDeathSeries.add((state.schedule.time()), totDead);
				// health status - percentage

				totalsusceptibleSeries.add((state.schedule.time()), (totalSus));
				totalExposedSeries.add((state.schedule.time()), (totalExp));
				totalInfectedSeries.add((state.schedule.time()), (totalInf));
				totalRecoveredSeries.add((state.schedule.time()), (totalRec));

				totalsusceptibleSeriesNewly.add((state.schedule.time()), (totalSusNewly));
				totalExposedSeriesNewly.add((state.schedule.time()), (totalExpNewly));
				totalInfectedSeriesNewly.add((state.schedule.time()), (totalInfNewly));
				totalRecoveredSeriesNewly.add((state.schedule.time()), (totalRecNewly));

				totalBacteriaLoadSeries.add((state.schedule.time()), (getTotalBacterialLoad() / 1000000.0));

				rainfallSeries.add((state.schedule.time()), rainfall.getCurrentRain());

				final int m = ((int) state.schedule.time()) % 60;

				final double t = (tm.currentHour((int) state.schedule.time())) + (m / 60.0);
				final int h = 1 + tm.dayCount((int) state.schedule.time()); //
				hourDialer.setValue(t);
				dayDialer.setValue(h);

				setTotalBacterialLoad(rainfall.getTotalBacterialLoad());

			}
		};

		schedule.scheduleRepeating(chartUpdater);
		// System.out.println("total:- "+ allRefugees.getAllObjects().numObjs);
	}
	// age class or ageset
	private int ageClass(final int age) {
		int a = 0;

		if (age < 5) {
			a = 0;
		} else if (age >= 5 && age < 12) {
			a = 1;
		} else if (age >= 12 && age < 18) {
			a = 2;
		} else if (age >= 18 && age < 60) {
			a = 3;
		} else {
			a = 4;
		}

		return a;
	}

	/*
	 * parameters getter and setter methods
	 */
	// initial number of agent

	public void killrefugee(final DRefugee f) {
		f.getFamily().removeMembers(f);

		if (f.getFamily().getMembers().numObjs == 0) {
			allFamilies.remove(f.getFamily());
		}
		allRefugees.removeLocal(f);
	}

	public void setNumberOfSuscipitable(final int expo) {
		totalSusciptible = expo;

	}

	public int getNumberOfSuscipitable() {

		return totalSusciptible;
	}

	public void setNumberOfExposed(final int expo) {
		totalExposed = expo;

	}

	public int getNumberOfExposed() {

		return totalExposed;
	}

	public void setNumberOfInfected(final int inf) {
		totalInfected = inf;

	}

	public int getNumberOfInfected() {

		return totalInfected;
	}

	public void setNumberOfRecovered(final int rec) {
		totalRecovered = rec;

	}

	public int getNumberOfRecovered() {

		return totalRecovered;
	}

	public void setNumberOfSuscipitableNewly(final int expo) {
		totalSusciptibleNewly = expo;

	}

	public int getNumberOfSuscipitableNewly() {

		return totalSusciptibleNewly;
	}

	public void setNumberOfExposedNewly(final int expo) {
		totalExposedNewly = expo;

	}

	public int getNumberOfExposedNewly() {

		return totalExposedNewly;
	}

	public void setNumberOfInfectedNewly(final int inf) {
		totalInfectedNewly = inf;

	}

	public int getNumberOfInfectedNewly() {
		return totalInfectedNewly;
	}

	public void setNumberOfRecoveredNewly(final int rec) {
		totalRecoveredNewly = rec;

	}

	public int getNumberOfRecoveredNewly() {
		return totalRecoveredNewly;
	}

	public void setTotalActivity(final int[] rec) {
		totalActivity = rec;

	}

	public int[] getTotalActivity() {

		return totalActivity;
	}

	public void setTotalBacterialLoad(final double r) {
		totalBacterialLoad = r;
	}

	public double getTotalBacterialLoad() {
		return totalBacterialLoad;
	}

	int PrevPop = 0;
	int curPop = 0;

	public int countDeath() {
		int death = 0;

		final int current = new Bag(allRefugees.getAllAgentsInStorage()).numObjs;
		PrevPop = curPop;
		death = PrevPop - current;
		curPop = current;
		if (death < 0) {
			death = 0;
		}
		return death;
	}
	
	public void finish() {
		super.finish();

		if (dObserver != null) {
			dObserver.finish();
		}

	}
	
	public static void main(final String[] args)
	{
		doLoopDistributed(DDadaab.class, args);
		System.exit(0);
	}

}
