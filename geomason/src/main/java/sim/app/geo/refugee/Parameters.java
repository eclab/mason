package sim.app.geo.refugee;
import java.net.URL;
import sim.app.geo.refugee.refugeeData.shapefiles.ShapefileData;
import sim.app.geo.refugee.refugeeData.csvs.CSVData;

class Parameters {

    public static double TEMPORAL_RESOLUTION = 0.4;// steps per hour
    //public static int NUM_ORIG_REFUGEES = 1;
    //public static int NUM_ORIG_PLACES = 7;
    public static int TRIALNO = 0;
    public static int TOTAL_POP = 14;
    // ----Refugee Characteristic Weights----//
    public static double AVG_FAMILY_SIZE = 6.3; // http://www.acted.org/en/food-security-situation-and-livelihood-intervention-opportunities-syrians-refugees-and-host-communit
    public static double FAMILY_SIZE_SD = 2.92; // same as above
    // -------Agent City Care Weights-------//
    public static double DANGER_CARE_WEIGHT = 0.2;
    public static double FAMILY_ABROAD_CARE_WEIGHT = 0.2;
    public static double POP_CARE = 0.75;
    public static double ECON_CARE = 0.75;
    //-------Agent Decision Weights----//
    public static double GOAL_CHANGE_PROB = 0.1;
    // -------Edge Weights-------//
    public static double MAX_EDGE_LENGTH = 1337284.917613;
    public static double MIN_EDGE_LENGTH = 81947.33824;
    public static double MIN_EDGE_COST = 0.0;
    public static double MAX_EDGE_COST = 3375;
    public static double ROAD_DEATH_PROB = 0.1;

    public static double COST_WEIGHT = 0.1;//0.1;
    public static double RISK_WEIGHT = 0.1;//0.1;
    public static double DISTANCE_WEIGHT = 0.1;//0.1;
    public static double SPEED_WEIGHT = 0.1;//0.1;
    public static double POP_WEIGHT = 10.0;
    public static double TRANSPORT_LEVEL_WEIGHT = 0.1;//0.1;
    public static double HEU_WEIGHT = 1/19348237.217718;

    public static double POP_BLOCK_METERS = 926.1;// Height and width of one
    // population block.
    public static int WORLD_TO_POP_SCALE = 10; // scale up from the population
    // data for each household
    public static double WORLD_DISCRETIZTION = 0.1;// discretization or buckets
    // for world granularity

    public static double WALKING_SPEED = 5.1;// km per hour
    // -------File paths-------//
    // public static String POP_PATH = "data/csvs/pop.csv";//have
    // public static String ROAD_INFO = "data/csvs/roadinfo.csv";
    // public static String CITY_INFO = "data/csvs/cityinfo.csv";
    // public static String AGE_DIST_PATH = "";//have, but only take columns
    // that say TOT
    public static URL REGION_SHP = ShapefileData.class.getResource("region.shp");// shapefile
    public static URL REGION_DBF = ShapefileData.class.getResource("region.dbf");// shapefile
    public static URL COUNTRY_SHP = ShapefileData.class.getResource("country2.shp");// shapefile
    public static URL COUNTRY_DBF = ShapefileData.class.getResource("country2.dbf");// shapefile
    public static URL CITY_SHP = ShapefileData.class.getResource("city.shp");// have
    public static URL CITY_DBF = ShapefileData.class.getResource("city.dbf");// have
    public static URL ROAD_SHP = ShapefileData.class.getResource("roads.shp");// shapefile 
    public static URL ROAD_DBF = ShapefileData.class.getResource("roads.dbf");// shapefile 
    public static URL ROADLINK_SHP = ShapefileData.class.getResource("routes5.shp");// shapefile 
    public static URL ROADLINK_DBF = ShapefileData.class.getResource("routes5.dbf");// shapefile 
    public static URL AGE_DIST = CSVData.class.getResource("age_dist2.csv");// have
    public static URL POP_DIST = CSVData.class.getResource("pop_dist.csv");
    public static URL FIN_DIST = CSVData.class.getResource("fin_dist.csv");
    // Edge attributes


    // population flow parameters

    public static double convertToKilometers(double val) {
        return (val * (Parameters.POP_BLOCK_METERS / Parameters.WORLD_TO_POP_SCALE)) / 1000.0;

    }

    public static double convertFromKilometers(double val) {
        return (val * 1000.0) / (Parameters.POP_BLOCK_METERS / Parameters.WORLD_TO_POP_SCALE);
    }

}
