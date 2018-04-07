package conflictdiamonds;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.csv4j.CSVWriter;
import sim.engine.SimState;
import sim.engine.Steppable;

/**
 * Conflict Diamonds Observer writes data to a series of csv files
 * Based on riftland worldobserver class
 * Thanks goes to mcoletti and jbasset
 * 
 * @author mcoletti, jbasset, and bpint
 */
public class ConflictDiamondsObserver implements Steppable {
    private BufferedWriter dataFileBuffer; // output file buffer for dataCSVFile_
    private CSVWriter dataCSVFile; // CSV file that contains run data

    int cStep;
    int minuteInDay;

    ConflictDiamonds conflictDiamonds;

    public final static int ORDERING = 3;
    private int step = 0;
    private boolean writeGrid =false;
    
    ConflictDiamondsObserver(ConflictDiamonds conflictDiamonds) {
        //<GCB>: you may want to adjust the number of columns based on these flags.
        // both in createLogFile, and step
        conflictDiamonds = null;
        startLogFile();
    }

    ConflictDiamondsObserver() {
        startLogFile();
    }
    
    private void startLogFile() {
        // Create a CSV file to capture data for this run.
        try {
            createLogFile();
            // First line of file contains field names
   
            String [] header = new String [] {"Job", "Step", "RegionID", "Total Population", "Total Residents", 
                "Total Rebels", "Initial Rebel", "Total Minors", "Eligible to Mine", "Food Poor", "Total Poor", "Not Poor", 
                "Active Labor Market", "Formal Employees", "Informal Employees",
                "Goal Stay Home", "Goal Find Informal Employment", "Goal Remain Employed", "Goal Rebel", "risk",
                "Motivation Very Poor", "Motivation Poor", "Opposition Density", "Rebel Density Miner", "Rebel Density Not Miner",
                "Rebel Density Minor", "Vision", "Government Control", "Proximate"};
            
            dataCSVFile.writeLine(header);
        }

        catch (IOException ex) {
            Logger.getLogger(ConflictDiamonds.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void step(SimState state) {
        conflictDiamonds  = (ConflictDiamonds)state;
        cStep = (int) conflictDiamonds.schedule.getSteps();
        String data [] = null;

        String job = Long.toString(state.job());
        String step = Integer.toString(cStep);
        
        for(int i = 0; i < conflictDiamonds.allRegions.size(); i++) {
            Region r = conflictDiamonds.allRegions.get(i);
            String regionID = Integer.toString(r.getRegionID());
            
            //convert agent attributes to String
            String risk = Double.toString(conflictDiamonds.params.global.getMaxRisk());
            String motivationVeryPoor = Double.toString(conflictDiamonds.params.global.getMinMotivationVeryPoorLevel());
            String motivationPoor = Double.toString(conflictDiamonds.params.global.getMinMotivationPoorLevel());
            String opposition = Double.toString(conflictDiamonds.params.global.getOpposition());
            String rebelDensityMiner = Double.toString(conflictDiamonds.params.global.getMinRebelDensityMiner());
            String rebelDensityNotMiner = Double.toString(conflictDiamonds.params.global.getMinRebelDensityNotMiner());
            String rebelDensityMinor = Double.toString(conflictDiamonds.params.global.getMinRebelDensityMinor());
            String vision = Integer.toString(conflictDiamonds.params.global.getAgentVision());
            String governmentControl = Double.toString(conflictDiamonds.params.global.getGovernmentControlOfMines());
            String proximate = Boolean.toString(conflictDiamonds.params.global.isProximateExperiment());
            
            int regionTotalPopulation = r.getResidingPopulation().size();
            int regionTotalResidents = r.getResidingPopulation().size() - r.getRebels().size();
            int regionTotalRebels = r.getRebels().size();
            int regionInitialRebels = r.getInitialRebel().size();
            int regionTotalMinors = r.getMinors().size();
            int regionEligibleToMine = r.getEligibleToMine().size();
            int regionFoodPoor = r.getFoodPoor().size();
            int regionTotalPoor = r.getTotalPoor().size();
            int regionNotPoor = r.getNotPoor().size();
            int regionActiveLaborMarket = r.getActiveLaborMarket().size();
            int regionFormalEmployees = r.getFormalEmployees().size();
            int regionInformalEmployees = r.getInformalEmployees().size(); 
            
            int regionGoalStayHome = r.getGoalStayHome().size();
            int regionGoalFindInformalEmployment = r.getGoalFindInformalEmployment().size();
            int regionGoalRemainEmployed = r.getGoalRemainEmployed().size();
            int regionGoalRebel = r.getGoalRebel().size();          
            
            String totalPopulation = Integer.toString(regionTotalPopulation);
            String totalResidents = Integer.toString(regionTotalResidents);
            String totalRebels = Integer.toString(regionTotalRebels);
            String totalMinors = Integer.toString(regionTotalMinors);
            String totalEligibleToMine = Integer.toString(regionEligibleToMine);
            String totalFoodPoor = Integer.toString(regionFoodPoor);
            String totalTotalPoor = Integer.toString(regionTotalPoor);
            String totalNotPoor = Integer.toString(regionNotPoor);
            String totalActiveLaborMarket = Integer.toString(regionActiveLaborMarket);
            String totalFormalEmployees = Integer.toString(regionFormalEmployees);
            String totalInformalEmployees = Integer.toString(regionInformalEmployees);
            String goalStayHome = Integer.toString(regionGoalStayHome);
            String goalFindInformalEmployment = Integer.toString(regionGoalFindInformalEmployment);
            String goalRemainEmployed = Integer.toString(regionGoalRemainEmployed);
            String goalRebel = Integer.toString(regionGoalRebel);
            String initialRebel = Integer.toString(regionInitialRebels);
             
            data = new String [] {job, step, regionID, totalPopulation, totalResidents, totalRebels, initialRebel,
                totalMinors, totalEligibleToMine, totalFoodPoor, totalTotalPoor, totalNotPoor, totalActiveLaborMarket,
                totalFormalEmployees, totalInformalEmployees, goalStayHome, goalFindInformalEmployment,
                goalRemainEmployed, goalRebel, risk, motivationVeryPoor, motivationPoor, opposition, rebelDensityMiner,
                rebelDensityNotMiner, rebelDensityMinor, vision, governmentControl, proximate };
            
             try {
                    this.dataCSVFile.writeLine(data);
                }
                catch (IOException ex) {
                    Logger.getLogger(ConflictDiamondsObserver.class.getName()).log(Level.SEVERE, null, ex);
                }           
        }
        this.step++;
    }
    
    void finish() {
        try {
            this.dataFileBuffer.close();
        }
        catch (IOException ex) {
            Logger.getLogger(ConflictDiamondsObserver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    //create files
    private void createLogFile() throws IOException {
        long now = System.currentTimeMillis();

        String filename = String.format("%ty%tm%td%tH%tM%tS", now, now, now, now, now, now, now, now, now, now, now, now, now, now, now, now, now, now, now, now, now, now, now, now, now, now, now, now, now, now, now)
            + "_output.csv";
        
        this.dataFileBuffer = new BufferedWriter(new FileWriter(filename));
        this.dataCSVFile = new CSVWriter(dataFileBuffer);
        
    }
    
    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException {
        out.writeInt(step);

    }

    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        step = in.readInt();

        startLogFile();
    }
        
    
}
