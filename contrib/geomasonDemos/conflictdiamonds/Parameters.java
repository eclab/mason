package conflictdiamonds;

import ec.util.Parameter;
import ec.util.ParameterDatabase;
import java.io.File;
import java.io.IOException;

/**
 * Parameters
 * 
 * Basic class that sets the value of all parameters at initialization
 * 
 * 
 * @author bpint
 */
public class Parameters {
    
    GlobalParamters global = new GlobalParamters();
  
    private final static String A_FILE = "-file";

    public Parameters(String[] args) {
        if (args != null) {
            loadParameters(openParameterDatabase(args));
        }
    }
    
    /**
     * Initialize parameter database from file
     *
     * If there exists an command line argument '-file', create a parameter
     * database from the file specified. Otherwise create an empty parameter
     * database.
     *
     * @param args contains command line arguments
     * @return newly created parameter data base
     *
     * @see loadParameters()
     */
    private static ParameterDatabase openParameterDatabase(String[] args) {
        ParameterDatabase parameters = null;
        for (int x = 0; x < args.length - 1; x++) {
            if (args[x].equals(A_FILE)) {
                try {
                    File parameterDatabaseFile = new File(args[x + 1]);
                    parameters = new ParameterDatabase(parameterDatabaseFile.getAbsoluteFile());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                break;
            }
        }
        if (parameters == null) {
            System.out.println("\nNot in a parameter Mode");//("\nNo parameter file was specified");
            parameters = new ParameterDatabase();
        }
        return parameters;
    }

    private void loadParameters(ParameterDatabase parameterDB) {       
        // global - enterprise    
        global.setAgentVision(returnIntParameter(parameterDB, "agentVision",
                global.getAgentVision())); 
        global.setGovernmentControlOfMines(returnDoubleParameter(parameterDB, "governmentControl",
                global.getGovernmentControlOfMines()));
        global.setInitialRebelPositionActualEvents(returnBooleanParameter(parameterDB, "initialRebelPositionActualEvents",
                global.isInitialRebelPositionActualEvents()));
        global.setMaxRisk(returnDoubleParameter(parameterDB, "maxRisk",
                global.getMaxRisk()));
        global.setMinMotivationPoorLevel(returnDoubleParameter(parameterDB, "minMotivationPoor",
                global.getMinMotivationPoorLevel()));
        global.setMinRebelDensityMiner(returnDoubleParameter(parameterDB, "minRebelDensityMiner",
                global.getMinRebelDensityMiner()));
        global.setMinMotivationVeryPoorLevel(returnDoubleParameter(parameterDB, "minMotivationVeryPoor",
                global.getMinMotivationVeryPoorLevel()));
        global.setMinRebelDensityMinor(returnDoubleParameter(parameterDB, "minRebelDensityMinor",
                global.getMinRebelDensityMinor()));
        global.setMinRebelDensityNotMiner(returnDoubleParameter(parameterDB, "minRebelDensityNotMiner",
                global.getMinRebelDensityNotMiner()));
        global.setOpposition(returnDoubleParameter(parameterDB, "oppositionDensity",
                global.getOpposition()));
        global.setOppositionActual(returnDoubleParameter(parameterDB, "oppositionDensityActualEvents",
                global.getOppositionActual()));
        global.setProximateExperiment(returnBooleanParameter(parameterDB, "proximateExperiment",
                global.isProximateExperiment()));
        global.setProximateExperiment(returnBooleanParameter(parameterDB, "minerProportions",
                global.isProximateExperiment()));

        
    }
    
    public int returnIntParameter(ParameterDatabase paramDB, String parameterName, int defaultValue) {
        return paramDB.getIntWithDefault(new Parameter(parameterName), null, defaultValue);
    }

    public boolean returnBooleanParameter(ParameterDatabase paramDB, String parameterName, boolean defaultValue) {
        return paramDB.getBoolean(new Parameter(parameterName), null, defaultValue);
    }

    double returnDoubleParameter(ParameterDatabase paramDB, String parameterName, double defaultValue) {
        return paramDB.getDoubleWithDefault(new Parameter(parameterName), null, defaultValue);
    }

    public class GlobalParamters {
      
        //the agent's vision in terms of number of surrounding parcels
        public int agentVision = 25; 
        //maximum risk associated with a parcel (remoteness and government control) required for opportunity to exist to mine or rebel
        public double maxRisk = .5; 
        //if agent is poor (income=1) and has employment, probability that agent will be motivated to mine
        public double minMotivationPoorLevel = .05; 
        //if agent is very poor (income=0) and has employment, probability that agent will be motivated to mine
        public double minMotivationVeryPoorLevel = .01; 
        //percent of population that strongly opposes government and has formed an opposition group
        public double opposition = .01;   
        //percent of population that makes up opposition if assume initial rebels were concentrated in area where opposition said to have began
        public double oppositionActual = .005;
        //level of government control over mines (0-no control, 1-full control)
        public double governmentControlOfMines = .0;
        //minimum density of rebels in neighborhood required for an agent to be motivated (forced) to become a rebel if agent is a miner
        public double minRebelDensityMiner = .01;
        //minimum density of rebels in neighborhood required for an agent to be motivated (forced) to become a rebel if agent is not a miner
        public double minRebelDensityNotMiner = .1; 
        //minimum density of rebels in neighborhood required for an agent to be motivated (forced) to become a rebel if agent is not a miner and is a minor (child soldier)
        public double minRebelDensityMinor = .01; 
        
        //variables for experiment purposes
        public boolean initialRebelPositionActualEvents = false; //randomly place initial rebels or in selected regions of the country
        public boolean proximateExperiment = false; //place diamond mines in Freetown
        public boolean useMiningProportions = false; //use density of miners to determine if agent will mine (instead of likelihood)
        
        public int getAgentVision() { return agentVision; }
        public void setAgentVision(int val) { if (val > 0) agentVision = val; }
        public double getMaxRisk() { return maxRisk; }
        public void setMaxRisk(double val) { if (val > 0) maxRisk = val; }
        public double getMinMotivationPoorLevel() { return minMotivationPoorLevel; }
        public void setMinMotivationPoorLevel(double val) { if (val > 0) minMotivationPoorLevel = val; }
        public double getMinMotivationVeryPoorLevel() { return minMotivationVeryPoorLevel; }
        public void setMinMotivationVeryPoorLevel(double val) { if (val > 0) minMotivationVeryPoorLevel = val; }
        public double getOpposition() { return opposition; }
        public void setOpposition(double val) { if (val > 0) opposition = val; }
        public double getOppositionActual() { return oppositionActual; }
        public void setOppositionActual(double val) { if (val > 0) oppositionActual = val; }
        public double getGovernmentControlOfMines() { return governmentControlOfMines; }
        public void setGovernmentControlOfMines(double val) { if (val > 0) governmentControlOfMines = val; }
        public double getMinRebelDensityMiner() { return minRebelDensityMiner; }
        public void setMinRebelDensityMiner(double val) { if (val > 0) minRebelDensityMiner = val; }
        public double getMinRebelDensityNotMiner() { return minRebelDensityNotMiner; }
        public void setMinRebelDensityNotMiner(double val) { if (val > 0) minRebelDensityNotMiner = val; }	
        public double getMinRebelDensityMinor() { return minRebelDensityMinor; }
        public void setMinRebelDensityMinor(double val) { if (val > 0) minRebelDensityMinor = val; }
        public boolean isInitialRebelPositionActualEvents() { return initialRebelPositionActualEvents; }
        public void setInitialRebelPositionActualEvents(boolean initialRebelPositionActualEvents) {
            this.initialRebelPositionActualEvents = initialRebelPositionActualEvents; }
        public void setProximateExperiment(boolean val) { this.proximateExperiment = val; }
        public boolean isProximateExperiment() { return proximateExperiment; }
        public void useMinerProportions(boolean val) { this.useMiningProportions = val; }
        public boolean useMinerProportions() { return useMiningProportions; }
                     
    }
    
    
}
