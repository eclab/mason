package ebola;


/**
 * Created by rohansuri on 7/7/15
 */
public class Parameters
{
    public static double SCALE = 0.01; //percentage of total population that agents will be created.  Maximimum of 1
    public static double POPULATION_FLOW_SCALE = 1.5;//scale of how many people move around
    public static int WORLD_TO_POP_SCALE = 10; //scale up from the population data for each household
    public static double WORLD_DISCRETIZTION = 0.1;//discretization or buckets for world granularity
    public static double POP_BLOCK_METERS = 926.1;//Height and width of one population block. (http://www.esri.com/news/arcuser/0400/wdside.html)
    public static double WORLD_LENGTH = WORLD_TO_POP_SCALE * POP_BLOCK_METERS;//The size of one grid cell in meters

    public static double TEMPORAL_RESOLUTION = 1;//steps per hour

    public static double WALKING_SPEED = 5.1;//km per hour

    //-------File paths-------//
    public static String POP_PATH = "/ebola/ebolaData/merged_pop.asc"; //Path to liberia population data (LandScan 2013)
    public static String ADMIN_PATH = "/ebola/ebolaData/merged_admin.asc";//Path to file that has administration and county boundaries for all three countries (LandScan 2013)
    public static String AGE_DIST_PATH = "/ebola/ebolaData/All_Age_Distribution.csv";//Path to file that has age distribution for each of the counties and provinces (LandScan 2013)
    public static String ROADS_SHAPE_PATH = "/ebola/ebolaData/roads_shapefile/all_roads_trim.shp";//Path to vector data for all roads
    public static String ROADS_COST_PATH = "/ebola/ebolaData/road_cost.dat";//Path to cost distance data for all allRoadNodes in the network
    public static String SCHOOLS_PATH = "/ebola/ebolaData/schools_shapefile/all_schools.shp";//Path to shapefile that has location of all primary schools
    public static String FARMS_PATH = "/ebola/ebolaData/farms_shapefile/all_farms.shp";
    public static String HOSPITALS_PATH = "/ebola/ebolaData/hospitals_shapefile/all_hospitals.shp";//Path to shapefile that has location of all health facilities
    public static String ADMIN_ID_PATH = "/ebola/ebolaData/admin_id.asc";//Path to file that contains the id for each county in each of the three countries, unique within each country but not between countries
    public static String MOVEMENT_PATH = "/ebola/ebolaData/movement/population_flow.csv";//Path to file containing movement data within each country
    public static String ACTUAL_CASES_GUINEA = "/ebola/ebolaData/actual_cases/guinea_actual.csv";//path to csv file containing actual cases for guinea
    public static String ACTUAL_CASES_LIBERIA = "/ebola/ebolaData/actual_cases/liberia_actual.csv";//path to csv file containing actual cases for liberia
    public static String ACTUAL_CASES_SIERRA_LEONE = "/ebola/ebolaData/actual_cases/sierra_leone_actual.csv";//path to csv file containing actual cases for sierra leone

    public static double MIN_POP_URBAN = 575.45;//Minimum population density per 926 meters or 1000 people per square mile to be urban. Source: http://cber.cba.ua.edu/asdc/urban_rural.html
    public static double MIN_POP_SURROUNDING = 287.73;//Minimum surrounding population density per 926 meters.  An urban district must be surrounded by
                                                      //by an total of this minimum density.  Source: http://cber.cba.ua.edu/asdc/urban_rural.html

    //population flow parameters
    public static double fromUrban = 0.0;//the percent of residents picked to move that need to be from an urban location

    //Contains id ranges (inclusive) for each county in Sierra Leone, Guinea, and Liberia - used to identify country and country specific statistics
    public static int MIN_LIB_COUNTY_ID = 1508;
    public static int MAX_LIB_COUNTY_ID = 1522;
    public static int MIN_SL_COUNTY_ID = 2379;
    public static int MAX_SL_COUNTY_ID = 2382;
    public static int MIN_GUINEA_COUNTY_ID = 1086;
    public static int MAX_GUINEA_COUNTY_ID = 1090;

    //-------Liberia---------//
    public static int LIBERIA = 2;//ID for Liberia
    public static double LIB_AVG_HOUSEHOLD_SIZE = 4.97; //Liberia's average household size (2008, http://www.euromonitor.com/medialibrary/PDF/Book_WEF_2014.pdf)
    public static double LIB_HOUSEHOLD_STDEV =  1.61;//TODO random, taken from TB model

    //-------Sierra Leone---------//
    public static int SL = 1;//ID for Sierra Leone
    public static double SL_AVG_HOUSEHOLD_SIZE = 5.56; //Sierra Leone's average household size (2008, http://www.euromonitor.com/medialibrary/PDF/Book_WEF_2014.pdf)


    //-------Guinea---------//
    public static int GUINEA = 0;//ID for Guinea
    public static double GUINEA_AVG_HOUSEHOLD_SIZE = 7; //Sierra Leone's average household size (2014, http://www.euromonitor.com/medialibrary/PDF/Book_WEF_2014.pdf)


    //Parameters for inactive vs labour force. Taken from: http://www.ilo.org/wcmsp5/groups/public/---dgreports/---stat/documents/presentation/wcms_156366.pdf
    //ages index 0 = ages 5-9,  1 = ages 10-14, 2 = ages 15-20, ... 75+
    //Urban Male
    public static double[] URBAN_MALE_LF_BY_AGE = {.054, .097, .144, .356, .574, .821, .826, .899, .878, .835, .760, .610, .733, .578, .363, .421, .576, .578};
    //Urban Female
    public static double[] URBAN_FEMALE_LF_BY_AGE = {.040, .078, .142, .390, .598, .701, .755, .798, .763, .736, .605, .517, 471, .429, .156, .384, .526, .533};
    //Rural Male
    public static double[] RURAL_MALE_LF_BY_AGE = {.220, .391, .434, .612, .750, .880, .889, .892, .944, .896, .829, .807, .843, .643, .484, .591, .749, .758};
    //Rural Female
    public static double[] RURAL_FEMALE_LF_BY_AGE = {.201, .307, .418, .570, .754, .770, .796, .800, .820, .747, .774, .664, .491, .512, .294, .553, .678, .695};

    //Parameters for reason of inactivity, either school or household work
    //  0      1       2       3       4
    //  5-14   15-24   25-34   35-54   65+
    //Urban Male
    public static double[] URBAN_MALE_INACTIVE_SCHOOL = {0.788, 0.785, 0.548, 0.17, 0.196, 0.077};
    //Urban Female
    public static double[] URBAN_FEMALE_INACTIVE_SCHOOL = {0.795, 0.67, 0.313, 0.232, 0.182, 0.171};
    //Rural Male
    public static double[] RURAL_MALE_INACTIVE_SCHOOL = {0.655, 0.699, 0.372, 0.302, 0.233, 0.124};
    //Rural Female
    public static double[] RURAL_FEMALE_INACTIVE_SCHOOL = {0.652, 0.544, 0.249, 0.223, 0.161, 0.108};

    //Parameters for unemployment of labour force
    //  0      1       2       3       4
    //  5-14   15-24   25-34   35-54   65+
    //Urban Male
    public static double[] URBAN_MALE_UNEMPLOYMENT = {0.068, 0.056, 0.038, 0.023, 0.029, 0.046};
    //Urban Female
    public static double[] URBAN_FEMALE_UNEMPLOYMENT = {0.146, 0.061, 0.04, 0.008, 0.002, 0.063};
    //Rural Male
    public static double[] RURAL_MALE_UNEMPLOYMENT = {0.021, 0.034, 0.021, 0.01, 0.023, 0.024};
    //Rural Female
    public static double[] RURAL_FEMALE_UNEMPLOYMENT = {0.032, 0.021, 0.021, 0.013, 0.006, 0.022};

    //Parameters for distribution of economic sector based on Urban/rural and male/female
    //Urban Male
    public static double[] URBAN_MALE_SECTORS = {0.242, 0.017, 0.128, 0.006, 0.003, 0.075, 0.161, 0.063, 0.023, 0.008, 0.015, 0, 0.008, 0.035, 0.011, 0.116, 0.049, 0.01, 0.025, 0.004, 0.002};
    //Urban Female
    public static double[] URBAN_FEMALE_SECTORS = {0.225, 0.006, 0.034, 0, 0, 0.022, 0.532, 0.004, 0.057, 0.002, 0.004, 0, 0, 0.005, 0.006, 0.056, 0.016, 0.007, 0.012, 0.012, 0};
    //Rural Male
    public static double[] RURAL_MALE_SECTORS = {0.647, 0.012, 0.149, 0, 0, 0.01, 0.079, 0.017, 0.013, 0, 0, 0, 0.003, 0.013, 0.004, 0.037, 0.006, 0, 0.009, 0.001, 0};
    //Rural Female
    public static double[] RURAL_FEMALE_SECTORS = {0.561, 0.01, 0.056, 0, 0, 0, 0.32, 0.003, 0.024, 0, 0.002, 0, 0, 0.005, 0, 0.007, 0.009, 0, 0.002, 0.003, 0};

    //Parameters for weekly hours by sector.  Columns are work hours and rows are economic sectors
    //              <25   25-35   35-39   40-48   59-59   60+
    //Agriculture
    //...
    //Trade
    public static double[][] MALE_WEEKLY_HOURS_BY_SECTOR = {{0.186, 0.115, 0.084, 0.298, 0.13,	0.188},
                                                        {0.088,	0.059, 0.047, 0.41, 0.186, 0.211},
                                                        {0.094,	0.065, 0.047, 0.235, 0.26, 0.298},
                                                        {0, 0, 0, 1, 0, 0},
                                                        {0, 0, 0, 1, 0, 0},
                                                        {0.059,	0.061, 0.052, 0.251, 0.243, 0.333},
                                                        {0.13, 0.056, 0.044, 0.145, 0.137, 0.488},
                                                        {0.023,	0.001, 0.032, 0.203, 0.102, 0.638},
                                                        {0.11, 0.08, 0.063, 0.145, 0.181, 0.422},
                                                        {0.167,	0.011, 0.005, 0.307, 0.249, 0.262},
                                                        {0,	0, 0.127, 0.375, 0.177, 0.32},
                                                        {0, 0, 0, 1, 0, 0},
                                                        {0.014,	0.075, 0.035, 0.28, 0.197, 0.399},
                                                        {0.027,	0.029, 0.013, 0.283, 0.195, 0.453},
                                                        {0.028,	0.003, 0.082, 0.376, 0.21, 0.3},
                                                        {0.069,	0.306, 0.07, 0.301, 0.12, 0.133},
                                                        {0.071,	0.045, 0.09, 0.389, 0.205, 0.2},
                                                        {0.182,	0.065, 0.024, 0.094, 0.351, 0.284},
                                                        {0.117,	0.111, 0.15, 0.222, 0.139, 0.261},
                                                        {0.149, 0.203, 0.011, 0.459, 0, 0.177},
                                                        {0, 0, 0, 1, 0, 0}};
    public static double[][] FEMALE_WEEKLY_HOURS_BY_SECTOR = {{0.208, 0.126, 0.091, 0.289, 0.105, 0.181},
                                                        {0.139, 0.126, 0.072, 0.443, 0.01, 0.21},
                                                        {0.219, 0.084, 0.058, 0.179, 0.126, 0.333},
                                                        {0, 0, 0, 1, 0, 0},
                                                        {0, 0, 0, 1, 0, 0},
                                                        {0.121, 0, 0.07, 0.124, 0.25, 0.435},
                                                        {0.126, 0.088, 0.065, 0.164, 0.132, 0.426},
                                                        {0.072, 0.002, 0, 0.116, 0.199, 0.611},
                                                        {0.201, 0.169, 0.087, 0.137, 0.072, 0.335},
                                                        {0, 0.03, 0, 0.523, 0.117, 0.331},
                                                        {0, 0.006, 0, 0.136, 0.729, 0.13},
                                                        {0, 0, 0, 1, 0, 0},
                                                        {0.074, 0, 0.288, 0.215, 0.03, 0.392},
                                                        {0.054, 0.018, 0.009, 0.574, 0.183, 0.161},
                                                        {0, 0, 0, 0.688, 0.168, 0.144},
                                                        {0.052, 0.28, 0.092, 0.195, 0.114, 0.267},
                                                        {0.066, 0.025, 0.012, 0.369, 0.174, 0.355},
                                                        {0.219, 0.2, 0, 0.167, 0.194, 0.219},
                                                        {0.238, 0.105, 0.116, 0.182, 0.1, 0.258},
                                                        {0.167, 0.006, 0, 0.381, 0.159, 0.287},
                                                        {0, 0, 0, 1, 0, 0}};

    //Parameters for work size by economic sector
    //              1   2-4   5-9   10-19   20-49   50+
    //Agriculture
    //...
    //Trade
    public static double[][] WORK_SIZE_BY_SECTOR = {{0.119,0.606,0.201,0.049,0.016,0.01},
                                                    {0.083,0.489,0.225,0.067,0.069,0.067},
                                                    {0.23,0.324,0.182,0.106,0.064,0.096},
                                                    {0.085,0.425,0.142,0.142,0,0.206},
                                                    {0.085,0.425,0.142,0.142,0,0.206},
                                                    {0.113,0.334,0.238,0.077,0.135,0.102},
                                                    {0.603,0.302,0.04,0.019,0.015,0.02},
                                                    {0.6,0.128,0.047,0.047,0.075,0.103},
                                                    {0.437,0.35,0.142,0.008,0.035,0.028},
                                                    {0.09,0.207,0.095,0.23,0.174,0.204},
                                                    {0.134,0.063,0.095,0.13,0.234,0.343},
                                                    {0.75,0.25,0,0,0,0},
                                                    {0.12,0.369,0.115,0.064,0.059,0.274},
                                                    {0.048,0.147,0.138,0.113,0.156,0.397},
                                                    {0.007,0.154,0.388,0.065,0.142,0.244},
                                                    {0.016,0.084,0.297,0.255,0.268,0.079},
                                                    {0.156,0.168,0.199,0.094,0.129,0.254},
                                                    {0.197,0.492,0.172,0.046,0.093,0},
                                                    {0.216,0.236,0.214,0.142,0.147,0.046},
                                                    {0.477,0.08,0.164,0.006,0.066,0.206},
                                                    {0.156,0.168,0.199,0.094,0.129,0.254}};
    public static int STUDENT_DAILY_HOURS = 7;

    //Commuting parameters for farms.  Uses a log normal distribution
    public static double AVERAGE_FARM_DISTANCE = 2;//kilometers
    public static double STDEV_FARM_DISTANCE = 1.5;//kilometers

    public static double AVERAGE_FARM_MAX = 7;//km
    public static double STDEV_FARM_MAX = 1;//km

    public static double OFF_ROAD_AVERAGE = 0.5;//km
    public static double OFF_ROAD_STDEV = 0.25;//km

    public static double convertToKilometers(double val)
    {
        return (val * (Parameters.POP_BLOCK_METERS/Parameters.WORLD_TO_POP_SCALE))/1000.0;
    }

    public static double convertFromKilometers(double val)
    {
        return (val*1000.0)/(Parameters.POP_BLOCK_METERS/Parameters.WORLD_TO_POP_SCALE);
    }

    //ebola Disease Model Parameters
    public static double SUSCEPTIBLE_TO_EXPOSED = 0.1 * Parameters.TEMPORAL_RESOLUTION;//per temporal resolution TODO Make indepent of temporal resolution
    public static double INCUBATION_PERIOD_AVERAGE = 13.0;//as reported by Who ebola Resonse Team source: http://www.nejm.org/doi/full/10.1056/NEJMoa1411100?rss=mostCited&
    public static double INCUBATION_PERIOD_STDEV = 1.0;//Standard deviation from reports above
    public static double CASE_FATALITY_RATIO = 0.70;//number of cases resulting in death
    public static double RECOVERY_PERIOD_AVERAGE = 14.0;
    public static double RECOVERY_PERIOD_STDEV = 1.0;

    public static double SUSCEPTIBLE_TO_EXPOSED_TRAVELERS = 0.1 * Parameters.TEMPORAL_RESOLUTION;//special for travellers

    public static  double FATALITY_PERIOD_AVERAGE = 8.0;
    public static double FATALITY_PERIOD_STDEV = 1.0;
    public static boolean INFECT_ONLY_YOUR_STRUCTURE = false;
}
