package refugee;

import java.io.*;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

import refugee.refugeeData.RefugeeData;
import sim.field.continuous.Continuous2D;
import sim.field.geo.GeomVectorField;
import sim.field.grid.SparseGrid2D;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.io.geo.ShapeFileImporter;
import sim.portrayal.grid.SparseGridPortrayal2D;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Int2D;
import sim.util.geo.MasonGeometry;
import net.sf.csv4j.*;
import org.apache.commons.math3.distribution.NormalDistribution;

class MigrationBuilder {
	public static Migration migrationSim;
	private static NormalDistribution nd = new NormalDistribution(Parameters.AVG_FAMILY_SIZE,
			Parameters.FAMILY_SIZE_SD);
	private static HashMap<Integer, ArrayList<Double>> age_dist;
	private static HashMap<Integer, Double> pop_dist;
	private static HashMap<Integer, NormalDistribution> fin_dist;

	// initialize world
	public static void initializeWorld(Migration sim) {

		migrationSim = sim;
		
		age_dist = new HashMap<Integer, ArrayList<Double>>();
		pop_dist = new HashMap<Integer, Double>();
		fin_dist = new HashMap<Integer, NormalDistribution>();

		String[] regionAttributes = { "REGION"};
		String[] countryAttributes = { "COUNTRY"};
		String[] roadAttributes = { "NAME1", "TYPE"};
		String[] cityAttributes = { "ID", "NAME_1", "ORIG", "POP", "SPOP_1", "QUOTA_1", "VIOL_1", "ECON_1", "FAMILY_1" };
		String[] roadLinksAttributes = { "ID", "FR", "TO", "SPEED_1", "SPOP", "COST", "TLEVEL_1", "DEATH_1", "LENGTH_1" };

		migrationSim.world_height = 500; // TODO - set correct size
		migrationSim.world_width = 500; // TODO - set correct size

		migrationSim.regions = new GeomVectorField(sim.world_width, sim.world_height);
		Bag regionAtt = new Bag(regionAttributes);
		
		migrationSim.countries = new GeomVectorField(sim.world_width, sim.world_height);
		Bag countryAtt = new Bag(countryAttributes);
		
		migrationSim.roads = new GeomVectorField(sim.world_width, sim.world_height);
		Bag roadAtt = new Bag(roadAttributes);

		migrationSim.cityPoints = new GeomVectorField(sim.world_width, sim.world_height);
		Bag cityAtt = new Bag(cityAttributes);
		migrationSim.cityGrid = new SparseGrid2D(sim.world_width, sim.world_height);

		migrationSim.roadNetwork = new Network();
		migrationSim.allRoadNodes = new SparseGrid2D(sim.world_width, sim.world_height);
		migrationSim.roadLinks = new GeomVectorField(sim.world_width, sim.world_height);
		Bag roadLinksAtt = new Bag(roadLinksAttributes);
		
		String[] files = { Parameters.REGION_SHP, Parameters.COUNTRY_SHP,Parameters.ROAD_SHP, Parameters.CITY_SHP, Parameters.ROADLINK_SHP };// shapefiles
		Bag[] attfiles = { regionAtt, countryAtt, roadAtt, cityAtt, roadLinksAtt };
		GeomVectorField[] vectorFields = { migrationSim.regions, migrationSim.countries,migrationSim.roads,migrationSim.cityPoints,migrationSim.roadLinks};
		readInShapefile(files, attfiles, vectorFields);// read in attributes

		// expand the extent to include all features
		Envelope MBR = migrationSim.regions.getMBR();
		MBR.expandToInclude(migrationSim.roadLinks.getMBR());
		MBR.expandToInclude(migrationSim.cityPoints.getMBR());

		migrationSim.regions.setMBR(MBR);
		migrationSim.countries.setMBR(MBR);
		migrationSim.roads.setMBR(MBR);
		migrationSim.roadLinks.setMBR(MBR);
		migrationSim.cityPoints.setMBR(MBR);

		makeCities(migrationSim.cityPoints, migrationSim.cityGrid, migrationSim.cities, migrationSim.cityList);
		extractFromRoadLinks(migrationSim.roadLinks, migrationSim);
		setUpAgeDist(Parameters.AGE_DIST);
		setUpPopDist(Parameters.POP_DIST);
		setUpFinDist(Parameters.FIN_DIST);

		// add refugees
		addRefugees();

	}

	private static void printCities() {
		for (Object city : migrationSim.cities) {
			City c = (City) city;
			System.out.format("Name: " + c.getName() + " Ref Pop: " + c.getRefugeePopulation());
			System.out.println("\n");
		}
	}

	/*public static class Node {
		public Int2D location;

		ArrayList<Edge> links;
		double weightOnLineString;// measures the weight on the line string from
									// 0
		public HashSet<LineString> lineStrings = new HashSet<LineString>();
		public int index;

		public Node(Int2D l) {
			location = l;
			links = new ArrayList<Edge>();
		}

		public ArrayList<Edge> getLinks() {
			return links;
		}

		@Override
		public String toString() {
			return "(" + location.getX() + ", " + location.getY() + ")";
		}
		//
	}*/

	static void makeCities(GeomVectorField cities_vector, SparseGrid2D grid, Bag addTo, Map<Integer, City> cityList) {
		Bag cities = cities_vector.getGeometries();
		Envelope e = cities_vector.getMBR();
		double xmin = e.getMinX(), ymin = e.getMinY(), xmax = e.getMaxX(), ymax = e.getMaxY();
		int xcols = migrationSim.world_width - 1, ycols = migrationSim.world_height - 1;
		System.out.println("Reading in Cities");
		for (int i = 0; i < cities.size(); i++) {
			MasonGeometry cityinfo = (MasonGeometry) cities.objs[i];
			Point point = cities_vector.getGeometryLocation(cityinfo);
			double x = point.getX(), y = point.getY();
			int xint = (int) Math.floor(xcols * (x - xmin) / (xmax - xmin)),
					yint = (int) (ycols - Math.floor(ycols * (y - ymin) / (ymax - ymin)));
			String name = cityinfo.getStringAttribute("NAME_1");
			int ID = cityinfo.getIntegerAttribute("ID");
			int origin = cityinfo.getIntegerAttribute("ORIG");
			double scaledPop = cityinfo.getDoubleAttribute("SPOP_1");
			int pop = cityinfo.getIntegerAttribute("POP");
			int quota = cityinfo.getIntegerAttribute("QUOTA_1");
			double violence = cityinfo.getDoubleAttribute("VIOL_1");
			double economy = cityinfo.getDoubleAttribute("ECON_1");
			double familyPresence = cityinfo.getDoubleAttribute("FAMILY_1");
			Int2D location = new Int2D(xint, yint);

			City city = new City(location, ID, name, origin, scaledPop, pop, quota, violence, economy, familyPresence);
			addTo.add(city);
			cityList.put(ID, city);
			grid.setObjectLocation(city, location);
		}
	}

	static void readInShapefile(String[] files, Bag[] attfiles, GeomVectorField[] vectorFields) {
		try {
			for (int i = 0; i < files.length; i++) {
				Bag attributes = attfiles[i];
				String filePath = files[i];

				URL shapeURI = getUrl(filePath);
				ShapeFileImporter.read(shapeURI, vectorFields[i], attributes);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private static URL getUrl(String nodesFilename) throws IOException {
		InputStream nodeStream = RefugeeData.class.getResourceAsStream(nodesFilename);
		try {
			if (!new File("./shapeFiles/").exists()) {
				new File("./shapeFiles/").mkdir();
			}
			File targetFile = new File("./shapeFiles/" + nodesFilename.split("/")[nodesFilename.split("/").length - 1]);
			OutputStream outStream = new FileOutputStream(targetFile);
			//outStream.write(buffer);
			int read = 0;
			byte[] bytes = new byte[1024];
			while ((read = nodeStream.read(bytes)) != -1) {
				outStream.write(bytes, 0, read);
			}
			outStream.close();
			nodeStream.close();
			if (nodesFilename.endsWith(".shp")) {
				getUrl(nodesFilename.replace("shp", "dbf"));
				getUrl(nodesFilename.replace("shp", "prj"));
				getUrl(nodesFilename.replace("shp", "sbx"));
				getUrl(nodesFilename.replace("shp", "sbn"));
				getUrl(nodesFilename.replace("shp", "shx"));
			}
			return targetFile.toURI().toURL();
		} catch (Exception e) {
			if (nodesFilename.endsWith("shp")) {
				e.printStackTrace();
				return null;
			} else {
				//e.printStackTrace();
				return null;
			}
		}
	}
	private static void addRefugees() {
		System.out.println("Adding Refugees ");
		migrationSim.world = new Continuous2D(Parameters.WORLD_DISCRETIZTION, migrationSim.world_width,
				migrationSim.world_height); // TODO set this correctly
		for (Object c : migrationSim.cities) {

			City city = (City) c;
			if (city.getOrigin() == 1) {
				int currentPop = 0;// 1,4,5,10,3,14,24
				int citypop = (int)Math.round(pop_dist.get(city.getID()) * Parameters.TOTAL_POP);
				System.out.println(city.getName() + ": " + citypop);
				while (currentPop <= citypop) { 
					RefugeeFamily r = createRefugeeFamily(city);
					System.out.println(r.getFamily().size());
					migrationSim.refugeeFamilies.add(r);
					for (Object o: r.getFamily()){
						Refugee refugee = (Refugee)o;
						currentPop++;
						city.addMember(refugee);
						migrationSim.refugees.add(refugee);
						Int2D loc = city.getLocation();
						double y_coord = (loc.y * Parameters.WORLD_TO_POP_SCALE)
								+ (int) (migrationSim.random.nextDouble() * Parameters.WORLD_TO_POP_SCALE);
						double x_coord = (loc.x * Parameters.WORLD_TO_POP_SCALE)
								+ (int) (migrationSim.random.nextDouble() * Parameters.WORLD_TO_POP_SCALE);
						migrationSim.world.setObjectLocation(refugee, new Double2D(x_coord, y_coord));
						int y_coordint = loc.y + (int) ((migrationSim.random.nextDouble() - 0.5) * 3);
						int x_coordint = loc.x + (int) ((migrationSim.random.nextDouble() - 0.5) * 3);
						migrationSim.total_pop++;
					}
					migrationSim.schedule.scheduleRepeating(r);

				}

			}

		}
	}

	private static RefugeeFamily createRefugeeFamily(City city) {

		// generate family
		int familySize = pickFamilySize();
		double finStatus = pick_fin_status(fin_dist, city.getID()) * familySize;
		//System.out.println(finStatus);
		RefugeeFamily refugeeFamily = new RefugeeFamily(city.getLocation(), familySize, city, finStatus);
		for (int i = 0; i < familySize; i++) {

			// first pick sex
			int sex;
			if (migrationSim.random.nextBoolean())
				sex = Constants.MALE;
			else
				sex = Constants.FEMALE;

			// now get age
			int age = pick_age(age_dist, city.getID());
			System.out.println(age);

			Refugee refugee = new Refugee(sex, age, refugeeFamily);
			refugeeFamily.getFamily().add(refugee);
		}
		return refugeeFamily;

	}

	private static int pick_age(HashMap<Integer, ArrayList<Double>> age_dist, int cityid) {
		int category = 0;
		double rand = migrationSim.random.nextDouble();
		ArrayList<Double> dist = age_dist.get(cityid);
		for (int i = 1; i < 4; i++) {
			if (rand >= dist.get(i - 1) && rand <= dist.get(i)) {
				category = i;
				System.out.println("" + category);
				break; // TODO DOES THIS ACTUALLY BREAK
			}
		}

		switch (category) {
		case 0:
			return migrationSim.random.nextInt(5); // 0-4
		case 1:
			return migrationSim.random.nextInt(13) + 5; // 5-17
		case 2:
			return migrationSim.random.nextInt(42) + 18; // 18-59
		case 3:
			return migrationSim.random.nextInt(41) + 60; // 60+
		default:
			return 0;
		}
		//return 5;

	}

	private static void setUpPopDist(String pop_dist_file) {
		try {
			// buffer reader for age distribution data
			CSVReader csvReader = new CSVReader(new InputStreamReader(RefugeeData.class.getResourceAsStream(pop_dist_file)));
			//csvReader.readLine();// skip the headers
			List<String> line = csvReader.readLine();
			while (!line.isEmpty()) {
				// read in the county ids
				int city_id = NumberFormat.getNumberInstance(java.util.Locale.US).parse(line.get(0)).intValue();
				// relevant info is from 5 - 21
				double percentage = Double.parseDouble(line.get(1));
				pop_dist.put(city_id, percentage);
				line = csvReader.readLine();
			}
			System.out.println(pop_dist);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (java.text.ParseException e) {
			e.printStackTrace();
		}
	}
	
	private static void setUpFinDist(String fin_dist_file) {
		try {
			// buffer reader for age distribution data
			CSVReader csvReader = new CSVReader(new InputStreamReader(RefugeeData.class.getResourceAsStream((fin_dist_file))));
			//csvReader.readLine();// skip the headers
			List<String> line = csvReader.readLine();
			while (!line.isEmpty()) {
				// read in the county ids
				int city_id = NumberFormat.getNumberInstance(java.util.Locale.US).parse(line.get(0)).intValue();
				// relevant info is from 5 - 21
				double avgfin = Double.parseDouble(line.get(2));
				double sd = Double.parseDouble(line.get(3));
				fin_dist.put(city_id, new NormalDistribution(avgfin, sd));
				line = csvReader.readLine();
			}
			System.out.println("fin");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (java.text.ParseException e) {
			e.printStackTrace();
		}
	}
	
	private static void setUpAgeDist(String age_dist_file) {
		try {
			// buffer reader for age distribution data
			CSVReader csvReader = new CSVReader(new InputStreamReader(RefugeeData.class.getResourceAsStream(age_dist_file)));
			csvReader.readLine();
			List<String> line = csvReader.readLine();
			while (!line.isEmpty()) {
				// read in the county ids
				int city_id = NumberFormat.getNumberInstance(java.util.Locale.US).parse(line.get(0)).intValue();
				// relevant info is from 5 - 21
				ArrayList<Double> list = new ArrayList<Double>();
				double sum = 0;
				for (int i = 1; i <= 4; i++) {
					double percentage = Double.parseDouble(line.get(i));
					sum += percentage;
					list.add(sum);
				}
				// System.out.println("sum = " + sum);
				// System.out.println();

				// now add it to the hashmap
				age_dist.put(city_id, list);

				line = csvReader.readLine();
			}
			System.out.println(age_dist);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (java.text.ParseException e) {
			e.printStackTrace();
		}
	}
	


	private static double pick_fin_status(HashMap<Integer, NormalDistribution> fin_dist, int cityid) {
		// TODO Auto-generated method stub
		NormalDistribution nd = fin_dist.get(cityid);
		return nd.sample();
		
	}

	private static int pickFamilySize() {
		int familySize = (int) Math.round(nd.sample());
		return familySize;
	}

	static void extractFromRoadLinks(GeomVectorField roadLinks, Migration migrationSim) {
		Bag geoms = roadLinks.getGeometries();
		Envelope e = roadLinks.getMBR();
		double xmin = e.getMinX(), ymin = e.getMinY(), xmax = e.getMaxX(), ymax = e.getMaxY();
		int xcols = migrationSim.world_width - 1, ycols = migrationSim.world_height - 1;
		int count = 0;

		for (Object o : geoms) {
			MasonGeometry gm = (MasonGeometry) o;
			int from = gm.getIntegerAttribute("FR");
			int to = gm.getIntegerAttribute("TO");
			double speed = gm.getDoubleAttribute("SPEED_1");
			double distance = gm.getDoubleAttribute("LENGTH_1");
			double spop = gm.getDoubleAttribute("SPOP");
			double cost = gm.getDoubleAttribute("COST");
			double transportlevel = gm.getDoubleAttribute("TLEVEL_1");
			double deaths = gm.getDoubleAttribute("DEATH_1");
			System.out.println("pop weight: " + spop);
			RoadInfo edgeinfo = new RoadInfo(gm.geometry, from, to, speed, spop, distance, cost, transportlevel, deaths);

			// build road network
			migrationSim.roadNetwork.addEdge(migrationSim.cityList.get(from), migrationSim.cityList.get(to), edgeinfo);
			migrationSim.roadNetwork.addEdge(migrationSim.cityList.get(to), migrationSim.cityList.get(from), edgeinfo);
		}
		
		//addRedirects();
	}
	
	/*static void addRedirects(){
		for (Object c: migrationSim.cities){
			City city = (City) c;
			if (migrationSim.roadNetwork.getEdgesOut(city) == null){
				for (Object e: migrationSim.roadNetwork.getEdgesIn(city)){
					Edge edge = (Edge) e;
					City reverseFrom = city;
					City reverseTo = (City)edge.getFrom();
					RoadInfo info = (RoadInfo) edge.getInfo();
					migrationSim.roadNetwork.addEdge(reverseFrom, reverseTo, info);
					
				}
				}
					
			}
		}*/
		
	

}
